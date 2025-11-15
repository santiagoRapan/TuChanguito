package com.example.tuchanguito.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tuchanguito.MyApplication
import com.example.tuchanguito.data.network.model.ShoppingListDto
import com.example.tuchanguito.ui.theme.ButtonBlue
import com.example.tuchanguito.ui.theme.ColorPrimary
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
    val app = context.applicationContext as MyApplication
    val viewModel: HomeViewModel = viewModel(
        factory = remember(app.shoppingListsRepository) { HomeViewModelFactory(app.shoppingListsRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            androidx.compose.material3.CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.app_name), color = Color.White) },
                colors = androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
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
            ActiveListSection(uiState.activeList, onOpenList, onCreateList)
            QuickActionsSection(onCreateList, onNewProduct, onConfigureCategories)
            LowStockSection()
        }
    }
}

@Composable
private fun ActiveListSection(
    active: ShoppingListDto?,
    onOpenList: (Long) -> Unit,
    onCreateList: () -> Unit
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
            Spacer(Modifier.height(24.dp))
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
