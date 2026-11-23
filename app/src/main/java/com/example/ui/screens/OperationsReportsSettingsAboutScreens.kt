package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
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
import com.example.ui.components.IcyCardWaveCorner
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.IcyGlossyIconTile
import com.example.ui.components.IcyGlossyTone
import com.example.ui.components.IcyPillChip
import com.example.ui.components.IcySplashProgressBar
import com.example.ui.components.PhoneCrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.components.ValidationPopupDialog
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentPurpleSoft
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.IcyGlowShadow
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

        IcyGlossyButton(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log $selectedType Reading / Cost", color = Color.White, fontWeight = FontWeight.Bold)
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
        IcyGlossyButton(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.Build, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Ice Machine / Compressor", color = Color.White, fontWeight = FontWeight.Bold)
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
        IcyGlossyButton(
            onClick = { showAdd = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Supplier (Salt / Ammonia / Bags)", color = Color.White, fontWeight = FontWeight.Bold)
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
        IcyGlossyButton(
            onClick = { showAdd = true },
            tone = IcyGlossyTone.RED,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Safety Incident", color = Color.White, fontWeight = FontWeight.Bold)
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

        IcyGlossyButton(
            onClick = {
                statementFilter = StatementFilterGroup.ALL
                showStatementPreview = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
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
                        IcyGlossyButton(
                            onClick = onOpenAiAuditInbox,
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
                    IcyGlossyButton(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdate,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        modifier = Modifier.testTag("check_for_updates_button")
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Check Updates", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    IcySplashProgressBar(
                                        progressPercent = st.progressPercent,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text("Downloading APK... ${st.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                                }
                                is AppUpdateDownloadState.ReadyToInstall -> {
                                    IcyGlossyButton(
                                        onClick = onInstallOrGrantPermission,
                                        tone = IcyGlossyTone.GREEN,
                                        modifier = Modifier.fillMaxWidth().testTag("install_downloaded_apk_button")
                                    ) {
                                        Text("Install Downloaded Update (اپڈیٹ انسٹال کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                is AppUpdateDownloadState.Failed -> {
                                    Text(st.message, fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                                    IcyGlossyButton(
                                        onClick = onStartUpdateDownload,
                                        tone = IcyGlossyTone.GREEN,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Retry Update Download", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                AppUpdateDownloadState.Idle -> {
                                    IcyGlossyButton(
                                        onClick = onStartUpdateDownload,
                                        tone = IcyGlossyTone.GREEN,
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
                        IcyGlossyButton(
                            onClick = {
                                val vc = pubVersionCode.toIntOrNull() ?: 0
                                onPublishAppUpdate(vc, pubVersionName, pubApkUrl, pubNotesEn, pubNotesUr, pubMandatory)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("publish_update_button")
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
                    IcyGlossyButton(
                        onClick = {
                            triedSettingsSave = true
                            if (phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)) {
                                settingsPopupMessage = "Factory phone number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nفون نمبر مکمل نہیں ہے، براہ کرم 11 ہندسے درج کریں۔"
                                return@IcyGlossyButton
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
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
    notificationCount: Int = 0,
    onBackClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit,
    onStartUpdateDownload: () -> Unit,
    onInstallOrGrantPermission: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val headingColor = if (darkTheme) Color.White else DeepBlue
    val subtitleColor = if (darkTheme) Color(0xFFBBD3FF) else Color(0xFF3E5682)
    val dividerColor = if (darkTheme) Color(0xFF2B4B85) else Color(0xFFBFD7FA)

    val bulletLines = remember(appRelease, isUpdateAvailable) {
        val raw = appRelease?.releaseNotes.orEmpty()
            .lines()
            .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotBlank() }
        val defaults = listOf(
            "New features and improvements.",
            "Enhanced performance and stability.",
            "Bug fixes and better user experience."
        )
        val combined = if (raw.isNotEmpty()) raw.take(3) else defaults
        val urNote = appRelease?.releaseNotesUrdu?.trim().orEmpty()
        if (isUpdateAvailable && urNote.isNotBlank()) combined + urNote else combined
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Custom Frosted Icy Header matching the reference 100%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp, bottom = 20.dp)
        ) {
            // Sweeping blue curved wave at base of header + crystal ice splash on right
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .align(Alignment.BottomCenter)
            ) {
                val w = size.width
                val h = size.height
                val sweep = Path().apply {
                    moveTo(0f, h * 0.30f)
                    cubicTo(w * 0.25f, h * 0.88f, w * 0.72f, h * 0.94f, w, h * 0.68f)
                    lineTo(w, h * 0.92f)
                    cubicTo(w * 0.68f, h * 1.04f, w * 0.22f, h * 0.96f, 0f, h * 0.46f)
                    close()
                }
                drawPath(
                    path = sweep,
                    brush = Brush.horizontalGradient(
                        colors = if (darkTheme) {
                            listOf(Color(0x5538BDF8), Color(0x880E6CF5))
                        } else {
                            listOf(Color(0x993EB6FF), Color(0xCC58BFFF), Color(0x9995D5FF))
                        }
                    )
                )
            }

            // Blended top-right ice cube splash graphic with feathered radial mask (no rectangular border)
            Image(
                painter = painterResource(R.drawable.img_icy_header_cubes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 14.dp)
                    .size(width = 172.dp, height = 86.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.radialGradient(
                                0.0f to Color.White,
                                0.62f to Color.White,
                                1.0f to Color.Transparent,
                                center = Offset(size.width * 0.56f, size.height * 0.58f),
                                radius = size.width * 0.60f
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("subscreen_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = headingColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "App Updates",
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = headingColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Stay Updated, Get the Latest Features",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Frosted circular Notification Bell with red count badge
                Box(modifier = Modifier.padding(end = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = CircleShape,
                                ambientColor = IcyGlowShadow.copy(alpha = 0.25f),
                                spotColor = IcyGlowShadow.copy(alpha = 0.25f)
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    if (darkTheme) listOf(Color(0xFF1A3568), Color(0xFF11244A))
                                    else listOf(Color.White, Color(0xFFEAF3FF))
                                )
                            )
                            .border(
                                width = 1.4.dp,
                                color = if (darkTheme) Color(0xFF3A5F9E) else Color.White,
                                shape = CircleShape
                            )
                            .clickable(onClick = onNotificationClick)
                            .testTag("updates_notification_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = headingColor,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    if (notificationCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = DangerRed,
                            border = BorderStroke(1.2.dp, Color.White),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Glossy Factory Tile + Version/Build Pills + Title
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    IcyGlossyIconTile(
                        size = 66.dp,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        FactoryIconSilhouette(
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.2.dp)
                            .height(60.dp)
                            .background(dividerColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IcyPillChip(text = "Version : v${BuildConfig.VERSION_NAME}")
                            IcyPillChip(text = "Build : ${BuildConfig.VERSION_CODE}")
                        }
                        Text(
                            text = "Brohi Ice Factory",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = headingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Smart Management for Ice Factory",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Card 2: Check for Updates + Glossy Check Now Button
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sync,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(40.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.2.dp)
                            .height(48.dp)
                            .background(dividerColor)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Check for Updates",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = headingColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = lastUpdateCheckedMessage?.takeIf { it.isNotBlank() }
                                ?: "Check for latest version",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isUpdateAvailable) SuccessGreen else subtitleColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IcyGlossyButton(
                        onClick = onCheckForUpdates,
                        enabled = !isCheckingUpdate,
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("about_check_updates_button")
                    ) {
                        if (isCheckingUpdate) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Checking",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Check Now",
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Card 3: Update Details with glossy (i) orb, glowing bullets & bottom-right icy wave
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IcyCardWaveCorner(
                        darkTheme = darkTheme,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 185.dp, height = 105.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IcyGlossyIconTile(
                                size = 34.dp,
                                shape = CircleShape
                            ) {
                                Canvas(modifier = Modifier.size(18.dp)) {
                                    val cx = size.width / 2f
                                    drawCircle(
                                        color = Color.White,
                                        radius = size.width * 0.12f,
                                        center = Offset(cx, size.height * 0.24f)
                                    )
                                    drawRoundRect(
                                        color = Color.White,
                                        topLeft = Offset(cx - size.width * 0.11f, size.height * 0.43f),
                                        size = Size(size.width * 0.22f, size.height * 0.44f),
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                }
                            }
                            Text(
                                text = "Update Details",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = headingColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (isUpdateAvailable && appRelease != null) {
                                IcyPillChip(text = "v${appRelease.latestVersionName}")
                            }
                        }

                        Column(
                            modifier = Modifier.padding(start = 6.dp, end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            bulletLines.forEach { line ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 5.dp)
                                            .size(8.dp)
                                            .shadow(3.dp, CircleShape, ambientColor = IcyGlowShadow, spotColor = IcyGlowShadow)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(Color(0xFF56C3FF), RoyalBlue)
                                                )
                                            )
                                    )
                                    Text(
                                        text = line,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = headingColor.copy(alpha = 0.90f),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 4: Live Download & Install Card matching reference 100% (Download Orb, Speed Pill, Ice Splash Progress Bar, % & ETA)
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IcyCardWaveCorner(
                        darkTheme = darkTheme,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(width = 195.dp, height = 115.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val st = updateDownloadState
                        val targetVersionLabel = appRelease?.latestVersionName?.ifBlank { null } ?: BuildConfig.VERSION_NAME

                        val statusTitle = when (st) {
                            is AppUpdateDownloadState.Downloading -> "Downloading v$targetVersionLabel..."
                            is AppUpdateDownloadState.ReadyToInstall -> "Ready to Install v$targetVersionLabel"
                            is AppUpdateDownloadState.Failed -> "Update Download Failed"
                            AppUpdateDownloadState.Idle -> if (isUpdateAvailable && appRelease != null) {
                                "Update v$targetVersionLabel Available"
                            } else {
                                "Latest Version v${BuildConfig.VERSION_NAME}"
                            }
                        }

                        val statusSubtitle = when (st) {
                            is AppUpdateDownloadState.Downloading -> {
                                val downStr = formatBytesMb(st.downloadedBytes)
                                val totalStr = if (st.totalBytes > 0L) formatBytesMb(st.totalBytes) else "Calculating..."
                                "$downStr / $totalStr"
                            }
                            is AppUpdateDownloadState.ReadyToInstall -> "Download complete • Ready to install"
                            is AppUpdateDownloadState.Failed -> st.message
                            AppUpdateDownloadState.Idle -> if (isUpdateAvailable && appRelease != null) {
                                "Build ${BuildConfig.VERSION_CODE}  →  Build ${appRelease.latestVersionCode}"
                            } else {
                                "Build ${BuildConfig.VERSION_CODE} • Up to Date"
                            }
                        }

                        val badgeChipText = when (st) {
                            is AppUpdateDownloadState.Downloading -> formatSpeedLabel(st.speedBytesPerSec)
                            is AppUpdateDownloadState.ReadyToInstall -> "100% Ready"
                            is AppUpdateDownloadState.Failed -> "Retry"
                            AppUpdateDownloadState.Idle -> if (isUpdateAvailable && appRelease != null) {
                                "Build ${appRelease.latestVersionCode}"
                            } else {
                                "Up to Date"
                            }
                        }

                        val displayPercent = when (st) {
                            is AppUpdateDownloadState.Downloading -> st.progressPercent
                            is AppUpdateDownloadState.ReadyToInstall -> 100
                            is AppUpdateDownloadState.Failed -> 0
                            AppUpdateDownloadState.Idle -> if (isUpdateAvailable) 0 else 100
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .shadow(8.dp, CircleShape, ambientColor = IcyGlowShadow, spotColor = IcyGlowShadow)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0xFFB5DAFF), CircleShape)
                                    .padding(3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IcyGlossyIconTile(
                                    size = 52.dp,
                                    shape = CircleShape,
                                    tone = when (st) {
                                        is AppUpdateDownloadState.Failed -> IcyGlossyTone.RED
                                        else -> IcyGlossyTone.BLUE
                                    }
                                ) {
                                    Icon(
                                        imageVector = when (st) {
                                            is AppUpdateDownloadState.ReadyToInstall -> Icons.Filled.CheckCircle
                                            else -> Icons.Filled.Download
                                        },
                                        contentDescription = "Update status",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = statusTitle,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = headingColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = statusSubtitle,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (st is AppUpdateDownloadState.Failed) DangerRed else headingColor.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IcyPillChip(
                                text = badgeChipText,
                                icon = if (st is AppUpdateDownloadState.Downloading || isUpdateAvailable) Icons.Filled.Bolt else Icons.Filled.CheckCircle
                            )
                        }

                        // Crystal Ice Splash Capsule Progress Bar + Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IcySplashProgressBar(
                                progressPercent = displayPercent,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$displayPercent%",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = headingColor
                            )
                        }

                        // Clock & Estimated Time Row
                        val etaRowText = when (st) {
                            is AppUpdateDownloadState.Downloading ->
                                "Estimated Time: ${formatEtaLabel(st.estimatedRemainingSeconds)}"
                            is AppUpdateDownloadState.ReadyToInstall ->
                                "Estimated Time: Completed • Tap Install below"
                            is AppUpdateDownloadState.Failed ->
                                "Download interrupted • Tap Retry below"
                            AppUpdateDownloadState.Idle -> if (isUpdateAvailable && appRelease != null) {
                                "Ready to download v$targetVersionLabel"
                            } else {
                                "Estimated Time: Complete • All features up to date"
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = headingColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = etaRowText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = headingColor
                            )
                        }

                        when (st) {
                            is AppUpdateDownloadState.ReadyToInstall -> {
                                IcyGlossyButton(
                                    onClick = onInstallOrGrantPermission,
                                    tone = IcyGlossyTone.GREEN,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("about_install_apk_button")
                                ) {
                                    Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Install Downloaded Update (اپڈیٹ انسٹال کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AppUpdateDownloadState.Failed -> {
                                IcyGlossyButton(
                                    onClick = onStartUpdateDownload,
                                    tone = IcyGlossyTone.BLUE,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Retry Update Download", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            AppUpdateDownloadState.Idle -> {
                                if (isUpdateAvailable && appRelease != null) {
                                    IcyGlossyButton(
                                        onClick = onStartUpdateDownload,
                                        tone = IcyGlossyTone.BLUE,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("about_update_now_button")
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Update Now (ابھی اپڈیٹ کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is AppUpdateDownloadState.Downloading -> Unit
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun FactoryIconSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Smoke puffs
        drawCircle(
            color = Color.White,
            radius = w * 0.09f,
            center = Offset(w * 0.72f, h * 0.16f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.065f,
            center = Offset(w * 0.60f, h * 0.24f)
        )

        // Chimney
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.72f, h * 0.26f),
            size = Size(w * 0.14f, h * 0.34f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Main factory building with sawtooth roof
        val factoryPath = Path().apply {
            moveTo(w * 0.08f, h * 0.90f)
            lineTo(w * 0.08f, h * 0.50f)
            lineTo(w * 0.34f, h * 0.36f)
            lineTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.60f, h * 0.36f)
            lineTo(w * 0.60f, h * 0.50f)
            lineTo(w * 0.92f, h * 0.50f)
            lineTo(w * 0.92f, h * 0.90f)
            close()
        }
        drawPath(path = factoryPath, color = Color.White)

        // 3 icy-blue windows inside factory
        val winColor = Color(0xFF0E6CF5)
        val winW = w * 0.12f
        val winH = h * 0.14f
        val winY = h * 0.64f
        listOf(0.20f, 0.44f, 0.68f).forEach { xFrac ->
            drawRoundRect(
                color = winColor,
                topLeft = Offset(w * xFrac, winY),
                size = Size(winW, winH),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}

private fun formatBytesMb(bytes: Long): String {
    if (bytes <= 0L) return "0.0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return String.format(java.util.Locale.US, "%.1f MB", mb)
}

private fun formatSpeedLabel(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "Starting"
    return if (bytesPerSec >= 1024L * 1024L) {
        String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
    } else {
        "${(bytesPerSec / 1024L).coerceAtLeast(1L)} KB/s"
    }
}

private fun formatEtaLabel(seconds: Long?): String {
    if (seconds == null || seconds <= 0L) return "Calculating..."
    return if (seconds < 60L) {
        "~ $seconds sec"
    } else {
        val mins = seconds / 60L
        val rem = seconds % 60L
        "~ ${mins}m ${rem}s"
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
                IcyGlossyButton(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdate,
                    contentPadding = PaddingValues(horizontal = 15.dp, vertical = 9.dp),
                    modifier = Modifier.testTag("about_check_updates_button")
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Check Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                IcySplashProgressBar(
                                    progressPercent = st.progressPercent,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Downloading APK... ${st.progressPercent}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                            }
                            is AppUpdateDownloadState.ReadyToInstall -> {
                                IcyGlossyButton(
                                    onClick = onInstallOrGrantPermission,
                                    tone = IcyGlossyTone.GREEN,
                                    modifier = Modifier.fillMaxWidth().testTag("about_install_apk_button")
                                ) {
                                    Text("Install Downloaded Update (اپڈیٹ انسٹال کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            is AppUpdateDownloadState.Failed -> {
                                Text(st.message, fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                                IcyGlossyButton(
                                    onClick = onStartUpdateDownload,
                                    tone = IcyGlossyTone.GREEN,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Retry Update Download", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                            AppUpdateDownloadState.Idle -> {
                                IcyGlossyButton(
                                    onClick = onStartUpdateDownload,
                                    tone = IcyGlossyTone.GREEN,
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
                    border = BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.45f)),
                    shape = CircleShape,
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
            IcyGlossyButton(
                onClick = {
                    editingNotice = null
                    showComposer = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
