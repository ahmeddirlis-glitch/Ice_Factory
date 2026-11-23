package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Customer
import com.example.data.model.DeliveryItem
import com.example.data.model.FactorySettings
import com.example.data.model.InventoryMovement
import com.example.data.model.MachineItem
import com.example.data.model.ProductionRecord
import com.example.data.model.Sale
import com.example.data.model.UserRole
import com.example.data.util.ExportHelper
import com.example.data.util.PakistaniFormatters
import com.example.data.util.StatementBuilder
import com.example.data.util.StatementDocumentModel
import com.example.ui.components.BrohiCard
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.CrystalTextField
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.PhoneCrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.components.StockDonutChart
import com.example.ui.components.ValidationPopupDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.DashboardMetrics
import kotlin.math.roundToInt

@Composable
fun ProductionScreen(
    activeRole: UserRole,
    metrics: DashboardMetrics,
    productionRecords: List<ProductionRecord>,
    machines: List<MachineItem>,
    onAddProduction: (tons: Double, productType: String, machineId: String, machineName: String, notes: String?) -> Unit,
    onUpdateProduction: (ProductionRecord) -> Unit,
    onDeleteProduction: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProd by remember { mutableStateOf<ProductionRecord?>(null) }
    var deletingProd by remember { mutableStateOf<ProductionRecord?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ice cubes hero banner matching Screen 9 of mockup
        Image(
            painter = painterResource(R.drawable.img_ice_cubes_banner),
            contentDescription = "Ice Factory Production",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEAF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Factory, contentDescription = null, tint = RoyalBlue)
                    }
                    Column {
                        Text("Today Production", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            PakistaniFormatters.formatTons(metrics.todayProductionTons),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEAF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = RoyalBlue)
                    }
                    Column {
                        Text("Total This Month", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            PakistaniFormatters.formatTons(metrics.monthProductionTons),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            IcyGlossyButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_production_button")
            ) {
                Icon(Icons.Filled.AddBox, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Production", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Text("Recent Records", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            productionRecords.forEach { rec ->
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${rec.productType} • ${rec.machineName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                "${PakistaniFormatters.formatDisplayDate(rec.date)}${rec.notes?.let { " • $it" } ?: ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            PakistaniFormatters.formatTons(rec.tons),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (activeRole.canEditOrDelete) {
                            IconButton(onClick = { editingProd = rec }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit production", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { deletingProd = rec }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete production", tint = DangerRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showAddDialog) {
        var tonsText by remember { mutableStateOf("5.0") }
        var productType by remember { mutableStateOf("Ice Blocks (10kg)") }
        var notes by remember { mutableStateOf("") }
        val firstMachine = machines.firstOrNull()
        CrystalFormDialog(
            onDismissRequest = { showAddDialog = false },
            title = "Record Ice Production",
            subtitle = "برف کی پیداوار اور اسٹاک اندراج",
            icon = Icons.Filled.AcUnit,
            confirmText = "Save & Add Stock",
            dismissText = "Cancel",
            onConfirm = {
                val tons = tonsText.toDoubleOrNull() ?: 0.0
                if (tons > 0) {
                    onAddProduction(
                        tons,
                        productType,
                        firstMachine?.id ?: "mach_1",
                        firstMachine?.name ?: "Plant #1",
                        notes.ifBlank { null }
                    )
                    showAddDialog = false
                }
            }
        ) {
            CrystalTextField(
                value = tonsText,
                onValueChange = { tonsText = it },
                englishLabel = "Quantity (Tons)",
                urduLabel = "مقدار (ٹن)",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Decimal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ice Blocks (10kg)", "Ice Cubes (5kg)").forEach { p ->
                    FilterChip(
                        selected = productType == p,
                        onClick = { productType = p },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(p, fontSize = 11.sp) }
                    )
                }
            }
            CrystalTextField(
                value = notes,
                onValueChange = { notes = it },
                englishLabel = "Notes (optional)",
                urduLabel = "نوٹس",
                leadingIcon = Icons.Filled.Notes
            )
        }
    }

    editingProd?.let { target ->
        var tonsText by remember(target) { mutableStateOf(target.tons.toString()) }
        var productType by remember(target) { mutableStateOf(target.productType) }
        var notes by remember(target) { mutableStateOf(target.notes ?: "") }
        CrystalFormDialog(
            onDismissRequest = { editingProd = null },
            title = "Edit Production Record",
            subtitle = "پیداوار ریکارڈ میں ترمیم",
            icon = Icons.Filled.Edit,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val tons = tonsText.toDoubleOrNull() ?: 0.0
                if (tons > 0) {
                    onUpdateProduction(target.copy(tons = tons, productType = productType, notes = notes.ifBlank { null }))
                    editingProd = null
                }
            }
        ) {
            CrystalTextField(
                value = tonsText,
                onValueChange = { tonsText = it },
                englishLabel = "Quantity (Tons)",
                urduLabel = "مقدار (ٹن)",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Decimal
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ice Blocks (10kg)", "Ice Cubes (5kg)").forEach { p ->
                    FilterChip(
                        selected = productType == p,
                        onClick = { productType = p },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        label = { Text(p, fontSize = 11.sp) }
                    )
                }
            }
            CrystalTextField(
                value = notes,
                onValueChange = { notes = it },
                englishLabel = "Notes",
                urduLabel = "نوٹس",
                leadingIcon = Icons.Filled.Notes
            )
        }
    }

    deletingProd?.let { target ->
        CrystalFormDialog(
            onDismissRequest = { deletingProd = null },
            title = "Delete Production Record?",
            subtitle = "پیداواری ریکارڈ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteProduction(target.id)
                deletingProd = null
            }
        ) {
            Text(
                "Delete ${PakistaniFormatters.formatTons(target.tons)} (${target.productType}) on ${target.date}?",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun InventoryColdStorageScreen(
    activeRole: UserRole,
    metrics: DashboardMetrics,
    movements: List<InventoryMovement>,
    onAddMovement: (product: String, tons: Double, location: String, type: String, notes: String?) -> Unit,
    onUpdateMovement: (InventoryMovement) -> Unit,
    onDeleteMovement: (String) -> Unit,
    onViewExpensesFinance: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var editingMv by remember { mutableStateOf<InventoryMovement?>(null) }
    var deletingMv by remember { mutableStateOf<InventoryMovement?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pill tab toggle matching Screen 11 ("Inventory" | "Finance")
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Inventory") })
            Tab(selected = selectedTab == 1, onClick = {
                selectedTab = 1
                onViewExpensesFinance()
            }, text = { Text("Finance") })
        }

        // Stock Overview Donut Chart Card matching Screen 11
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Stock Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StockDonutChart(
                        totalTons = metrics.totalStockTons,
                        blocksPercent = metrics.blocksPercent,
                        cubesPercent = metrics.cubesPercent
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    ) {
                        Text(
                            PakistaniFormatters.formatTons(metrics.totalStockTons),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("Total Stock", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(RoyalBlue))
                            Text(
                                "Ice Blocks ${metrics.blocksPercent.roundToInt()}% (${PakistaniFormatters.formatTons(metrics.iceBlocksTons)})",
                                fontSize = 12.sp
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(WarningOrange))
                            Text(
                                "Ice Cubes ${metrics.cubesPercent.roundToInt()}% (${PakistaniFormatters.formatTons(metrics.iceCubesTons)})",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Expenses This Month strip matching Screen 11
        BrohiCard(modifier = Modifier.fillMaxWidth(), onClick = onViewExpensesFinance) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEAF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = RoyalBlue)
                    }
                    Column {
                        Text("Expenses (This Month)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            PakistaniFormatters.formatPkr(metrics.monthExpensesTotal),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp
                        )
                    }
                }
                Text("View Details →", color = RoyalBlue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }

        IcyGlossyButton(
            onClick = { showMoveDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("stock_movement_button")
        ) {
            Text("Record Stock In / Out", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Text("Cold Storage Movements", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        movements.forEach { mv ->
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${mv.product} (${mv.location})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (!mv.notes.isNullOrBlank()) {
                            Text(mv.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (mv.movementType == "IN") SuccessGreenSoft else DangerRedSoft
                        ) {
                            Text(
                                text = "${mv.movementType} ${PakistaniFormatters.formatTons(mv.tons)}",
                                color = if (mv.movementType == "IN") SuccessGreen else DangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (activeRole.canEditOrDelete) {
                            IconButton(onClick = { editingMv = mv }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit stock", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { deletingMv = mv }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete stock", tint = DangerRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMoveDialog) {
        var product by remember { mutableStateOf("Ice Blocks") }
        var type by remember { mutableStateOf("IN") }
        var tonsText by remember { mutableStateOf("1.5") }
        var location by remember { mutableStateOf("Cold Room A") }
        CrystalFormDialog(
            onDismissRequest = { showMoveDialog = false },
            title = "Cold Storage Movement",
            subtitle = "کولڈ اسٹوریج میں برف کی آمد و رفت",
            icon = Icons.Filled.Inventory2,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val t = tonsText.toDoubleOrNull() ?: 0.0
                if (t > 0) {
                    onAddMovement(product, t, location, type, "Manual stock adjustment")
                    showMoveDialog = false
                }
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ice Blocks", "Ice Cubes").forEach { p ->
                    FilterChip(
                        selected = product == p,
                        onClick = { product = p },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(p) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("IN", "OUT").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text("Stock $t") }
                    )
                }
            }
            CrystalTextField(
                value = tonsText,
                onValueChange = { tonsText = it },
                englishLabel = "Tons",
                urduLabel = "ٹن",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Decimal
            )
            CrystalTextField(
                value = location,
                onValueChange = { location = it },
                englishLabel = "Cold Room / Tank",
                urduLabel = "کولڈ روم / ٹینک",
                leadingIcon = Icons.Filled.LocationOn
            )
        }
    }

    editingMv?.let { target ->
        var product by remember(target) { mutableStateOf(target.product) }
        var type by remember(target) { mutableStateOf(target.movementType) }
        var tonsText by remember(target) { mutableStateOf(target.tons.toString()) }
        var location by remember(target) { mutableStateOf(target.location) }
        CrystalFormDialog(
            onDismissRequest = { editingMv = null },
            title = "Edit Stock Movement",
            subtitle = "اسٹاک موومنٹ میں ترمیم",
            icon = Icons.Filled.Edit,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val t = tonsText.toDoubleOrNull() ?: 0.0
                if (t > 0) {
                    onUpdateMovement(target.copy(product = product, movementType = type, tons = t, location = location.trim()))
                    editingMv = null
                }
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ice Blocks", "Ice Cubes").forEach { p ->
                    FilterChip(
                        selected = product == p,
                        onClick = { product = p },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(p) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("IN", "OUT").forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text("Stock $t") }
                    )
                }
            }
            CrystalTextField(
                value = tonsText,
                onValueChange = { tonsText = it },
                englishLabel = "Tons",
                urduLabel = "ٹن",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Decimal
            )
            CrystalTextField(
                value = location,
                onValueChange = { location = it },
                englishLabel = "Location",
                urduLabel = "مقام",
                leadingIcon = Icons.Filled.LocationOn
            )
        }
    }

    deletingMv?.let { target ->
        CrystalFormDialog(
            onDismissRequest = { deletingMv = null },
            title = "Delete Stock Movement?",
            subtitle = "اسٹاک اندراج حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteMovement(target.id)
                deletingMv = null
            }
        ) {
            Text("Remove ${target.movementType} ${PakistaniFormatters.formatTons(target.tons)} (${target.product})?", fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesInvoicingScreen(
    activeRole: UserRole,
    customers: List<Customer>,
    sales: List<Sale>,
    settings: FactorySettings,
    onCreateSale: (customerId: String, customerName: String, product: String, qty: Long, unitPrice: Double, total: Double, paid: Boolean) -> Unit,
    onUpdateSale: (Sale) -> Unit,
    onDeleteSale: (String) -> Unit,
    onMarkSalePaid: (Sale) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingSale by remember { mutableStateOf<Sale?>(null) }
    var deletingSale by remember { mutableStateOf<Sale?>(null) }
    var previewSale by remember { mutableStateOf<Sale?>(null) }
    var custExpanded by remember { mutableStateOf(false) }
    var prodExpanded by remember { mutableStateOf(false) }

    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    val products = listOf(
        "Ice Blocks (10kg)" to (settings.iceBlockRate.takeIf { it > 0 } ?: 1200.0),
        "Ice Cubes (5kg)" to (settings.iceCubeRate.takeIf { it > 0 } ?: 850.0)
    )
    var selectedProduct by remember { mutableStateOf(products.first()) }
    var quantity by remember { mutableLongStateOf(1L) }
    var isPaid by remember { mutableStateOf(true) }

    val totalAmount = quantity * selectedProduct.second

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("New Sale") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Invoices") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("History") })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Customer dropdown
                        ExposedDropdownMenuBox(
                            expanded = custExpanded,
                            onExpandedChange = { custExpanded = it }
                        ) {
                            CrystalTextField(
                                value = selectedCustomer?.name ?: "Walk-in Cash Customer",
                                onValueChange = {},
                                readOnly = true,
                                englishLabel = "Customer",
                                urduLabel = "گاہک",
                                leadingIcon = Icons.Filled.Person,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = custExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = custExpanded, onDismissRequest = { custExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Walk-in Cash Customer") },
                                    onClick = { selectedCustomer = null; custExpanded = false }
                                )
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (Due: ${PakistaniFormatters.formatPkr(c.balance)})") },
                                        onClick = { selectedCustomer = c; custExpanded = false }
                                    )
                                }
                            }
                        }

                        // Product dropdown
                        ExposedDropdownMenuBox(
                            expanded = prodExpanded,
                            onExpandedChange = { prodExpanded = it }
                        ) {
                            CrystalTextField(
                                value = selectedProduct.first,
                                onValueChange = {},
                                readOnly = true,
                                englishLabel = "Product",
                                urduLabel = "پروڈکٹ",
                                leadingIcon = Icons.Filled.AcUnit,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                                products.forEach { prod ->
                                    DropdownMenuItem(
                                        text = { Text("${prod.first} — ${PakistaniFormatters.formatPkr(prod.second)}") },
                                        onClick = { selectedProduct = prod; prodExpanded = false }
                                    )
                                }
                            }
                        }

                        // Quantity stepper matching Screen 10: [ -  1  + ]
                        Text("Quantity", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.testTag("qty_minus_button")
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(
                                    text = "$quantity",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.testTag("qty_plus_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isPaid,
                                onClick = { isPaid = true },
                                label = { Text("Cash / Paid") }
                            )
                            FilterChip(
                                selected = !isPaid,
                                onClick = { isPaid = false },
                                label = { Text("Credit (Udhar)") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Amount", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = PakistaniFormatters.formatPkr(totalAmount),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IcyGlossyButton(
                            onClick = {
                                onCreateSale(
                                    selectedCustomer?.id ?: "",
                                    selectedCustomer?.name ?: if (isPaid) "Walk-in Customer" else "Credit Customer",
                                    selectedProduct.first,
                                    quantity,
                                    selectedProduct.second,
                                    totalAmount,
                                    isPaid
                                )
                                quantity = 1L
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_invoice_button")
                        ) {
                            Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Invoice", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTab == 1) "All Invoices (${sales.size})" else "Recent Sales",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (selectedTab == 0) {
                    Text(
                        text = "View All →",
                        color = RoyalBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            val displaySales = if (selectedTab == 0) sales.take(5) else sales
            displaySales.forEach { sale ->
                BrohiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { previewSale = sale }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (sale.paid) SuccessGreen else WarningOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${sale.invoiceNumber} • ${sale.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${PakistaniFormatters.formatDisplayDate(sale.date)} • ${sale.product} x${sale.quantity} • ${if (sale.paid) "Paid" else "Credit"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = PakistaniFormatters.formatPkr(sale.total),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (!sale.paid) {
                                    Text(
                                        "Mark Paid",
                                        color = RoyalBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onMarkSalePaid(sale) }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { previewSale = sale },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = "View Invoice", tint = DeepBlue, modifier = Modifier.size(17.dp))
                                }
                                IconButton(
                                    onClick = { ExportHelper.exportAndShareInvoicePdf(context, sale, settings) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share Invoice PDF", tint = RoyalBlue, modifier = Modifier.size(17.dp))
                                }
                                if (activeRole.canEditOrDelete) {
                                    IconButton(onClick = { editingSale = sale }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit invoice", tint = RoyalBlue, modifier = Modifier.size(17.dp))
                                    }
                                    IconButton(onClick = { deletingSale = sale }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete invoice", tint = DangerRed, modifier = Modifier.size(17.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingSale?.let { target ->
        var custName by remember(target) { mutableStateOf(target.customerName) }
        var qtyText by remember(target) { mutableStateOf(target.quantity.toString()) }
        var unitPriceText by remember(target) { mutableStateOf(target.unitPrice.toLong().toString()) }
        var paid by remember(target) { mutableStateOf(target.paid) }
        CrystalFormDialog(
            onDismissRequest = { editingSale = null },
            title = "Edit Invoice ${target.invoiceNumber}",
            subtitle = "انوائس کی تفصیلات تبدیل کریں",
            icon = Icons.Filled.ReceiptLong,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val q = (qtyText.toLongOrNull() ?: target.quantity).coerceAtLeast(1L)
                val up = (unitPriceText.toDoubleOrNull() ?: target.unitPrice).coerceAtLeast(0.0)
                val tot = q * up
                onUpdateSale(target.copy(customerName = custName.trim().ifBlank { target.customerName }, quantity = q, unitPrice = up, total = tot, paid = paid, paidAmount = if (paid) tot else 0.0))
                editingSale = null
            }
        ) {
            CrystalTextField(
                value = custName,
                onValueChange = { custName = it },
                englishLabel = "Customer Name",
                urduLabel = "گاہک کا نام",
                leadingIcon = Icons.Filled.Person
            )
            CrystalTextField(
                value = qtyText,
                onValueChange = { qtyText = it },
                englishLabel = "Quantity",
                urduLabel = "تعداد",
                leadingIcon = Icons.Filled.Numbers,
                keyboardType = KeyboardType.Number
            )
            CrystalTextField(
                value = unitPriceText,
                onValueChange = { unitPriceText = it },
                englishLabel = "Unit Price (Rs.)",
                urduLabel = "فی عدد قیمت",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = paid,
                    onClick = { paid = true },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuccessGreen, selectedLabelColor = Color.White),
                    label = { Text("Paid (نقد)") }
                )
                FilterChip(
                    selected = !paid,
                    onClick = { paid = false },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DangerRed, selectedLabelColor = Color.White),
                    label = { Text("Credit (ادھار)") }
                )
            }
        }
    }

    deletingSale?.let { target ->
        CrystalFormDialog(
            onDismissRequest = { deletingSale = null },
            title = "Delete Invoice?",
            subtitle = "بل مستقل حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteSale(target.id)
                deletingSale = null
            }
        ) {
            Text(
                "Permanently delete ${target.invoiceNumber} (${target.customerName}) for ${PakistaniFormatters.formatPkr(target.total)}?",
                fontSize = 13.sp
            )
        }
    }
    previewSale?.let { target ->
        SalesInvoicePreviewDialog(
            sale = target,
            settings = settings,
            canMarkPaid = activeRole.canManageStaff || activeRole.canEditOrDelete || activeRole.isOwner,
            onMarkPaid = {
                onMarkSalePaid(target)
                previewSale = target.copy(paid = true, paidAmount = target.total)
            },
            onExportPdf = { ExportHelper.exportAndShareInvoicePdf(context, target, settings) },
            onDismiss = { previewSale = null }
        )
    }
}

@Composable
fun CustomersCreditScreen(
    activeRole: UserRole,
    customers: List<Customer>,
    sales: List<Sale> = emptyList(),
    settings: FactorySettings = FactorySettings(),
    onAddCustomer: (name: String, phone: String, address: String?, balance: Double) -> Unit,
    onUpdateCustomer: (Customer) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onReceivePayment: (Customer, Double) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var deletingCustomer by remember { mutableStateOf<Customer?>(null) }
    var payTarget by remember { mutableStateOf<Customer?>(null) }
    var statementPreview by remember { mutableStateOf<StatementDocumentModel?>(null) }

    val totalPending = customers.sumOf { it.balance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Total Credit Pending (Udhar)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(PakistaniFormatters.formatPkr(totalPending), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = DangerRed)
                    }
                    IcyGlossyButton(
                        onClick = { showAddDialog = true }
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = {
                        statementPreview = StatementBuilder.buildAllCreditCustomersStatement(
                            customers = customers,
                            sales = sales,
                            settings = settings
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().testTag("all_credit_statement_button")
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null, tint = DeepBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Credit Statement (ادھار اسٹیٹمنٹ)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                }
            }
        }

        customers.forEach { c ->
            BrohiCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        statementPreview = StatementBuilder.buildCustomerStatement(
                            customer = c,
                            sales = sales,
                            settings = settings
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${c.phone} ${c.address?.let { "• $it" } ?: ""}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Balance: ${PakistaniFormatters.formatPkr(c.balance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (c.balance > 0) DangerRed else SuccessGreen
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = {
                                    statementPreview = StatementBuilder.buildCustomerStatement(
                                        customer = c,
                                        sales = sales,
                                        settings = settings
                                    )
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = "Customer Statement", tint = DeepBlue, modifier = Modifier.size(18.dp))
                            }
                            if (c.balance > 0) {
                                OutlinedButton(onClick = { payTarget = c }) {
                                    Text("Receive", fontSize = 12.sp)
                                }
                            }
                            if (activeRole.canEditOrDelete) {
                                IconButton(onClick = { editingCustomer = c }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit customer", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deletingCustomer = c }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete customer", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    statementPreview?.let { model ->
        FinancialAndCreditStatementDialog(
            statement = model,
            onExportPdf = { ExportHelper.exportAndShareStatementPdf(context, model) },
            onDismiss = { statementPreview = null }
        )
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phoneDigits by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var balance by remember { mutableStateOf("0") }
        var triedSave by remember { mutableStateOf(false) }
        var popupMessage by remember { mutableStateOf<String?>(null) }
        CrystalFormDialog(
            onDismissRequest = { showAddDialog = false },
            title = "Add Customer",
            subtitle = "نیا گاہک یا دکاندار شامل کریں",
            icon = Icons.Filled.PersonAdd,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                triedSave = true
                if (name.isBlank()) {
                    popupMessage = "Please enter customer or shop name."
                    return@CrystalFormDialog
                }
                if (phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)) {
                    popupMessage = "Phone number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nفون نمبر مکمل نہیں ہے، براہ کرم 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                onAddCustomer(
                    name.trim(),
                    PakistaniFormatters.formatPhone(phoneDigits),
                    address.trim().ifBlank { null },
                    balance.toDoubleOrNull() ?: 0.0
                )
                showAddDialog = false
            }
        ) {
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Customer / Shop Name",
                urduLabel = "گاہک / دکان کا نام",
                leadingIcon = Icons.Filled.Storefront
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
                value = address,
                onValueChange = { address = it },
                englishLabel = "Address / Market",
                urduLabel = "پتہ / مارکیٹ",
                leadingIcon = Icons.Filled.LocationOn
            )
            CrystalTextField(
                value = balance,
                onValueChange = { balance = it },
                englishLabel = "Opening Credit (Rs.)",
                urduLabel = "ابتدائی ادھار",
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

    payTarget?.let { customer ->
        var amount by remember { mutableStateOf(customer.balance.toLong().toString()) }
        CrystalFormDialog(
            onDismissRequest = { payTarget = null },
            title = "Receive Payment",
            subtitle = "${customer.name} سے وصولی",
            icon = Icons.Filled.Payments,
            confirmText = "Confirm",
            dismissText = "Cancel",
            onConfirm = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    onReceivePayment(customer, amt)
                    payTarget = null
                }
            }
        ) {
            CrystalTextField(
                value = amount,
                onValueChange = { amount = it },
                englishLabel = "Payment Amount (Rs.)",
                urduLabel = "وصول شدہ رقم",
                leadingIcon = Icons.Filled.Payments,
                keyboardType = KeyboardType.Number
            )
        }
    }

    editingCustomer?.let { c ->
        var name by remember(c) { mutableStateOf(c.name) }
        var phoneDigits by remember(c) { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(c.phone)) }
        var address by remember(c) { mutableStateOf(c.address ?: "") }
        var balance by remember(c) { mutableStateOf(c.balance.toLong().toString()) }
        var triedSave by remember(c) { mutableStateOf(false) }
        var popupMessage by remember { mutableStateOf<String?>(null) }
        CrystalFormDialog(
            onDismissRequest = { editingCustomer = null },
            title = "Edit Customer",
            subtitle = "گاہک کی معلومات اپ ڈیٹ کریں",
            icon = Icons.Filled.Edit,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                triedSave = true
                if (name.isBlank()) {
                    popupMessage = "Please enter customer name."
                    return@CrystalFormDialog
                }
                if (phoneDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(phoneDigits)) {
                    popupMessage = "Phone number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nفون نمبر مکمل نہیں ہے، براہ کرم 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                onUpdateCustomer(
                    c.copy(
                        name = name.trim(),
                        phone = PakistaniFormatters.formatPhone(phoneDigits),
                        address = address.trim().ifBlank { null },
                        balance = balance.toDoubleOrNull() ?: c.balance
                    )
                )
                editingCustomer = null
            }
        ) {
            CrystalTextField(
                value = name,
                onValueChange = { name = it },
                englishLabel = "Name",
                urduLabel = "نام",
                leadingIcon = Icons.Filled.Storefront
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
                value = address,
                onValueChange = { address = it },
                englishLabel = "Address",
                urduLabel = "پتہ",
                leadingIcon = Icons.Filled.LocationOn
            )
            CrystalTextField(
                value = balance,
                onValueChange = { balance = it },
                englishLabel = "Balance (Rs.)",
                urduLabel = "بقایا رقم",
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

    deletingCustomer?.let { c ->
        CrystalFormDialog(
            onDismissRequest = { deletingCustomer = null },
            title = "Delete Customer?",
            subtitle = "گاہک کو فہرست سے ہٹائیں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteCustomer(c.id)
                deletingCustomer = null
            }
        ) {
            Text("Remove ${c.name} from Customers & Credit?", fontSize = 13.sp)
        }
    }
}

@Composable
fun DeliveriesVehiclesScreen(
    activeRole: UserRole,
    deliveries: List<DeliveryItem>,
    onSaveDelivery: (DeliveryItem) -> Unit,
    onDeleteDelivery: (String) -> Unit
) {
    var editingDelivery by remember { mutableStateOf<DeliveryItem?>(null) }
    var showAddOrEdit by remember { mutableStateOf(false) }
    var deletingDelivery by remember { mutableStateOf<DeliveryItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IcyGlossyButton(
            onClick = { editingDelivery = null; showAddOrEdit = true },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dispatch Ice Delivery", color = Color.White, fontWeight = FontWeight.Bold)
        }

        deliveries.forEach { d ->
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${d.vehicleNumber} • ${d.driverName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${d.route} → ${d.customerName} (${PakistaniFormatters.formatTons(d.tons)})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (d.status) {
                                    "Delivered" -> SuccessGreenSoft
                                    "On route" -> Color(0xFFEAF2FF)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    d.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (d.status == "Delivered") SuccessGreen else DeepBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (activeRole.canEditOrDelete) {
                                IconButton(onClick = { editingDelivery = d; showAddOrEdit = true }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit delivery", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deletingDelivery = d }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete delivery", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Idle", "On route", "Delivered").forEach { status ->
                            FilterChip(
                                selected = d.status == status,
                                onClick = { onSaveDelivery(d.copy(status = status)) },
                                label = { Text(status, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddOrEdit) {
        val base = editingDelivery
        var vehicle by remember(base) { mutableStateOf(base?.vehicleNumber ?: "") }
        var driver by remember(base) { mutableStateOf(base?.driverName ?: "") }
        var route by remember(base) { mutableStateOf(base?.route ?: "") }
        var customer by remember(base) { mutableStateOf(base?.customerName ?: "") }
        var tons by remember(base) { mutableStateOf(if (base != null) base.tons.toString() else "2.0") }
        CrystalFormDialog(
            onDismissRequest = { showAddOrEdit = false },
            title = if (base == null) "New Vehicle Dispatch" else "Edit Vehicle Dispatch",
            subtitle = "گاڑی اور ڈلیوری کی تفصیلات",
            icon = Icons.Filled.LocalShipping,
            confirmText = if (base == null) "Dispatch" else "Save",
            dismissText = "Cancel",
            onConfirm = {
                if (vehicle.isNotBlank() && driver.isNotBlank()) {
                    onSaveDelivery(
                        (base ?: DeliveryItem(status = "On route")).copy(
                            vehicleNumber = vehicle.trim(),
                            driverName = driver.trim(),
                            route = route.trim(),
                            customerName = customer.trim(),
                            tons = tons.toDoubleOrNull() ?: 1.0
                        )
                    )
                    showAddOrEdit = false
                }
            }
        ) {
            CrystalTextField(
                value = vehicle,
                onValueChange = { vehicle = it },
                englishLabel = "Vehicle Plate (e.g. KHI-4821)",
                urduLabel = "گاڑی نمبر",
                leadingIcon = Icons.Filled.DirectionsCar
            )
            CrystalTextField(
                value = driver,
                onValueChange = { driver = it },
                englishLabel = "Driver Name",
                urduLabel = "ڈرائیور کا نام",
                leadingIcon = Icons.Filled.Person
            )
            CrystalTextField(
                value = route,
                onValueChange = { route = it },
                englishLabel = "Route",
                urduLabel = "روٹ",
                leadingIcon = Icons.Filled.Route
            )
            CrystalTextField(
                value = customer,
                onValueChange = { customer = it },
                englishLabel = "Customer",
                urduLabel = "گاہک",
                leadingIcon = Icons.Filled.Storefront
            )
            CrystalTextField(
                value = tons,
                onValueChange = { tons = it },
                englishLabel = "Ice Load (Tons)",
                urduLabel = "وزن (ٹن)",
                leadingIcon = Icons.Filled.Scale,
                keyboardType = KeyboardType.Decimal
            )
        }
    }

    deletingDelivery?.let { d ->
        CrystalFormDialog(
            onDismissRequest = { deletingDelivery = null },
            title = "Delete Delivery?",
            subtitle = "ڈلیوری ریکارڈ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteDelivery(d.id)
                deletingDelivery = null
            }
        ) {
            Text("Delete dispatch ${d.vehicleNumber} (${d.driverName})?", fontSize = 13.sp)
        }
    }
}
