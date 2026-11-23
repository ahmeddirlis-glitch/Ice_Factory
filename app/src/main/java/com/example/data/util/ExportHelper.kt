package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.FactorySettings
import com.example.data.model.Sale
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    private const val PAGE_W = 595f
    private const val PAGE_H = 842f

    fun exportAndSharePdf(
        context: Context,
        fileName: String,
        title: String,
        subtitle: String,
        lines: List<String>
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "$fileName.pdf")
            val doc = PdfDocument()
            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas

            val headerPaint = Paint().apply {
                color = Color.parseColor("#0A3A9E")
                style = Paint.Style.FILL
            }
            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint().apply {
                color = Color.parseColor("#DCE8FF")
                textSize = 11f
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#0D2149")
                textSize = 12f
            }
            val boldPaint = Paint().apply {
                color = Color.parseColor("#0A3A9E")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            canvas.drawRect(0f, 0f, 595f, 92f, headerPaint)
            canvas.drawText("BROHI ICE FACTORY", 32f, 38f, titlePaint)
            canvas.drawText(title, 32f, 60f, subPaint)
            canvas.drawText(subtitle, 32f, 76f, subPaint)

            var y = 126f
            for (line in lines) {
                if (y > 790f) {
                    doc.finishPage(page)
                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                val paint = if (line.startsWith("---")) boldPaint else bodyPaint
                canvas.drawText(line.removePrefix("---").trim(), 32f, y, paint)
                y += 22f
            }

            doc.finishPage(page)
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            shareFile(context, file, "application/pdf", title)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a styled Left-Side Design Sales Invoice PDF (Deep Navy + Gold Ribbon + Circular Seal Stamp + QR Code).
     */
    fun exportAndShareInvoicePdf(
        context: Context,
        sale: Sale,
        settings: FactorySettings = FactorySettings()
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeInv = sale.invoiceNumber.ifBlank { "INV" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(dir, "invoice_$safeInv.pdf")
            val doc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            // 1. Left-Template Deep Navy + Gold Wave Header
            drawLeftInvoiceHeader(canvas, settings)

            // 2. Metadata Bar
            val metaY = 236f
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val invNo = sale.invoiceNumber.ifBlank { "BIF-0001" }
            val dateText = sale.date.ifBlank { PakistaniFormatters.todayIso() }
            canvas.drawText("Invoice No: $invNo   |   Date: $dateText   |   Currency: PKR (Rs.)", PAGE_W / 2f, metaY, metaPaint)

            // Software Factory Address Line
            val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9.5f
                textAlign = Paint.Align.CENTER
            }
            val addressLine = "${settings.address.ifBlank { "SITE Industrial Area, Karachi" }}  •  Tel: ${settings.phone.ifBlank { "0333-2862498" }}"
            canvas.drawText(addressLine, PAGE_W / 2f, metaY + 16f, addrPaint)

            // 3. Customer Section & Circular Rubber Stamp Seal
            val custY = 285f
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0A3A9E")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val custValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            // Customer icon circle
            val iconCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0A3A9E")
            }
            canvas.drawCircle(52f, custY + 6f, 14f, iconCirclePaint)
            val whiteInitial = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("C", 52f, custY + 11f, whiteInitial)
            canvas.drawText("Customer:", 76f, custY + 3f, labelPaint)
            canvas.drawText(sale.customerName.ifBlank { "Walk-in Customer" }, 76f, custY + 21f, custValuePaint)

            val paidAmt = if (sale.paid) sale.total else sale.paidAmount.coerceIn(0.0, sale.total)
            val stampText = when {
                sale.paid -> "PAID"
                paidAmt > 0.0 -> "PARTIAL"
                else -> "CREDIT"
            }
            val stampColor = when {
                sale.paid -> Color.parseColor("#15803D")
                paidAmt > 0.0 -> Color.parseColor("#C2410C")
                else -> Color.parseColor("#B91C1C")
            }
            drawCircularSealStamp(canvas, 492f, custY + 14f, 40f, stampText, stampColor)

            // 4. Table Header & Line Item Row
            val tableLeft = 36f
            val tableRight = PAGE_W - 36f
            val tableTop = 342f
            val headerH = 34f
            val rowH = 38f

            val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#063682")
            }
            canvas.drawRoundRect(RectF(tableLeft, tableTop, tableRight, tableTop + headerH), 6f, 6f, thPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#3B82F6")
                style = Paint.Style.STROKE
                strokeWidth = 1.1f
            }
            val cols = floatArrayOf(tableLeft, tableLeft + 36f, tableLeft + 240f, tableLeft + 318f, tableLeft + 415f, tableRight)
            val headers = listOf("#", "Product", "Quantity", "Unit Price", "Total Amount")
            val thTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            for (i in headers.indices) {
                val tx = if (i == 1) cols[i] + 10f else (cols[i] + cols[i + 1]) / 2f
                thTextPaint.textAlign = if (i == 1) Paint.Align.LEFT else Paint.Align.CENTER
                canvas.drawText(headers[i], tx, tableTop + 21f, thTextPaint)
            }

            // Row box
            val rowRect = RectF(tableLeft, tableTop + headerH, tableRight, tableTop + headerH + rowH)
            canvas.drawRect(rowRect, borderPaint)
            for (i in 1 until cols.size - 1) {
                canvas.drawLine(cols[i], tableTop, cols[i], tableTop + headerH + rowH, borderPaint)
            }

            val tdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rowVals = listOf(
                "1",
                sale.product,
                sale.quantity.toString(),
                PakistaniFormatters.formatPkr(sale.unitPrice),
                PakistaniFormatters.formatPkr(sale.total)
            )
            val rowBaseline = tableTop + headerH + 23f
            for (i in rowVals.indices) {
                val tx = if (i == 1) cols[i] + 10f else (cols[i] + cols[i + 1]) / 2f
                tdPaint.textAlign = if (i == 1) Paint.Align.LEFT else Paint.Align.CENTER
                canvas.drawText(ellipsize(rowVals[i], 28), tx, rowBaseline, tdPaint)
            }

            // 5. Sky-Blue Summary Pills
            var pillY = tableTop + headerH + rowH + 20f
            drawSummaryPill(canvas, 250f, tableRight, pillY, "Total Amount:", PakistaniFormatters.formatPkr(sale.total))
            pillY += 32f
            drawSummaryPill(canvas, 250f, tableRight, pillY, "Paid Amount:", PakistaniFormatters.formatPkr(paidAmt))
            val dueAmt = (sale.total - paidAmt).coerceAtLeast(0.0)
            if (dueAmt > 0.0) {
                pillY += 32f
                drawSummaryPill(canvas, 250f, tableRight, pillY, "Balance Due:", PakistaniFormatters.formatPkr(dueAmt), Color.parseColor("#FEE2E2"), Color.parseColor("#B91C1C"))
            }

            // 6. Dark Navy "Amount in Words" Banner
            val wordsTop = pillY + 46f
            val wordsRect = RectF(tableLeft, wordsTop, tableRight, wordsTop + 50f)
            val wordsBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#002466")
            }
            canvas.drawRoundRect(wordsRect, 8f, 8f, wordsBgPaint)
            val wordsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CBD5E1")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val wordsValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Amount in Words:", tableLeft + 18f, wordsTop + 19f, wordsLabelPaint)
            canvas.drawText(ellipsize(PakistaniFormatters.amountInWords(sale.total), 68), tableLeft + 18f, wordsTop + 37f, wordsValPaint)

            // 7. Footer: Prepared By | QR Code | Thank You + Bottom Navy/Gold Wave
            val qrPayload = "BROHI-INVOICE|$invNo|CUST:${sale.customerName}|TOTAL:${sale.total.toLong()}|STATUS:$stampText|DATE:$dateText"
            val qrBitmap = ImageAndQrUtils.generateQrCodeBitmap(qrPayload, 220)
            drawDocumentFooter(canvas, settings, qrBitmap, isLeftGoldTheme = true, footerNote = "For Your Business")

            doc.finishPage(page)
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            shareFile(context, file, "application/pdf", "Sales Invoice $invNo")
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a styled Right-Side Design PDF (Icy Sky Blue + Deep Blue Wave Banner + Circular Stamp + QR Code)
     * for Customer/Credit Statements and Full Financial/Transaction Statements. Supports multi-page output.
     */
    fun exportAndShareStatementPdf(
        context: Context,
        statement: StatementDocumentModel
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val safeNum = statement.statementNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val file = File(dir, "statement_$safeNum.pdf")
            val doc = PdfDocument()

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            var page = doc.startPage(pageInfo)
            var canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // 1. Right-Template Icy Sky Blue + Deep Blue Wave Header
            drawRightStatementHeader(canvas, statement.title, statement.slogan, statement.settings)

            // 2. Metadata Strip
            val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Statement No: ${statement.statementNumber}   |   Date: ${statement.dateDisplay}   |   Currency: PKR (Rs.)",
                PAGE_W / 2f,
                228f,
                metaPaint
            )

            val addrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9.2f
                textAlign = Paint.Align.CENTER
            }
            val addr = "${statement.settings.address.ifBlank { "SITE Industrial Area, Karachi" }}  •  Tel: ${statement.settings.phone.ifBlank { "0333-2862498" }}"
            canvas.drawText(addr, PAGE_W / 2f, 244f, addrPaint)

            // 3. Party / Scope Info + Circular Stamp
            val partyY = 270f
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0A3A9E")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#475569")
                textSize = 9.5f
            }
            canvas.drawText(statement.partyLabel, 36f, partyY, labelPaint)
            canvas.drawText(ellipsize(statement.partyName, 44), 36f, partyY + 17f, valPaint)
            canvas.drawText(ellipsize(statement.partySubline, 62), 36f, partyY + 32f, subPaint)

            val stampColor = if (statement.stampPositive) Color.parseColor("#15803D") else Color.parseColor("#B91C1C")
            drawCircularSealStamp(canvas, 505f, partyY + 14f, 36f, statement.stampLabel, stampColor)

            // 4. Table Header
            val tableLeft = 32f
            val tableRight = PAGE_W - 32f
            val cols = floatArrayOf(tableLeft, tableLeft + 26f, tableLeft + 92f, tableLeft + 186f, tableLeft + 366f, tableLeft + 448f, tableRight)
            val headers = listOf("#", statement.colHeader1, statement.colHeader2, statement.colHeader3, statement.colHeader4, statement.colHeader5)

            var y = 322f
            drawStatementTableHeader(canvas, tableLeft, tableRight, y, cols, headers)
            y += 28f

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#60A5FA")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }
            val tdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 9.5f
            }
            val altRowPaint = Paint().apply {
                color = Color.parseColor("#F5FAFF")
            }

            val rowsToPrint = statement.rows
            if (rowsToPrint.isEmpty()) {
                canvas.drawRect(tableLeft, y, tableRight, y + 28f, borderPaint)
                tdPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("No recorded entries match this statement.", PAGE_W / 2f, y + 18f, tdPaint)
                y += 34f
            } else {
                val rowHeight = 24f
                for ((idx, item) in rowsToPrint.withIndex()) {
                    if (y > 748f) {
                        doc.finishPage(page)
                        pageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                        page = doc.startPage(pageInfo)
                        canvas = page.canvas
                        canvas.drawColor(Color.WHITE)

                        val contPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.parseColor("#0A3A9E")
                            textSize = 11f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                        canvas.drawText("BROHI ICE FACTORY — ${statement.title} (Page $pageNum)", tableLeft, 36f, contPaint)
                        y = 50f
                        drawStatementTableHeader(canvas, tableLeft, tableRight, y, cols, headers)
                        y += 28f
                    }
                    if (idx % 2 == 1) {
                        canvas.drawRect(tableLeft, y, tableRight, y + rowHeight, altRowPaint)
                    }
                    canvas.drawRect(tableLeft, y, tableRight, y + rowHeight, borderPaint)
                    for (c in 1 until cols.size - 1) {
                        canvas.drawLine(cols[c], y, cols[c], y + rowHeight, borderPaint)
                    }
                    val rowTexts = listOf(
                        item.index.toString(),
                        ellipsize(item.date, 11),
                        ellipsize(item.referenceOrCategory, 16),
                        ellipsize(item.description, 31),
                        if (item.primaryAmount > 0.0) PakistaniFormatters.formatPkr(item.primaryAmount) else "-",
                        if (item.secondaryAmount > 0.0) PakistaniFormatters.formatPkr(item.secondaryAmount) else "-"
                    )
                    val baseline = y + 16f
                    for (c in rowTexts.indices) {
                        tdPaint.typeface = if (c >= 4) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                        val tx = when {
                            c == 0 -> (cols[c] + cols[c + 1]) / 2f
                            c >= 4 -> cols[c + 1] - 6f
                            else -> cols[c] + 5f
                        }
                        tdPaint.textAlign = when {
                            c == 0 -> Paint.Align.CENTER
                            c >= 4 -> Paint.Align.RIGHT
                            else -> Paint.Align.LEFT
                        }
                        canvas.drawText(rowTexts[c], tx, baseline, tdPaint)
                    }
                    y += rowHeight
                }
            }

            // Ensure room for summary pills + Amount in Words + Footer (~275f height needed)
            if (y > 540f) {
                doc.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                y = 44f
            } else {
                y += 14f
            }

            // 5. Summary Pills
            drawSummaryPill(canvas, 255f, tableRight, y, statement.summaryPrimaryLabel, PakistaniFormatters.formatPkr(statement.summaryPrimaryValue))
            y += 29f
            drawSummaryPill(canvas, 255f, tableRight, y, statement.summarySecondaryLabel, PakistaniFormatters.formatPkr(statement.summarySecondaryValue))
            y += 29f
            val netBg = if (statement.netAmount >= 0) Color.parseColor("#DBEAFE") else Color.parseColor("#FEE2E2")
            val netFg = if (statement.netAmount >= 0) Color.parseColor("#002466") else Color.parseColor("#B91C1C")
            drawSummaryPill(canvas, 255f, tableRight, y, statement.netLabel, PakistaniFormatters.formatPkr(statement.netAmount), netBg, netFg)
            y += 40f

            // 6. Blue-to-Cyan Gradient "Amount in Words" Banner
            val wordsRect = RectF(tableLeft, y, tableRight, y + 48f)
            val gradPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    wordsRect.left,
                    wordsRect.top,
                    wordsRect.right,
                    wordsRect.top,
                    Color.parseColor("#002466"),
                    Color.parseColor("#1D9BF0"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(wordsRect, 8f, 8f, gradPaint)
            val wLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#DCE8FF")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val wValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Amount in Words:", tableLeft + 16f, y + 18f, wLabelPaint)
            canvas.drawText(ellipsize(statement.amountInWords, 70), tableLeft + 16f, y + 36f, wValPaint)

            // 7. Footer: Prepared By | QR Code | Thank You + Bottom Blue-Cyan Wave
            val qrBitmap = ImageAndQrUtils.generateQrCodeBitmap(statement.qrPayload, 220)
            drawDocumentFooter(canvas, statement.settings, qrBitmap, isLeftGoldTheme = false, footerNote = "Authorized Statement")

            doc.finishPage(page)
            FileOutputStream(file).use { doc.writeTo(it) }
            doc.close()

            shareFile(context, file, "application/pdf", statement.title)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun drawLeftInvoiceHeader(canvas: Canvas, settings: FactorySettings) {
        // Gold swoosh lower ribbon
        val goldRibbon = Path().apply {
            moveTo(0f, 0f)
            lineTo(PAGE_W, 0f)
            lineTo(PAGE_W, 178f)
            cubicTo(PAGE_W * 0.65f, 132f, PAGE_W * 0.25f, 225f, 0f, 176f)
            close()
        }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 120f, PAGE_W, 200f,
                intArrayOf(
                    Color.parseColor("#C58E24"),
                    Color.parseColor("#F5D061"),
                    Color.parseColor("#D49C2B")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(goldRibbon, goldPaint)

        // Deep Navy header wave
        val navyWave = Path().apply {
            moveTo(0f, 0f)
            lineTo(PAGE_W, 0f)
            lineTo(PAGE_W, 152f)
            cubicTo(PAGE_W * 0.68f, 110f, PAGE_W * 0.28f, 208f, 0f, 150f)
            close()
        }
        val navyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, PAGE_W, 170f,
                Color.parseColor("#001A4D"),
                Color.parseColor("#0849B2"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(navyWave, navyPaint)

        // Mountain & Snowflake crest
        drawMountainSnowflakePdf(canvas, 96f, 44f, 68f, Color.WHITE)

        // Title: BROHI ICE FACTORY
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText("BROHI", 96f, 108f, brandPaint)
        canvas.drawText("ICE FACTORY", 96f, 126f, subBrandPaint)

        // Gold Slogan on Right
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5D061")
            textSize = 18f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Pure Ice... Cool Business...", PAGE_W - 28f, 56f, sloganPaint)

        // Decorative ice cubes cluster outline on right
        drawIceCubeClusterPdf(canvas, PAGE_W - 115f, 82f, Color.parseColor("#93C5FD"))

        // Center Gold Hexagon Banner: SALES INVOICE
        val bx = PAGE_W / 2f
        val by = 186f
        val bw = 132f
        val bh = 21f
        val hex = Path().apply {
            moveTo(bx - bw, by)
            lineTo(bx - bw + 16f, by - bh)
            lineTo(bx + bw - 16f, by - bh)
            lineTo(bx + bw, by)
            lineTo(bx + bw - 16f, by + bh)
            lineTo(bx - bw + 16f, by + bh)
            close()
        }
        val hexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                bx - bw, by - bh, bx + bw, by + bh,
                Color.parseColor("#E3AE36"),
                Color.parseColor("#F8DC7B"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(hex, hexPaint)
        val hexTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#001C55")
            textSize = 17.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SALES INVOICE", bx, by + 6f, hexTextPaint)
    }

    private fun drawRightStatementHeader(canvas: Canvas, title: String, slogan: String, settings: FactorySettings) {
        // Icy sky-blue frosted header backdrop
        val skyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, PAGE_W, 180f,
                Color.parseColor("#DDF0FF"),
                Color.parseColor("#F4FAFF"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, PAGE_W, 168f, skyPaint)

        // Top cyan swoosh accent
        val cyanArc = Path().apply {
            moveTo(0f, 0f)
            lineTo(PAGE_W * 0.48f, 0f)
            cubicTo(PAGE_W * 0.25f, 24f, 60f, 70f, 0f, 128f)
            close()
        }
        canvas.drawPath(cyanArc, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BBE2FF") })

        // Navy crest & title on left
        val navyColor = Color.parseColor("#0A347E")
        drawMountainSnowflakePdf(canvas, 96f, 38f, 64f, navyColor)
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BROHI", 96f, 98f, brandPaint)
        canvas.drawText("ICE FACTORY", 96f, 117f, subPaint)

        // Right Slogan + ice cube art
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A347E")
            textSize = 17f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(slogan, PAGE_W - 28f, 52f, sloganPaint)
        drawIceCubeClusterPdf(canvas, PAGE_W - 115f, 82f, Color.parseColor("#0284C7"))

        // Flowing Blue-to-Cyan Banner Ribbon across bottom of header
        val ribbonWave = Path().apply {
            moveTo(0f, 152f)
            cubicTo(PAGE_W * 0.30f, 124f, PAGE_W * 0.68f, 198f, PAGE_W, 146f)
            lineTo(PAGE_W, 194f)
            cubicTo(PAGE_W * 0.66f, 212f, PAGE_W * 0.32f, 162f, 0f, 194f)
            close()
        }
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 140f, PAGE_W, 200f,
                Color.parseColor("#002B78"),
                Color.parseColor("#1D9BF0"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(ribbonWave, ribbonPaint)

        // Center pill for Title
        val titleBgRect = RectF(108f, 154f, PAGE_W - 108f, 196f)
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                titleBgRect.left, titleBgRect.top, titleBgRect.right, titleBgRect.top,
                Color.parseColor("#00266E"),
                Color.parseColor("#0959BA"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(titleBgRect, 14f, 14f, pillPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, PAGE_W / 2f, 181f, titlePaint)
    }

    private fun drawCircularSealStamp(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        label: String,
        stampColor: Int
    ) {
        canvas.save()
        canvas.rotate(-14f, cx, cy)

        val outerRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val innerDashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
        }
        val fillTint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            alpha = 18
            style = Paint.Style.FILL
        }

        canvas.drawCircle(cx, cy, radius, fillTint)
        canvas.drawCircle(cx, cy, radius, outerRing)
        canvas.drawCircle(cx, cy, radius - 6f, innerDashed)

        val topMini = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            textSize = 6.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("★ BROHI ICE ★", cx, cy - radius * 0.42f, topMini)
        canvas.drawText("VERIFIED", cx, cy + radius * 0.56f, topMini)

        val mainText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stampColor
            textSize = if (label.length <= 5) 14.5f else 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, cx, cy + 5f, mainText)

        canvas.restore()
    }

    private fun drawStatementTableHeader(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        cols: FloatArray,
        headers: List<String>
    ) {
        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#063682") }
        canvas.drawRoundRect(RectF(left, top, right, top + 28f), 6f, 6f, thPaint)
        val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        for (i in headers.indices) {
            val tx = when {
                i == 0 -> (cols[i] + cols[i + 1]) / 2f
                i >= 4 -> cols[i + 1] - 6f
                else -> cols[i] + 5f
            }
            thText.textAlign = when {
                i == 0 -> Paint.Align.CENTER
                i >= 4 -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }
            canvas.drawText(headers[i], tx, top + 18f, thText)
        }
    }

    private fun drawSummaryPill(
        canvas: Canvas,
        left: Float,
        right: Float,
        top: Float,
        label: String,
        value: String,
        bgColor: Int = Color.parseColor("#B6DBFA"),
        textColor: Int = Color.parseColor("#001C55")
    ) {
        val rect = RectF(left, top, right, top + 24f)
        canvas.drawRoundRect(rect, 7f, 7f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor })
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val vp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(label, left + 12f, top + 16f, lp)
        canvas.drawText(value, right - 12f, top + 16f, vp)
    }

    private fun drawDocumentFooter(
        canvas: Canvas,
        settings: FactorySettings,
        qrBitmap: Bitmap?,
        isLeftGoldTheme: Boolean,
        footerNote: String
    ) {
        val footerTop = PAGE_H - 154f

        val prepBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A347E")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val prepSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E3A70")
            textSize = 9.5f
        }
        canvas.drawText("Prepared By:", 34f, footerTop + 26f, prepBold)
        canvas.drawText(settings.companyName.ifBlank { "Brohi Ice Factory" }, 34f, footerTop + 40f, prepSub)
        canvas.drawText("Management System", 34f, footerTop + 53f, prepSub)

        if (qrBitmap != null) {
            val qrSize = 68f
            val qrLeft = PAGE_W / 2f - qrSize / 2f
            canvas.drawBitmap(qrBitmap, null, RectF(qrLeft, footerTop + 8f, qrLeft + qrSize, footerTop + 8f + qrSize), null)
            val scanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D2149")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Scan to Verify", PAGE_W / 2f, footerTop + 88f, scanPaint)
        }

        val thankPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A347E")
            textSize = 21f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            textAlign = Paint.Align.RIGHT
        }
        val thankSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0A347E")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Thank You!", PAGE_W - 34f, footerTop + 40f, thankPaint)
        canvas.drawText(footerNote, PAGE_W - 34f, footerTop + 58f, thankSub)

        // Bottom Accent Wave
        val waveBack = Path().apply {
            moveTo(0f, PAGE_H - 46f)
            cubicTo(PAGE_W * 0.35f, PAGE_H - 76f, PAGE_W * 0.65f, PAGE_H - 18f, PAGE_W, PAGE_H - 52f)
            lineTo(PAGE_W, PAGE_H)
            lineTo(0f, PAGE_H)
            close()
        }
        val waveFront = Path().apply {
            moveTo(0f, PAGE_H - 34f)
            cubicTo(PAGE_W * 0.35f, PAGE_H - 64f, PAGE_W * 0.65f, PAGE_H - 8f, PAGE_W, PAGE_H - 40f)
            lineTo(PAGE_W, PAGE_H)
            lineTo(0f, PAGE_H)
            close()
        }
        if (isLeftGoldTheme) {
            canvas.drawPath(waveBack, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E3AE36") })
            canvas.drawPath(waveFront, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#001C55") })
        } else {
            canvas.drawPath(waveBack, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#38BDF8") })
            canvas.drawPath(waveFront, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#06429E") })
        }
    }

    private fun drawMountainSnowflakePdf(canvas: Canvas, cx: Float, topY: Float, size: Float, color: Int) {
        val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val left = cx - size / 2f
        val path = Path().apply {
            moveTo(left + size * 0.05f, topY + size * 0.55f)
            lineTo(left + size * 0.35f, topY + size * 0.08f)
            lineTo(left + size * 0.56f, topY + size * 0.36f)
            lineTo(left + size * 0.72f, topY + size * 0.16f)
            lineTo(left + size * 0.95f, topY + size * 0.55f)
        }
        canvas.drawPath(path, mPaint)
    }

    private fun drawIceCubeClusterPdf(canvas: Canvas, startX: Float, startY: Float, strokeColor: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            alpha = 110
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            alpha = 32
            style = Paint.Style.FILL
        }
        val boxes = listOf(
            RectF(startX, startY, startX + 26f, startY + 26f),
            RectF(startX + 22f, startY - 12f, startX + 50f, startY + 16f),
            RectF(startX + 46f, startY + 8f, startX + 74f, startY + 36f)
        )
        boxes.forEach { r ->
            canvas.drawRoundRect(r, 4f, 4f, fill)
            canvas.drawRoundRect(r, 4f, 4f, p)
        }
    }

    private fun ellipsize(text: String, maxChars: Int): String {
        val clean = text.trim()
        return if (clean.length <= maxChars) clean else "${clean.take((maxChars - 1).coerceAtLeast(1))}…"
    }

    fun exportAndShareCsv(
        context: Context,
        fileName: String,
        headers: List<String>,
        rows: List<List<String>>
    ): Result<File> {
        return try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, "$fileName.csv")
            val sb = StringBuilder()
            sb.appendLine(headers.joinToString(",") { escapeCsv(it) })
            for (row in rows) {
                sb.appendLine(row.joinToString(",") { escapeCsv(it) })
            }
            file.writeText(sb.toString(), Charsets.UTF_8)
            shareFile(context, file, "text/csv", fileName)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
