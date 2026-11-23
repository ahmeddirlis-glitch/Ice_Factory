package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.FactorySettings
import com.example.data.model.Sale
import com.example.data.util.ImageAndQrUtils
import com.example.data.util.PakistaniFormatters
import com.example.data.util.StatementDocumentModel
import com.example.data.util.StatementFilterGroup
import com.example.data.util.StatementKind
import com.example.ui.components.IcyGlossyButton
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlin.math.cos
import kotlin.math.sin

private val DocNavyDark = Color(0xFF001A4D)
private val DocNavyMid = Color(0xFF063682)
private val DocRoyalCyan = Color(0xFF1D9BF0)
private val DocGoldDark = Color(0xFFC58E24)
private val DocGoldBright = Color(0xFFF6D365)
private val DocSkyPill = Color(0xFFB6DBFA)
private val DocTableBorder = Color(0xFF3B82F6)

private val HexagonPillShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val indent = (h * 0.42f).coerceAtMost(w * 0.14f)
    moveTo(0f, h / 2f)
    lineTo(indent, 0f)
    lineTo(w - indent, 0f)
    lineTo(w, h / 2f)
    lineTo(w - indent, h)
    lineTo(indent, h)
    close()
}

/**
 * Left-Side Template Design: Deep Navy + Gold Ribbon + Ice Cubes + Gold Hexagon "SALES INVOICE"
 * + Circular Seal Stamp (PAID / PARTIAL / CREDIT) + Amount in Words + QR Verification.
 */
@Composable
fun SalesInvoicePreviewDialog(
    sale: Sale,
    settings: FactorySettings,
    canMarkPaid: Boolean = false,
    onMarkPaid: (() -> Unit)? = null,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val invNo = sale.invoiceNumber.ifBlank { "BIF-0001" }
    val dateDisplay = sale.date.ifBlank { PakistaniFormatters.todayIso() }
    val paidAmt = if (sale.paid) sale.total else sale.paidAmount.coerceIn(0.0, sale.total)
    val dueAmt = (sale.total - paidAmt).coerceAtLeast(0.0)
    val stampLabel = when {
        sale.paid -> "PAID"
        paidAmt > 0.0 -> "PARTIAL"
        else -> "CREDIT"
    }
    val stampColor = when {
        sale.paid -> Color(0xFF15803D)
        paidAmt > 0.0 -> Color(0xFFC2410C)
        else -> Color(0xFFB91C1C)
    }

    val qrPayload = remember(sale.id, invNo, sale.customerName, sale.total, stampLabel) {
        "BROHI-INVOICE|$invNo|CUST:${sale.customerName}|TOTAL:${sale.total.toLong()}|STATUS:$stampLabel|DATE:$dateDisplay"
    }
    val qrBitmap = remember(qrPayload) {
        ImageAndQrUtils.generateQrCodeBitmap(
            content = qrPayload,
            sizePx = 280,
            foregroundArgb = android.graphics.Color.rgb(5, 28, 72),
            backgroundArgb = android.graphics.Color.WHITE
        )?.asImageBitmap()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 460.dp)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Scrollable Paper Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .background(Color.White)
                ) {
                    // Left-Template Deep Navy + Gold Wave Header
                    LeftInvoiceTemplateHeader(settings = settings)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Metadata Bar
                        Text(
                            text = "Invoice No: $invNo  |  Date: $dateDisplay  |  Currency: PKR (Rs.)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DocNavyDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${settings.address.ifBlank { "SITE Industrial Area, Karachi" }} • Tel: ${settings.phone.ifBlank { "0333-2862498" }}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Customer Row + Circular Stamp Seal (Circle Paid Stamp per user request)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DocNavyMid,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Customer:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DocNavyMid
                                    )
                                    Text(
                                        text = sale.customerName.ifBlank { "Walk-in Customer" },
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DocNavyDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            CircularRubberStamp(
                                label = stampLabel,
                                stampColor = stampColor,
                                modifier = Modifier.size(72.dp)
                            )
                        }

                        // 5-Column Navy Line-Item Table
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, DocTableBorder, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DocNavyMid)
                                    .padding(horizontal = 6.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableHeaderCell("#", Modifier.weight(0.55f), TextAlign.Center)
                                TableHeaderCell("Product", Modifier.weight(2.2f), TextAlign.Start)
                                TableHeaderCell("Qty", Modifier.weight(0.75f), TextAlign.Center)
                                TableHeaderCell("Unit Price", Modifier.weight(1.45f), TextAlign.End)
                                TableHeaderCell("Total", Modifier.weight(1.55f), TextAlign.End)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 6.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableBodyCell("1", Modifier.weight(0.55f), TextAlign.Center, bold = true)
                                TableBodyCell(sale.product, Modifier.weight(2.2f), TextAlign.Start, bold = true)
                                TableBodyCell(sale.quantity.toString(), Modifier.weight(0.75f), TextAlign.Center, bold = true)
                                TableBodyCell(PakistaniFormatters.formatPkr(sale.unitPrice), Modifier.weight(1.45f), TextAlign.End)
                                TableBodyCell(PakistaniFormatters.formatPkr(sale.total), Modifier.weight(1.55f), TextAlign.End, bold = true)
                            }
                        }

                        // Sky-Blue Summary Pills
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            DocumentSummaryPillRow(
                                label = "Total Amount:",
                                value = PakistaniFormatters.formatPkr(sale.total)
                            )
                            DocumentSummaryPillRow(
                                label = "Paid Amount:",
                                value = PakistaniFormatters.formatPkr(paidAmt)
                            )
                            if (dueAmt > 0.0) {
                                DocumentSummaryPillRow(
                                    label = "Balance Due:",
                                    value = PakistaniFormatters.formatPkr(dueAmt),
                                    bgColor = Color(0xFFFEE2E2),
                                    textColor = Color(0xFFB91C1C)
                                )
                            }
                        }

                        // Dark Navy "Amount in Words" Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DocNavyDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Amount in Words:",
                                        fontSize = 10.sp,
                                        color = Color(0xFFCBD5E1),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = PakistaniFormatters.amountInWords(sale.total),
                                        fontSize = 11.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Footer: Prepared By | QR | Thank You
                        DocumentFooterInfoRow(
                            settings = settings,
                            qrBitmap = qrBitmap,
                            thankSubtext = "For Your Business"
                        )
                    }

                    // Bottom Navy + Gold Swoosh Wave
                    BottomTemplateWave(isLeftGoldTheme = true)
                }

                HorizontalDivider(color = CrystalBorderLight)

                // Sticky Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FBFF))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrystalBorderLight),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 46.dp)
                            .testTag("close_invoice_preview_button")
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 12.sp)
                    }

                    if (canMarkPaid && !sale.paid && onMarkPaid != null) {
                        OutlinedButton(
                            onClick = onMarkPaid,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.heightIn(min = 46.dp)
                        ) {
                            Text("Mark Paid", fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 12.sp)
                        }
                    }

                    IcyGlossyButton(
                        onClick = onExportPdf,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .testTag("export_invoice_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share / PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Right-Side Template Design: Icy Sky-Blue + Deep Blue Wave Ribbon + Ice Cubes
 * for Customer/Credit Ledger Statements AND Full Financial & Transaction Statements.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinancialAndCreditStatementDialog(
    statement: StatementDocumentModel,
    selectedFinancialFilter: StatementFilterGroup? = null,
    onSelectFinancialFilter: ((StatementFilterGroup) -> Unit)? = null,
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    val qrBitmap = remember(statement.qrPayload) {
        ImageAndQrUtils.generateQrCodeBitmap(
            content = statement.qrPayload,
            sizePx = 280,
            foregroundArgb = android.graphics.Color.rgb(5, 28, 72),
            backgroundArgb = android.graphics.Color.WHITE
        )?.asImageBitmap()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 480.dp)
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Scrollable Paper Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .background(Color.White)
                ) {
                    // Right-Template Icy Sky Blue + Deep Blue Wave Header
                    RightStatementTemplateHeader(
                        title = statement.title,
                        slogan = statement.slogan,
                        settings = statement.settings
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Metadata Bar
                        Text(
                            text = "Statement No: ${statement.statementNumber}  |  Date: ${statement.dateDisplay}  |  PKR (Rs.)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DocNavyDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${statement.settings.address.ifBlank { "SITE Industrial Area, Karachi" }} • Tel: ${statement.settings.phone.ifBlank { "0333-2862498" }}",
                            fontSize = 9.5.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Interactive Transaction Filter Chips for Financial Statement
                        if (statement.kind == StatementKind.FINANCIAL_LEDGER &&
                            selectedFinancialFilter != null &&
                            onSelectFinancialFilter != null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatementFilterGroup.entries.forEach { group ->
                                    FilterChip(
                                        selected = selectedFinancialFilter == group,
                                        onClick = { onSelectFinancialFilter(group) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DocNavyMid,
                                            selectedLabelColor = Color.White
                                        ),
                                        label = { Text(group.label, fontSize = 10.5.sp, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        // Party / Scope Header + Circular Rubber Stamp Seal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = DocNavyMid,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (statement.kind == StatementKind.FINANCIAL_LEDGER) {
                                                Icons.Filled.AccountBalance
                                            } else {
                                                Icons.Filled.Person
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = statement.partyLabel,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DocNavyMid
                                    )
                                    Text(
                                        text = statement.partyName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DocNavyDark,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = statement.partySubline,
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF475569),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            CircularRubberStamp(
                                label = statement.stampLabel,
                                stampColor = if (statement.stampPositive) Color(0xFF15803D) else Color(0xFFB91C1C),
                                modifier = Modifier.size(68.dp)
                            )
                        }

                        // Extra Accounting Metrics Grid
                        if (statement.extraMetrics.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                maxItemsInEachRow = 2
                            ) {
                                statement.extraMetrics.forEach { (k, v) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF0F7FF),
                                        border = BorderStroke(0.8.dp, CrystalBorderLight),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                                            Text(k, fontSize = 9.sp, color = Color(0xFF475569), maxLines = 1)
                                            Text(v, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DocNavyDark, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }

                        // 6-Column Bordered Transaction Table
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, DocTableBorder, RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DocNavyMid)
                                    .padding(horizontal = 5.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TableHeaderCell("#", Modifier.weight(0.42f), TextAlign.Center)
                                TableHeaderCell(statement.colHeader1, Modifier.weight(1.0f), TextAlign.Start)
                                TableHeaderCell(statement.colHeader2, Modifier.weight(1.15f), TextAlign.Start)
                                TableHeaderCell(statement.colHeader3, Modifier.weight(1.75f), TextAlign.Start)
                                TableHeaderCell(statement.colHeader4, Modifier.weight(1.05f), TextAlign.End)
                                TableHeaderCell(statement.colHeader5, Modifier.weight(1.05f), TextAlign.End)
                            }

                            if (statement.rows.isEmpty()) {
                                Text(
                                    text = "No financial entries recorded for this selection.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                )
                            } else {
                                statement.rows.forEachIndexed { idx, item ->
                                    if (idx > 0) {
                                        HorizontalDivider(color = DocTableBorder.copy(alpha = 0.3f), thickness = 0.6.dp)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (idx % 2 == 1) Color(0xFFF4F9FF) else Color.White)
                                            .padding(horizontal = 5.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TableBodyCell(item.index.toString(), Modifier.weight(0.42f), TextAlign.Center, fontSize = 9.5f)
                                        TableBodyCell(item.date, Modifier.weight(1.0f), TextAlign.Start, fontSize = 9f)
                                        TableBodyCell(item.referenceOrCategory, Modifier.weight(1.15f), TextAlign.Start, bold = true, fontSize = 9f)
                                        TableBodyCell(item.description, Modifier.weight(1.75f), TextAlign.Start, fontSize = 9f)
                                        TableBodyCell(
                                            text = if (item.primaryAmount > 0.0) PakistaniFormatters.formatPkr(item.primaryAmount, includeSymbol = false) else "-",
                                            modifier = Modifier.weight(1.05f),
                                            align = TextAlign.End,
                                            bold = item.primaryAmount > 0.0,
                                            textColor = if (item.primaryAmount > 0.0) SuccessGreen else Color(0xFF64748B),
                                            fontSize = 9.5f
                                        )
                                        TableBodyCell(
                                            text = if (item.secondaryAmount > 0.0) PakistaniFormatters.formatPkr(item.secondaryAmount, includeSymbol = false) else "-",
                                            modifier = Modifier.weight(1.05f),
                                            align = TextAlign.End,
                                            bold = item.secondaryAmount > 0.0,
                                            textColor = if (item.secondaryAmount > 0.0 && statement.kind == StatementKind.FINANCIAL_LEDGER) DangerRed else DocNavyDark,
                                            fontSize = 9.5f
                                        )
                                    }
                                }
                            }
                        }

                        // Sky-Blue Summary Pills
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            DocumentSummaryPillRow(
                                label = statement.summaryPrimaryLabel,
                                value = PakistaniFormatters.formatPkr(statement.summaryPrimaryValue)
                            )
                            DocumentSummaryPillRow(
                                label = statement.summarySecondaryLabel,
                                value = PakistaniFormatters.formatPkr(statement.summarySecondaryValue)
                            )
                            val isNegNet = statement.netAmount < 0.0
                            DocumentSummaryPillRow(
                                label = statement.netLabel,
                                value = PakistaniFormatters.formatPkr(statement.netAmount),
                                bgColor = if (isNegNet) Color(0xFFFEE2E2) else Color(0xFFDBEAFE),
                                textColor = if (isNegNet) Color(0xFFB91C1C) else DocNavyDark
                            )
                        }

                        // Right-Template Blue-to-Cyan Gradient "Amount in Words" Ribbon
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(DocNavyDark, DocRoyalCyan))
                                )
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Amount in Words:",
                                        fontSize = 10.sp,
                                        color = Color(0xFFDCE8FF),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = statement.amountInWords,
                                        fontSize = 11.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Footer: Prepared By | QR | Thank You
                        DocumentFooterInfoRow(
                            settings = statement.settings,
                            qrBitmap = qrBitmap,
                            thankSubtext = "Authorized Statement"
                        )
                    }

                    // Bottom Blue + Cyan Wave
                    BottomTemplateWave(isLeftGoldTheme = false)
                }

                HorizontalDivider(color = CrystalBorderLight)

                // Sticky Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FBFF))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CrystalBorderLight),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .heightIn(min = 46.dp)
                            .testTag("close_statement_preview_button")
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 12.5.sp)
                    }

                    IcyGlossyButton(
                        onClick = onExportPdf,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .testTag("export_statement_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Statement PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftInvoiceTemplateHeader(settings: FactorySettings) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
    ) {
        // Blended ice cube imagery on upper right
        Image(
            painter = painterResource(R.drawable.img_ice_cubes_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .alpha(0.34f)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Golden wave ribbon below navy header
            val goldRibbon = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.84f)
                cubicTo(w * 0.64f, h * 0.62f, w * 0.28f, h * 1.02f, 0f, h * 0.82f)
                close()
            }
            drawPath(
                path = goldRibbon,
                brush = Brush.horizontalGradient(listOf(DocGoldDark, DocGoldBright, DocGoldDark))
            )

            // Deep Royal Navy header curve
            val navyWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.72f)
                cubicTo(w * 0.68f, h * 0.50f, w * 0.30f, h * 0.94f, 0f, h * 0.72f)
                close()
            }
            drawPath(
                path = navyWave,
                brush = Brush.linearGradient(
                    colors = listOf(DocNavyDark, Color(0xFF04307E), Color(0xFF0A4EB8)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h * 0.8f)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TemplateMountainCrest(tint = Color.White, modifier = Modifier.size(46.dp))
                Text(
                    text = "BROHI",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    letterSpacing = 1.5.sp,
                    lineHeight = 22.sp
                )
                Text(
                    text = "ICE FACTORY",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "Pure Ice...",
                    color = DocGoldBright,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Cool Business...",
                    color = DocGoldBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = settings.companyName.ifBlank { "Brohi Ice Factory" },
                    color = Color(0xFFDCE8FF),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Gold Hexagon Badge overlapping bottom center of header
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
                .clip(HexagonPillShape)
                .background(Brush.horizontalGradient(listOf(DocGoldDark, DocGoldBright, DocGoldDark)))
                .padding(horizontal = 32.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "SALES INVOICE",
                color = DocNavyDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.5.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun RightStatementTemplateHeader(
    title: String,
    slogan: String,
    settings: FactorySettings
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(174.dp)
            .background(
                Brush.verticalGradient(listOf(Color(0xFFD8EEFF), Color(0xFFF5FAFF), Color.White))
            )
    ) {
        // Blended ice cube imagery on upper right
        Image(
            painter = painterResource(R.drawable.img_ice_cubes_banner),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .alpha(0.28f)
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top cyan swoosh accent
            val topSwoosh = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.45f, 0f)
                cubicTo(w * 0.25f, h * 0.14f, w * 0.08f, h * 0.35f, 0f, h * 0.58f)
                close()
            }
            drawPath(path = topSwoosh, color = Color(0xFFB3E0FF))

            // Flowing Blue-to-Cyan Wave Ribbon at bottom of header
            val waveRibbon = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(w * 0.30f, h * 0.56f, w * 0.68f, h * 0.90f, w, h * 0.66f)
                lineTo(w, h * 0.92f)
                cubicTo(w * 0.65f, h * 1.02f, w * 0.32f, h * 0.78f, 0f, h * 0.92f)
                close()
            }
            drawPath(
                path = waveRibbon,
                brush = Brush.horizontalGradient(listOf(DocNavyDark, DocRoyalCyan))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TemplateMountainCrest(tint = DocNavyMid, modifier = Modifier.size(44.dp))
                Text(
                    text = "BROHI",
                    color = DocNavyMid,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    letterSpacing = 1.5.sp,
                    lineHeight = 22.sp
                )
                Text(
                    text = "ICE FACTORY",
                    color = DocNavyMid,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = slogan,
                    color = DocNavyMid,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = settings.companyName.ifBlank { "Brohi Ice Factory" },
                    color = RoyalBlue,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Deep Blue Wave Title Pill at bottom center of header
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(listOf(DocNavyDark, DocNavyMid, DocRoyalCyan)))
                .padding(horizontal = 24.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp,
                letterSpacing = 0.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Classic Circular Rubber Stamp Seal (e.g. PAID / PARTIAL / CREDIT / PROFIT / DEFICIT)
 * built specifically as a circular seal per user instructions.
 */
@Composable
fun CircularRubberStamp(
    label: String,
    stampColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.rotate(-14f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 3.dp.toPx()
            drawCircle(color = stampColor.copy(alpha = 0.08f), radius = r)
            drawCircle(
                color = stampColor,
                radius = r,
                style = Stroke(width = 2.6.dp.toPx())
            )
            drawCircle(
                color = stampColor,
                radius = r - 5.dp.toPx(),
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Text(
                text = "★ BROHI ★",
                fontSize = 6.5.sp,
                fontWeight = FontWeight.Bold,
                color = stampColor,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = if (label.length <= 6) 12.5.sp else 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = stampColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                letterSpacing = 0.6.sp
            )
            Text(
                text = "VERIFIED",
                fontSize = 6.sp,
                fontWeight = FontWeight.Bold,
                color = stampColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TemplateMountainCrest(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mPath = Path().apply {
            moveTo(w * 0.05f, h * 0.58f)
            lineTo(w * 0.35f, h * 0.12f)
            lineTo(w * 0.56f, h * 0.40f)
            lineTo(w * 0.72f, h * 0.20f)
            lineTo(w * 0.95f, h * 0.58f)
        }
        drawPath(
            path = mPath,
            color = tint,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val cx = w * 0.50f
        val cy = h * 0.72f
        val r = w * 0.20f
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            val dx = (r * cos(angle)).toFloat()
            val dy = (r * sin(angle)).toFloat()
            drawLine(
                color = tint,
                start = Offset(cx, cy),
                end = Offset(cx + dx, cy + dy),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TableHeaderCell(
    text: String,
    modifier: Modifier,
    align: TextAlign
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Bold,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun TableBodyCell(
    text: String,
    modifier: Modifier,
    align: TextAlign,
    bold: Boolean = false,
    textColor: Color = DocNavyDark,
    fontSize: Float = 10f
) {
    Text(
        text = text,
        color = textColor,
        fontSize = fontSize.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        textAlign = align,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 2.dp)
    )
}

@Composable
private fun DocumentSummaryPillRow(
    label: String,
    value: String,
    bgColor: Color = DocSkyPill,
    textColor: Color = DocNavyDark
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth(0.76f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )
        }
    }
}

@Composable
private fun DocumentFooterInfoRow(
    settings: FactorySettings,
    qrBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    thankSubtext: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = "Prepared By:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = DocNavyMid
            )
            Text(
                text = "${settings.companyName.ifBlank { "Brohi Ice Factory" }}\nManagement System",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = DocNavyDark,
                lineHeight = 12.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(0.9f)
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Document Verification QR Code",
                    modifier = Modifier
                        .size(58.dp)
                        .border(0.8.dp, CrystalBorderLight, RoundedCornerShape(4.dp))
                        .padding(2.dp)
                )
            }
            Text(
                text = "Scan to Verify",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = DocNavyDark
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1.0f)
        ) {
            Text(
                text = "Thank You!",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Serif,
                color = DocNavyMid
            )
            Text(
                text = thankSubtext,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = DocNavyDark
            )
        }
    }
}

@Composable
private fun BottomTemplateWave(isLeftGoldTheme: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val w = size.width
        val h = size.height
        val backWave = Path().apply {
            moveTo(0f, h * 0.25f)
            cubicTo(w * 0.35f, -h * 0.15f, w * 0.65f, h * 0.70f, w, h * 0.18f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        val frontWave = Path().apply {
            moveTo(0f, h * 0.48f)
            cubicTo(w * 0.35f, h * 0.05f, w * 0.65f, h * 0.88f, w, h * 0.38f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        if (isLeftGoldTheme) {
            drawPath(backWave, Brush.horizontalGradient(listOf(DocGoldDark, DocGoldBright)))
            drawPath(frontWave, DocNavyDark)
        } else {
            drawPath(backWave, Color(0xFF38BDF8))
            drawPath(frontWave, Brush.horizontalGradient(listOf(DocNavyDark, DocNavyMid)))
        }
    }
}
