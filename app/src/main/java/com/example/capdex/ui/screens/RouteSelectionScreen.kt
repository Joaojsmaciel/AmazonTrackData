package com.example.capdex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.capdex.data.model.Boat
import com.example.capdex.data.model.Route
import com.example.capdex.data.model.Stop
import com.example.capdex.data.model.CommonStops
import com.example.capdex.ui.viewmodel.BoatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteSelectionScreen(
    boatViewModel: BoatViewModel = viewModel(),
    onRouteSelected: (Boat, Route) -> Unit,
    onBack: () -> Unit
) {
    val uiState by boatViewModel.uiState.collectAsState()
    var selectedBoat by remember { mutableStateOf<Boat?>(null) }
    var showCustomRouteDialog by remember { mutableStateOf(false) }
    
    // Carregar barcos quando a tela abrir
    LaunchedEffect(Unit) {
        boatViewModel.loadUserBoats()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecionar Rota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            
            // Instrução
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Selecione a embarcação e a rota para iniciar o rastreamento GPS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.boats.isEmpty()) {
                // Nenhuma embarcação cadastrada
                EmptyBoatsState()
            } else {
                // Selecionar embarcação
                Text(
                    text = "1. Selecione a Embarcação",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.boats) { boat ->
                        BoatSelectionCard(
                            boat = boat,
                            isSelected = selectedBoat?.id == boat.id,
                            onSelect = { selectedBoat = boat }
                        )
                    }
                }
                
                // Selecionar rota (só aparece se tiver barco selecionado)
                if (selectedBoat != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "2. Selecione a Rota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (selectedBoat!!.routes.isEmpty()) {
                        // Nenhuma rota cadastrada
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Route,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma rota cadastrada para esta embarcação",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Rotas cadastradas
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedBoat!!.routes) { route ->
                                RouteSelectionCard(
                                    route = route,
                                    onClick = {
                                        onRouteSelected(selectedBoat!!, route)
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botão para rota não cadastrada
                    OutlinedButton(
                        onClick = { showCustomRouteDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rota Não Cadastrada")
                    }
                }
            }
            
            // Mensagem de erro
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    
    // Dialog para criar rota customizada
    if (showCustomRouteDialog && selectedBoat != null) {
        CustomRouteDialog(
            onDismiss = { showCustomRouteDialog = false },
            onRouteCreated = { route ->
                onRouteSelected(selectedBoat!!, route)
                showCustomRouteDialog = false
            }
        )
    }
}

@Composable
fun BoatSelectionCard(
    boat: Boat,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsBoat,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isSelected) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = boat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${boat.type}${boat.capacity?.let { " • $it passageiros" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${boat.routes.size} rota(s) cadastrada(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RouteSelectionCard(
    route: Route,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = route.getRouteDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (route.estimatedDurationMinutes > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Duração estimada: ${route.estimatedDurationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBoatsState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.DirectionsBoat,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nenhuma embarcação cadastrada",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Cadastre uma embarcação primeiro na tela anterior",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRouteDialog(
    onDismiss: () -> Unit,
    onRouteCreated: (Route) -> Unit
) {
    var routeName by remember { mutableStateOf("") }
    var selectedOrigin by remember { mutableStateOf<Stop?>(null) }
    var selectedDestination by remember { mutableStateOf<Stop?>(null) }
    var estimatedTime by remember { mutableStateOf("") }
    var expandedOrigin by remember { mutableStateOf(false) }
    var expandedDestination by remember { mutableStateOf(false) }
    
    val availableStops = CommonStops.ALL_STOPS
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rota Não Cadastrada") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Crie uma rota temporária para esta viagem",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Nome da Rota (opcional)") },
                    placeholder = { Text("Ex: Viagem especial") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Origem
                ExposedDropdownMenuBox(
                    expanded = expandedOrigin,
                    onExpandedChange = { expandedOrigin = it }
                ) {
                    OutlinedTextField(
                        value = selectedOrigin?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Origem *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOrigin)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedOrigin,
                        onDismissRequest = { expandedOrigin = false }
                    ) {
                        availableStops.forEach { stop ->
                            DropdownMenuItem(
                                text = { Text("${stop.name} - ${stop.city}") },
                                onClick = {
                                    selectedOrigin = stop
                                    expandedOrigin = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Destino
                ExposedDropdownMenuBox(
                    expanded = expandedDestination,
                    onExpandedChange = { expandedDestination = it }
                ) {
                    OutlinedTextField(
                        value = selectedDestination?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Destino *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDestination)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDestination,
                        onDismissRequest = { expandedDestination = false }
                    ) {
                        availableStops
                            .filter { it.id != selectedOrigin?.id }
                            .forEach { stop ->
                                DropdownMenuItem(
                                    text = { Text("${stop.name} - ${stop.city}") },
                                    onClick = {
                                        selectedDestination = stop
                                        expandedDestination = false
                                    }
                                )
                            }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = estimatedTime,
                    onValueChange = { if (it.all { char -> char.isDigit() }) estimatedTime = it },
                    label = { Text("Tempo estimado (minutos)") },
                    placeholder = { Text("Ex: 180") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOrigin != null && selectedDestination != null) {
                        val route = Route(
                            id = "temp_${System.currentTimeMillis()}",
                            name = routeName.ifBlank { 
                                "${selectedOrigin?.city} - ${selectedDestination?.city}" 
                            },
                            origin = selectedOrigin!!,
                            destination = selectedDestination!!,
                            stops = emptyList(),
                            estimatedDurationMinutes = estimatedTime.toIntOrNull() ?: 0,
                            isFrequent = false
                        )
                        onRouteCreated(route)
                    }
                },
                enabled = selectedOrigin != null && selectedDestination != null
            ) {
                Text("Iniciar Rastreamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
