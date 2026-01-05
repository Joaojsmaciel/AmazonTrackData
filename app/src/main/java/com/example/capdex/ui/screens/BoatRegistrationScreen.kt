package com.example.capdex.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.capdex.data.model.Boat
import com.example.capdex.data.model.Route
import com.example.capdex.data.model.Stop
import com.example.capdex.data.model.CommonStops
import com.example.capdex.ui.viewmodel.BoatViewModel
import com.example.capdex.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoatRegistrationScreen(
    authViewModel: AuthViewModel = viewModel(),
    boatViewModel: BoatViewModel = viewModel(),
    onBoatRegistered: () -> Unit
) {
    val uiState by boatViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var boatName by remember { mutableStateOf("") }
    var boatType by remember { mutableStateOf("Barco") }
    var capacity by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var showRouteDialog by remember { mutableStateOf(false) }
    var currentBoat by remember { mutableStateOf<Boat?>(null) }
    
    // Launcher para selecionar imagem
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    // Observar mudanças de mensagens e atualizar currentBoat
    LaunchedEffect(uiState.successMessage, uiState.selectedBoat) {
        uiState.successMessage?.let {
            Log.d("BoatRegistrationScreen", "Success message: $it")
        }
        uiState.selectedBoat?.let { boat ->
            Log.d("BoatRegistrationScreen", "Boat atualizado: ${boat.id}")
            currentBoat = boat
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastrar Embarcação") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Título
            Text(
                text = "🚤 Cadastre sua embarcação",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Área de foto
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    selectedImageUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Foto do barco",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    photoUrl != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(photoUrl),
                            contentDescription = "Foto do barco",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = "Adicionar foto",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Toque para adicionar foto",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // Loading overlay durante upload
                if (uiState.isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Nome do barco
            OutlinedTextField(
                value = boatName,
                onValueChange = { boatName = it },
                label = { Text("Nome do Barco *") },
                placeholder = { Text("Ex: Barco São Pedro") },
                leadingIcon = {
                    Icon(Icons.Default.DirectionsBoat, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tipo de embarcação
            var expandedType by remember { mutableStateOf(false) }
            val boatTypes = listOf("Barco", "Lancha", "Balsa", "Ferry", "Outro")
            
            ExposedDropdownMenuBox(
                expanded = expandedType,
                onExpandedChange = { expandedType = it }
            ) {
                OutlinedTextField(
                    value = boatType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Embarcação") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    leadingIcon = {
                        Icon(Icons.Default.Category, contentDescription = null)
                    }
                )
                ExposedDropdownMenu(
                    expanded = expandedType,
                    onDismissRequest = { expandedType = false }
                ) {
                    boatTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                boatType = type
                                expandedType = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Capacidade (opcional)
            OutlinedTextField(
                value = capacity,
                onValueChange = { if (it.all { char -> char.isDigit() }) capacity = it },
                label = { Text("Capacidade de Passageiros") },
                placeholder = { Text("Ex: 50") },
                leadingIcon = {
                    Icon(Icons.Default.People, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Seção de Rotas
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rotas Cadastradas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showRouteDialog = true },
                            enabled = currentBoat != null
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Adicionar rota",
                                tint = if (currentBoat != null) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (currentBoat == null) {
                        Text(
                            text = "Salve a embarcação primeiro para adicionar rotas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (currentBoat?.routes.isNullOrEmpty()) {
                        Text(
                            text = "Nenhuma rota cadastrada ainda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        currentBoat?.routes?.forEach { route ->
                            RouteItem(
                                route = route,
                                onDelete = {
                                    currentBoat?.id?.let { boatId ->
                                        boatViewModel.removeRoute(boatId, route.id)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botão Salvar
            Button(
                onClick = {
                    Log.d("BoatRegistrationScreen", "Botão Salvar clicado")
                    val user = authViewModel.uiState.value.user
                    Log.d("BoatRegistrationScreen", "User: ${user?.uid}, name: ${user?.fullName}")
                    
                    val boat = Boat(
                        id = currentBoat?.id ?: "",
                        ownerId = user?.uid ?: "",  
                        ownerName = user?.fullName ?: "",
                        name = boatName,
                        photoUrl = photoUrl,
                        type = boatType,
                        capacity = capacity.toIntOrNull(),
                        routes = currentBoat?.routes ?: emptyList()
                    )
                    
                    Log.d("BoatRegistrationScreen", "Boat criado: id=${boat.id}, name=${boat.name}, ownerId=${boat.ownerId}")
                    Log.d("BoatRegistrationScreen", "selectedImageUri: $selectedImageUri")
                    
                    // Primeiro salva o barco para obter o ID
                    boatViewModel.saveBoat(boat)
                    
                    // Depois faz upload da foto se houver
                    if (selectedImageUri != null) {
                        val boatId = currentBoat?.id ?: ""
                        if (boatId.isNotEmpty()) {
                            Log.d("BoatRegistrationScreen", "Fazendo upload da foto para boat: $boatId")
                            boatViewModel.uploadBoatPhoto(boatId, selectedImageUri!!) { url ->
                                Log.d("BoatRegistrationScreen", "Upload concluído, atualizando URL: $url")
                                photoUrl = url
                                boatViewModel.saveBoat(boat.copy(photoUrl = url))
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = boatName.isNotBlank() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar Embarcação")
                }
            }
            
            // Botão Continuar (só aparece se já salvou)
            if (currentBoat != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onBoatRegistered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Continuar para o Mapa")
                }
            }
            
            // Mensagens de erro/sucesso
            uiState.errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            uiState.successMessage?.let { success ->
                LaunchedEffect(success) {
                    // Atualiza o barco atual após salvar
                    currentBoat = uiState.selectedBoat
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = success,
                    color = Color(0xFF4CAF50),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    
    // Dialog para adicionar rota
    if (showRouteDialog) {
        AddRouteDialog(
            onDismiss = { showRouteDialog = false },
            onRouteAdded = { route ->
                currentBoat?.id?.let { boatId ->
                    boatViewModel.addRoute(boatId, route)
                }
                showRouteDialog = false
            }
        )
    }
}

@Composable
fun RouteItem(
    route: Route,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = route.getRouteDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover rota",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRouteDialog(
    onDismiss: () -> Unit,
    onRouteAdded: (Route) -> Unit
) {
    var routeName by remember { mutableStateOf("") }
    var selectedOrigin by remember { mutableStateOf<Stop?>(null) }
    var selectedDestination by remember { mutableStateOf<Stop?>(null) }
    var selectedStops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var estimatedTime by remember { mutableStateOf("") }
    var expandedOrigin by remember { mutableStateOf(false) }
    var expandedDestination by remember { mutableStateOf(false) }
    
    val availableStops = CommonStops.ALL_STOPS
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Rota") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it },
                    label = { Text("Nome da Rota") },
                    placeholder = { Text("Ex: Manaus - Itacoatiara") },
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
                            name = routeName.ifBlank { 
                                "${selectedOrigin?.city} - ${selectedDestination?.city}" 
                            },
                            origin = selectedOrigin!!,
                            destination = selectedDestination!!,
                            stops = selectedStops,
                            estimatedDurationMinutes = estimatedTime.toIntOrNull() ?: 0
                        )
                        onRouteAdded(route)
                    }
                },
                enabled = selectedOrigin != null && selectedDestination != null
            ) {
                Text("Adicionar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
