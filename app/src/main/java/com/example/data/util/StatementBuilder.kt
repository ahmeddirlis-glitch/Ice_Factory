package com.example.data.util

import com.example.data.model.Customer
import com.example.data.model.Expense
import com.example.data.model.FactorySettings
import com.example.data.model.FundingEntry
import com.example.data.model.OwnerAccountingSummary
import com.example.data.model.PayrollRecord
import com.example.data.model.Sale
import com.example.data.model.SupplierItem
import com.example.data.model.UtilityReading
import kotlin.math.abs

enum class StatementKind {
    CUSTOMER_CREDIT,
    ALL_CREDIT_CUSTOMERS,
    FINANCIAL_LEDGER
}

enum class StatementFilterGroup(val label: String) {
    ALL("All Entries"),
    REVENUE("Sales & Revenue"),
    EXPENSES_UTILITIES("Expenses & Utilities"),
    FUNDING_PAYROLL("Funding & Payroll")
}

data class StatementRowItem(
    val index: Int,
    val date: String,
    val referenceOrCategory: String,
    val description: String,
    val detailBadge: String,
    val primaryAmount: Double,
    val secondaryAmount: Double,
    val group: StatementFilterGroup = StatementFilterGroup.ALL
)

data class StatementDocumentModel(
    val kind: StatementKind,
    val statementNumber: String,
    val title: String,
    val slogan: String,
    val partyLabel: String,
    val partyName: String,
    val partySubline: String,
    val dateDisplay: String,
    val stampLabel: String,
    val stampPositive: Boolean,
    val colHeader1: String,
    val colHeader2: String,
    val colHeader3: String,
    val colHeader4: String,
    val colHeader5: String,
    val rows: List<StatementRowItem>,
    val summaryPrimaryLabel: String,
    val summaryPrimaryValue: Double,
    val summarySecondaryLabel: String,
    val summarySecondaryValue: Double,
    val netLabel: String,
    val netAmount: Double,
    val amountInWords: String,
    val extraMetrics: List<Pair<String, String>>,
    val qrPayload: String,
    val settings: FactorySettings
)

object StatementBuilder {

    fun buildCustomerStatement(
        customer: Customer,
        sales: List<Sale>,
        settings: FactorySettings
    ): StatementDocumentModel {
        val matchedSales = sales.filter {
            (it.customerId.isNotBlank() && it.customerId == customer.id) ||
                it.customerName.equals(customer.name, ignoreCase = true)
        }.sortedBy { it.date }

        val rows = mutableListOf<StatementRowItem>()
        var totalBilledFromSales = 0.0
        var totalPaidFromSales = 0.0

        matchedSales.forEach { sale ->
            val paidAmt = if (sale.paid) sale.total else sale.paidAmount.coerceIn(0.0, sale.total)
            totalBilledFromSales += sale.total
            totalPaidFromSales += paidAmt
            rows += StatementRowItem(
                index = rows.size + 1,
                date = sale.date.ifBlank { PakistaniFormatters.todayIso() },
                referenceOrCategory = sale.invoiceNumber.ifBlank { "INV" },
                description = "${sale.product} (x${sale.quantity})",
                detailBadge = if (sale.paid) "Paid" else "Credit",
                primaryAmount = sale.total,
                secondaryAmount = paidAmt,
                group = StatementFilterGroup.REVENUE
            )
        }

        val netFromInvoices = totalBilledFromSales - totalPaidFromSales
        val ledgerDelta = customer.balance - netFromInvoices
        var totalBilled = totalBilledFromSales
        var totalPaid = totalPaidFromSales

        if (ledgerDelta > 1.0) {
            totalBilled += ledgerDelta
            rows += StatementRowItem(
                index = 0,
                date = PakistaniFormatters.todayIso(),
                referenceOrCategory = "OPEN-BAL",
                description = "Opening / Carried Credit Balance",
                detailBadge = "Opening",
                primaryAmount = ledgerDelta,
                secondaryAmount = 0.0
            )
        } else if (ledgerDelta < -1.0) {
            val recovered = abs(ledgerDelta)
            totalPaid += recovered
            rows += StatementRowItem(
                index = rows.size + 1,
                date = PakistaniFormatters.todayIso(),
                referenceOrCategory = "PMT-RCV",
                description = "Credit Payment Received / Recovery",
                detailBadge = "Credit",
                primaryAmount = 0.0,
                secondaryAmount = recovered
            )
        }

        val orderedRows = rows.sortedBy { it.date }.mapIndexed { idx, row -> row.copy(index = idx + 1) }
        val cleanName = PakistaniFormatters.formatTitleCase(customer.name).ifBlank { "Credit Customer" }
        val codeSuffix = customer.id.filter { it.isLetterOrDigit() }.takeLast(4).uppercase().ifBlank { "1001" }
        val statementNo = "BIF-CS-$codeSuffix"
        val isCleared = customer.balance <= 0.0

        return StatementDocumentModel(
            kind = StatementKind.CUSTOMER_CREDIT,
            statementNumber = statementNo,
            title = "CUSTOMER STATEMENT",
            slogan = "Keeping You Cool Always...",
            partyLabel = "Customer:",
            partyName = cleanName,
            partySubline = listOfNotNull(
                customer.phone.takeIf { it.isNotBlank() },
                customer.address?.takeIf { it.isNotBlank() }
            ).joinToString(" • ").ifBlank { "Registered Credit Account" },
            dateDisplay = PakistaniFormatters.todayIso(),
            stampLabel = if (isCleared) "PAID" else "CREDIT DUE",
            stampPositive = isCleared,
            colHeader1 = "Date",
            colHeader2 = "Invoice / Ref",
            colHeader3 = "Particulars",
            colHeader4 = "Billed",
            colHeader5 = "Paid",
            rows = orderedRows,
            summaryPrimaryLabel = "Total Billed:",
            summaryPrimaryValue = totalBilled,
            summarySecondaryLabel = "Total Paid:",
            summarySecondaryValue = totalPaid,
            netLabel = "Balance Due:",
            netAmount = customer.balance,
            amountInWords = PakistaniFormatters.amountInWords(customer.balance),
            extraMetrics = listOf(
                "Total Invoices" to "${matchedSales.size}",
                "Current Due" to PakistaniFormatters.formatPkr(customer.balance)
            ),
            qrPayload = "BROHI-STATEMENT|$statementNo|CUST:$cleanName|DUE:${customer.balance.toLong()}|DATE:${PakistaniFormatters.todayIso()}",
            settings = settings
        )
    }

    fun buildAllCreditCustomersStatement(
        customers: List<Customer>,
        sales: List<Sale>,
        settings: FactorySettings
    ): StatementDocumentModel {
        val creditCustomers = customers.sortedByDescending { it.balance }
        val rows = creditCustomers.mapIndexed { idx, c ->
            val custSalesTotal = sales.filter {
                (it.customerId.isNotBlank() && it.customerId == c.id) ||
                    it.customerName.equals(c.name, ignoreCase = true)
            }.sumOf { it.total }
            val billed = maxOf(custSalesTotal, c.balance)
            val paid = (billed - c.balance).coerceAtLeast(0.0)
            StatementRowItem(
                index = idx + 1,
                date = c.phone.ifBlank { "-" },
                referenceOrCategory = PakistaniFormatters.formatTitleCase(c.name),
                description = c.address?.takeIf { it.isNotBlank() } ?: "Credit Ledger",
                detailBadge = if (c.balance > 0) "Due" else "Clear",
                primaryAmount = billed,
                secondaryAmount = c.balance
            )
        }
        val totalDue = creditCustomers.sumOf { it.balance }
        val totalBilledAll = rows.sumOf { it.primaryAmount }
        val statementNo = "BIF-CR-${PakistaniFormatters.todayIso().replace("-", "").takeLast(4)}"

        return StatementDocumentModel(
            kind = StatementKind.ALL_CREDIT_CUSTOMERS,
            statementNumber = statementNo,
            title = "CREDIT LEDGER STATEMENT",
            slogan = "Keeping You Cool Always...",
            partyLabel = "Ledger Scope:",
            partyName = "All Credit & Receivable Accounts",
            partySubline = "${creditCustomers.count { it.balance > 0 }} Active Credit Customers • Total ${creditCustomers.size} Accounts",
            dateDisplay = PakistaniFormatters.todayIso(),
            stampLabel = if (totalDue <= 0.0) "SETTLED" else "RECEIVABLE",
            stampPositive = totalDue <= 0.0,
            colHeader1 = "Phone",
            colHeader2 = "Customer",
            colHeader3 = "Location / Note",
            colHeader4 = "Billed",
            colHeader5 = "Due Bal.",
            rows = rows,
            summaryPrimaryLabel = "Total Sales / Billed:",
            summaryPrimaryValue = totalBilledAll,
            summarySecondaryLabel = "Total Recovered:",
            summarySecondaryValue = (totalBilledAll - totalDue).coerceAtLeast(0.0),
            netLabel = "Total Credit Due:",
            netAmount = totalDue,
            amountInWords = PakistaniFormatters.amountInWords(totalDue),
            extraMetrics = listOf(
                "Active Udhar" to "${creditCustomers.count { it.balance > 0 }} Accounts",
                "Total Pending" to PakistaniFormatters.formatPkr(totalDue)
            ),
            qrPayload = "BROHI-CREDIT-LEDGER|$statementNo|ACCOUNTS:${creditCustomers.size}|DUE:${totalDue.toLong()}",
            settings = settings
        )
    }

    fun buildFinancialStatement(
        sales: List<Sale>,
        expenses: List<Expense>,
        fundingList: List<FundingEntry>,
        utilities: List<UtilityReading>,
        payroll: List<PayrollRecord>,
        suppliers: List<SupplierItem>,
        summary: OwnerAccountingSummary,
        settings: FactorySettings,
        filterGroup: StatementFilterGroup = StatementFilterGroup.ALL
    ): StatementDocumentModel {
        val allRows = mutableListOf<StatementRowItem>()

        // 1. Sales / Revenue Entries
        sales.forEach { s ->
            val statusText = if (s.paid) "Cash Paid" else "Credit Udhar"
            allRows += StatementRowItem(
                index = 0,
                date = s.date.ifBlank { PakistaniFormatters.todayIso() },
                referenceOrCategory = "Revenue • ${s.invoiceNumber.ifBlank { "Sale" }}",
                description = "${s.customerName} — ${s.product} x${s.quantity} ($statusText)",
                detailBadge = if (s.paid) "Inflow" else "Credit",
                primaryAmount = s.total,
                secondaryAmount = 0.0,
                group = StatementFilterGroup.REVENUE
            )
        }

        // 2. Operating Expenses & Capital / Build Investments
        expenses.forEach { e ->
            allRows += StatementRowItem(
                index = 0,
                date = e.date.ifBlank { PakistaniFormatters.todayIso() },
                referenceOrCategory = if (e.capitalInvestment) "Capital Build" else "Exp: ${e.category}",
                description = e.description.ifBlank { e.category },
                detailBadge = if (e.capitalInvestment) "Build" else "Expense",
                primaryAmount = 0.0,
                secondaryAmount = e.amount,
                group = StatementFilterGroup.EXPENSES_UTILITIES
            )
        }

        // 3. Utility Readings (Electricity / Diesel / Water)
        utilities.forEach { u ->
            if (u.amount > 0.0) {
                allRows += StatementRowItem(
                    index = 0,
                    date = u.date.ifBlank { PakistaniFormatters.todayIso() },
                    referenceOrCategory = "Utility: ${u.type}",
                    description = "${u.unitsOrLiters.toLong()} units/L${u.notes?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""}",
                    detailBadge = "Utility",
                    primaryAmount = 0.0,
                    secondaryAmount = u.amount,
                    group = StatementFilterGroup.EXPENSES_UTILITIES
                )
            }
        }

        // 4. Funding & Loans Raised + Repayments
        fundingList.forEach { f ->
            allRows += StatementRowItem(
                index = 0,
                date = f.date.ifBlank { PakistaniFormatters.todayIso() },
                referenceOrCategory = "Funding: ${f.type}",
                description = "${f.name} (${f.source})",
                detailBadge = "Capital In",
                primaryAmount = f.amount,
                secondaryAmount = f.repaid,
                group = StatementFilterGroup.FUNDING_PAYROLL
            )
        }

        // 5. Payroll / Salary Records
        payroll.forEach { p ->
            if (p.netPay > 0.0) {
                allRows += StatementRowItem(
                    index = 0,
                    date = p.paidDate?.takeIf { it.isNotBlank() } ?: "${p.month}-01",
                    referenceOrCategory = "Salary • ${p.month}",
                    description = "${p.employeeName} (${if (p.paid) "Paid" else "Pending"})",
                    detailBadge = if (p.paid) "Payroll" else "Pending",
                    primaryAmount = 0.0,
                    secondaryAmount = p.netPay,
                    group = StatementFilterGroup.FUNDING_PAYROLL
                )
            }
        }

        // 6. Supplier Accounts & Dues
        suppliers.forEach { sup ->
            if (sup.totalPurchases > 0.0 || sup.dues > 0.0) {
                allRows += StatementRowItem(
                    index = 0,
                    date = sup.lastPaymentDate?.takeIf { it.isNotBlank() } ?: PakistaniFormatters.todayIso(),
                    referenceOrCategory = "Supplier",
                    description = "${sup.name} (${sup.itemSupplied}) • Due: ${PakistaniFormatters.formatPkr(sup.dues)}",
                    detailBadge = "Supplier",
                    primaryAmount = 0.0,
                    secondaryAmount = sup.totalPurchases,
                    group = StatementFilterGroup.EXPENSES_UTILITIES
                )
            }
        }

        val filtered = when (filterGroup) {
            StatementFilterGroup.ALL -> allRows
            else -> allRows.filter { it.group == filterGroup }
        }
            .sortedByDescending { it.date }
            .mapIndexed { idx, item -> item.copy(index = idx + 1) }

        val totalRevenue = sales.sumOf { it.total }
        val totalOperatingExp = expenses.filter { !it.capitalInvestment }.sumOf { it.amount }
        val totalCapitalBuild = expenses.filter { it.capitalInvestment }.sumOf { it.amount }
        val totalUtilitiesRecorded = utilities.sumOf { it.amount }
        val totalPayrollPaid = payroll.filter { it.paid }.sumOf { it.netPay }
        val totalInflowFiltered = filtered.sumOf { it.primaryAmount }
        val totalOutflowFiltered = filtered.sumOf { it.secondaryAmount }

        val displayInflow = if (filterGroup == StatementFilterGroup.ALL) totalRevenue else totalInflowFiltered
        val displayOutflow = if (filterGroup == StatementFilterGroup.ALL) totalOperatingExp else totalOutflowFiltered
        val displayNet = if (filterGroup == StatementFilterGroup.ALL) summary.regularProfitSoFar else (totalInflowFiltered - totalOutflowFiltered)
        val statementNo = "BIF-FIN-${PakistaniFormatters.todayIso().replace("-", "").takeLast(4)}"

        return StatementDocumentModel(
            kind = StatementKind.FINANCIAL_LEDGER,
            statementNumber = statementNo,
            title = "FINANCIAL STATEMENT",
            slogan = "Keeping You Cool Always...",
            partyLabel = "Statement Scope:",
            partyName = "${settings.companyName.ifBlank { "Brohi Ice Factory" }} — ${filterGroup.label}",
            partySubline = "Revenue, Expenses, Capital Build, Funding, Utilities & Payroll Transactions (${filtered.size} Entries)",
            dateDisplay = PakistaniFormatters.todayIso(),
            stampLabel = if (displayNet >= 0) "PROFIT" else "DEFICIT",
            stampPositive = displayNet >= 0,
            colHeader1 = "Date",
            colHeader2 = "Category",
            colHeader3 = "Transaction Details",
            colHeader4 = "In (+)",
            colHeader5 = "Out (-)",
            rows = filtered,
            summaryPrimaryLabel = if (filterGroup == StatementFilterGroup.ALL) "Total Sales Revenue:" else "Total Inflow (+):",
            summaryPrimaryValue = displayInflow,
            summarySecondaryLabel = if (filterGroup == StatementFilterGroup.ALL) "Operating Expenses:" else "Total Outflow (-):",
            summarySecondaryValue = displayOutflow,
            netLabel = if (filterGroup == StatementFilterGroup.ALL) "Net Regular Profit:" else "Net Balance:",
            netAmount = displayNet,
            amountInWords = PakistaniFormatters.amountInWords(displayNet),
            extraMetrics = listOf(
                "Funding Raised" to PakistaniFormatters.formatPkr(summary.totalFundingRaised),
                "Still to Repay" to PakistaniFormatters.formatPkr(summary.stillToRepay),
                "Factory Build" to PakistaniFormatters.formatPkr(totalCapitalBuild),
                "Utilities Logged" to PakistaniFormatters.formatPkr(totalUtilitiesRecorded),
                "Salaries Paid" to PakistaniFormatters.formatPkr(totalPayrollPaid),
                "Customer Udhar" to PakistaniFormatters.formatPkr(summary.creditPendingFromCustomers)
            ),
            qrPayload = "BROHI-FIN-STATEMENT|$statementNo|REV:${totalRevenue.toLong()}|EXP:${totalOperatingExp.toLong()}|PROFIT:${summary.regularProfitSoFar.toLong()}",
            settings = settings
        )
    }
}
