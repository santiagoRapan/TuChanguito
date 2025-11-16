package com.example.tuchanguito.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.R
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.ColorPrimary
import com.example.tuchanguito.ui.theme.ColorPrimaryBorder
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
    onCreateList: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val viewModel: HomeViewModel = viewModel(
        factory = remember(app.shoppingListsRepository, app.pantryRepository) {
            HomeViewModelFactory(app.shoppingListsRepository, app.pantryRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.LowStockAdded -> {
                    val destination = event.listName ?: context.getString(R.string.home_low_stock_destination_fallback)
                    val message = context.getString(
                        R.string.home_low_stock_add,
                        event.quantity,
                        destination
                    )
                    snackbarHost.showSnackbar(message)
                }
                is HomeEvent.ShowError -> {
                    snackbarHost.showSnackbar(event.message)
                }
            }
        }
    }

    var pendingLowStock by remember { mutableStateOf<LowStockItemUi?>(null) }
    var selectedListId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingQuantity by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(pendingLowStock, uiState.listOptions) {
        if (pendingLowStock != null && selectedListId == null && uiState.listOptions.isNotEmpty()) {
            selectedListId = uiState.listOptions.first().id
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.app_name), color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ActiveListSection(
                active = uiState.activeList,
                onOpenList = onOpenList,
                onCreateList = onCreateList,
                purchasedCount = uiState.activePurchasedCount,
                totalCount = uiState.activeTotalCount,
                progress = uiState.activeProgress
            )
            QuickActionsSection(onCreateList, onNewProduct, onConfigureCategories)
            LowStockSection(
                items = uiState.lowStockItems,
                onAdd = { item ->
                    pendingLowStock = item
                    selectedListId = uiState.listOptions.firstOrNull()?.id
                    pendingQuantity = item.suggestedQuantity.toString()
                }
            )
        }
    }

    val dialogItem = pendingLowStock
    if (dialogItem != null) {
        LowStockAddDialog(
            item = dialogItem,
            listOptions = uiState.listOptions,
            selectedListId = selectedListId,
            quantityText = pendingQuantity,
            isProcessing = uiState.isProcessingLowStock,
            onListSelected = { selectedListId = it },
            onQuantityChange = { newValue -> pendingQuantity = newValue.filter { ch -> ch.isDigit() } },
            onDismiss = {
                if (!uiState.isProcessingLowStock) {
                    pendingLowStock = null
                    selectedListId = null
                    pendingQuantity = ""
                }
            },
            onConfirm = {
                val targetList = selectedListId
                val quantity = pendingQuantity.toIntOrNull()
                    ?: dialogItem.suggestedQuantity
                if (targetList != null) {
                    viewModel.addLowStockItemToList(dialogItem.pantryItemId, targetList, quantity)
                    pendingLowStock = null
                    selectedListId = null
                    pendingQuantity = ""
                }
            }
        )
    }
}

@Composable
private fun ActiveListSection(
    active: ShoppingListDto?,
    onOpenList: (Long) -> Unit,
    onCreateList: () -> Unit,
    purchasedCount: Int,
    totalCount: Int,
    progress: Float
) {
    HomeSectionCard(containerColor = ButtonBlue) {
        Text(
            text = stringResource(id = R.string.home_active_list_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        if (active != null) {
            Text(
                text = active.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(12.dp))

            if (totalCount > 0) {
                // Contenedor con esquinas redondeadas
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.purchase_progress_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress.coerceIn(0f, 1f),
                            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
                            label = "homeActiveProgress"
                        )
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        val percent = (progress * 100).toInt()
                        Text(
                            text = stringResource(
                                id = R.string.purchase_progress_fmt,
                                purchasedCount,
                                totalCount,
                                percent
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onOpenList(active.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ButtonBlue)
                ) {
                    Text(text = stringResource(id = R.string.home_active_list_open_button))
                }
            }
        } else {
            Text(
                text = stringResource(id = R.string.home_active_list_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = onCreateList,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f))
                ) {
                    Text(text = stringResource(id = R.string.home_create_list_button))
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onCreateList: () -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit
) {
    val actions = listOf(
        QuickActionItem(
            icon = Icons.Rounded.PlaylistAdd,
            label = stringResource(id = R.string.home_quick_action_new_list_title),
            onClick = onCreateList
        ),
        QuickActionItem(
            icon = Icons.Rounded.AddShoppingCart,
            label = stringResource(id = R.string.home_quick_action_new_product_title),
            onClick = onNewProduct
        ),
        QuickActionItem(
            icon = Icons.Rounded.Category,
            label = stringResource(id = R.string.home_quick_action_categories_title),
            onClick = onConfigureCategories
        )
    )

    HomeSectionCard {
        Text(
            text = stringResource(id = R.string.home_quick_actions_title),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryTextBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        QuickActionsRow(actions = actions)
    }
}

@Composable
private fun LowStockSection(
    items: List<LowStockItemUi>,
    onAdd: (LowStockItemUi) -> Unit
) {
    HomeSectionCard {
        Text(
            text = stringResource(id = R.string.home_low_stock_title),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryTextBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                text = stringResource(id = R.string.home_low_stock_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val quantityDisplay = if ((item.currentQuantity % 1.0) == 0.0) {
                                item.currentQuantity.toInt().toString()
                            } else {
                                String.format(Locale.getDefault(), "%.1f", item.currentQuantity)
                            }
                            Text(
                                text = item.name.ifBlank { stringResource(id = R.string.home_low_stock_unknown_product) },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$quantityDisplay ${item.unit} • mín. ${item.lowStockThreshold}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onAdd(item) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(id = R.string.home_low_stock_add_button_content_desc),
                                tint = ButtonBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun QuickActionsRow(actions: List<QuickActionItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            QuickActionCard(
                modifier = Modifier.weight(1f),
                action = action
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    action: QuickActionItem
) {
    Surface(
        modifier = modifier.height(132.dp),
        onClick = action.onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, ColorPrimaryBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ButtonBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = ButtonBlue
                )
            }
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class QuickActionItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LowStockAddDialog(
    item: LowStockItemUi,
    listOptions: List<ShoppingListOption>,
    selectedListId: Long?,
    quantityText: String,
    isProcessing: Boolean,
    onListSelected: (Long?) -> Unit,
    onQuantityChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val hasLists = listOptions.isNotEmpty()
    val isValidQuantity = quantityText.toIntOrNull()?.let { it > 0 } == true
    val confirmEnabled = hasLists && selectedListId != null && isValidQuantity && !isProcessing
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        confirmButton = {
            TextButton(onClick = { if (confirmEnabled) onConfirm() }, enabled = confirmEnabled) {
                Text(text = stringResource(id = R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!isProcessing) onDismiss() }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        title = { Text(text = stringResource(id = R.string.home_low_stock_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val displayName = if (item.name.isNotBlank()) {
                    item.name
                } else {
                    stringResource(id = R.string.home_low_stock_unknown_product)
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (hasLists) {
                    val selectedName = listOptions.firstOrNull { it.id == selectedListId }?.name.orEmpty()
                    Box {
                        OutlinedTextField(
                            value = selectedName,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isProcessing,
                            label = { Text(stringResource(id = R.string.home_low_stock_select_list)) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isProcessing) { expanded = true }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        onListSelected(option.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(id = R.string.home_low_stock_no_lists),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(id = R.string.home_low_stock_quantity_label)) },
                    singleLine = true
                )
            }
        }
    )
}
