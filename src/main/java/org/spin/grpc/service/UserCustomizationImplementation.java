/************************************************************************************
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                     *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                    *
 * This program is free software: you can redistribute it and/or modify             *
 * it under the terms of the GNU General Public License as published by             *
 * the Free Software Foundation, either version 2 of the License, or                *
 * (at your option) any later version.                                              *
 * This program is distributed in the hope that it will be useful,                  *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the                     *
 * GNU General Public License for more details.                                     *
 * You should have received a copy of the GNU General Public License                *
 * along with this program. If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/

package org.spin.grpc.service;

import java.util.ArrayList;
import java.util.List;

import org.adempiere.core.domains.models.I_AD_Browse;
import org.adempiere.core.domains.models.I_AD_Browse_Field;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Process;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.adempiere.core.domains.models.I_AD_Role;
import org.adempiere.core.domains.models.I_AD_Tab;
import org.adempiere.core.domains.models.I_AD_User;
import org.adempiere.core.domains.models.I_AD_View_Column;
import org.adempiere.core.domains.models.I_ASP_Level;
import org.adempiere.core.domains.models.X_ASP_Level;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.MBrowse;
import org.adempiere.model.MBrowseField;
import org.adempiere.model.MViewColumn;
import org.compiere.model.MBrowseCustom;
import org.compiere.model.MBrowseFieldCustom;
import org.compiere.model.MColumn;
import org.compiere.model.MField;
import org.compiere.model.MFieldCustom;
import org.compiere.model.MProcess;
import org.compiere.model.MProcessCustom;
import org.compiere.model.MProcessPara;
import org.compiere.model.MProcessParaCustom;
import org.compiere.model.MRole;
import org.compiere.model.MTab;
import org.compiere.model.MTabCustom;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.MWindowCustom;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.Empty;
import org.spin.backend.grpc.user_customization.Level;
import org.spin.backend.grpc.user_customization.LevelCustomization;
import org.spin.backend.grpc.user_customization.ListCustomizationsLevelRequest;
import org.spin.backend.grpc.user_customization.ListCustomizationsLevelResponse;
import org.spin.backend.grpc.user_customization.ListRolesRequest;
import org.spin.backend.grpc.user_customization.ListRolesResponse;
import org.spin.backend.grpc.user_customization.ListUsersRequest;
import org.spin.backend.grpc.user_customization.ListUsersResponse;
import org.spin.backend.grpc.user_customization.Role;
import org.spin.backend.grpc.user_customization.SaveBrowseCustomizationRequest;
import org.spin.backend.grpc.user_customization.SaveProcessCustomizationRequest;
import org.spin.backend.grpc.user_customization.SaveWindowCustomizationRequest;
import org.spin.backend.grpc.user_customization.User;
import org.spin.backend.grpc.user_customization.UserCustomizationGrpc.UserCustomizationImplBase;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.RecordUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for User Customization
 */
public class UserCustomizationImplementation extends UserCustomizationImplBase {
	/** Logger */
	private CLogger log = CLogger.getCLogger(UserCustomizationImplementation.class);

	static String IS_DISPLAYED_COLUMN_NAME = "IsDefaultDisplayed";

	@Override
	public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListUsersResponse.Builder builder = listUsers(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListUsersResponse.Builder listUsers(ListUsersRequest request) {
		List<Object> parameters = new ArrayList<>();
		String whereClause = "";
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause = RecordUtil.addSearchValueAndGet("", I_AD_User.Table_Name, request.getSearchValue(), parameters);

			if (!Util.isEmpty(whereClause, true)) {
				whereClause = whereClause.replace(" WHERE ", "");
				whereClause += " AND ";
			}
		}

		whereClause += "AD_User_ID > 0";
		Query query = new Query(
				Env.getCtx(),
			I_AD_User.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
		;
		int recordCount = query.count();

		ListUsersResponse.Builder builderList = ListUsersResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		query
			.setLimit(limit, offset)
			.list(MUser.class)
			.forEach(user -> {
				User.Builder builder = convertUser(user);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private User.Builder convertUser(MUser user) {
		User.Builder builder = User.newBuilder();
		if (user == null || user.getAD_User_ID() <= 0) {
			return builder;
		}

		builder.setId(user.getAD_User_ID())
			.setUuid(
				ValueUtil.validateNull(user.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(user.getValue())
			)
			.setName(
				ValueUtil.validateNull(user.getName())
			)
			.setDescription(
				ValueUtil.validateNull(user.getDescription())
			)
		;

		return builder;
	}


	@Override
	public void listRoles(ListRolesRequest request, StreamObserver<ListRolesResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListRolesResponse.Builder builder = listRoles(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListRolesResponse.Builder listRoles(ListRolesRequest request) {
		List<Object> parameters = new ArrayList<>();
		String whereClause = "";
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause = RecordUtil.addSearchValueAndGet("", I_AD_Role.Table_Name, request.getSearchValue(), parameters);

			if (!Util.isEmpty(whereClause, true)) {
				whereClause = whereClause.replace(" WHERE ", "");
			}
		}

		Query query = new Query(
				Env.getCtx(),
			I_AD_Role.Table_Name,
			whereClause,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
		;
		int recordCount = query.count();

		ListRolesResponse.Builder builderList = ListRolesResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		query
			.setLimit(limit, offset)
			.list(MRole.class)
			.forEach(user -> {
				Role.Builder builder = convertRole(user);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private Role.Builder convertRole(MRole role) {
		Role.Builder builder = Role.newBuilder();
		if (role == null) {
			return builder;
		}
		
		builder.setId(role.getAD_Role_ID())
			.setUuid(
				ValueUtil.validateNull(role.getUUID())
			)
			.setName(
				ValueUtil.validateNull(role.getName())
			)
			.setDescription(
				ValueUtil.validateNull(role.getDescription())
			)
		;

		return builder;
	}


	@Override
	public void listCustomizationsLevel(ListCustomizationsLevelRequest request, StreamObserver<ListCustomizationsLevelResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			ListCustomizationsLevelResponse.Builder builder = listCustomizationsLevel(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private ListCustomizationsLevelResponse.Builder listCustomizationsLevel(ListCustomizationsLevelRequest request) {
		List<Object> parameters = new ArrayList<>();
		String whereClause = "";
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause = RecordUtil.addSearchValueAndGet("", I_ASP_Level.Table_Name, request.getSearchValue(), parameters);

			if (!Util.isEmpty(whereClause, true)) {
				whereClause = whereClause.replace(" WHERE ", "");
			}
		}

		Query query = new Query(
				Env.getCtx(),
			I_ASP_Level.Table_Name,
			null,
			null
		)
			.setOnlyActiveRecords(true)
			.setParameters(parameters)
		;
		int recordCount = query.count();

		ListCustomizationsLevelResponse.Builder builderList = ListCustomizationsLevelResponse.newBuilder();
		builderList.setRecordCount(recordCount);

		String nexPageToken = null;
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;

		// Set page token
		if (LimitUtil.isValidNextPageToken(recordCount, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}
		builderList.setNextPageToken(ValueUtil.validateNull(nexPageToken));

		query
			.setLimit(limit, offset)
			.list(X_ASP_Level.class)
			.forEach(aspLevel -> {
				LevelCustomization.Builder builder = convertLevelCustomization(aspLevel);
				builderList.addRecords(builder);
			});

		return builderList;
	}

	private LevelCustomization.Builder convertLevelCustomization(X_ASP_Level aspLevel) {
		LevelCustomization.Builder builder = LevelCustomization.newBuilder();
		if (aspLevel == null || aspLevel.getASP_Level_ID() <= 0) {
			return builder;
		}

		builder.setId(aspLevel.getASP_Level_ID())
			.setUuid(
				ValueUtil.validateNull(aspLevel.getUUID())
			)
			.setValue(
				ValueUtil.validateNull(aspLevel.getValue())
			)
			.setName(
				ValueUtil.validateNull(aspLevel.getName())
			)
			.setDescription(
				ValueUtil.validateNull(aspLevel.getDescription())
			)
		;

		return builder;
	}


	private PO getEntityToCustomizationType(int levelType, int levelId, String levelUuid) {
		String tableName = null;
		if (Level.CLIENT_VALUE == levelType) {
			tableName = I_ASP_Level.Table_Name;
		} else if (Level.ROLE_VALUE == levelType) {
			tableName = I_AD_Role.Table_Name;
		} else if (Level.USER_VALUE == levelType) {
			tableName = I_AD_User.Table_Name;
		} else {
			throw new AdempiereException("@LevelType@ @NotFound@");
		}

		PO entityType = RecordUtil.getEntity(Env.getCtx(), tableName, levelUuid, levelId, null);
		if (entityType == null) {
			throw new AdempiereException(
				"@" + tableName + "_ID@ @NotFound@"
			);
		}

		return entityType;
	}


	@Override
	public void saveWindowCustomization(SaveWindowCustomizationRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = saveWindowCustomization(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder saveWindowCustomization(SaveWindowCustomizationRequest request) {
		if (Util.isEmpty(request.getTabUuid(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Tab_ID@");
		}

		Trx.run(transactionName -> {
			int tabId = RecordUtil.getIdFromUuid(I_AD_Tab.Table_Name, request.getTabUuid(), transactionName);
			MTab tab = MTab.get(Env.getCtx(), tabId);
			if (tab == null || tab.getAD_Tab_ID() <= 0) {
				throw new AdempiereException("@AD_Tab_ID@ @NotFound@");
			}
			MTable table = MTable.get(Env.getCtx(), tab.getAD_Table_ID());

			// validate level, role, user
			PO entity = getEntityToCustomizationType(request.getLevel().getNumber(), request.getLevelId(), request.getLevelUuid());
			String columnKey = entity.get_TableName() + "_ID";

			// instance window
			String sql = "SELECT AD_WindowCustom_ID FROM AD_WindowCustom WHERE AD_Window_ID = ? AND " + columnKey + " = ? ";
			int customWindowId = DB.getSQLValueEx(
				transactionName,
				sql,
				tab.getAD_Window_ID(), entity.get_ID()
			);
			MWindowCustom customWindow = null;
			if (customWindowId < 1) {
				// Add Window, Tabs and Fields (if IsGenerateFields)
				customWindow = new MWindowCustom(Env.getCtx(), 0, transactionName);
				// customWindow.setASP_Level_ID(getLevelId());
				customWindow.set_ValueOfColumn(columnKey, entity.get_ID());
				customWindow.setAD_Window_ID(tab.getAD_Window_ID());
				customWindow.setHierarchyType(MWindowCustom.HIERARCHYTYPE_Overwrite);
				customWindow.saveEx();
				// customWindowId = customWindow.getAD_WindowCustom_ID();
			} else {
				customWindow = new MWindowCustom(Env.getCtx(), customWindowId, transactionName);
			}

			// instance tab
			List<MTabCustom> tabsCustomList = customWindow.getTabs();
			MTabCustom customTab = tabsCustomList.stream()
				.filter(tabItem -> {
					return tabItem.getAD_Tab_ID() == tab.getAD_Tab_ID();
				})
				.findFirst()
				.orElse(null)
			;

			if (customTab == null) {
				customTab = new MTabCustom(customWindow);
				customTab.setAD_Tab_ID(tab.getAD_Tab_ID());
				customTab.saveEx();
			}

			List<MFieldCustom> customFieldsList = customTab.getFields();
			request.getFieldAttributesList().forEach(fieldAttributes -> {
				if (Util.isEmpty(fieldAttributes.getColumnName(), true)) {
					log.warning(
						Msg.getMsg(Env.getCtx(), "@ColumnName@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
					);
					return;
				}

				int columnId = MColumn.getColumn_ID(table.getTableName(), fieldAttributes.getColumnName());
				if (columnId <= 0) {
					log.warning(
						Msg.getMsg(Env.getCtx(), "@AD_Column_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
					);
					return;
				}

				// instance field
				MField field = new Query(
						Env.getCtx(),
					I_AD_Field.Table_Name,
					I_AD_Field.COLUMNNAME_AD_Column_ID + " = ? AND " + I_AD_Field.COLUMNNAME_AD_Tab_ID + " = ?",
					transactionName
				)
					.setParameters(columnId, tab.getAD_Tab_ID())
					.setOnlyActiveRecords(true)
					.first()
				;
				if (field == null || field.getAD_Field_ID() <= 0) {
					log.warning(
						Msg.getMsg(Env.getCtx(), "@AD_Field_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
					);
					return;
				}

				// instance custom field
				MFieldCustom customField = customFieldsList.stream()
					.filter(fieldItem -> {
						return fieldItem.getAD_Field_ID() == field.getAD_Field_ID();
					})
					.findFirst()
					.orElse(null)
				;

				// Panel sequence
				customField.setSeqNo(fieldAttributes.getSequence());
				// checks if the column exists in the database
				if (customField.get_ColumnIndex(IS_DISPLAYED_COLUMN_NAME) >= 0) {
					customField.set_ValueOfColumn(
						IS_DISPLAYED_COLUMN_NAME,
						fieldAttributes.getIsDefaultDisplayed()
					);
				}
				customField.saveEx();
			});
		});

		Empty.Builder builder = Empty.newBuilder();
		return builder;
	}


	@Override
	public void saveBrowseCustomization(SaveBrowseCustomizationRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = saveBrowseCustomization(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder saveBrowseCustomization(SaveBrowseCustomizationRequest request) {
		if (Util.isEmpty(request.getBrowseUuid(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Browse_ID@");
		}
		int browseId = RecordUtil.getIdFromUuid(I_AD_Browse.Table_Name, request.getBrowseUuid(), null);
		MBrowse browse = MBrowse.get(Env.getCtx(), browseId);
		if (browse == null || browse.getAD_Browse_ID() <= 0) {
			throw new AdempiereException("@AD_Browse_ID@ @NotFound@");
		}

		// validate level, role, user
		PO entity = getEntityToCustomizationType(request.getLevel().getNumber(), request.getLevelId(), request.getLevelUuid());
		String columnKey = entity.get_TableName() + "_ID";


		// instance browse
		String sql = "SELECT AD_Browse_ID FROM AD_BrowseCustom WHERE AD_Browse_ID = ? AND " + columnKey + " = ? ";
		int customBrowseId = DB.getSQLValueEx(
			null,
			sql,
			browse.getAD_Browse_ID(), entity.get_ID()
		);

		MBrowseCustom customBrowse = null;
		if (customBrowseId < 0) {
			customBrowse = new MBrowseCustom(Env.getCtx(), 0, null);
			// customBrowse.setASP_Level_ID(getLevelId());
			customBrowse.set_ValueOfColumn(columnKey, entity.get_ID());
			customBrowse.setAD_Browse_ID(browse.getAD_Browse_ID());
			customBrowse.setHierarchyType(MBrowseCustom.HIERARCHYTYPE_Overwrite);
			customBrowse.saveEx();
		} else {
			customBrowse = new MBrowseCustom(Env.getCtx(), customBrowseId, null);
		}
		List<MBrowseFieldCustom> customBrowseFieldList = customBrowse.getFields();
		request.getFieldAttributesList().forEach(fieldAttributes -> {
			if (Util.isEmpty(fieldAttributes.getColumnName(), true)) {
				log.warning(
					Msg.getMsg(Env.getCtx(), "@AD_Column_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
				);
				return;
			}

			// instance view column
			MViewColumn viewColumn = new Query(
					Env.getCtx(),
				I_AD_View_Column.Table_Name,
				"AD_View_ID = ? AND ColumnName = ?",
				null
			)
				.setParameters(browse.getAD_View_ID(), fieldAttributes.getColumnName())
				.setOnlyActiveRecords(true)
				.first()
			;
			if (viewColumn == null || viewColumn.getAD_View_Column_ID() <= 0) {
				log.warning(
					Msg.getMsg(Env.getCtx(), "@AD_View_Column_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
				);
				return;
			}

			// instance browse field
			MBrowseField browseField = new Query(
					Env.getCtx(),
				I_AD_Browse_Field.Table_Name,
				"AD_Browse_ID = ? AND AD_View_Column_ID = ?",
				null
			)
				.setParameters(browse.getAD_Browse_ID(), viewColumn.getAD_View_Column_ID())
				.setOnlyActiveRecords(true)
				.first()
			;
			if (browseField == null || browseField.getAD_Browse_Field_ID() <= 0) {
				log.warning(
					Msg.getMsg(Env.getCtx(), "@AD_Browse_Field_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
				);
				return;
			}

			// instance browse field custom
			MBrowseFieldCustom customBrowseField = customBrowseFieldList.stream()
				.filter(browseFieldItem -> {
					return browseFieldItem.getAD_Browse_Field_ID() == browseField.getAD_Browse_Field_ID();
				})
				.findFirst()
				.orElse(null)
			;

			// Query Criteria sequence
			customBrowseField.setSeqNoGrid(fieldAttributes.getSequence());
			// checks if the column exists in the database
			if (customBrowseField.get_ColumnIndex(IS_DISPLAYED_COLUMN_NAME) >= 0) {
				customBrowseField.set_ValueOfColumn(
					IS_DISPLAYED_COLUMN_NAME,
					fieldAttributes.getIsDefaultDisplayed()
				);
			}
			customBrowseField.saveEx();
		});

		Empty.Builder builder = Empty.newBuilder();
		return builder;
	}


	@Override
	public void saveProcessCustomization(SaveProcessCustomizationRequest request, StreamObserver<Empty> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Requested is Null");
			}
			Empty.Builder builder = saveProcessCustomization(request);
			responseObserver.onNext(builder.build());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
			responseObserver.onError(Status.INTERNAL
				.withDescription(e.getLocalizedMessage())
				.withCause(e)
				.asRuntimeException()
			);
		}
	}

	private Empty.Builder saveProcessCustomization(SaveProcessCustomizationRequest request) {
		if (Util.isEmpty(request.getProcessUuid(), true)) {
			throw new AdempiereException("@FillMandatory@ @AD_Process_ID@");
		}
		int processId = RecordUtil.getIdFromUuid(I_AD_Process.Table_Name, request.getProcessUuid(), null);
		MProcess process = MProcess.get(Env.getCtx(), processId);
		if (process == null || process.getAD_Process_ID() <= 0) {
			throw new AdempiereException("@AD_Process_ID@ @NotFound@");
		}

		// validate level, role, user
		PO entity = getEntityToCustomizationType(request.getLevel().getNumber(), request.getLevelId(), request.getLevelUuid());
		String columnKey = entity.get_TableName() + "_ID";


		// instance process
		String sql = "SELECT AD_ProcessCustom_ID FROM AD_ProcessCustom WHERE AD_Process_ID = ? AND " + columnKey + " = ? ";
		int customProcessId = DB.getSQLValueEx(
			null,
			sql,
			process.getAD_Process_ID(), entity.get_ID()
		);

		MProcessCustom customProcess = null;
		if (customProcessId < 1) {
			customProcess = new MProcessCustom(Env.getCtx(), 0, null);
			// customProcess.setASP_Level_ID(getLevelId());
			customProcess.set_ValueOfColumn(columnKey, entity.get_ID());
			customProcess.setAD_Process_ID(process.getAD_Process_ID());
			customProcess.setHierarchyType(MProcessCustom.HIERARCHYTYPE_Overwrite);
			customProcess.saveEx();
			customProcessId = customProcess.getAD_ProcessCustom_ID();
		} else {
			customProcess = new MProcessCustom(Env.getCtx(), customProcessId, null);
		}

		List<MProcessParaCustom> customProcessParametersList = customProcess.getParameters();
		request.getFieldAttributesList().forEach(fieldAttributes -> {
			if (Util.isEmpty(fieldAttributes.getColumnName(), true)) {
				log.warning(
					Msg.getMsg(Env.getCtx(), "@AD_Process_Para_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
				);
				return;
			}
			// instance process parameter
			MProcessPara processParameter = new Query(
					Env.getCtx(),
				I_AD_Process_Para.Table_Name,
				"AD_Process_ID = ? AND ColumnName = ?",
				null
			)
				.setParameters(process.getAD_Process_ID(), fieldAttributes.getColumnName())
				.setOnlyActiveRecords(true)
				.first()
			;
			if (processParameter == null || processParameter.getAD_Process_Para_ID() <= 0) {
				log.warning(
					Msg.getMsg(Env.getCtx(), "@AD_Process_Para_ID@ (" + fieldAttributes.getColumnName() + ") @NotFound@")
				);
				return;
			}

			// instance custom process parameter
			MProcessParaCustom customProcessParameter = customProcessParametersList.stream()
				.filter(processParameterItem -> {
					return processParameterItem.getAD_Process_Para_ID() == processParameter.getAD_Process_Para_ID();
				})
				.findFirst()
				.orElse(null)
			;

			customProcessParameter.setSeqNo(fieldAttributes.getSequence());
			// checks if the column exists in the database
			if (customProcessParameter.get_ColumnIndex(IS_DISPLAYED_COLUMN_NAME) >= 0) {
				customProcessParameter.set_ValueOfColumn(
					IS_DISPLAYED_COLUMN_NAME,
					fieldAttributes.getIsDefaultDisplayed()
				);
			}
			customProcessParameter.saveEx();
		});

		Empty.Builder builder = Empty.newBuilder();
		return builder;
	}

}
