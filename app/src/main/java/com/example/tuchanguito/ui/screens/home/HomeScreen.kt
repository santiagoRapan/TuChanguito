package com.example.tuchanguito.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tuchanguito.R
import com.example.tuchanguito.data.AppRepository
import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.data.model.ListItem
import com.example.tuchanguito.data.model.ShoppingList
import com.example.tuchanguito.ui.theme.PrimaryTextBlue
import com.example.tuchanguito.ui.theme.ButtonBlue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenList: (Long) -> Unit,
    onNewProduct: () -> Unit,
    onConfigureCategories: () -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { AppRepository.get(context) }
    val prefs = remember { PreferencesManager(context) }

    val lists by repo.activeLists().collectAsState(initial = emptyList())
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
        }
    }
}

@Composable
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
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
