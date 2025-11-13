package com.example.tuchanguito.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.ColorPrimaryBorder
import com.example.tuchanguito.ui.theme.PrimaryTextBlue

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
            ActiveListSection(active, items, onOpenList, onCreateList)
            QuickActionsSection(onCreateList, onNewProduct, onConfigureCategories)
            LowStockSection()
        }
    }
}

@Composable
private fun ActiveListSection(
    active: ShoppingList?,
    items: List<ListItem>,
    onOpenList: (Long) -> Unit,
    onCreateList: () -> Unit
) {
    HomeSectionCard {
        Text(
            text = stringResource(id = R.string.home_active_list_title),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryTextBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))

        if (active != null) {
            val acquiredCount = items.count { it.acquired }
            val pendingCount = items.size - acquiredCount
            val progress = if (items.isEmpty()) 0f else acquiredCount.toFloat() / items.size.toFloat()

            Text(
                text = active.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(id = R.string.home_active_list_pending, pendingCount),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(id = R.string.home_active_list_total, total(items)),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = ButtonBlue,
                trackColor = ButtonBlue.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onOpenList(active.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue, contentColor = Color.White)
                ) {
                    Text(text = stringResource(id = R.string.home_active_list_open_button))
                }
            }
        } else {
            Text(
                text = stringResource(id = R.string.home_active_list_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_active_list_empty_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinedButton(onClick = onCreateList) {
                    Text(text = stringResource(id = R.string.home_create_list_button))
                }
            }
        }
    }
}

private fun total(items: List<ListItem>): Int = items.sumOf { it.quantity * 100 } // demo pricing

@Composable
private fun QuickActionsSection(
    onCreateList: () -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit
) {
    HomeSectionCard {
        Text(
            text = stringResource(id = R.string.home_quick_actions_title),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryTextBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.PlaylistAdd,
                label = stringResource(id = R.string.home_quick_action_new_list_title),
                description = stringResource(id = R.string.home_quick_action_new_list_subtitle),
                onClick = onCreateList
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.AddShoppingCart,
                label = stringResource(id = R.string.home_quick_action_new_product_title),
                description = stringResource(id = R.string.home_quick_action_new_product_subtitle),
                onClick = onNewProduct
            )
            QuickActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Category,
                label = stringResource(id = R.string.home_quick_action_categories_title),
                description = stringResource(id = R.string.home_quick_action_categories_subtitle),
                onClick = onConfigureCategories
            )
        }
    }
}

@Composable
private fun LowStockSection() {
    HomeSectionCard {
        Text(
            text = stringResource(id = R.string.home_low_stock_title),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryTextBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.home_low_stock_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, ColorPrimaryBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
