package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.AppReleaseConfig
import com.example.data.model.Expense
import com.example.data.model.FactoryNotice
import com.example.data.model.FactorySettings
import com.example.data.model.FundingEntry
import com.example.data.model.MachineItem
import com.example.data.model.OwnerAccountingSummary
import com.example.data.model.PayrollRecord
import com.example.data.model.SafetyIncident
import com.example.data.model.Sale
import com.example.data.model.SupplierItem
import com.example.data.model.UserRole
import com.example.data.model.UtilityReading
import com.example.data.util.AppStrings
import com.example.data.util.AppUpdateDownloadState
import com.example.data.util.ExportHelper
import com.example.data.util.PakistaniFormatters
import com.example.data.util.StatementBuilder
import com.example.data.util.StatementFilterGroup
import com.example.ui.components.BilingualPillLabel
import com.example.ui.components.BrohiCard
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.CrystalTextField
import com.example.ui.components.PhoneCrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.components.ValidationPopupDialog
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentPurpleSoft
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WarningOrangeSoft
import kotlin.math.roundToInt

@Composable
fun UtilitiesScreen(
    activeRole: UserRole,
    readings: List<UtilityReading>,
    onSaveUtility: (type: String, meterReading: Double, unitsOrLiters: Double, amount: Double, notes: String?) -> Unit,
    onUpdateUtility: (UtilityReading) -> Unit,
    onDeleteUtility: (String) -> Unit
) {
    var selectedType by remember { mutableStateOf("Electricity") }
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<UtilityReading?>(null) }
    var deleteTarget by remember { mutableStateOf<UtilityReading?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Electricity", "Diesel", "Water").forEach { t ->
                FilterChip(
                    selected = selectedType == t,
                    onClick = { selectedType = t },
                    label = { Text(t) }
                )
            }
        }

        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log $selectedType Reading / Cost", fontWeight = FontWeight.Bold)
        }

        readings.filter { it.type == selectedType }.forEach { r ->
            val icon = when (r.type) {
                "Diesel" -> Icons.Filled.LocalGasStation
                "Water" -> Icons.Filled.WaterDrop
                else -> Icons.Filled.Bolt
            }
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Icon(icon, contentDescription = null, tint = RoyalBlue)
                            Column {
                                Text(
                                    "${r.type} • ${r.unitsOrLiters} units",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Meter: ${r.meterReading.toLong()} • ${PakistaniFormatters.formatDisplayDate(r.date)}",
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
                                text = PakistaniFormatters.formatPkr(r.amount),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = DangerRed,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (!r.notes.isNullOrBlank() || activeRole.canEditOrDelete) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = r.notes.orEmpty(),
                                fontSize = 11.sp,
                                color = DeepBlue,
                                modifier = Modifier.weight(1f)
                            )
                            if (activeRole.canEditOrDelete) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { editTarget = r }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit utility reading", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { deleteTarget = r }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete utility reading", tint = DangerRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var meter by remember { mutableStateOf("") }
        var units by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        CrystalFormDialog(
            onDismissRequest = { showAdd = false },
            title = "Add $selectedType Entry",
            subtitle = "بجلی / ڈیزل / پانی کی ریڈنگ درج کریں",
            icon = when (selectedType) {
                "Diesel" -> Icons.Filled.LocalGasStation
                "Water" -> Icons.Filled.WaterDrop
                else -> Icons.Filled.Bolt
            },
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                onSaveUtility(
                    selectedType,
                    meter.toDoubleOrNull() ?: 0.0,
                    units.toDoubleOrNull() ?: 0.0,
                    amount.toDoubleOrNull() ?: 0.0,
                    notes.ifBlank { null }
                )
                showAdd = false
            }
        ) {
            CrystalTextField(
                value = meter,
                onValueChange = { meter = it },
                englishLabel = "Meter / Tank Reading",
                urduLabel = "میٹر ریڈنگ",
                leadingIcon = Icons.Filled.Speed,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = units,
                onValueChange = { units = it },
                englishLabel = if (selectedType == "Diesel") "Liters" else "Units",
                urduLabel = if (selectedType == "Diesel") "لیٹر" else "یونٹس",
                leadingIcon = if (selectedType == "Diesel") Icons.Filled.LocalGasStation else Icons.Filled.Bolt,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Bill / Cost (Rs.)",
                urduLabel = "بل کی رقم",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
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

    editTarget?.let { r ->
        var type by remember(r.id) { mutableStateOf(r.type) }
        var meter by remember(r.id) { mutableStateOf(r.meterReading.toString()) }
        var units by remember(r.id) { mutableStateOf(r.unitsOrLiters.toString()) }
        var amount by remember(r.id) { mutableStateOf(r.amount.toLong().toString()) }
        var date by remember(r.id) { mutableStateOf(r.date) }
        var notes by remember(r.id) { mutableStateOf(r.notes ?: "") }
        CrystalFormDialog(
            onDismissRequest = { editTarget = null },
            title = "Edit Utility Reading",
            subtitle = "یوٹیلٹی ریکارڈ میں ترمیم",
            icon = Icons.Filled.Edit,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                onUpdateUtility(
                    r.copy(
                        type = type,
                        meterReading = meter.toDoubleOrNull() ?: r.meterReading,
                        unitsOrLiters = units.toDoubleOrNull() ?: r.unitsOrLiters,
                        amount = amount.toDoubleOrNull() ?: r.amount,
                        date = date.trim().ifBlank { r.date },
                        notes = notes.trim().ifBlank { null }
                    )
                )
                editTarget = null
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Electricity", "Diesel", "Water").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(t, fontSize = 11.sp) }
                    )
                }
            }
            CrystalTextField(
                value = meter,
                onValueChange = { meter = it },
                englishLabel = "Meter / Tank Reading",
                urduLabel = "میٹر ریڈنگ",
                leadingIcon = Icons.Filled.Speed,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = units,
                onValueChange = { units = it },
                englishLabel = if (type == "Diesel") "Liters" else "Units",
                urduLabel = if (type == "Diesel") "لیٹر" else "یونٹس",
                leadingIcon = Icons.Filled.Bolt,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Bill / Cost (Rs.)",
                urduLabel = "بل رقم",
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
            CrystalTextField(
                value = notes,
                onValueChange = { notes = it },
                englishLabel = "Notes",
                urduLabel = "نوٹس",
                leadingIcon = Icons.Filled.Notes
            )
        }
    }

    deleteTarget?.let { r ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Utility Reading?",
            subtitle = "ریکارڈ مستقل طور پر حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteUtility(r.id)
                deleteTarget = null
            }
        ) {
            Text(
                text = "Remove ${r.type} entry (${PakistaniFormatters.formatPkr(r.amount)} on ${r.date})?",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaintenanceScreen(
    activeRole: UserRole,
    machines: List<MachineItem>,
    onSaveMachine: (MachineItem) -> Unit,
    onDeleteMachine: (String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<MachineItem?>(null) }
    var deleteTarget by remember { mutableStateOf<MachineItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.Build, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Ice Machine / Compressor", fontWeight = FontWeight.Bold)
        }

        machines.forEach { m ->
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(m.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Model: ${m.model} • ${m.capacityTonsPerDay} Tons/day",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Last Service: ${m.lastServicedDate}\nNext Service: ${m.nextServiceDate}",
                                fontSize = 11.sp,
                                color = DeepBlue
                            )
                            if (!m.breakdownNote.isNullOrBlank()) {
                                Text("Note: ${m.breakdownNote}", fontSize = 11.sp, color = DangerRed)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (m.status) {
                                "Breakdown" -> DangerRedSoft
                                "Maintenance Due" -> WarningOrangeSoft
                                else -> SuccessGreenSoft
                            }
                        ) {
                            Text(
                                m.status,
                                color = when (m.status) {
                                    "Breakdown" -> DangerRed
                                    "Maintenance Due" -> WarningOrange
                                    else -> SuccessGreen
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Operational", "Maintenance Due", "Breakdown").forEach { status ->
                            FilterChip(
                                selected = m.status == status,
                                onClick = {
                                    val today = PakistaniFormatters.todayIso()
                                    onSaveMachine(
                                        m.copy(
                                            status = status,
                                            lastServicedDate = if (status == "Operational") today else m.lastServicedDate
                                        )
                                    )
                                },
                                label = {
                                    Text(
                                        text = status,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    if (activeRole.canEditOrDelete) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { editTarget = m }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit machine", tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, color = RoyalBlue)
                            }
                            TextButton(onClick = { deleteTarget = m }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete machine", tint = DangerRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 12.sp, color = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var cap by remember { mutableStateOf("15") }
        CrystalFormDialog(
            onDismissRequest = { showAdd = false },
            title = "Add Machine / Plant",
            subtitle = "نئی مشین یا کمپریسر شامل کریں",
            icon = Icons.Filled.PrecisionManufacturing,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                if (name.isNotBlank()) {
                    onSaveMachine(
                        MachineItem(
                            name = name.trim(),
                            model = model.trim(),
                            capacityTonsPerDay = cap.toDoubleOrNull() ?: 10.0,
                            lastServicedDate = PakistaniFormatters.todayIso(),
                            nextServiceDate = "2027-01-15",
                            status = "Operational"
                        )
                    )
                    showAdd = false
                }
            }
        ) {
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Machine Name",
                urduLabel = "مشین کا نام",
                leadingIcon = Icons.Filled.PrecisionManufacturing
            )
            CrystalTextField(
                value = model,
                onValueChange = { model = it },
                englishLabel = "Model",
                urduLabel = "ماڈل",
                leadingIcon = Icons.Filled.Category
            )
            CrystalTextField(
                value = cap,
                onValueChange = { cap = it },
                englishLabel = "Capacity (Tons/day)",
                urduLabel = "پیداواری صلاحیت",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Number
            )
        }
    }

    editTarget?.let { m ->
        var name by remember(m.id) { mutableStateOf(m.name) }
        var model by remember(m.id) { mutableStateOf(m.model) }
        var cap by remember(m.id) { mutableStateOf(m.capacityTonsPerDay.toString()) }
        var lastService by remember(m.id) { mutableStateOf(m.lastServicedDate) }
        var nextService by remember(m.id) { mutableStateOf(m.nextServiceDate) }
        var note by remember(m.id) { mutableStateOf(m.breakdownNote ?: "") }
        var status by remember(m.id) { mutableStateOf(m.status) }
        CrystalFormDialog(
            onDismissRequest = { editTarget = null },
            title = "Edit Machine",
            subtitle = "مشین کی تفصیلات اپ ڈیٹ کریں",
            icon = Icons.Filled.Build,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                if (name.isNotBlank()) {
                    onSaveMachine(
                        m.copy(
                            name = name.trim(),
                            model = model.trim(),
                            capacityTonsPerDay = cap.toDoubleOrNull() ?: m.capacityTonsPerDay,
                            lastServicedDate = lastService.trim().ifBlank { m.lastServicedDate },
                            nextServiceDate = nextService.trim().ifBlank { m.nextServiceDate },
                            breakdownNote = note.trim().ifBlank { null },
                            status = status
                        )
                    )
                    editTarget = null
                }
            }
        ) {
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Machine Name",
                urduLabel = "مشین کا نام",
                leadingIcon = Icons.Filled.PrecisionManufacturing
            )
            CrystalTextField(
                value = model,
                onValueChange = { model = it },
                englishLabel = "Model",
                urduLabel = "ماڈل",
                leadingIcon = Icons.Filled.Category
            )
            CrystalTextField(
                value = cap,
                onValueChange = { cap = it },
                englishLabel = "Capacity (Tons/day)",
                urduLabel = "صلاحیت",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = lastService,
                onValueChange = { lastService = it },
                englishLabel = "Last Service Date",
                urduLabel = "آخری سروس",
                leadingIcon = Icons.Filled.CalendarMonth
            )
            CrystalTextField(
                value = nextService,
                onValueChange = { nextService = it },
                englishLabel = "Next Service Date",
                urduLabel = "اگلی سروس",
                leadingIcon = Icons.Filled.CalendarMonth
            )
            CrystalTextField(
                value = note,
                onValueChange = { note = it },
                englishLabel = "Breakdown / Service Note",
                urduLabel = "مرمت نوٹ",
                leadingIcon = Icons.Filled.Notes
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Operational", "Maintenance Due", "Breakdown").forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(st, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }
        }
    }

    deleteTarget?.let { m ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Machine?",
            subtitle = "مشین کا ریکارڈ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteMachine(m.id)
                deleteTarget = null
            }
        ) {
            Text("Remove ${m.name} (${m.model})? This cannot be undone.", fontSize = 13.sp)
        }
    }
}

@Composable
fun SuppliersScreen(
    activeRole: UserRole,
    suppliers: List<SupplierItem>,
    onSaveSupplier: (SupplierItem) -> Unit,
    onDeleteSupplier: (String) -> Unit,
    onPaySupplier: (SupplierItem, Double) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<SupplierItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SupplierItem?>(null) }
    var payTarget by remember { mutableStateOf<SupplierItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Supplier (Salt / Ammonia / Bags)", fontWeight = FontWeight.Bold)
        }

        suppliers.forEach { sup ->
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${sup.itemSupplied} • ${sup.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (sup.dues > 0) DangerRedSoft else SuccessGreenSoft
                        ) {
                            Text(
                                text = "Dues: ${PakistaniFormatters.formatPkr(sup.dues)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (sup.dues > 0) DangerRed else SuccessGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (sup.dues > 0) {
                            OutlinedButton(onClick = { payTarget = sup }) {
                                Text("Pay Due", fontSize = 12.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                        if (activeRole.canEditOrDelete) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editTarget = sup }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit supplier", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deleteTarget = sup }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete supplier", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var phoneDigits by remember { mutableStateOf("") }
        var item by remember { mutableStateOf("Salt & Packaging") }
        var dues by remember { mutableStateOf("0") }
        var triedSave by remember { mutableStateOf(false) }
        var popupMessage by remember { mutableStateOf<String?>(null) }
        CrystalFormDialog(
            onDismissRequest = { showAdd = false },
            title = "Add Supplier",
            subtitle = "نیا سپلائر شامل کریں",
            icon = Icons.Filled.Business,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                triedSave = true
                if (name.isBlank()) {
                    popupMessage = "Please enter supplier name."
                    return@CrystalFormDialog
                }
                if (phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)) {
                    popupMessage = "Phone number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nفون نمبر مکمل نہیں ہے، براہ کرم 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                val d = dues.toDoubleOrNull() ?: 0.0
                onSaveSupplier(
                    SupplierItem(
                        name = name.trim(),
                        phone = PakistaniFormatters.formatPhone(phoneDigits),
                        itemSupplied = item.trim(),
                        totalPurchases = d,
                        dues = d
                    )
                )
                showAdd = false
            }
        ) {
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Supplier Name",
                urduLabel = "سپلائر کا نام",
                leadingIcon = Icons.Filled.Person
            )
            PhoneCrystalTextField(
                digitsValue = phoneDigits,
                onDigitsChange = { phoneDigits = it },
                englishLabel = "Phone",
                urduLabel = "فون نمبر",
                leadingIcon = Icons.Filled.Phone,
                isError = triedSave && phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)
            )
            CrystalTextField(
                value = item,
                onValueChange = { item = it },
                englishLabel = "Supplied Material",
                urduLabel = "فراہم کردہ سامان",
                leadingIcon = Icons.Filled.Inventory2
            )
            CrystalTextField(
                value = dues,
                onValueChange = { dues = it },
                englishLabel = "Pending Dues (Rs.)",
                urduLabel = "واجب الادا رقم",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
        }
        ValidationPopupDialog(
            title = "Number Is Not Complete (نمبر مکمل نہیں ہے)",
            message = popupMessage,
            onDismiss = { popupMessage = null }
        )
    }

    editTarget?.let { sup ->
        var name by remember(sup.id) { mutableStateOf(sup.name) }
        var phoneDigits by remember(sup.id) { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(sup.phone)) }
        var item by remember(sup.id) { mutableStateOf(sup.itemSupplied) }
        var totalPurchases by remember(sup.id) { mutableStateOf(sup.totalPurchases.toLong().toString()) }
        var dues by remember(sup.id) { mutableStateOf(sup.dues.toLong().toString()) }
        var triedSave by remember(sup.id) { mutableStateOf(false) }
        var popupMessage by remember { mutableStateOf<String?>(null) }
        CrystalFormDialog(
            onDismissRequest = { editTarget = null },
            title = "Edit Supplier",
            subtitle = "سپلائر کی معلومات اپ ڈیٹ کریں",
            icon = Icons.Filled.Edit,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                triedSave = true
                if (name.isBlank()) {
                    popupMessage = "Please enter supplier name."
                    return@CrystalFormDialog
                }
                if (phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)) {
                    popupMessage = "Phone number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nفون نمبر مکمل نہیں ہے، براہ کرم 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                onSaveSupplier(
                    sup.copy(
                        name = name.trim(),
                        phone = PakistaniFormatters.formatPhone(phoneDigits),
                        itemSupplied = item.trim(),
                        totalPurchases = totalPurchases.toDoubleOrNull() ?: sup.totalPurchases,
                        dues = dues.toDoubleOrNull() ?: sup.dues
                    )
                )
                editTarget = null
            }
        ) {
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Supplier Name",
                urduLabel = "سپلائر کا نام",
                leadingIcon = Icons.Filled.Person
            )
            PhoneCrystalTextField(
                digitsValue = phoneDigits,
                onDigitsChange = { phoneDigits = it },
                englishLabel = "Phone",
                urduLabel = "فون نمبر",
                leadingIcon = Icons.Filled.Phone,
                isError = triedSave && phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)
            )
            CrystalTextField(
                value = item,
                onValueChange = { item = it },
                englishLabel = "Supplied Material",
                urduLabel = "فراہم کردہ سامان",
                leadingIcon = Icons.Filled.Inventory2
            )
            CrystalTextField(
                value = totalPurchases,
                onValueChange = { totalPurchases = it },
                englishLabel = "Total Purchases (Rs.)",
                urduLabel = "کل خریداری",
                leadingIcon = Icons.Filled.ReceiptLong,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = dues,
                onValueChange = { dues = it },
                englishLabel = "Pending Dues (Rs.)",
                urduLabel = "بقایا جات",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
        }
        ValidationPopupDialog(
            title = "Number Is Not Complete (نمبر مکمل نہیں ہے)",
            message = popupMessage,
            onDismiss = { popupMessage = null }
        )
    }

    deleteTarget?.let { sup ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Supplier?",
            subtitle = "سپلائر حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteSupplier(sup.id)
                deleteTarget = null
            }
        ) {
            Text("Remove ${sup.name} (${sup.itemSupplied})?", fontSize = 13.sp)
        }
    }

    payTarget?.let { sup ->
        var amtText by remember { mutableStateOf(sup.dues.toLong().toString()) }
        CrystalFormDialog(
            onDismissRequest = { payTarget = null },
            title = "Pay ${sup.name}",
            subtitle = "سپلائر کو ادائیگی اور خرچہ درج کریں",
            icon = Icons.Filled.Payments,
            confirmText = "Pay & Record",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amtText.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onPaySupplier(sup, amt)
                    payTarget = null
                }
            }
        ) {
            CrystalTextField(
                value = amtText,
                onValueChange = { amtText = it },
                englishLabel = "Payment Amount (Rs.)",
                urduLabel = "ادائیگی کی رقم",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SafetyIncidentsScreen(
    activeRole: UserRole,
    incidents: List<SafetyIncident>,
    reporterName: String,
    onSaveIncident: (SafetyIncident) -> Unit,
    onDeleteIncident: (String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<SafetyIncident?>(null) }
    var deleteTarget by remember { mutableStateOf<SafetyIncident?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { showAdd = true },
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Safety Incident", fontWeight = FontWeight.Bold)
        }

        incidents.forEach { inc ->
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(inc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (inc.severity == "High") DangerRedSoft else WarningOrangeSoft
                            ) {
                                Text(
                                    "${inc.severity} • ${inc.status}",
                                    color = if (inc.severity == "High") DangerRed else WarningOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (activeRole.canEditOrDelete) {
                                IconButton(onClick = { editTarget = inc }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit incident", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deleteTarget = inc }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete incident", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Text(inc.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!inc.followUp.isNullOrBlank()) {
                        Text("Follow-up: ${inc.followUp}", fontSize = 11.sp, color = DeepBlue)
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Open", "Investigating", "Resolved").forEach { st ->
                            FilterChip(
                                selected = inc.status == st,
                                onClick = { onSaveIncident(inc.copy(status = st)) },
                                label = { Text(st, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var severity by remember { mutableStateOf("Low") }
        CrystalFormDialog(
            onDismissRequest = { showAdd = false },
            title = "Report Safety Incident",
            subtitle = "حفاظتی واقعہ کی رپورٹ درج کریں",
            icon = Icons.Filled.WarningAmber,
            confirmText = "Submit",
            dismissText = "Cancel",
            accentColor = DangerRed,
            onConfirm = {
                if (title.isNotBlank()) {
                    onSaveIncident(
                        SafetyIncident(
                            title = title.trim(),
                            description = desc.trim(),
                            severity = severity,
                            status = "Open",
                            reportedBy = reporterName
                        )
                    )
                    showAdd = false
                }
            }
        ) {
            CrystalTextField(
                value = title,
                onValueChange = { title = it },
                englishLabel = "Title",
                urduLabel = "عنوان",
                leadingIcon = Icons.Filled.Title
            )
            CrystalTextField(
                value = desc,
                onValueChange = { desc = it },
                englishLabel = "Description",
                urduLabel = "تفصیل",
                leadingIcon = Icons.Filled.Description,
                singleLine = false
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low", "Medium", "High").forEach { s ->
                    FilterChip(
                        selected = severity == s,
                        onClick = { severity = s },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (s == "High") DangerRed else RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(s) }
                    )
                }
            }
        }
    }

    editTarget?.let { inc ->
        var title by remember(inc.id) { mutableStateOf(inc.title) }
        var desc by remember(inc.id) { mutableStateOf(inc.description) }
        var severity by remember(inc.id) { mutableStateOf(inc.severity) }
        var status by remember(inc.id) { mutableStateOf(inc.status) }
        var followUp by remember(inc.id) { mutableStateOf(inc.followUp ?: "") }
        CrystalFormDialog(
            onDismissRequest = { editTarget = null },
            title = "Edit Safety Incident",
            subtitle = "حفاظتی واقعہ اپ ڈیٹ کریں",
            icon = Icons.Filled.Edit,
            confirmText = "Save Changes",
            dismissText = "Cancel",
            onConfirm = {
                if (title.isNotBlank()) {
                    onSaveIncident(
                        inc.copy(
                            title = title.trim(),
                            description = desc.trim(),
                            severity = severity,
                            status = status,
                            followUp = followUp.trim().ifBlank { null }
                        )
                    )
                    editTarget = null
                }
            }
        ) {
            CrystalTextField(
                value = title,
                onValueChange = { title = it },
                englishLabel = "Title",
                urduLabel = "عنوان",
                leadingIcon = Icons.Filled.Title
            )
            CrystalTextField(
                value = desc,
                onValueChange = { desc = it },
                englishLabel = "Description",
                urduLabel = "تفصیل",
                leadingIcon = Icons.Filled.Description,
                singleLine = false
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Low", "Medium", "High").forEach { s ->
                    FilterChip(
                        selected = severity == s,
                        onClick = { severity = s },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (s == "High") DangerRed else RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(s) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Open", "Investigating", "Resolved").forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(st, fontSize = 10.sp) }
                    )
                }
            }
            CrystalTextField(
                value = followUp,
                onValueChange = { followUp = it },
                englishLabel = "Follow-up Note",
                urduLabel = "فالو اپ نوٹ",
                leadingIcon = Icons.Filled.Notes,
                singleLine = false
            )
        }
    }

    deleteTarget?.let { inc ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Incident?",
            subtitle = "واقعہ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteIncident(inc.id)
                deleteTarget = null
            }
        ) {
            Text("Remove safety incident '${inc.title}'?", fontSize = 13.sp)
        }
    }
}

@Composable
fun ReportsAnalyticsScreen(
    sales: List<Sale>,
    expenses: List<Expense>,
    summary: OwnerAccountingSummary,
    strings: AppStrings,
    fundingList: List<FundingEntry> = emptyList(),
    utilities: List<UtilityReading> = emptyList(),
    payroll: List<PayrollRecord> = emptyList(),
    suppliers: List<SupplierItem> = emptyList(),
    settings: FactorySettings = FactorySettings()
) {
    val context = LocalContext.current
    var showStatementPreview by remember { mutableStateOf(false) }
    var statementFilter by remember { mutableStateOf(StatementFilterGroup.ALL) }
    val today = PakistaniFormatters.todayIso()
    val month = PakistaniFormatters.currentMonthIso()

    val todaySales = sales.filter { it.date == today }.sumOf { it.total }
    val todayOpExp = expenses.filter { !it.capitalInvestment && it.date == today }.sumOf { it.amount }
    val monthSales = sales.filter { it.date.startsWith(month) }.sumOf { it.total }
    val monthOpExp = expenses.filter { !it.capitalInvestment && it.date.startsWith(month) }.sumOf { it.amount }

    val byCategory = expenses.filter { !it.capitalInvestment }
        .groupBy { it.category }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Profit & Loss Report (Owner)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                HorizontalDivider()
                ReportRow("Today Sales", PakistaniFormatters.formatPkr(todaySales), RoyalBlue)
                ReportRow("Today Operating Expenses", PakistaniFormatters.formatPkr(todayOpExp), DangerRed)
                ReportRow("Today Net Profit", PakistaniFormatters.formatPkr(todaySales - todayOpExp), SuccessGreen)
                HorizontalDivider()
                ReportRow("This Month Sales", PakistaniFormatters.formatPkr(monthSales), RoyalBlue)
                ReportRow("This Month Operating Exp", PakistaniFormatters.formatPkr(monthOpExp), DangerRed)
                ReportRow("This Month Net Profit", PakistaniFormatters.formatPkr(monthSales - monthOpExp), SuccessGreen)
                HorizontalDivider()
                ReportRow("Total Sales So Far", PakistaniFormatters.formatPkr(summary.totalSalesSoFar), DeepBlue)
                ReportRow("Regular Profit So Far", PakistaniFormatters.formatPkr(summary.regularProfitSoFar), SuccessGreen)
                ReportRow("Break-Even Recovery", "${summary.recoveryPercent.roundToInt()}%", RoyalBlue)
            }
        }

        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Operating Expense Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                HorizontalDivider()
                byCategory.forEach { (cat, amt) ->
                    ReportRow(cat, PakistaniFormatters.formatPkr(amt), DeepBlue)
                }
            }
        }

        Button(
            onClick = {
                statementFilter = StatementFilterGroup.ALL
                showStatementPreview = true
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryBlueGradient)
                .testTag("view_reports_financial_statement")
        ) {
            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Full Financial Statement (مالیاتی اسٹیٹمنٹ)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val statement = StatementBuilder.buildFinancialStatement(
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
                    ExportHelper.exportAndShareStatementPdf(context, statement)
                },
                modifier = Modifier.weight(1f).testTag("export_reports_pdf")
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.exportPdf, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = {
                    val headers = listOf("Date", "Type", "Category/Customer", "Amount (PKR)")
                    val rows = sales.map { listOf(it.date, "Sale", it.customerName, it.total.toLong().toString()) } +
                        expenses.map { listOf(it.date, if (it.capitalInvestment) "Factory Build" else "Expense", it.category, it.amount.toLong().toString()) }
                    ExportHelper.exportAndShareCsv(context, "brohi_financial_ledger", headers, rows)
                },
                modifier = Modifier.weight(1f).testTag("export_reports_excel")
            ) {
                Icon(Icons.Filled.TableChart, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.exportExcel, fontSize = 12.sp)
            }
        }
    }

    if (showStatementPreview) {
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
            onExportPdf = { ExportHelper.exportAndShareStatementPdf(context, statement) },
            onDismiss = { showStatementPreview = false }
        )
    }
}

@Composable
private fun ReportRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: FactorySettings,
    darkMode: Boolean,
    language: String,
    activeRole: UserRole,
    canSwitchWorkspaceRole: Boolean,
    hasPendingCloudSync: Boolean = false,
    appRelease: AppReleaseConfig? = null,
    isUpdateAvailable: Boolean = false,
    isCheckingUpdate: Boolean = false,
    lastUpdateCheckedMessage: String? = null,
    updateDownloadState: AppUpdateDownloadState = AppUpdateDownloadState.Idle,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangeLanguage: (String) -> Unit,
    onChangeRole: (UserRole) -> Unit,
    onSaveSettings: (FactorySettings) -> Unit,
    onClearDemoData: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    onStartUpdateDownload: () -> Unit = {},
    onInstallOrGrantPermission: () -> Unit = {},
    onPublishAppUpdate: (
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotesEn: String,
        releaseNotesUr: String,
        mandatory: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },
    aiAuditEnabled: Boolean = false,
    aiAuditAutoWatch: Boolean = true,
    aiAuditPendingCount: Int = 0,
    aiAuditTotalCount: Int = 0,
    onToggleAiAuditEnabled: (Boolean) -> Unit = {},
    onToggleAiAuditAutoWatch: (Boolean) -> Unit = {},
    onOpenAiAuditInbox: () -> Unit = {},
    onClearAiAuditInbox: () -> Unit = {}
) {
    var companyName by remember(settings.companyName) { mutableStateOf(settings.companyName) }
    var phoneDigits by remember(settings.phone) { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(settings.phone)) }
    var address by remember(settings.address) { mutableStateOf(settings.address) }
    var blockRate by remember(settings.iceBlockRate) { mutableStateOf(settings.iceBlockRate.toLong().toString()) }
    var cubeRate by remember(settings.iceCubeRate) { mutableStateOf(settings.iceCubeRate.toLong().toString()) }
    var startDate by remember(settings.operationsStartDate) { mutableStateOf(settings.operationsStartDate) }
    var showClearDemoConfirm by remember { mutableStateOf(false) }
    var triedSettingsSave by remember { mutableStateOf(false) }
    var settingsPopupMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Appearance & Language Card
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("App Preferences", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode (Blue Identity)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Switches between Ice Blue Light & Deep Blue Dark", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = darkMode, onCheckedChange = onToggleDarkMode, modifier = Modifier.testTag("settings_dark_mode_switch"))
                }

                HorizontalDivider()
                Text("Language / زبان", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = language == "en",
                        onClick = { onChangeLanguage("en") },
                        label = { Text("English (LTR)") }
                    )
                    FilterChip(
                        selected = language == "ur",
                        onClick = { onChangeLanguage("ur") },
                        label = { Text("اردو (RTL)") }
                    )
                }

                if (canSwitchWorkspaceRole) {
                    HorizontalDivider()
                    Text("Workspace Role View (عہدہ ویو)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.forEach { r ->
                            FilterChip(
                                selected = activeRole == r,
                                onClick = { onChangeRole(r) },
                                label = { Text(r.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // SUPER ADMIN ONLY: AI Editor & Prompt Generator (Device-Local Smart Live Watcher)
        if (canSwitchWorkspaceRole) {
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    "AI Editor & Prompt Generator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = DeepBlue
                                )
                                Text(
                                    "Super Admin Only • Smart In-App Live Watcher (Device Local)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = aiAuditEnabled,
                            onCheckedChange = onToggleAiAuditEnabled,
                            modifier = Modifier.testTag("settings_ai_audit_switch")
                        )
                    }

                    if (aiAuditEnabled) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Watch Mode (خودکار نگرانی)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "Silently audits visited screens & live runtime error logs without slowing phone.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = aiAuditAutoWatch,
                                onCheckedChange = onToggleAiAuditAutoWatch,
                                modifier = Modifier.testTag("settings_ai_autowatch_switch")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAiAuditInbox,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("settings_open_audit_inbox_button")
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (aiAuditPendingCount > 0) "Audit Inbox ($aiAuditPendingCount)" else "Audit Inbox",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        if (aiAuditTotalCount > 0) {
                            OutlinedButton(
                                onClick = onClearAiAuditInbox,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                            ) {
                                Text("Clear", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sync Status Card
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (hasPendingCloudSync) Icons.Filled.Sync else Icons.Filled.CloudDone,
                    contentDescription = null,
                    tint = if (hasPendingCloudSync) WarningOrange else SuccessGreen
                )
                Column {
                    Text("Local-First Storage & Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = if (hasPendingCloudSync) {
                            "Saved in Local DB & Image Cache • Syncing pending edits to Cloud..."
                        } else {
                            "Data & Images stored locally first • Auto-synced with Cloud DB"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Live App Updates & Release Channel (Accessible to Super Admin, Admin, Manager & Staff)
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("App Updates (ایپ اپڈیٹس)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Live AI Studio Build",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdate,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier.testTag("check_for_updates_button")
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Check Updates", fontSize = 12.sp)
                        }
                    }
                }

                if (!lastUpdateCheckedMessage.isNullOrBlank()) {
                    Text(
                        text = lastUpdateCheckedMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isUpdateAvailable) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isUpdateAvailable && appRelease != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SuccessGreen.copy(alpha = 0.11f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "New Version Ready: v${appRelease.latestVersionName} (Build ${appRelease.latestVersionCode})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SuccessGreen
                            )
                            if (appRelease.releaseNotes.isNotBlank()) {
                                Text(appRelease.releaseNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            if (appRelease.releaseNotesUrdu.isNotBlank()) {
                                Text(appRelease.releaseNotesUrdu, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            when (val st = updateDownloadState) {
                                is AppUpdateDownloadState.Downloading -> {
                                    LinearProgressIndicator(
                                        progress = { (st.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = SuccessGreen
                                    )
                                    Text("Downloading APK... ${st.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                is AppUpdateDownloadState.ReadyToInstall -> {
                                    Button(
                                        onClick = onInstallOrGrantPermission,
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.fillMaxWidth().testTag("install_downloaded_apk_button")
                                    ) {
                                        Text("Install Downloaded Update (اپڈیٹ انسٹال کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is AppUpdateDownloadState.NeedsInstallPermission -> {
                                    Button(
                                        onClick = onInstallOrGrantPermission,
                                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                        modifier = Modifier.fillMaxWidth().testTag("grant_install_permission_button")
                                    ) {
                                        Text("Allow Unknown Apps & Install (اجازت دیں)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is AppUpdateDownloadState.Failed -> {
                                    Text(st.message, fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                                    Button(
                                        onClick = onStartUpdateDownload,
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Retry Update Download", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                AppUpdateDownloadState.Idle -> {
                                    Button(
                                        onClick = onStartUpdateDownload,
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        modifier = Modifier.fillMaxWidth().testTag("settings_update_now_button")
                                    ) {
                                        Text("Update Now (ابھی اپڈیٹ کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Super Admin Only: Publish New Update to All Users
        if (activeRole.isOwner) {
            var showPublishPanel by remember { mutableStateOf(false) }
            var pubVersionCode by remember(appRelease) {
                mutableStateOf((appRelease?.latestVersionCode?.coerceAtLeast(BuildConfig.VERSION_CODE) ?: BuildConfig.VERSION_CODE).toString())
            }
            var pubVersionName by remember(appRelease) { mutableStateOf(appRelease?.latestVersionName?.ifBlank { null } ?: BuildConfig.VERSION_NAME) }
            var pubApkUrl by remember(appRelease) {
                mutableStateOf(
                    appRelease?.apkDownloadUrl?.ifBlank { null }
                        ?: com.example.data.util.AppUpdateManager.DEFAULT_APK_DOWNLOAD_URL
                )
            }
            var pubNotesEn by remember(appRelease) {
                mutableStateOf(
                    appRelease?.releaseNotes?.ifBlank { null }
                        ?: "New Two-Sided Digital CNIC Card with QR Code, Exact Notification Badge Count, 5s Splash Screen with SHIPDAR & TECH branding, and Enhanced Security."
                )
            }
            var pubNotesUr by remember(appRelease) {
                mutableStateOf(
                    appRelease?.releaseNotesUrdu?.ifBlank { null }
                        ?: "نیا ٹو سائیڈڈ ڈیجیٹل شناختی کارڈ مع کیو آر کوڈ، نوٹیفکیشن بیج کاؤنٹ، برانڈڈ سپلیش اسکرین اور سیکیورٹی اپڈیٹس۔"
                )
            }
            var pubMandatory by remember(appRelease) { mutableStateOf(appRelease?.mandatory ?: false) }

            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Publish App Update (سپر ایڈمن ریلیز)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Broadcast an APK update alert to all approved Staff, Managers & Admins.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showPublishPanel = !showPublishPanel }) {
                            Text(if (showPublishPanel) "Hide" else "Configure", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (showPublishPanel) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            CrystalTextField(
                                value = pubVersionCode,
                                onValueChange = { pubVersionCode = it.filter { ch -> ch.isDigit() } },
                                englishLabel = "Build #",
                                urduLabel = "ورژن کوڈ",
                                leadingIcon = Icons.Filled.SystemUpdate,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            CrystalTextField(
                                value = pubVersionName,
                                onValueChange = { pubVersionName = it },
                                englishLabel = "Version",
                                urduLabel = "نام",
                                leadingIcon = Icons.Filled.Title,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CrystalTextField(
                            value = pubApkUrl,
                            onValueChange = { pubApkUrl = it },
                            englishLabel = "APK HTTPS URL",
                            urduLabel = "اے پی کے لنک",
                            leadingIcon = Icons.Filled.Link,
                            placeholder = "https://.../brohi-ice-factory.apk",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        CrystalTextField(
                            value = pubNotesEn,
                            onValueChange = { pubNotesEn = it },
                            englishLabel = "Release Notes (English)",
                            urduLabel = "",
                            leadingIcon = Icons.Filled.Description
                        )
                        CrystalTextField(
                            value = pubNotesUr,
                            onValueChange = { pubNotesUr = it },
                            englishLabel = "Release Notes (Urdu)",
                            urduLabel = "تفصیل اردو میں",
                            leadingIcon = Icons.Filled.Description
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mandatory Update (لازمی اپڈیٹ)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Switch(checked = pubMandatory, onCheckedChange = { pubMandatory = it })
                        }
                        Button(
                            onClick = {
                                val vc = pubVersionCode.toIntOrNull() ?: 0
                                onPublishAppUpdate(vc, pubVersionName, pubApkUrl, pubNotesEn, pubNotesUr, pubMandatory)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("publish_update_button")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Publish Update to All Users", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Factory Info & Rates (Super Admin editable)
        if (activeRole.isOwner) {
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Factory Info & Product Rates", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    CrystalTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        englishLabel = "Company Name",
                        urduLabel = "کمپنی کا نام",
                        leadingIcon = Icons.Filled.Business
                    )
                    PhoneCrystalTextField(
                        digitsValue = phoneDigits,
                        onDigitsChange = { phoneDigits = it },
                        englishLabel = "Factory Phone",
                        urduLabel = "فیکٹری فون",
                        isError = triedSettingsSave && phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)
                    )
                    CrystalTextField(
                        value = address,
                        onValueChange = { address = it },
                        englishLabel = "Factory Address",
                        urduLabel = "فیکٹری پتہ",
                        leadingIcon = Icons.Filled.LocationOn
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CrystalTextField(
                            value = blockRate,
                            onValueChange = { blockRate = it },
                            englishLabel = "Ice Block (Rs.)",
                            urduLabel = "",
                            leadingIcon = Icons.Filled.AcUnit,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        CrystalTextField(
                            value = cubeRate,
                            onValueChange = { cubeRate = it },
                            englishLabel = "Ice Cube (Rs.)",
                            urduLabel = "",
                            leadingIcon = Icons.Filled.AcUnit,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CrystalTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        englishLabel = "Operations Start Date",
                        urduLabel = "آغاز کی تاریخ",
                        leadingIcon = Icons.Filled.CalendarMonth
                    )
                    Button(
                        onClick = {
                            triedSettingsSave = true
                            if (phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)) {
                                settingsPopupMessage = "Factory phone number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nفون نمبر مکمل نہیں ہے، براہ کرم 11 ہندسے درج کریں۔"
                                return@Button
                            }
                            onSaveSettings(
                                settings.copy(
                                    companyName = companyName.trim(),
                                    phone = PakistaniFormatters.formatPhone(phoneDigits),
                                    address = address.trim(),
                                    iceBlockRate = blockRate.toDoubleOrNull() ?: 1200.0,
                                    iceCubeRate = cubeRate.toDoubleOrNull() ?: 850.0,
                                    operationsStartDate = startDate.trim().ifBlank { "2026-01-01" }
                                )
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryBlueGradient)
                    ) {
                        Text("Save Factory Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    ValidationPopupDialog(
                        title = "Number Is Not Complete (نمبر مکمل نہیں ہے)",
                        message = settingsPopupMessage,
                        onDismiss = { settingsPopupMessage = null }
                    )
                }
            }

            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sample / [Demo] Data Management", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DangerRed)
                    Text(
                        "Remove all pre-loaded [Demo] staff, attendance, production, sales, customers, expenses, and machinery entries in one tap.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showClearDemoConfirm = true },
                        border = BorderStroke(1.dp, DangerRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All [Demo] Sample Records", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showClearDemoConfirm) {
        CrystalFormDialog(
            onDismissRequest = { showClearDemoConfirm = false },
            title = "Clear All [Demo] Records?",
            subtitle = "نمونہ ریکارڈز مستقل حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Clear [Demo] Data",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onClearDemoData()
                showClearDemoConfirm = false
            }
        ) {
            Text(
                "All pre-loaded [Demo] sample records across staff, attendance, sales, production, expenses, and equipment will be permanently deleted. Your own added records remain untouched.",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AboutScreen(
    appRelease: AppReleaseConfig? = null,
    isUpdateAvailable: Boolean = false,
    isCheckingUpdate: Boolean = false,
    lastUpdateCheckedMessage: String? = null,
    updateDownloadState: AppUpdateDownloadState = AppUpdateDownloadState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onStartUpdateDownload: () -> Unit = {},
    onInstallOrGrantPermission: () -> Unit = {},
    onOpenUpdatesScreen: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(PrimaryBlueGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AcUnit,
                contentDescription = "Brohi Ice Factory",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }
        Text(
            text = "Brohi Ice Factory",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DeepBlue
        )
        Text(
            text = "Sales, production, attendance, salary and expenses of your ice factory in one app.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutRow("Version", "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                HorizontalDivider()
                AboutRow("Update Channel", "GitHub (${com.example.data.util.AppUpdateManager.GITHUB_OWNER}/${com.example.data.util.AppUpdateManager.GITHUB_REPO})")
                HorizontalDivider()
                AboutRow("Year", "2026")
                HorizontalDivider()
                AboutRow("Prepared By", "SHIPDAR & TECH")
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Contact Number", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                        Text("0333-2862498", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                    }
                }
            }
        }

        AppUpdateChannelCard(
            appRelease = appRelease,
            isUpdateAvailable = isUpdateAvailable,
            isCheckingUpdate = isCheckingUpdate,
            lastUpdateCheckedMessage = lastUpdateCheckedMessage,
            updateDownloadState = updateDownloadState,
            onCheckForUpdates = onCheckForUpdates,
            onStartUpdateDownload = onStartUpdateDownload,
            onInstallOrGrantPermission = onInstallOrGrantPermission,
            onOpenFullUpdates = onOpenUpdatesScreen
        )
    }
}

@Composable
fun AppUpdatesScreen(
    appRelease: AppReleaseConfig?,
    isUpdateAvailable: Boolean,
    isCheckingUpdate: Boolean,
    lastUpdateCheckedMessage: String?,
    updateDownloadState: AppUpdateDownloadState,
    onCheckForUpdates: () -> Unit,
    onStartUpdateDownload: () -> Unit,
    onInstallOrGrantPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryBlueGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = "App Updates",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "GitHub Direct Auto-Updates",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepBlue
                    )
                    Text(
                        text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RoyalBlue
                    )
                    Text(
                        text = "براہِ راست گِٹ ہب آٹو اپڈیٹ • ${com.example.data.util.AppUpdateManager.GITHUB_OWNER}/${com.example.data.util.AppUpdateManager.GITHUB_REPO}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AppUpdateChannelCard(
            appRelease = appRelease,
            isUpdateAvailable = isUpdateAvailable,
            isCheckingUpdate = isCheckingUpdate,
            lastUpdateCheckedMessage = lastUpdateCheckedMessage,
            updateDownloadState = updateDownloadState,
            onCheckForUpdates = onCheckForUpdates,
            onStartUpdateDownload = onStartUpdateDownload,
            onInstallOrGrantPermission = onInstallOrGrantPermission
        )

        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "How Direct GitHub Updates Work",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepBlue
                )
                Text(
                    text = "• Every time new code is pushed to GitHub, a signed APK is automatically generated and published to GitHub Releases.\n• All approved users (Super Admin, Admin, Manager & Staff) automatically receive the update popup when a newer build is ready — just tap Update Now.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "گٹ ہب پر نیا کوڈ پش ہوتے ہی ایپ خود بخود تیار ہو کر ریلیز ہو جاتی ہے اور تمام صارفین کے پاس اپڈیٹ کا پاپ اپ آ جاتا ہے۔",
                    fontSize = 11.5.sp,
                    color = RoyalBlue,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AppUpdateChannelCard(
    appRelease: AppReleaseConfig?,
    isUpdateAvailable: Boolean,
    isCheckingUpdate: Boolean,
    lastUpdateCheckedMessage: String?,
    updateDownloadState: AppUpdateDownloadState,
    onCheckForUpdates: () -> Unit,
    onStartUpdateDownload: () -> Unit,
    onInstallOrGrantPermission: () -> Unit,
    onOpenFullUpdates: (() -> Unit)? = null
) {
    BrohiCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SystemUpdate,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Check for Updates (اپڈیٹ چیک)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = if (appRelease != null) {
                            "Latest Published: v${appRelease.latestVersionName} (Build ${appRelease.latestVersionCode})"
                        } else {
                            "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("about_check_updates_button")
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Check Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!lastUpdateCheckedMessage.isNullOrBlank()) {
                Text(
                    text = lastUpdateCheckedMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isUpdateAvailable) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isUpdateAvailable && appRelease != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreen.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "New Version Ready: v${appRelease.latestVersionName} (Build ${appRelease.latestVersionCode})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SuccessGreen
                        )
                        if (appRelease.releaseNotes.isNotBlank()) {
                            Text(appRelease.releaseNotes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (appRelease.releaseNotesUrdu.isNotBlank()) {
                            Text(appRelease.releaseNotesUrdu, fontSize = 12.sp, color = RoyalBlue)
                        }

                        when (val st = updateDownloadState) {
                            is AppUpdateDownloadState.Downloading -> {
                                LinearProgressIndicator(
                                    progress = { (st.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = SuccessGreen
                                )
                                Text("Downloading APK... ${st.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            is AppUpdateDownloadState.ReadyToInstall -> {
                                Button(
                                    onClick = onInstallOrGrantPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("about_install_apk_button")
                                ) {
                                    Text("Install Downloaded Update (اپڈیٹ انسٹال کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AppUpdateDownloadState.NeedsInstallPermission -> {
                                Button(
                                    onClick = onInstallOrGrantPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange),
                                    modifier = Modifier.fillMaxWidth().testTag("about_grant_permission_button")
                                ) {
                                    Text("Allow Unknown Apps & Install (اجازت دیں)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AppUpdateDownloadState.Failed -> {
                                Text(st.message, fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                                Button(
                                    onClick = onStartUpdateDownload,
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Update Download", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            AppUpdateDownloadState.Idle -> {
                                Button(
                                    onClick = onStartUpdateDownload,
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.fillMaxWidth().testTag("about_update_now_button")
                                ) {
                                    Text("Update Now (ابھی اپڈیٹ کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (onOpenFullUpdates != null) {
                OutlinedButton(
                    onClick = onOpenFullUpdates,
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("about_open_updates_screen_button")
                ) {
                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open App Updates Page (اپڈیٹ صفحہ کھولیں)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepBlue)
                }
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoticesScreen(
    activeRole: UserRole,
    notices: List<FactoryNotice>,
    reporterName: String,
    onSaveNotice: (FactoryNotice) -> Unit,
    onDeleteNotice: (String) -> Unit
) {
    var editingNotice by remember { mutableStateOf<FactoryNotice?>(null) }
    var showComposer by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FactoryNotice?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activeRole.canEditOrDelete) {
            Button(
                onClick = {
                    editingNotice = null
                    showComposer = true
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryBlueGradient)
                    .testTag("add_notice_button")
            ) {
                Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                Spacer(modifier = Modifier.width(8.dp))
                BilingualPillLabel(
                    english = "Publish Notice / Holiday",
                    urdu = "نیا اعلان جاری کریں",
                    textColor = Color.White
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEAF2FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Official Brohi Ice Factory Holiday & Shift Announcements (فیکٹری کے سرکاری اعلانات)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlue
                    )
                }
            }
        }

        if (notices.isEmpty()) {
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(40.dp))
                    Text("No Factory Notices Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Holiday notices, shift updates, and salary announcements appear here.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        notices.forEach { notice ->
            val (badgeBg, badgeFg, catUrdu) = when (notice.noticeType.lowercase()) {
                "holiday" -> Triple(AccentPurpleSoft, AccentPurple, "چھٹی")
                "urgent" -> Triple(DangerRedSoft, DangerRed, "ضروری")
                "salary" -> Triple(SuccessGreenSoft, SuccessGreen, "تنخواہ")
                else -> Triple(Color(0xFFEAF2FF), RoyalBlue, "اعلان")
            }

            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                                Text(
                                    text = "${notice.noticeType} • $catUrdu",
                                    color = badgeFg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (!notice.active) {
                                Surface(shape = RoundedCornerShape(8.dp), color = WarningOrangeSoft) {
                                    Text(
                                        text = "Archived",
                                        color = WarningOrange,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = PakistaniFormatters.formatDisplayDate(notice.date),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(notice.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    if (notice.titleUrdu.isNotBlank()) {
                        Text(notice.titleUrdu, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = DeepBlue)
                    }
                    if (notice.message.isNotBlank()) {
                        Text(notice.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (notice.messageUrdu.isNotBlank()) {
                        Text(notice.messageUrdu, fontSize = 12.sp, color = RoyalBlue)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "By ${notice.createdByName.ifBlank { "Management" }}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activeRole.canEditOrDelete) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingNotice = notice
                                        showComposer = true
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit notice", tint = RoyalBlue, modifier = Modifier.size(17.dp))
                                }
                                IconButton(
                                    onClick = { deleteTarget = notice },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete notice", tint = DangerRed, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showComposer) {
        val base = editingNotice
        var category by remember(base) { mutableStateOf(base?.noticeType ?: "Holiday") }
        var title by remember(base) { mutableStateOf(base?.title ?: "") }
        var titleUrdu by remember(base) { mutableStateOf(base?.titleUrdu ?: "") }
        var message by remember(base) { mutableStateOf(base?.message ?: "") }
        var messageUrdu by remember(base) { mutableStateOf(base?.messageUrdu ?: "") }
        var isActive by remember(base) { mutableStateOf(base?.active ?: true) }
        var error by remember { mutableStateOf<String?>(null) }

        CrystalFormDialog(
            onDismissRequest = { showComposer = false },
            title = if (base == null) "Publish Factory Notice (اعلان)" else "Edit Notice (ترمیم)",
            subtitle = "Broadcast factory holidays, urgent alerts & salary updates",
            icon = Icons.Filled.Campaign,
            confirmText = if (base == null) "Publish (شائع کریں)" else "Save Notice",
            dismissText = "Cancel",
            onConfirm = {
                if (title.isBlank()) {
                    error = "Please enter a notice title"
                    return@CrystalFormDialog
                }
                onSaveNotice(
                    (base ?: FactoryNotice()).copy(
                        noticeType = category,
                        title = title.trim(),
                        titleUrdu = titleUrdu.trim(),
                        message = message.trim(),
                        messageUrdu = messageUrdu.trim(),
                        createdByName = reporterName,
                        active = isActive,
                        date = base?.date?.ifBlank { PakistaniFormatters.todayIso() } ?: PakistaniFormatters.todayIso()
                    )
                )
                showComposer = false
            }
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Holiday", "Urgent", "Salary", "General").forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(cat, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
            CrystalTextField(
                value = title,
                onValueChange = { title = it; error = null },
                englishLabel = "Title (English) *",
                urduLabel = "عنوان",
                leadingIcon = Icons.Filled.Title,
                placeholder = "e.g. Tomorrow is Factory Holiday"
            )
            CrystalTextField(
                value = titleUrdu,
                onValueChange = { titleUrdu = it },
                englishLabel = "Title (Urdu)",
                urduLabel = "اردو عنوان",
                leadingIcon = Icons.Filled.Campaign,
                placeholder = "مثلاً کل فیکٹری بند رہے گی"
            )
            CrystalTextField(
                value = message,
                onValueChange = { message = it },
                englishLabel = "Details (English)",
                urduLabel = "تفصیل",
                leadingIcon = Icons.Filled.Description,
                singleLine = false
            )
            CrystalTextField(
                value = messageUrdu,
                onValueChange = { messageUrdu = it },
                englishLabel = "Details (Urdu)",
                urduLabel = "اردو پیغام",
                leadingIcon = Icons.Filled.Description,
                singleLine = false
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active on Dashboard (فعال)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
            if (!error.isNullOrBlank()) {
                Text(error!!, color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    deleteTarget?.let { n ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Notice?",
            subtitle = "اعلان مستقل حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteNotice(n.id)
                deleteTarget = null
            }
        ) {
            Text("Remove notice '${n.title}' permanently?", fontSize = 13.sp)
        }
    }
}
