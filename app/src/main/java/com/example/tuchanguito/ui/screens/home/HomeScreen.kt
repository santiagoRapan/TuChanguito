package com.example.tuchanguito.ui.screens.home

<<<<<<< HEAD
import androidx.compose.foundation.layout.*
=======
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
>>>>>>> login
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
import com.example.tuchanguito.data.AppRepository
<<<<<<< HEAD
import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.data.model.ListItem
=======
>>>>>>> login
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.ui.theme.ButtonBlue
<<<<<<< HEAD
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
=======
import com.example.tuchanguito.ui.theme.ColorPrimaryBorder
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
>>>>>>> login

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
    onCreateList: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val prefs = remember { PreferencesManager(context) }

    val lists by repo.activeLists().collectAsState(initial = emptyList())
<<<<<<< HEAD
    val lastOpened by prefs.lastOpenedListId.collectAsState(initial = null)
    // Prefer last opened; if none, choose the most recently created (by id). If still none, do not show a card.
    val active = lists.firstOrNull { it.id == lastOpened } ?: lists.maxByOrNull { it.id }

    val newListLabel = stringResource(id = R.string.new_list)
    val newProductLabel = stringResource(id = R.string.new_product)
    val lowStockLabel = stringResource(id = R.string.low_stock)
    // Hoisted strings for use inside coroutines / non-composable lambdas
    val createListDialogTitle = stringResource(id = R.string.create_list_dialog_title)
    val listAlreadyExists = stringResource(id = R.string.list_already_exists)
    val createErrorLabel = stringResource(id = R.string.create_error)
    val createLabel = stringResource(id = R.string.create)
    val cancelLabel = stringResource(id = R.string.cancel)
    val newProductTitle = stringResource(id = R.string.new_product)

    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var showNewListDialog by remember { mutableStateOf(false) }
    var showNewProductDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.app_name), color = PrimaryTextBlue) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val scrollMod = if (isLandscape) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(Modifier.fillMaxSize().then(scrollMod).padding(padding).padding(16.dp)) {
            if (active != null) {
                val activeItems by repo.itemsForList(active.id).collectAsState(initial = emptyList())
                ActiveListCard(active, activeItems) { id ->
                    coroutineScope.launch { prefs.setLastOpenedListId(id) }
                    onOpenList(id)
                }
                Spacer(Modifier.height(16.dp))
            }

            // Center the two quick action buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Row(Modifier.wrapContentWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    QuickAction(newListLabel) { showNewListDialog = true }
                    QuickAction(newProductLabel) { showNewProductDialog = true }
                }
            }

            // New List AlertDialog (matches ListsScreen style)
            if (showNewListDialog) {
                var newName by remember { mutableStateOf("") }
                val nameTrim = newName.trim()
                val duplicate = lists.any { it.title.equals(nameTrim, ignoreCase = true) }
                var busy by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { if (!busy) { showNewListDialog = false; newName = "" } },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                busy = true
                                if (duplicate) { snackbarHost.showSnackbar(listAlreadyExists); busy = false; return@launch }
                                val id = try { repo.createList(nameTrim) } catch (t: Throwable) { snackbarHost.showSnackbar(t.message ?: createErrorLabel); 0L } finally { busy = false }
                                if (id > 0) { showNewListDialog = false; newName = ""; coroutineScope.launch { prefs.setLastOpenedListId(id) }; onOpenList(id) }
                            }
                        }, enabled = nameTrim.isNotBlank() && !busy && !duplicate) { Text(createLabel) }
                    },
                    dismissButton = { TextButton(onClick = { if (!busy) { showNewListDialog = false; newName = "" } }) { Text(cancelLabel) } },
                    title = { Text(createListDialogTitle) },
                    text = {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text(stringResource(id = R.string.list_name_label)) },
                            singleLine = true,
                            isError = duplicate,
                            supportingText = { if (duplicate) Text(listAlreadyExists) }
                        )
                    }
                )
            }

            // New Product AlertDialog (matches ProductsScreen style)
            if (showNewProductDialog) {
                var name by remember { mutableStateOf("") }
                var priceText by remember { mutableStateOf("") }
                var unit by remember { mutableStateOf("") }
                var busy by remember { mutableStateOf(false) }
                val valid = name.isNotBlank()

                AlertDialog(
                    onDismissRequest = { if (!busy) showNewProductDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                busy = true
                                try {
                                    val price = priceText.toDoubleOrNull() ?: 0.0
                                    repo.createProductRemote(name.trim(), price, unit.ifBlank { "u" }, null)
                                    showNewProductDialog = false
                                    onNewProduct()
                                } catch (t: Throwable) {
                                    snackbarHost.showSnackbar(t.message ?: createErrorLabel)
                                } finally { busy = false }
                            }
                        }, enabled = valid && !busy) { Text(createLabel) }
                    },
                    dismissButton = { TextButton(onClick = { if (!busy) showNewProductDialog = false }) { Text(cancelLabel) } },
                    title = { Text(newProductTitle) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(id = R.string.name_label)) }, singleLine = true)
                            OutlinedTextField(value = priceText, onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text(stringResource(id = R.string.price_label)) }, singleLine = true)
                            OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text(stringResource(id = R.string.unit_label)) }, singleLine = true)
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(lowStockLabel, style = MaterialTheme.typography.titleLarge)
            // Additional content (pantry low stock) could go here
=======
    val active = lists.firstOrNull()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(id = R.string.app_name), color = PrimaryTextBlue) }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ActiveListSection(active, onOpenList, onCreateList)
            QuickActionsSection(onCreateList, onNewProduct, onConfigureCategories)
            LowStockSection()
>>>>>>> login
        }
    }
}

@Composable
<<<<<<< HEAD
private fun ActiveListCard(active: ShoppingList?, items: List<ListItem>, onOpen: (Long) -> Unit) {
    val acquiredCount = items.count { it.acquired }
    val progress = if (items.isEmpty()) 0f else acquiredCount.toFloat() / items.size.toFloat()
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = ButtonBlue) {
        Column(Modifier.padding(16.dp)) {
            Text(active?.title ?: stringResource(id = R.string.default_active_list_title), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)), color = Color.White, trackColor = Color(0xFF8ECBF5))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${items.size - acquiredCount} pendientes", color = Color.White)
                Text("$${total(items)}", color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = { if (active != null) onOpen(active.id) }, colors = ButtonDefaults.buttonColors(containerColor = ButtonBlue, contentColor = Color.White)) { Text(stringResource(id = R.string.open_label)) }
        }
    }
}

private fun total(items: List<ListItem>): Int = items.sumOf { it.quantity * 100 } // demo pricing

@Composable
private fun QuickAction(label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, tonalElevation = 1.dp, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(52.dp).clip(CircleShape),
                color = ButtonBlue,
                contentColor = Color.White,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "add", tint = Color.White)
=======
private fun ActiveListSection(
    active: ShoppingList?,
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
                text = active.title,
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
>>>>>>> login
                }
            }
        }
    }
}
<<<<<<< HEAD
=======

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
>>>>>>> login
