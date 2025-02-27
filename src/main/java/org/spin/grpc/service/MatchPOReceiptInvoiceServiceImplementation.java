/************************************************************************************
 * Copyright (C) 2012-2023 E.R.P. Consultores y Asociados, C.A.                     *
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

import org.adempiere.exceptions.AdempiereException;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.adempiere.core.domains.models.I_C_BPartner;
import org.adempiere.core.domains.models.I_C_Invoice;
import org.adempiere.core.domains.models.I_C_Order;
import org.adempiere.core.domains.models.I_M_InOut;
import org.adempiere.core.domains.models.I_M_Product;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MProduct;
import org.compiere.model.MRole;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;
import org.spin.backend.grpc.common.ListLookupItemsResponse;
import org.spin.backend.grpc.common.LookupItem;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedFromRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedFromResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedToRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchedToResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchesTypesFromRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListMatchesTypesToRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListProductsRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListProductsResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListSearchModesRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ListVendorsRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.MatchMode;
import org.spin.backend.grpc.form.match_po_receipt_invoice.MatchType;
import org.spin.backend.grpc.form.match_po_receipt_invoice.Matched;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ProcessRequest;
import org.spin.backend.grpc.form.match_po_receipt_invoice.ProcessResponse;
import org.spin.backend.grpc.form.match_po_receipt_invoice.Product;
import org.spin.backend.grpc.form.match_po_receipt_invoice.MatchPORReceiptInvoiceGrpc.MatchPORReceiptInvoiceImplBase;
import org.spin.base.db.LimitUtil;
import org.spin.base.util.LookupUtil;
import org.spin.base.util.ReferenceUtil;
import org.spin.base.util.SessionManager;
import org.spin.base.util.ValueUtil;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author Edwin Betancourt, EdwinBetanc0urt@outlook.com, https://github.com/EdwinBetanc0urt
 * Service for backend of Match PO-Receipt-Invoice form
 */
public class MatchPOReceiptInvoiceServiceImplementation extends MatchPORReceiptInvoiceImplBase {
	/**	Logger			*/
	private CLogger log = CLogger.getCLogger(MatchPOReceiptInvoiceServiceImplementation.class);



	@Override
	public void listMatchesTypesFrom(ListMatchesTypesFromRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listMatchesTypesFrom(request);
			responseObserver.onNext(builderList.build());
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

	ListLookupItemsResponse.Builder listMatchesTypesFrom(ListMatchesTypesFromRequest request) {
		Properties context = Env.getCtx();

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		// Invoice
		LookupItem.Builder lookupInvoice = LookupItem.newBuilder()
			.setId(MatchType.INVOICE_VALUE)
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchType.INVOICE_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, I_C_Invoice.COLUMNNAME_C_Invoice_ID)
				).build())
		;
		builderList.addRecords(lookupInvoice);

		// Receipt
		LookupItem.Builder lookupReceipt = LookupItem.newBuilder()
			.setId(MatchType.RECEIPT_VALUE)
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchType.RECEIPT_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, I_M_InOut.COLUMNNAME_M_InOut_ID)
				).build())
		;
		builderList.addRecords(lookupReceipt);

		// Purchase Order
		LookupItem.Builder lookupOrder = LookupItem.newBuilder()
			.setId(MatchType.PURCHASE_ORDER_VALUE)
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchType.PURCHASE_ORDER_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, I_C_Order.COLUMNNAME_C_Order_ID)
				).build())
		;
		builderList.addRecords(lookupOrder);

		return builderList;
	}



	@Override
	public void listMatchesTypesTo(ListMatchesTypesToRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listMatchesTypesTo(request);
			responseObserver.onNext(builderList.build());
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

	ListLookupItemsResponse.Builder listMatchesTypesTo(ListMatchesTypesToRequest request) {
		Properties context = Env.getCtx();
		MatchType matchTypeFrom = request.getMatchFromType();

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		// Invoice
		if (matchTypeFrom == MatchType.RECEIPT) {
			LookupItem.Builder lookupInvoice = LookupItem.newBuilder()
				.setId(MatchType.INVOICE_VALUE)
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					ValueUtil.getValueFromInt(
						MatchType.INVOICE_VALUE
					).build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueUtil.getValueFromString(
						Msg.translate(context, I_C_Invoice.COLUMNNAME_C_Invoice_ID)
					).build())
			;
			builderList.addRecords(lookupInvoice);
		}

		// Receipt
		if (matchTypeFrom == MatchType.INVOICE || matchTypeFrom == MatchType.PURCHASE_ORDER) {
			LookupItem.Builder lookupReceipt = LookupItem.newBuilder()
				.setId(MatchType.RECEIPT_VALUE)
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					ValueUtil.getValueFromInt(
						MatchType.RECEIPT_VALUE
					).build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueUtil.getValueFromString(
						Msg.translate(context, I_M_InOut.COLUMNNAME_M_InOut_ID)
					).build())
			;
			builderList.addRecords(lookupReceipt);
		}

		// Purchase Order
		if (matchTypeFrom == MatchType.RECEIPT) {
			LookupItem.Builder lookupOrder = LookupItem.newBuilder()
				.setId(MatchType.PURCHASE_ORDER_VALUE)
				.putValues(
					LookupUtil.VALUE_COLUMN_KEY,
					ValueUtil.getValueFromInt(
						MatchType.PURCHASE_ORDER_VALUE
					).build()
				)
				.putValues(
					LookupUtil.DISPLAY_COLUMN_KEY,
					ValueUtil.getValueFromString(
						Msg.translate(context, I_C_Order.COLUMNNAME_C_Order_ID)
					).build())
			;
			builderList.addRecords(lookupOrder);
		}

		return builderList;
	}

	@Override
	public void listSearchModes(ListSearchModesRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listSearchModes(request);
			responseObserver.onNext(builderList.build());
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

	ListLookupItemsResponse.Builder listSearchModes(ListSearchModesRequest request) {
		Properties context = Env.getCtx();

		ListLookupItemsResponse.Builder builderList = ListLookupItemsResponse.newBuilder();

		LookupItem.Builder lookupMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_NOT_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, "NotMatched")
				).build())
		;
		builderList.addRecords(lookupMatched);

		LookupItem.Builder lookupUnMatched = LookupItem.newBuilder()
			.putValues(
				LookupUtil.VALUE_COLUMN_KEY,
				ValueUtil.getValueFromInt(
					MatchMode.MODE_MATCHED_VALUE
				).build()
			)
			.putValues(
				LookupUtil.DISPLAY_COLUMN_KEY,
				ValueUtil.getValueFromString(
					Msg.translate(context, "Matched")
				).build())
		;
		builderList.addRecords(lookupUnMatched);

		return builderList;
	}



	@Override
	public void listVendors(ListVendorsRequest request, StreamObserver<ListLookupItemsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListLookupItemsResponse.Builder builderList = listVendors(request);
			responseObserver.onNext(builderList.build());
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

	ListLookupItemsResponse.Builder listVendors(ListVendorsRequest request) {
		// Business Partner
		int validationRuleId = 52520; // C_BPartner - Vendor
		MLookupInfo reference = ReferenceUtil.getReferenceLookupInfo(
			DisplayType.TableDir,
			0,
			I_C_BPartner.COLUMNNAME_C_BPartner_ID,
			validationRuleId
		);

		ListLookupItemsResponse.Builder builderList = UserInterfaceServiceImplementation.listLookupItems(
			reference,
			null,
			request.getPageSize(),
			request.getPageToken(),
			request.getSearchValue()
		);

		return builderList;
	}



	@Override
	public void listProducts(ListProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListProductsResponse.Builder builder = listProducts(request);
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

	private ListProductsResponse.Builder listProducts(ListProductsRequest request) {
		//	Dynamic where clause
		String whereClause = "IsPurchased = 'Y' AND IsSummary = 'N' ";
		//	Parameters
		List<Object> parameters = new ArrayList<Object>();
		//	For search value
		if (!Util.isEmpty(request.getSearchValue(), true)) {
			whereClause += " AND ("
				+ "UPPER(Value) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(Name) LIKE '%' || UPPER(?) || '%' "
				+ "OR UPPER(UPC) = UPPER(?) "
				+ "OR UPPER(SKU) = UPPER(?) "
				+ ")"
			;
			//	Add parameters
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
			parameters.add(request.getSearchValue());
		}

		Query query = new Query(
			Env.getCtx(),
			I_M_Product.Table_Name,
			whereClause.toString(),
			null
		)
			.setClient_ID()
			.setParameters(parameters);

		int count = query.count();
		String nexPageToken = "";
		int pageNumber = LimitUtil.getPageNumber(SessionManager.getSessionUuid(), request.getPageToken());
		int limit = LimitUtil.getPageSize(request.getPageSize());
		int offset = (pageNumber - 1) * limit;
		//	Set page token
		if (LimitUtil.isValidNextPageToken(count, offset, limit)) {
			nexPageToken = LimitUtil.getPagePrefix(SessionManager.getSessionUuid()) + (pageNumber + 1);
		}

		ListProductsResponse.Builder builderList = ListProductsResponse.newBuilder()
			.setRecordCount(count)
			.setNextPageToken(
				ValueUtil.validateNull(nexPageToken)
			)
		;

		query.setLimit(limit, offset)
			.list(MProduct.class)
			.forEach(product -> {
				Product.Builder builder = convertProduct(product);
				builderList.addRecords(builder);
			});
		;

		return builderList;
	}

	static Product.Builder convertProduct(int productId) {
		Product.Builder builder = Product.newBuilder();
		if (productId <= 0) {
			return builder;
		}
		MProduct product = MProduct.get(Env.getCtx(), productId);
		return convertProduct(product);
	}
	static Product.Builder convertProduct(MProduct product) {
		Product.Builder builder = Product.newBuilder();
		if (product == null) {
			return builder;
		}
		builder.setId(product.getM_Product_ID())
			.setUuid(
				ValueUtil.validateNull(product.getUUID())
			)
			.setUpc(
				ValueUtil.validateNull(product.getUPC())
			)
			.setSku(
				ValueUtil.validateNull(product.getSKU())
			)
			.setValue(
				ValueUtil.validateNull(product.getValue())
			)
			.setName(
				ValueUtil.validateNull(product.getName())
			)
		;

		return builder;
	}



	@Override
	public void listMatchedFrom(ListMatchedFromRequest request, StreamObserver<ListMatchedFromResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListMatchedFromResponse.Builder builder = listMatchedFrom(request);
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

	private ListMatchedFromResponse.Builder listMatchedFrom(ListMatchedFromRequest request) {
		boolean isMatched = request.getMatchMode() == MatchMode.MODE_MATCHED;
		int matchFromType = request.getMatchFromTypeValue();
		int matchToType = request.getMatchToTypeValue();

		final String dateColumn = org.spin.form.match_po_receipt_invoice.Util.getDateColumn(matchFromType);
		final String sql = org.spin.form.match_po_receipt_invoice.Util.getSQL(isMatched, matchFromType, matchToType);
		final String groupBy = org.spin.form.match_po_receipt_invoice.Util.getGroupBy(isMatched, matchFromType);

		String whereClause = "";
		if (request.getProductId() > 0) {
			whereClause += " AND lin.M_Product_ID = " + request.getProductId();
		}
		if (request.getVendorId() > 0) {
			whereClause += " AND hdr.C_BPartner_ID = " + request.getVendorId();
		}

		// Date filter
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(request.getDateFrom());
		Timestamp dateTo = ValueUtil.getTimestampFromLong(request.getDateTo());
		if (dateFrom != null && dateTo != null) {
			whereClause += " AND " + dateColumn + " BETWEEN " + DB.TO_DATE(dateFrom)
				+ " AND " + DB.TO_DATE(dateTo)
			;
		} else if (dateFrom != null) {
			whereClause += " AND " + dateColumn + " >= " + DB.TO_DATE(dateFrom);
		} else if (dateTo != null) {
			whereClause += " AND " + dateColumn + " <= " + DB.TO_DATE(dateTo);
		}

		int recordCount = 0;

		final String sqlWithAccess = MRole.getDefault().addAccessSQL(
			sql + whereClause,
			"hdr",
			MRole.SQL_FULLYQUALIFIED,
			MRole.SQL_RO
		) + groupBy;

		ListMatchedFromResponse.Builder builderList = ListMatchedFromResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlWithAccess, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				recordCount++;

				Matched.Builder matchedBuilder = org.spin.form.match_po_receipt_invoice.Util.convertMatched(rs);
				builderList.addRecords(matchedBuilder);
			}
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		builderList.setRecordCount(recordCount);

		return builderList;
	}



	@Override
	public void listMatchedTo(ListMatchedToRequest request, StreamObserver<ListMatchedToResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ListMatchedToResponse.Builder builder = listMatchedTo(request);
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

	private ListMatchedToResponse.Builder listMatchedTo(ListMatchedToRequest request) {
		boolean isMatched = request.getMatchMode() == MatchMode.MODE_MATCHED;
		int matchFromType = request.getMatchFromTypeValue();
		int matchToType = request.getMatchToTypeValue();
		int matchFromSelectedId = request.getMatchFromSelectedId();

		final String dateColumn = org.spin.form.match_po_receipt_invoice.Util.getDateColumn(matchFromType);
		final String sql = org.spin.form.match_po_receipt_invoice.Util.getSQL(isMatched, matchFromType, matchToType);
		final String groupBy = org.spin.form.match_po_receipt_invoice.Util.getGroupBy(isMatched, matchFromType);

		String whereClause = "";
		if (request.getProductId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @M_Product_ID@");
		}
		whereClause += " AND lin.M_Product_ID = " + request.getProductId();

		if (request.getVendorId() <= 0) {
			throw new AdempiereException("@FillMandatory@ @C_BPartner_ID@");
		}
		whereClause += " AND hdr.C_BPartner_ID = " + request.getVendorId();

		// Date filter
		Timestamp dateFrom = ValueUtil.getTimestampFromLong(request.getDateFrom());
		Timestamp dateTo = ValueUtil.getTimestampFromLong(request.getDateTo());
		if (dateFrom != null && dateTo != null) {
			whereClause += " AND " + dateColumn + " BETWEEN " + DB.TO_DATE(dateFrom)
				+ " AND " + DB.TO_DATE(dateTo)
			;
		} else if (dateFrom != null) {
			whereClause += " AND " + dateColumn + " >= " + DB.TO_DATE(dateFrom);
		} else if (dateTo != null) {
			whereClause += " AND " + dateColumn + " <= " + DB.TO_DATE(dateTo);
		}

		if (request.getIsSameQuantity()) {
			final String quantityColumn = org.spin.form.match_po_receipt_invoice.Util.getQuantityColumn(matchFromType);
			Matched.Builder matchedFromSelected = org.spin.form.match_po_receipt_invoice.Util.getMatchedSelectedFrom(matchFromSelectedId, isMatched, matchFromType, matchToType);
			BigDecimal quantity = ValueUtil.getBigDecimalFromDecimal(
				matchedFromSelected.getQuantity()
			);
			whereClause += " AND " + quantityColumn + " = " + quantity;
		}

		int recordCount = 0;

		final String sqlWithAccess = MRole.getDefault().addAccessSQL(
			sql + whereClause,
			"hdr",
			MRole.SQL_FULLYQUALIFIED,
			MRole.SQL_RO
		) + groupBy;

		ListMatchedToResponse.Builder builderList = ListMatchedToResponse.newBuilder();
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sqlWithAccess, null);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				recordCount++;

				Matched.Builder matchedBuilder = org.spin.form.match_po_receipt_invoice.Util.convertMatched(rs);
				builderList.addRecords(matchedBuilder);
			}
		}
		catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}

		builderList.setRecordCount(recordCount);

		return builderList;
	}



	@Override
	public void process(ProcessRequest request, StreamObserver<ProcessResponse> responseObserver) {
		try {
			if (request == null) {
				throw new AdempiereException("Object Request Null");
			}

			ProcessResponse.Builder builder = process(request);
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

	private ProcessResponse.Builder process(ProcessRequest request) {
		if (request.getMatchFromSelectedId() <= 0) {
			throw new AdempiereException("");
		}

		AtomicReference<String> atomicStatus = new AtomicReference<String>();

		Trx.run(transactionName -> {
			boolean isMatchMode = MatchMode.MODE_MATCHED == request.getMatchMode();
			final BigDecimal quantity = ValueUtil.getBigDecimalFromDecimal(
				request.getQuantity()
			);
			boolean isMatchFromOder = MatchType.PURCHASE_ORDER == request.getMatchFromType();
			boolean isMatchFromReceipt = MatchType.RECEIPT == request.getMatchFromType();

			boolean isMatchToOrder = MatchType.PURCHASE_ORDER == request.getMatchToType();

			Matched.Builder matchedFromSelected = org.spin.form.match_po_receipt_invoice.Util.getMatchedSelectedFrom(
				request.getMatchFromSelectedId(),
				isMatchMode,
				request.getMatchFromType().getNumber(),
				request.getMatchToType().getNumber()
			);

			request.getMatchedToSelectionsList().forEach(lineMatchedTo -> {
				BigDecimal mathcedQuantity = ValueUtil.getBigDecimalFromDecimal(lineMatchedTo.getMatchedQuantity());
				BigDecimal documentQuantity = ValueUtil.getBigDecimalFromDecimal(lineMatchedTo.getQuantity());

				Optional<BigDecimal> qty = Optional.empty();
				Optional<BigDecimal> docQty = Optional.ofNullable((BigDecimal) documentQuantity);
				Optional<BigDecimal> matchedQty = Optional.ofNullable((BigDecimal) mathcedQuantity);
				Optional<BigDecimal> totalQty = Optional.ofNullable(
					quantity
				);
				
				if (!isMatchMode) {
					qty = docQty; //  doc
				}

				qty = Optional.ofNullable(qty.orElse(Env.ZERO).subtract(matchedQty.orElse(Env.ZERO)));
				if (qty.isPresent() && qty.get().compareTo(totalQty.orElse(Env.ZERO)) > 0) {
					qty = totalQty;
				}
				if (totalQty.isPresent()) {
					totalQty = Optional.ofNullable(totalQty.get().subtract(qty.orElse(Env.ZERO)));
				}

				//  Invoice or PO
				boolean isInvoice = true;
				if (isMatchFromOder || isMatchToOrder) {
					isInvoice = false;
				}
				//  Get Shipment_ID
				int inOutLineId = 0;
				int lineId = 0;
				if (isMatchFromReceipt) {
					inOutLineId = matchedFromSelected.getId();      //  upper table
					lineId = lineMatchedTo.getId();
				}
				else {
					inOutLineId = lineMatchedTo.getId();    //  lower table
					lineId = matchedFromSelected.getId();
				}

				org.spin.form.match_po_receipt_invoice.Util.createMatchRecord(
					isInvoice,
					inOutLineId,
					lineId,
					qty.orElse(Env.ZERO),
					transactionName
				);
			});

			atomicStatus.set("");
		});

		return ProcessResponse.newBuilder()
			.setMessage(
				ValueUtil.validateNull(
					atomicStatus.get()
				)
			)
		;
	}


}
