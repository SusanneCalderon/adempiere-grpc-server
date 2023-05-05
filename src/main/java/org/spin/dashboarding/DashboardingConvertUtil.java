/*************************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                              *
 * This program is free software; you can redistribute it and/or modify it           *
 * under the terms version 2 or later of the GNU General Public License as published *
 * by the Free Software Foundation. This program is distributed in the hope          *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied        *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                  *
 * See the GNU General Public License for more details.                              *
 * You should have received a copy of the GNU General Public License along           *
 * with this program; if not, write to the Free Software Foundation, Inc.,           *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                            *
 * For the text or an alternative of this public license, you may reach us           *
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, S.A. All Rights Reserved. *
 * Contributor(s): Edwin Betancourt, EdwinBetanc0urt@outlook.com                     *
 *************************************************************************************/

package org.spin.dashboarding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.adempiere.apps.graph.GraphColumn;
import org.adempiere.core.domains.models.I_AD_Column;
import org.adempiere.core.domains.models.I_AD_Field;
import org.adempiere.core.domains.models.I_AD_Process_Para;
import org.compiere.model.MChart;
import org.compiere.model.MColorSchema;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.print.MPrintColor;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.spin.backend.grpc.dashboarding.ChartData;
import org.spin.backend.grpc.dashboarding.ColorSchema;
import org.spin.backend.grpc.dashboarding.Filter;
import org.spin.backend.grpc.dashboarding.WindowDashboard;
import org.spin.backend.grpc.dashboarding.WindowDashboardParameter;
import org.spin.base.util.ValueUtil;

/**
 * This class was created for add all convert methods for POS form
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com , https://github.com/EdwinBetanc0urt
 */
public class DashboardingConvertUtil {

	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(DashboardingConvertUtil.class);

	/**
	 * Get color as hex
	 * @param printColorId
	 * @return
	 */
	public static String getColorAsHex(int printColorId) {
		if (printColorId <= 0) {
			return "";
		}
		MPrintColor printColor = MPrintColor.get(
			Env.getCtx(),
			printColorId
		);
		int color = 0;
		try {
			color = Integer.parseInt(printColor.getCode());
		} catch (Exception e) {
			log.severe(e.getLocalizedMessage());
		}
		return ValueUtil.validateNull(
			String.format("#%06X", (0xFFFFFF & color))
		);
	}

	/**
	 * Convert Filter Values from gRPC to ADempiere object values
	 * @param values
	 * @return
	 */
	public static Map<String, Object> convertFilterValuesToObjects(List<Filter> filtersList) {
		Map<String, Object> convertedValues = new HashMap<>();
		if (filtersList == null || filtersList.size() <= 0) {
			return convertedValues;
		}
		for (Filter filter : filtersList) {
			Object value = null;
			// to IN or NOT IN clause
			if (filter.getValuesList() != null && filter.getValuesList().size() > 0) {
				List<Object> values = new ArrayList<Object>();
				filter.getValuesList().forEach(valueBuilder -> {
					Object currentValue = ValueUtil.getObjectFromValue(
						valueBuilder
					);
					values.add(currentValue);
				});
				value = values;
			}
			else {
				value = ValueUtil.getObjectFromValue(filter.getValue());
				// to BETWEEN clause
				if (filter.hasValueTo()) {
					Object currentValue = value;
					List<Object> values = new ArrayList<Object>();
					values.add(currentValue);
					values.add(
						ValueUtil.getObjectFromValue(
							filter.getValueTo()
						)
					);
					value = values;
				}
			}
			convertedValues.put(
				filter.getColumnName(),
				value
			);
		}
		//
		return convertedValues;
	}

	@SuppressWarnings("unchecked")
	public static void addCollectionParameters(Object objectColelction, List<Object> parameters) {
		if (objectColelction instanceof Collection) {
			try {
				Collection<Object> collection = (Collection<Object>) objectColelction;
				// for-each loop
				for (Object rangeValue : collection) {
					parameters.add(rangeValue);
				}
			}
			catch (Exception e) {
			}
		}
	}

	public static ColorSchema.Builder convertColorSchema1(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				ValueUtil.getDecimalFromInt(
					colorSchema.getMark1Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor1_ID()
				)
			)
		;
		return builder;
	}

	public static ColorSchema.Builder convertColorSchema2(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				ValueUtil.getDecimalFromInt(
					colorSchema.getMark2Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor2_ID()
				)
			)
		;
		return builder;
	}

	public static ColorSchema.Builder convertColorSchema3(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				ValueUtil.getDecimalFromInt(
					colorSchema.getMark3Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor3_ID()
				)
			)
		;
		return builder;
	}

	public static ColorSchema.Builder convertColorSchema4(MColorSchema colorSchema) {
		ColorSchema.Builder builder = ColorSchema.newBuilder();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return builder;
		}
		builder.setPercent(
				ValueUtil.getDecimalFromInt(
					colorSchema.getMark4Percent()
				)
			)
			.setColor(
				getColorAsHex(
					colorSchema.getAD_PrintColor4_ID()
				)
			)
		;
		return builder;
	}


	public static List<ColorSchema> convertColorSchemasList(MColorSchema colorSchema) {
		List<ColorSchema> colorSchemasList = new ArrayList<ColorSchema>();
		if (colorSchema == null || colorSchema.getPA_ColorSchema_ID() <= 0) {
			return colorSchemasList;
		}

		// First mark
		colorSchemasList.add(
			convertColorSchema1(colorSchema).build()
		);

		// Second Mark
		colorSchemasList.add(
			convertColorSchema2(colorSchema).build()
		);

		// Third Mark
		colorSchemasList.add(
			convertColorSchema3(colorSchema).build()
		);

		// Four Mark
		colorSchemasList.add(
			convertColorSchema4(colorSchema).build()
		);

		return colorSchemasList;
	}


	public static ChartData.Builder convertChartData(GraphColumn grapColumn) {
		ChartData.Builder builder = ChartData.newBuilder();
		if (grapColumn == null) {
			return builder;
		}
		
		builder.setName(
				ValueUtil.validateNull(
					grapColumn.getLabel()
				)
			)
			.setValue(
				ValueUtil.getDecimalFromBigDecimal(
					new BigDecimal(grapColumn.getValue())
				)
			)
		;

		return builder;
	}


	public static WindowDashboardParameter.Builder convertWindowDashboardParameter(PO chartParameter) {
		WindowDashboardParameter.Builder builder = WindowDashboardParameter.newBuilder();
		if (chartParameter == null || chartParameter.get_ID() <= 0) {
			return builder;
		}
		builder.setId(chartParameter.get_ID())
			.setUuid(
				ValueUtil.validateNull(chartParameter.get_UUID())
			)
			.setName(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_Name)
				)
			)
			.setDescription(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_Description)
				)
			)
			.setHelp(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_Help)
				)
			)
			.setSequence(
				chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_SeqNo)
			)
			.setColumnName(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ColumnName)
				)
			)
			.setColumnSql(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString("ColumnSQL")
				)
			)
			.setElementId(
				chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Element_ID)
			)
			.setFieldId(
				chartParameter.get_ValueAsInt(I_AD_Field.COLUMNNAME_AD_Field_ID)
			)
			.setIsMandatory(
				chartParameter.get_ValueAsBoolean(I_AD_Process_Para.COLUMNNAME_IsMandatory)
			)
			.setIsRange(
				chartParameter.get_ValueAsBoolean(I_AD_Process_Para.COLUMNNAME_IsRange)
			)
			.setDefaultValue(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_DefaultValue)
				)
			)
			.setDisplayType(
				chartParameter.get_ValueAsInt(I_AD_Process_Para.COLUMNNAME_AD_Reference_ID)
			)
			.setVFormat(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_VFormat)
				)
			)
			.setValueMax(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ValueMax)
				)
			)
			.setValueMin(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ValueMin)
				)
			)
			.setDisplayLogic(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_DisplayLogic)
				)
			)
			.setReadOnlyLogic(
				ValueUtil.validateNull(
					chartParameter.get_ValueAsString(I_AD_Process_Para.COLUMNNAME_ReadOnlyLogic)
				)
			)
		;
		return builder;
	}


	public static WindowDashboard.Builder convertWindowDashboard(MChart chartDefinition) {
		WindowDashboard.Builder builder = WindowDashboard.newBuilder();
		if (chartDefinition == null || chartDefinition.getAD_Chart_ID() <= 0) {
			return builder;
		}

		builder = WindowDashboard.newBuilder()
			.setId(chartDefinition.getAD_Chart_ID())
			.setUuid(
				ValueUtil.validateNull(chartDefinition.getUUID())
			)
			.setName(
				ValueUtil.validateNull(chartDefinition.getName())
			)
			.setDescription(
				ValueUtil.validateNull(chartDefinition.getDescription())
			)
			.setDashboardType("chart")
			.setChartType(
				ValueUtil.validateNull(chartDefinition.getChartType())
			)
			.setIsCollapsible(true)
			.setIsOpenByDefault(true)
		;

		return builder;
	}

	public static List<String> getContextColumnsByWindowChart(int windowChartAllocationId) {
		List<String> contextColumnsList = new ArrayList<String>();

		new Query(
				Env.getCtx(),
				"ECA50_WindowChartParameter",
				"ECA50_WindowChart_ID = ? AND ECA50_IsEnableSelection = 'N'",
				null
			)
			.setParameters(windowChartAllocationId)
			.setOnlyActiveRecords(true)
			.<PO>list()
			.forEach(windowChartParameter -> {
				String contextColumn = ValueUtil.validateNull(
					windowChartParameter.get_ValueAsString(I_AD_Column.COLUMNNAME_ColumnName)
				);
				contextColumnsList.add(contextColumn);
			})
		;

		return contextColumnsList;
	}

}