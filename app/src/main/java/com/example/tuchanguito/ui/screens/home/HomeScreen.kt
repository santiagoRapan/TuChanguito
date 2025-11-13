package com.example.tuchanguito.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.ColorPrimaryBorder
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
    onCreateList: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { AppRepository.get(context) }

    val lists by repo.activeLists().collectAsState(initial = emptyList())
    val active = lists.firstOrNull()
    val items by if (active != null) {
        repo.itemsForList(active.id).collectAsState(initial = emptyList())
    } else remember { mutableStateOf(emptyList()) }
    val pantryItems by repo.pantry().collectAsState(initial = emptyList())
    val products by repo.products().collectAsState(initial = emptyList())

    val lowStockItems = remember(pantryItems, products) {
        pantryItems
            .filter { it.quantity <= it.lowStockThreshold }
            .map { pantry ->
                val product = products.firstOrNull { it.id == pantry.productId }
                LowStockItemUi(
                    id = pantry.id,
                    productName = product?.name,
                    neededQuantity = max(1, pantry.lowStockThreshold - pantry.quantity)
                )
            }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.app_name), color = PrimaryTextBlue) }) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ActiveListCard(active, items, onOpenList, onCreateList)
            QuickActionsRow(onCreateList, onNewProduct, onConfigureCategories)
            LowStockSection(
                activeList = active,
                lowStockItems = lowStockItems,
                onAddItem = { _ -> active?.let { onOpenList(it.id) } ?: onCreateList() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveListCard(
    active: ShoppingList?,
    items: List<ListItem>,
    onOpenList: (Long) -> Unit,
    onCreateList: () -> Unit
) {
    val title = active?.title ?: stringResource(id = R.string.home_active_list_placeholder)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (active != null) onOpenList(active.id) },
        enabled = active != null,
        shape = RoundedCornerShape(24.dp),
        color = ButtonBlue,
        contentColor = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (active != null) {
                val acquiredCount = items.count { it.acquired }
                val pendingCount = items.size - acquiredCount
                val progress = if (items.isEmpty()) 0f else acquiredCount.toFloat() / items.size.toFloat()

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.home_active_list_pending, pendingCount),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(id = R.string.home_active_list_total, total(items)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = stringResource(id = R.string.home_active_list_empty_short),
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCreateList,
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = stringResource(id = R.string.home_create_list_button))
                }
            }
        }
    }
}

private fun total(items: List<ListItem>): Int = items.sumOf { it.quantity * 100 } // demo pricing

@Composable
private fun QuickActionsRow(
    onCreateList: () -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.PlaylistAdd,
            label = stringResource(id = R.string.home_quick_action_new_list_title),
            onClick = onCreateList
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.AddShoppingCart,
            label = stringResource(id = R.string.home_quick_action_new_product_title),
            onClick = onNewProduct
        )
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Category,
            label = stringResource(id = R.string.home_quick_action_categories_title),
            onClick = onConfigureCategories
        )
    }
}

@Composable
private fun LowStockSection(
    activeList: ShoppingList?,
    lowStockItems: List<LowStockItemUi>,
    onAddItem: (LowStockItemUi) -> Unit
) {
    val destinationName = activeList?.title ?: stringResource(id = R.string.home_low_stock_destination_fallback)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.home_low_stock_title),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryTextBlue,
            fontWeight = FontWeight.SemiBold
        )
        if (lowStockItems.isEmpty()) {
            HomeSectionCard {
                Text(
                    text = stringResource(id = R.string.home_low_stock_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                lowStockItems.forEach { item ->
                    LowStockItemCard(
                        item = item,
                        destinationName = destinationName,
                        onAdd = { onAddItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = ButtonBlue
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LowStockItemCard(
    item: LowStockItemUi,
    destinationName: String,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName ?: stringResource(id = R.string.home_low_stock_unknown_product),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        id = R.string.home_low_stock_add,
                        item.neededQuantity,
                        destinationName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ColorPrimaryBorder),
                tonalElevation = 0.dp
            ) {
                IconButton(onClick = onAdd) {
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

private data class LowStockItemUi(
    val id: Long,
    val productName: String?,
    val neededQuantity: Int
)
