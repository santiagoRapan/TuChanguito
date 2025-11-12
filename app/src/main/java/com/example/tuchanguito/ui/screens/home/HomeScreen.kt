package com.example.tuchanguito.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.LowStockItemSummary
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    onGoToLists: () -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lists by repo.activeLists().collectAsState(initial = emptyList())
    val activeList = lists.firstOrNull()
    val activeListId = activeList?.id
    val listItemsFlow = remember(activeListId) {
        activeListId?.let { repo.itemsForList(it) } ?: flowOf(emptyList())
    }
    val listItems by listItemsFlow.collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())
    val lowStock by repo.lowStockItems().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        runCatching { repo.refreshLists() }
        runCatching { repo.syncPantry() }
    }
    LaunchedEffect(activeListId) {
        if (activeListId != null) {
            runCatching { repo.syncListItems(activeListId) }
        }
    }

    val productMap = remember(products) { products.associateBy { it.id } }
    val totalItems = listItems.size
    val purchasedCount = listItems.count { it.acquired }
    val pendingCount = (totalItems - purchasedCount).coerceAtLeast(0)
    val totalCost = listItems.sumOf { item ->
        val price = productMap[item.productId]?.price ?: 0.0
        price * item.quantity
    }
    val progress = if (totalItems == 0) 0f else purchasedCount.toFloat() / totalItems.toFloat()
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "AR")).apply {
            currency = Currency.getInstance("ARS")
            maximumFractionDigits = 2
        }
    }
    val totalCostText = remember(totalCost) { currencyFormatter.format(totalCost) }
    val lowStockPreview = remember(lowStock) { lowStock.take(3) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = PrimaryTextBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = PrimaryTextBlue
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ActiveListCard(
                activeList = activeList,
                pendingCount = pendingCount,
                totalCostText = totalCostText,
                progress = progress,
                onOpenList = onOpenList,
                onGoToLists = onGoToLists
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.home_quick_actions),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                QuickActionsRow(
                    onNewList = onGoToLists,
                    onNewProduct = onNewProduct,
                    onConfigureCategories = onConfigureCategories
                )
            }

            LowStockCard(
                items = lowStockPreview,
                activeListName = activeList?.title,
                onAddItem = { entry ->
                    val targetList = activeList
                    if (targetList == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.home_no_active_list_error)
                            )
                        }
                    } else {
                        scope.launch {
                            val quantity = (entry.lowStockThreshold - entry.quantity).coerceAtLeast(1)
                            val unitLabel = entry.unit.ifBlank { "u" }
                            val result = runCatching {
                                repo.addItemRemote(targetList.id, entry.productId, quantity, unitLabel)
                                repo.syncListItems(targetList.id)
                            }
                            val message = if (result.isSuccess) {
                                context.getString(R.string.home_add_success, entry.name)
                            } else {
                                context.getString(R.string.home_add_error, entry.name)
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ActiveListCard(
    activeList: ShoppingList?,
    pendingCount: Int,
    totalCostText: String,
    progress: Float,
    onOpenList: (Long) -> Unit,
    onGoToLists: () -> Unit,
) {
    val gradient = remember {
        Brush.verticalGradient(listOf(ButtonBlue, PrimaryTextBlue))
    }
    val pendingLabel = stringResource(R.string.home_pending_label, pendingCount)
    val todayLabel = stringResource(R.string.home_today_label)

    Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 2.dp) {
        Box(
            modifier = Modifier
                .background(gradient, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = activeList?.title ?: stringResource(R.string.home_default_list_title),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = todayLabel,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = pendingLabel, color = Color.White)
                    Text(text = totalCostText, color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                if (activeList != null) {
                    Button(
                        onClick = { onOpenList(activeList.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ButtonBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.home_open_list), fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.home_no_lists_message),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = onGoToLists,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ButtonBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.home_create_list), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onNewList: () -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            icon = Icons.Filled.Add,
            title = stringResource(R.string.new_list),
            subtitle = stringResource(R.string.home_new_list_hint),
            modifier = Modifier.weight(1f),
            onClick = onNewList
        )
        QuickActionCard(
            icon = Icons.Filled.ShoppingCart,
            title = stringResource(R.string.new_product),
            subtitle = stringResource(R.string.home_new_product_hint),
            modifier = Modifier.weight(1f),
            onClick = onNewProduct
        )
        QuickActionCard(
            icon = Icons.Filled.Settings,
            title = stringResource(R.string.configure_categories),
            subtitle = stringResource(R.string.home_configure_categories_hint),
            modifier = Modifier.weight(1f),
            onClick = onConfigureCategories
        )
    }
}

@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ButtonBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ButtonBlue)
            }
            Text(title, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LowStockCard(
    items: List<LowStockItemSummary>,
    activeListName: String?,
    onAddItem: (LowStockItemSummary) -> Unit,
) {
    Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.home_low_stock_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (items.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_low_stock_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.forEach { entry ->
                        LowStockRow(
                            entry = entry,
                            activeListName = activeListName,
                            enabled = activeListName != null,
                            onAdd = { onAddItem(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockRow(
    entry: LowStockItemSummary,
    activeListName: String?,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val suggestedQuantity = (entry.lowStockThreshold - entry.quantity).coerceAtLeast(1)
    val subtitle = if (activeListName != null) {
        stringResource(R.string.home_add_to_list, suggestedQuantity, activeListName)
    } else {
        stringResource(R.string.home_add_to_list_disabled, suggestedQuantity)
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (enabled) ButtonBlue else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = enabled, onClick = onAdd),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
