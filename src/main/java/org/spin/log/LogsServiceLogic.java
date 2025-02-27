/************************************************************************************
 * Copyright (C) 2018-present E.R.P. Consultores y Asociados, C.A.                  *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.spin.log;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.adempiere.core.domains.models.I_AD_ChangeLog;
import org.adempiere.core.domains.models.I_AD_PInstance;
import org.compiere.model.MChangeLog;
import org.compiere.model.MPInstance;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.spin.backend.grpc.common.ProcessLog;
import org.spin.backend.grpc.logs.ChangeLog;
import org.spin.backend.grpc.logs.EntityLog;
import org.spin.backend.grpc.logs.ListUserActivitesRequest;
import org.spin.backend.grpc.logs.ListUserActivitesResponse;
import org.spin.backend.grpc.logs.UserActivity;
import org.spin.backend.grpc.logs.UserActivityType;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service Logic for backend of Logs
 */
public class LogsServiceLogic {
	public static ListUserActivitesResponse.Builder listUserActivites(ListUserActivitesRequest request) {
		final int userId = Env.getAD_User_ID(Env.getCtx());
		Timestamp date = ValueUtil.getTimestampFromLong(
			request.getDate()
		);
		if (date == null) {
			// set current date
			date = new Timestamp(System.currentTimeMillis());
		}

		final String whereClauseProcessLog = "AD_User_ID = ? AND TRUNC(Created, 'DD') = ?";
		Query queryProcessLogs = new Query(
			Env.getCtx(),
			I_AD_PInstance.Table_Name,
			whereClauseProcessLog,
			null
		)
			.setParameters(userId, date)
		;

		//	Get page and count
		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		int count = queryProcessLogs.count();

		List<MPInstance> processInstanceList = queryProcessLogs
			.setLimit(limit, offset)
			.setOrderBy(I_AD_PInstance.COLUMNNAME_Created + " DESC")
			.<MPInstance>list();
		List<ProcessLog.Builder> processLogsList = new ArrayList<>();
		//	Convert Process Instance
		for(MPInstance processInstance : processInstanceList) {
			ProcessLog.Builder valueObject = LogsConvertUtil.convertProcessLog(processInstance);
			// builderProcessLogs.addProcessLogs(valueObject.build());
			processLogsList.add(valueObject);
		}

		List<UserActivity> userActivitiesList = new ArrayList<>();
		processLogsList.stream().forEach(processLog -> {
			UserActivity.Builder userBuilder = UserActivity.newBuilder();
			userBuilder.setUserActivityType(UserActivityType.PROCESS_LOG);
			userBuilder.setProcessLog(processLog);
			userActivitiesList.add(userBuilder.build());
		});


		String whereClauseRecordsLog = "CreatedBy = ? AND TRUNC(Created, 'DD') = ?";
		Query queryRecordLogs = new Query(
			Env.getCtx(),
			I_AD_ChangeLog.Table_Name,
			whereClauseRecordsLog,
			null
		)
			.setParameters(userId, date);
		count += queryRecordLogs.count();
		List<MChangeLog> recordLogList = queryRecordLogs
			.setLimit(limit, offset)
			.setOrderBy(I_AD_PInstance.COLUMNNAME_Created + " DESC")
			.<MChangeLog>list();

		ListUserActivitesResponse.Builder builderList = ListUserActivitesResponse.newBuilder();


		//	convert changes
		Map<Integer, EntityLog.Builder> indexMap = new HashMap<Integer, EntityLog.Builder>();
		recordLogList.stream().filter(recordLog -> !indexMap.containsKey(recordLog.getAD_ChangeLog_ID())).forEach(recordLog -> {
			indexMap.put(recordLog.getAD_ChangeLog_ID(), LogsConvertUtil.convertRecordLogHeader(recordLog));
		});
		recordLogList.forEach(recordLog -> {
			ChangeLog.Builder changeLog = LogsConvertUtil.convertChangeLog(recordLog);
			EntityLog.Builder recordLogBuilder = indexMap.get(recordLog.getAD_ChangeLog_ID());
			recordLogBuilder.addChangeLogs(changeLog);
			// indexMap.put(recordLog.getAD_ChangeLog_ID(), recordLogBuilder);

			UserActivity.Builder userBuilder = UserActivity.newBuilder();
			userBuilder.setUserActivityType(UserActivityType.ENTITY_LOG);
			userBuilder.setEntityLog(recordLogBuilder);
			userActivitiesList.add(userBuilder.build());
		});

		List<UserActivity> recordsList = userActivitiesList.stream().sorted((u1, u2) -> {
			Timestamp from = null;
			if (u1.getUserActivityType() == UserActivityType.ENTITY_LOG) {
				from = ValueUtil.getTimestampFromLong(u1.getEntityLog().getLogDate());
			} else {
				from = ValueUtil.getTimestampFromLong(u1.getProcessLog().getLastRun());
			}

			Timestamp to = null;
			if (u2.getUserActivityType() == UserActivityType.ENTITY_LOG) {
				to = ValueUtil.getTimestampFromLong(u2.getEntityLog().getLogDate());
			} else {
				to = ValueUtil.getTimestampFromLong(u2.getProcessLog().getLastRun());
			}

			if (from == null || to == null) {
				// prevent Null Pointer Exception
				return 0;
			}
			return (int) (to.getTime() - from.getTime());

		})
		.collect(Collectors.toList());


		builderList.setRecordCount(count);
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));
		builderList.addAllRecords(recordsList);

		return builderList;
	}

}
