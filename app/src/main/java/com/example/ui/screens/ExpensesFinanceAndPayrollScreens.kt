package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.FactorySettings
import com.example.data.model.FundingEntry
import com.example.data.model.OwnerAccountingSummary
import com.example.data.model.PayrollRecord
import com.example.data.model.Sale
import com.example.data.model.SupplierItem
import com.example.data.model.UserRole
import com.example.data.model.UtilityReading
import com.example.data.util.ExportHelper
import com.example.data.util.PakistaniFormatters
import com.example.data.util.StatementBuilder
import com.example.data.util.StatementFilterGroup
import com.example.ui.components.BrohiCard
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.CrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WarningOrangeSoft
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpensesFinanceScreen(
    activeRole: UserRole,
    expenses: List<Expense>,
    fundingList: List<FundingEntry>,
    summary: OwnerAccountingSummary,
    onAddExpense: (description: String, amount: Double, category: String) -> Unit,
    onUpdateExpense: (Expense) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onAddFunding: (source: String, name: String, type: String, amount: Double, interest: Double, dueDate: String?, notes: String?) -> Unit,
    onUpdateFunding: (FundingEntry) -> Unit,
    onDeleteFunding: (String) -> Unit,
    onRepayFunding: (FundingEntry, Double) -> Unit,
    sales: List<Sale> = emptyList(),
    utilities: List<UtilityReading> = emptyList(),
    payroll: List<PayrollRecord> = emptyList(),
    suppliers: List<SupplierItem> = emptyList(),
    settings: FactorySettings = FactorySettings()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(if (activeRole.isOwner) 0 else 1) }
    var showAddExpense by remember { mutableStateOf(false) }
    var editExpenseTarget by remember { mutableStateOf<Expense?>(null) }
    var deleteExpenseTarget by remember { mutableStateOf<Expense?>(null) }
    var showAddFunding by remember { mutableStateOf(false) }
    var editFundingTarget by remember { mutableStateOf<FundingEntry?>(null) }
    var deleteFundingTarget by remember { mutableStateOf<FundingEntry?>(null) }
    var repayTarget by remember { mutableStateOf<FundingEntry?>(null) }
    var showFinancialStatement by remember { mutableStateOf(false) }
    var statementFilter by remember { mutableStateOf(StatementFilterGroup.ALL) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (activeRole.isOwner) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Summary") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Expenses") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Funding") })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeRole.isOwner && selectedTab == 0) {
                // Owner Accounting Summary Card
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = RoyalBlue)
                            Text("Owner Accounting Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        HorizontalDivider()
                        SummaryLine("1. Total Funding Raised", PakistaniFormatters.formatPkr(summary.totalFundingRaised), DeepBlue)
                        SummaryLine("2. Still to Repay", PakistaniFormatters.formatPkr(summary.stillToRepay), WarningOrange)
                        SummaryLine("3. Factory Investment (Build)", PakistaniFormatters.formatPkr(summary.factoryInvestment), DeepBlue)
                        SummaryLine("4. Total Sales So Far", PakistaniFormatters.formatPkr(summary.totalSalesSoFar), RoyalBlue)
                        SummaryLine("5. Credit Pending (Customers)", PakistaniFormatters.formatPkr(summary.creditPendingFromCustomers), DangerRed)
                        HorizontalDivider()
                        SummaryLine(
                            "6. Regular Profit So Far",
                            PakistaniFormatters.formatPkr(summary.regularProfitSoFar),
                            if (summary.regularProfitSoFar >= 0) SuccessGreen else DangerRed
                        )
                    }
                }

                // Break-Even Card
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = SuccessGreen)
                                Text("Break-Even Tracker", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreenSoft) {
                                Text(
                                    "${summary.recoveryPercent.roundToInt()}% Recovered",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (summary.recoveryPercent / 100f).coerceIn(0f, 1f) },
                            color = if (summary.isBreakEvenReached) SuccessGreen else RoyalBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        SummaryLine("Factory Setup Investment", PakistaniFormatters.formatPkr(summary.factoryInvestment), DeepBlue)
                        SummaryLine("Still to Recover", PakistaniFormatters.formatPkr(summary.stillToRecover), WarningOrange)
                        SummaryLine("Avg Monthly Profit", "${PakistaniFormatters.formatPkr(summary.averageMonthlyProfit)} / mo", RoyalBlue)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (summary.isBreakEvenReached) SuccessGreenSoft else Color(0xFFEAF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Estimated Break-Even: ${summary.estimatedCompletionLabel}",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (summary.isBreakEvenReached) Color(0xFF0A5625) else DeepBlue,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Financial & Transaction Statement CTA (Right-Side Blue Wave Design)
                Button(
                    onClick = {
                        statementFilter = StatementFilterGroup.ALL
                        showFinancialStatement = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlueGradient)
                        .testTag("open_financial_statement_button")
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Financial Statement (مالیاتی اسٹیٹمنٹ)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

            if (!activeRole.isOwner || selectedTab == 1) {
                Button(
                    onClick = { showAddExpense = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlueGradient)
                        .testTag("add_expense_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense", color = Color.White, fontWeight = FontWeight.Bold)
                }

                expenses.forEach { exp ->
                    val canModifyExpense = activeRole.canEditOrDelete && (!exp.capitalInvestment || activeRole.isOwner)
                    BrohiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (exp.capitalInvestment) WarningOrangeSoft else Color(0xFFEAF2FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.ReceiptLong,
                                            contentDescription = null,
                                            tint = if (exp.capitalInvestment) WarningOrange else RoyalBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(exp.description, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(
                                            "${exp.category} • ${PakistaniFormatters.formatDisplayDate(exp.date)} • ${exp.source}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DangerRedSoft
                                ) {
                                    Text(
                                        text = PakistaniFormatters.formatPkr(exp.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = DangerRed,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (canModifyExpense) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { editExpenseTarget = exp }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit expense", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deleteExpenseTarget = exp }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete expense", tint = DangerRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (activeRole.isOwner && selectedTab == 2) {
                Button(
                    onClick = { showAddFunding = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_funding_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record Funding / Loan", fontWeight = FontWeight.Bold)
                }

                fundingList.forEach { entry ->
                    val remaining = (entry.amount - entry.repaid).coerceAtLeast(0.0)
                    BrohiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text("${entry.name} (${entry.source})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        "${entry.type} • ${PakistaniFormatters.formatDisplayDate(entry.date)}${entry.dueDate?.let { " • Due $it" } ?: ""}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFEAF2FF)) {
                                    Text(
                                        PakistaniFormatters.formatPkr(entry.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = DeepBlue,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { editFundingTarget = entry }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit funding", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deleteFundingTarget = entry }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete funding", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Repaid: ${PakistaniFormatters.formatPkr(entry.repaid)} | Remaining: ${PakistaniFormatters.formatPkr(remaining)}",
                                    fontSize = 12.sp,
                                    color = if (remaining > 0) WarningOrange else SuccessGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (remaining > 0) {
                                    OutlinedButton(onClick = { repayTarget = entry }) {
                                        Text("Repay", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFinancialStatement) {
        val statement = remember(
            sales,
            expenses,
            fundingList,
            utilities,
            payroll,
            suppliers,
            summary,
            settings,
            statementFilter
        ) {
            StatementBuilder.buildFinancialStatement(
                sales = sales,
                expenses = expenses,
                fundingList = fundingList,
                utilities = utilities,
                payroll = payroll,
                suppliers = suppliers,
                summary = summary,
                settings = settings,
                filterGroup = statementFilter
            )
        }
        FinancialAndCreditStatementDialog(
            statement = statement,
            selectedFinancialFilter = statementFilter,
            onSelectFinancialFilter = { statementFilter = it },
            onExportPdf = {
                ExportHelper.exportAndShareStatementPdf(context, statement)
            },
            onDismiss = { showFinancialStatement = false }
        )
    }

    if (showAddExpense) {
        val categories = buildList {
            addAll(listOf("Electricity", "Diesel", "Water", "Rent", "Refreshment", "Maintenance", "Supplier", "Other"))
            if (activeRole.isOwner) add("Factory Build")
        }
        var category by remember { mutableStateOf(categories.first()) }
        var desc by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        CrystalFormDialog(
            onDismissRequest = { showAddExpense = false },
            title = "Add Expense",
            subtitle = "نیا فیکٹری خرچہ درج کریں",
            icon = Icons.Filled.ReceiptLong,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onAddExpense(desc.ifBlank { category }, amt, category)
                    showAddExpense = false
                }
            }
        ) {
            Text("Category (کیٹیگری)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = DeepBlue)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(cat, fontSize = 10.sp) }
                    )
                }
            }
            CrystalTextField(
                value = desc,
                onValueChange = { desc = it },
                englishLabel = "Description",
                urduLabel = "تفصیل",
                leadingIcon = Icons.Filled.Description
            )
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Amount (Rs.)",
                urduLabel = "رقم (روپے)",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
        }
    }

    if (showAddFunding) {
        var source by remember { mutableStateOf("Friend") }
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("Loan") }
        var amount by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2027-06-01") }
        CrystalFormDialog(
            onDismissRequest = { showAddFunding = false },
            title = "Record Funding / Loan",
            subtitle = "قرض یا سرمایہ کاری کا اندراج",
            icon = Icons.Filled.AccountBalance,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0 && name.isNotBlank()) {
                    onAddFunding(source, name.trim(), type, amt, 0.0, dueDate.ifBlank { null }, null)
                    showAddFunding = false
                }
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Friend", "Bank", "Investor", "Customer").forEach { s ->
                    FilterChip(
                        selected = source == s,
                        onClick = { source = s },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(s, fontSize = 10.sp) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Loan", "Investment").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(t) }
                    )
                }
            }
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Person / Bank Name",
                urduLabel = "نام / بینک",
                leadingIcon = Icons.Filled.Person
            )
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Amount (Rs.)",
                urduLabel = "رقم (روپے)",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                englishLabel = "Due Date (YYYY-MM-DD)",
                urduLabel = "واپسی کی تاریخ",
                leadingIcon = Icons.Filled.CalendarMonth
            )
        }
    }

    repayTarget?.let { entry ->
        var amtText by remember { mutableStateOf(((entry.amount - entry.repaid).coerceAtLeast(0.0)).toLong().toString()) }
        CrystalFormDialog(
            onDismissRequest = { repayTarget = null },
            title = "Repay ${entry.name}",
            subtitle = "قرض / سرمایہ کی واپسی",
            icon = Icons.Filled.Payments,
            confirmText = "Confirm Repayment",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amtText.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onRepayFunding(entry, amt)
                    repayTarget = null
                }
            }
        ) {
            CrystalTextField(
                value = amtText,
                onValueChange = { amtText = it },
                englishLabel = "Repayment Amount (Rs.)",
                urduLabel = "واپس کی گئی رقم",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
        }
    }

    editExpenseTarget?.let { exp ->
        val categories = buildList {
            addAll(listOf("Electricity", "Diesel", "Water", "Rent", "Refreshment", "Maintenance", "Supplier", "Other"))
            if (activeRole.isOwner || exp.capitalInvestment) add("Factory Build")
        }.distinct()
        var category by remember(exp.id) { mutableStateOf(exp.category.ifBlank { categories.first() }) }
        var desc by remember(exp.id) { mutableStateOf(exp.description) }
        var amount by remember(exp.id) { mutableStateOf(exp.amount.toLong().toString()) }
        var date by remember(exp.id) { mutableStateOf(exp.date) }
        CrystalFormDialog(
            onDismissRequest = { editExpenseTarget = null },
            title = "Edit Expense",
            subtitle = "اخراجات کے ریکارڈ میں ترمیم",
            icon = Icons.Filled.Edit,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onUpdateExpense(
                        exp.copy(
                            category = category,
                            description = desc.trim().ifBlank { category },
                            amount = amt,
                            capitalInvestment = category.equals("Factory Build", ignoreCase = true),
                            date = date.trim().ifBlank { exp.date }
                        )
                    )
                    editExpenseTarget = null
                }
            }
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(cat, fontSize = 10.sp) }
                    )
                }
            }
            CrystalTextField(
                value = desc,
                onValueChange = { desc = it },
                englishLabel = "Description",
                urduLabel = "تفصیل",
                leadingIcon = Icons.Filled.Description
            )
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Amount (Rs.)",
                urduLabel = "رقم (روپے)",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = date,
                onValueChange = { date = it },
                englishLabel = "Date (YYYY-MM-DD)",
                urduLabel = "تاریخ",
                leadingIcon = Icons.Filled.CalendarMonth
            )
        }
    }

    deleteExpenseTarget?.let { exp ->
        CrystalFormDialog(
            onDismissRequest = { deleteExpenseTarget = null },
            title = "Delete Expense?",
            subtitle = "خرچے کا ریکارڈ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteExpense(exp)
                deleteExpenseTarget = null
            }
        ) {
            Text(
                "Remove '${exp.description}' (${PakistaniFormatters.formatPkr(exp.amount)})? This cannot be undone.",
                fontSize = 13.sp
            )
        }
    }

    editFundingTarget?.let { entry ->
        var source by remember(entry.id) { mutableStateOf(entry.source) }
        var type by remember(entry.id) { mutableStateOf(entry.type) }
        var name by remember(entry.id) { mutableStateOf(entry.name) }
        var amount by remember(entry.id) { mutableStateOf(entry.amount.toLong().toString()) }
        var repaid by remember(entry.id) { mutableStateOf(entry.repaid.toLong().toString()) }
        var dueDate by remember(entry.id) { mutableStateOf(entry.dueDate ?: "") }
        var notes by remember(entry.id) { mutableStateOf(entry.notes ?: "") }
        CrystalFormDialog(
            onDismissRequest = { editFundingTarget = null },
            title = "Edit Funding / Loan",
            subtitle = "فنڈنگ ریکارڈ میں ترمیم",
            icon = Icons.Filled.Edit,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                val rep = repaid.toDoubleOrNull() ?: 0.0
                if (amt > 0 && name.isNotBlank()) {
                    onUpdateFunding(
                        entry.copy(
                            source = source,
                            type = type,
                            name = name.trim(),
                            amount = amt,
                            repaid = rep.coerceAtMost(amt),
                            dueDate = dueDate.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null }
                        )
                    )
                    editFundingTarget = null
                }
            }
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Friend", "Bank", "Investor", "Customer").forEach { s ->
                    FilterChip(
                        selected = source == s,
                        onClick = { source = s },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(s, fontSize = 10.sp) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Loan", "Investment").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(t) }
                    )
                }
            }
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Person / Bank Name",
                urduLabel = "نام / بینک",
                leadingIcon = Icons.Filled.Person
            )
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Amount (Rs.)",
                urduLabel = "رقم (روپے)",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = repaid,
                onValueChange = { repaid = it },
                englishLabel = "Repaid (Rs.)",
                urduLabel = "واپس شدہ رقم",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = dueDate,
                onValueChange = { dueDate = it },
                englishLabel = "Due Date (YYYY-MM-DD)",
                urduLabel = "واپسی کی تاریخ",
                leadingIcon = Icons.Filled.CalendarMonth
            )
            CrystalTextField(
                value = notes,
                onValueChange = { notes = it },
                englishLabel = "Notes",
                urduLabel = "نوٹس",
                leadingIcon = Icons.Filled.Notes
            )
        }
    }

    deleteFundingTarget?.let { entry ->
        CrystalFormDialog(
            onDismissRequest = { deleteFundingTarget = null },
            title = "Delete Funding Record?",
            subtitle = "فنڈنگ ریکارڈ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteFunding(entry.id)
                deleteFundingTarget = null
            }
        ) {
            Text("Remove ${entry.name} (${PakistaniFormatters.formatPkr(entry.amount)})?", fontSize = 13.sp)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1, softWrap = false)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SalaryPayrollScreen(
    activeRole: UserRole,
    employees: List<Employee>,
    payrollRecords: List<PayrollRecord>,
    onSavePayroll: (PayrollRecord) -> Unit,
    onDeletePayroll: (String) -> Unit,
    onMarkPaid: (PayrollRecord) -> Unit,
    onSaveEmployee: (Employee) -> Unit = {}
) {
    var showCreate by remember { mutableStateOf(false) }
    var createPreselectedEmp by remember { mutableStateOf<Employee?>(null) }
    var editTarget by remember { mutableStateOf<PayrollRecord?>(null) }
    var deleteTarget by remember { mutableStateOf<PayrollRecord?>(null) }
    var selectedEmployeeProfileId by remember { mutableStateOf<String?>(null) }

    val selectedEmployeeProfile = remember(selectedEmployeeProfileId, employees) {
        employees.firstOrNull { it.id == selectedEmployeeProfileId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activeRole.isOwner) {
            Button(
                onClick = {
                    createPreselectedEmp = employees.firstOrNull()
                    showCreate = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Salary Slip", fontWeight = FontWeight.Bold)
            }
        }

        // Compact Tappable Staff Pay Cards
        if (employees.isNotEmpty()) {
            Text(
                text = if (activeRole == UserRole.STAFF) {
                    "My Salary Profile (میری تنخواہ — تفصیل کیلئے ٹچ کریں)"
                } else {
                    "Staff Pay Profiles (Tap any card for full history)"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = DeepBlue
            )
            employees.chunked(2).forEach { rowEmps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowEmps.forEach { emp ->
                        val empSlips = payrollRecords.filter {
                            it.employeeId == emp.id || it.employeeName.equals(emp.name, ignoreCase = true)
                        }
                        val dueTotal = empSlips.filter { !it.paid }.sumOf { it.netPay }
                        val paidTotal = empSlips.filter { it.paid }.sumOf { it.netPay }
                        val startSalary = computeStartingSalary(emp, empSlips)
                        val increment = (emp.salary - startSalary).coerceAtLeast(0.0)

                        Surface(
                            onClick = { selectedEmployeeProfileId = emp.id },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("staff_pay_card_${emp.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = emp.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = "${emp.role} • Now ${PakistaniFormatters.formatPkr(emp.salary)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepBlue,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (increment > 0) {
                                    Text(
                                        text = "Start ${PakistaniFormatters.formatPkr(startSalary)} (+${PakistaniFormatters.formatPkr(increment)})",
                                        fontSize = 10.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (dueTotal > 0) WarningOrangeSoft else SuccessGreenSoft
                                ) {
                                    Text(
                                        text = if (dueTotal > 0) {
                                            "Due: ${PakistaniFormatters.formatPkr(dueTotal)}"
                                        } else {
                                            "Paid: ${PakistaniFormatters.formatPkr(paidTotal)}"
                                        },
                                        color = if (dueTotal > 0) WarningOrange else SuccessGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (rowEmps.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Text(
            text = "All Salary Slips (${payrollRecords.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = DeepBlue
        )

        payrollRecords.forEach { rec ->
            BrohiCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val match = employees.firstOrNull {
                        it.id == rec.employeeId || it.name.equals(rec.employeeName, ignoreCase = true)
                    }
                    if (match != null) selectedEmployeeProfileId = match.id
                }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(rec.employeeName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Month: ${rec.month}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (rec.paid) SuccessGreenSoft else WarningOrangeSoft
                        ) {
                            Text(
                                text = if (rec.paid) "Paid ${rec.paidDate ?: ""}" else "Pending",
                                color = if (rec.paid) SuccessGreen else WarningOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        "Base: ${PakistaniFormatters.formatPkr(rec.baseSalary)} • Advances: ${PakistaniFormatters.formatPkr(rec.advances)} • Deductions: ${PakistaniFormatters.formatPkr(rec.deductions)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Net Pay: ${PakistaniFormatters.formatPkr(rec.netPay)}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DeepBlue,
                            maxLines = 1,
                            softWrap = false
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!rec.paid && activeRole.isOwner) {
                                Button(
                                    onClick = { onMarkPaid(rec) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pay", fontSize = 12.sp)
                                }
                            }
                            if (activeRole.isOwner) {
                                IconButton(onClick = { editTarget = rec }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit salary", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deleteTarget = rec }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete salary", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedEmployeeProfile?.let { emp ->
        EmployeeSalaryProfileSheet(
            activeRole = activeRole,
            employee = emp,
            slips = payrollRecords.filter {
                it.employeeId == emp.id || it.employeeName.equals(emp.name, ignoreCase = true)
            },
            onDismiss = { selectedEmployeeProfileId = null },
            onSaveEmployee = onSaveEmployee,
            onGenerateSlip = {
                createPreselectedEmp = emp
                showCreate = true
            },
            onMarkPaid = onMarkPaid,
            onEditSlip = { slip -> editTarget = slip },
            onDeleteSlip = { slip -> deleteTarget = slip }
        )
    }

    if (showCreate) {
        var selectedEmp by remember(createPreselectedEmp) { mutableStateOf(createPreselectedEmp ?: employees.firstOrNull()) }
        var month by remember { mutableStateOf(PakistaniFormatters.currentMonthIso()) }
        var advances by remember { mutableStateOf("0") }
        var deductions by remember { mutableStateOf("0") }
        CrystalFormDialog(
            onDismissRequest = { showCreate = false },
            title = "Generate Monthly Salary",
            subtitle = "ماہانہ تنخواہ کی پرچی بنائیں",
            icon = Icons.Filled.Payments,
            confirmText = "Save Slip",
            dismissText = "Cancel",
            onConfirm = {
                val emp = selectedEmp ?: return@CrystalFormDialog
                val adv = advances.toDoubleOrNull() ?: 0.0
                val ded = deductions.toDoubleOrNull() ?: 0.0
                onSavePayroll(
                    PayrollRecord(
                        employeeId = emp.id,
                        employeeName = emp.name,
                        month = month.trim().ifBlank { PakistaniFormatters.currentMonthIso() },
                        baseSalary = emp.salary,
                        advances = adv,
                        deductions = ded,
                        netPay = (emp.salary - adv - ded).coerceAtLeast(0.0),
                        paid = false
                    )
                )
                showCreate = false
            }
        ) {
            if (employees.isEmpty()) {
                Text("No staff found. Add employees in Users & Roles first.", fontSize = 12.sp)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    employees.forEach { emp ->
                        FilterChip(
                            selected = selectedEmp?.id == emp.id,
                            onClick = { selectedEmp = emp },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                            label = { Text("${emp.name} (${PakistaniFormatters.formatPkr(emp.salary)})", fontSize = 11.sp) }
                        )
                    }
                }
            }
            CrystalTextField(
                value = month,
                onValueChange = { month = it },
                englishLabel = "Salary Month (YYYY-MM)",
                urduLabel = "مہینہ",
                leadingIcon = Icons.Filled.CalendarMonth
            )
            CrystalTextField(
                value = advances,
                onValueChange = { advances = it },
                englishLabel = "Advances (Rs.)",
                urduLabel = "پیشگی (ایڈوانس)",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = deductions,
                onValueChange = { deductions = it },
                englishLabel = "Deductions (Rs.)",
                urduLabel = "کٹوتی",
                leadingIcon = Icons.Filled.MoneyOff,
                keyboardType = KeyboardType.Number
            )
        }
    }

    editTarget?.let { rec ->
        var base by remember(rec.id) { mutableStateOf(rec.baseSalary.toLong().toString()) }
        var advances by remember(rec.id) { mutableStateOf(rec.advances.toLong().toString()) }
        var deductions by remember(rec.id) { mutableStateOf(rec.deductions.toLong().toString()) }
        var paid by remember(rec.id) { mutableStateOf(rec.paid) }
        CrystalFormDialog(
            onDismissRequest = { editTarget = null },
            title = "Edit Salary: ${rec.employeeName}",
            subtitle = "تنخواہ کے ریکارڈ میں ترمیم",
            icon = Icons.Filled.Edit,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                val b = base.toDoubleOrNull() ?: rec.baseSalary
                val adv = advances.toDoubleOrNull() ?: 0.0
                val ded = deductions.toDoubleOrNull() ?: 0.0
                onSavePayroll(
                    rec.copy(
                        baseSalary = b,
                        advances = adv,
                        deductions = ded,
                        netPay = (b - adv - ded).coerceAtLeast(0.0),
                        paid = paid,
                        paidDate = if (paid) (rec.paidDate ?: PakistaniFormatters.todayIso()) else null
                    )
                )
                editTarget = null
            }
        ) {
            CrystalTextField(
                value = base,
                onValueChange = { base = it },
                englishLabel = "Base Salary (Rs.)",
                urduLabel = "بنیادی تنخواہ",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = advances,
                onValueChange = { advances = it },
                englishLabel = "Advances (Rs.)",
                urduLabel = "ایڈوانس",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = deductions,
                onValueChange = { deductions = it },
                englishLabel = "Deductions (Rs.)",
                urduLabel = "کٹوتی",
                leadingIcon = Icons.Filled.MoneyOff,
                keyboardType = KeyboardType.Number
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Marked Paid (ادا شدہ)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = paid, onCheckedChange = { paid = it })
                }
            }
        }
    }

    deleteTarget?.let { rec ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Salary Slip?",
            subtitle = "تنخواہ پرچی حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeletePayroll(rec.id)
                deleteTarget = null
            }
        ) {
            Text(
                "Remove ${rec.employeeName}'s ${rec.month} salary slip (${PakistaniFormatters.formatPkr(rec.netPay)})?",
                fontSize = 13.sp
            )
        }
    }
}

internal fun computeStartingSalary(employee: Employee, slips: List<PayrollRecord>): Double {
    if (employee.initialSalary > 0.0) return employee.initialSalary
    val minHistorical = slips.mapNotNull { it.baseSalary.takeIf { b -> b > 0.0 } }.minOrNull()
    return minHistorical ?: employee.salary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeSalaryProfileSheet(
    activeRole: UserRole,
    employee: Employee,
    slips: List<PayrollRecord>,
    onDismiss: () -> Unit,
    onSaveEmployee: (Employee) -> Unit,
    onGenerateSlip: () -> Unit,
    onMarkPaid: (PayrollRecord) -> Unit,
    onEditSlip: (PayrollRecord) -> Unit,
    onDeleteSlip: (PayrollRecord) -> Unit
) {
    val totalSalaryPaid = slips.filter { it.paid }.sumOf { it.netPay }
    val totalAdvances = slips.sumOf { it.advances }
    val pendingDue = slips.filter { !it.paid }.sumOf { it.netPay }
    val startingSalary = computeStartingSalary(employee, slips)
    val salaryIncrease = (employee.salary - startingSalary).coerceAtLeast(0.0)
    val sortedSlips = remember(slips) { slips.sortedByDescending { it.month } }

    var showEditSalaries by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Profile Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryBlueGradient)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = employee.name,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${employee.role} • Joined: ${employee.joinDate.ifBlank { "N/A" }}",
                            color = Color(0xFFDCE8FF),
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (activeRole.isOwner) {
                            Surface(
                                onClick = { showEditSalaries = true },
                                shape = RoundedCornerShape(10.dp),
                                color = lerp(DeepBlue, Color.White, 0.22f)
                            ) {
                                Text(
                                    text = "Edit Pay",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close profile", tint = Color.White)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2x2 Mini Pay Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PayMiniCard(
                        title = "Total Salary Taken",
                        value = PakistaniFormatters.formatPkr(totalSalaryPaid),
                        subtitle = "${slips.count { it.paid }} paid slip(s)",
                        containerColor = SuccessGreenSoft,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PayMiniCard(
                        title = "Advances Taken",
                        value = PakistaniFormatters.formatPkr(totalAdvances),
                        subtitle = "Deducted in slips",
                        containerColor = Color(0xFFEAF2FF),
                        accentColor = RoyalBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PayMiniCard(
                        title = "Pending / Due Salary",
                        value = PakistaniFormatters.formatPkr(pendingDue),
                        subtitle = "${slips.count { !it.paid }} unpaid slip(s)",
                        containerColor = if (pendingDue > 0) DangerRedSoft else SuccessGreenSoft,
                        accentColor = if (pendingDue > 0) DangerRed else SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                    PayMiniCard(
                        title = "Current Salary",
                        value = PakistaniFormatters.formatPkr(employee.salary),
                        subtitle = if (salaryIncrease > 0) "+${PakistaniFormatters.formatPkr(salaryIncrease)} growth" else "Initial rate",
                        containerColor = Color(0xFFF3E8FF),
                        accentColor = DeepBlue,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Joining Salary -> Current Salary Growth Progression Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Salary Progression",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DeepBlue
                            )
                            Surface(shape = RoundedCornerShape(8.dp), color = SuccessGreenSoft) {
                                Text(
                                    text = "+${PakistaniFormatters.formatPkr(salaryIncrease)} Increase",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Starting Salary (${employee.joinDate.take(4).ifBlank { "Join" }})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    PakistaniFormatters.formatPkr(startingSalary),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Current Salary", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    PakistaniFormatters.formatPkr(employee.salary),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = SuccessGreen,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Quick Actions inside Profile (Super Admin only)
                if (activeRole.isOwner) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onGenerateSlip,
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Slip", fontSize = 12.sp, maxLines = 1, softWrap = false)
                        }
                        if (pendingDue > 0) {
                            Button(
                                onClick = {
                                    slips.filter { !it.paid }.forEach { onMarkPaid(it) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pay Due", fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Complete Transaction History
                Text(
                    text = "Salary Transactions (${sortedSlips.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepBlue
                )

                if (sortedSlips.isEmpty()) {
                    Text(
                        text = if (activeRole.isOwner) {
                            "No salary transactions recorded yet for ${employee.name}. Tap 'New Slip' above to create one."
                        } else {
                            "No salary transactions recorded yet for ${employee.name}."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    sortedSlips.forEach { rec ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Month: ${rec.month}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (rec.paid) SuccessGreenSoft else WarningOrangeSoft
                                    ) {
                                        Text(
                                            text = if (rec.paid) "Paid ${rec.paidDate ?: ""}" else "Due",
                                            color = if (rec.paid) SuccessGreen else WarningOrange,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Base: ${PakistaniFormatters.formatPkr(rec.baseSalary)} • Adv: ${PakistaniFormatters.formatPkr(rec.advances)} • Ded: ${PakistaniFormatters.formatPkr(rec.deductions)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Net: ${PakistaniFormatters.formatPkr(rec.netPay)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = DeepBlue,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!rec.paid && activeRole.isOwner) {
                                            TextButton(onClick = { onMarkPaid(rec) }) {
                                                Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                            }
                                        }
                                        if (activeRole.isOwner) {
                                            IconButton(onClick = { onEditSlip(rec) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit slip", tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(onClick = { onDeleteSlip(rec) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete slip", tint = DangerRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditSalaries) {
        var initText by remember(employee.id) { mutableStateOf(startingSalary.toLong().toString()) }
        var currText by remember(employee.id) { mutableStateOf(employee.salary.toLong().toString()) }
        var joinDateText by remember(employee.id) { mutableStateOf(employee.joinDate) }
        CrystalFormDialog(
            onDismissRequest = { showEditSalaries = false },
            title = "Update ${employee.name}'s Pay",
            subtitle = "ملازم کی تنخواہ اور شمولیت کی تاریخ",
            icon = Icons.Filled.Payments,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val curr = currText.toDoubleOrNull() ?: employee.salary
                val init = initText.toDoubleOrNull() ?: curr
                onSaveEmployee(
                    employee.copy(
                        initialSalary = init,
                        salary = curr,
                        joinDate = joinDateText.trim().ifBlank { employee.joinDate }
                    )
                )
                showEditSalaries = false
            }
        ) {
            CrystalTextField(
                value = initText,
                onValueChange = { initText = it },
                englishLabel = "Starting / Joining Salary (Rs.)",
                urduLabel = "ابتدائی تنخواہ",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = currText,
                onValueChange = { currText = it },
                englishLabel = "Current Salary (Rs.)",
                urduLabel = "موجودہ تنخواہ",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = joinDateText,
                onValueChange = { joinDateText = it },
                englishLabel = "Join Date (YYYY-MM-DD)",
                urduLabel = "شمولیت کی تاریخ",
                leadingIcon = Icons.Filled.CalendarMonth
            )
        }
    }
}

@Composable
private fun PayMiniCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
