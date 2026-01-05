package com.example.capdex.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.capdex.ui.viewmodel.AuthViewModel
import com.example.capdex.ui.viewmodel.MapViewModel
import com.example.capdex.ui.viewmodel.LocationViewModel
import com.google.android.gms.location.LocationServices
import android.os.Build
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBoatList: () -> Unit = {},
    onNavigateToRouteSelection: () -> Unit = {},
    mapViewModel: MapViewModel = viewModel(),
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationViewModel: LocationViewModel = viewModel()
    
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    val trackedUsers by mapViewModel.trackedUsers.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()
    val trackingState by locationViewModel.trackingState.collectAsState()
    
    // Estado para mostrar perfil
    var showProfileDialog by remember { mutableStateOf(false) }
    
    // Obtém dados do usuário atual
    val authState by (authViewModel?.uiState?.collectAsState() ?: remember { mutableStateOf(null) })
    val currentUser = authState?.user
    
    // Define o ID do usuário atual no MapViewModel
    LaunchedEffect(currentUser?.uid) {
        currentUser?.uid?.let { uid ->
            mapViewModel.setCurrentUserId(uid)
        }
    }
    
    // Configuração do OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        locationViewModel.updateLocationPermission(isGranted)
        if (isGranted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = GeoPoint(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                // Ignora erro
            }
        }
    }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationViewModel.updateNotificationPermission(granted)
    }
    
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            locationViewModel.updateLocationPermission(true)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        currentLocation = GeoPoint(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                // Ignora erro
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // Verifica permissão de notificação
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            locationViewModel.updateNotificationPermission(hasNotificationPermission)
        } else {
            locationViewModel.updateNotificationPermission(true)
        }
    }
    
    Scaffold(
        bottomBar = {
            // Barra inferior moderna com bordas arredondadas
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.White,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 12.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão Perfil
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Color(0xFFE3F2FD),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Perfil",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Perfil",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Botão Embarcações (só para barqueiros)
                    if (currentUser?.userType == com.example.capdex.data.model.UserType.BARQUEIRO) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onNavigateToBoatList,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        Color(0xFFFFF3E0),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsBoat,
                                    contentDescription = "Embarcações",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Barcos",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Botão Mapa (central, maior)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        FloatingActionButton(
                            onClick = { /* Já estamos no mapa */ },
                            modifier = Modifier.size(68.dp),
                            containerColor = Color(0xFF2196F3),
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Mapa",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mapa",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                    
                    // Botão Sair
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { 
                                authViewModel?.signOut()
                                onNavigateBack() 
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Color(0xFFE3F2FD),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sair",
                                tint = Color(0xFF2196F3),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sair",
                            fontSize = 12.sp,
                            color = Color(0xFF666666),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // Botão de rastreamento para Mototaxi e Barqueiro
            currentUser?.let { user ->
                if (user.userType.displayName != "Passageiro") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Surface(
                            onClick = {
                                if (trackingState.isTracking) {
                                    locationViewModel.stopTracking(context)
                                } else {
                                    // Se for barqueiro, precisa selecionar barco e rota primeiro
                                    if (currentUser?.userType == com.example.capdex.data.model.UserType.BARQUEIRO) {
                                        onNavigateToRouteSelection()
                                    } else {
                                        // Outros usuários iniciam rastreamento direto
                                        if (!trackingState.hasLocationPermission) {
                                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                                                   !trackingState.hasNotificationPermission) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            locationViewModel.startTracking(context, user)
                                        }
                                    }
                                }
                            },
                            color = if (trackingState.isTracking) Color(0xFFFF5252) else Color(0xFF4CAF50),
                            shape = RoundedCornerShape(28.dp),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (trackingState.isTracking) 
                                        androidx.compose.material.icons.Icons.Default.Stop 
                                    else 
                                        androidx.compose.material.icons.Icons.Default.PlayArrow,
                                    contentDescription = if (trackingState.isTracking) "Parar" else "Iniciar",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (trackingState.isTracking) 
                                        "Rastreamento Ativo" 
                                    else 
                                        "Rastreamento Inativo",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasLocationPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Permissão de localização necessária",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Text("Conceder Permissão")
                    }
                }
            } else {
                var mapView by remember { mutableStateOf<MapView?>(null) }
                
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            
                            // Desabilita os controles de zoom padrão
                            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
                            
                            // Posição inicial (Manaus centro)
                            val startPoint = currentLocation ?: GeoPoint(-3.1190, -60.0217)
                            controller.setZoom(13.0)
                            controller.setCenter(startPoint)
                            
                            // Adiciona overlay de localização do usuário
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            locationOverlay.enableFollowLocation()
                            overlays.add(locationOverlay)
                            
                            mapView = this
                        }
                    },
                    update = { map ->
                        // Remove apenas marcadores de usuários rastreados
                        val toRemove = map.overlays.filterIsInstance<Marker>().filter { 
                            it.id != "myLocation" 
                        }
                        map.overlays.removeAll(toRemove.toSet())
                        
                        // Adiciona marcadores para usuários rastreados
                        trackedUsers.forEach { user ->
                            val marker = Marker(map).apply {
                                position = GeoPoint(user.latitude, user.longitude)
                                title = user.userName
                                snippet = "${user.userType} - ${String.format("%.1f", user.speed * 3.6)} km/h"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                
                                // Define ícone baseado no tipo de usuário
                                val iconResource = when {
                                    user.userType.contains("MOTOTAXI", ignoreCase = true) || 
                                    user.userType.contains("Mototaxi", ignoreCase = true) -> {
                                        subDescription = "🏍️ Mototaxi"
                                        com.example.capdex.R.drawable.ic_mototaxi
                                    }
                                    user.userType.contains("BARQUEIRO", ignoreCase = true) || 
                                    user.userType.contains("Barqueiro", ignoreCase = true) -> {
                                        subDescription = "🚤 Barqueiro"
                                        com.example.capdex.R.drawable.ic_barqueiro
                                    }
                                    else -> {
                                        subDescription = "📍 ${user.userType}"
                                        null
                                    }
                                }
                                
                                // Aplica o ícone personalizado
                                iconResource?.let { resId ->
                                    icon = context.getDrawable(resId)
                                }
                            }
                            map.overlays.add(marker)
                        }
                        
                        // Se houver usuários, centraliza no primeiro
                        if (trackedUsers.isNotEmpty()) {
                            val firstUser = trackedUsers.first()
                            map.controller.animateTo(GeoPoint(firstUser.latitude, firstUser.longitude))
                        }
                        
                        map.invalidate()
                    }
                )
                
                // Legenda compacta no topo do mapa
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(
                            icon = Icons.Default.TwoWheeler,
                            label = "Mototaxi",
                            color = Color(0xFF2196F3)
                        )
                        LegendItem(
                            icon = Icons.Default.DirectionsBoat,
                            label = "Barqueiro",
                            color = Color(0xFF00BCD4)
                        )
                    }
                }
                
                // Botões de Zoom - lado esquerdo inferior
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botão Zoom In
                    FloatingActionButton(
                        onClick = {
                            mapView?.controller?.zoomIn()
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = Color(0xFF2196F3)
                        )
                    }
                    
                    // Botão Zoom Out
                    FloatingActionButton(
                        onClick = {
                            mapView?.controller?.zoomOut()
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
                
                // Contador de veículos - canto superior direito
                if (trackedUsers.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${trackedUsers.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Text(
                                text = "veículo${if (trackedUsers.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Indicador de carregamento
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }
        }
        
        // Dialog de perfil
        if (showProfileDialog) {
            authViewModel?.let { vm ->
                val authState by vm.uiState.collectAsState()
                authState.user?.let { user ->
                    AlertDialog(
                        onDismissRequest = { showProfileDialog = false },
                        title = {
                            Text(
                                text = "Perfil",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProfileInfoRow(label = "Nome", value = user.fullName)
                                ProfileInfoRow(label = "Email", value = user.email)
                                ProfileInfoRow(label = "CPF", value = user.cpf)
                                ProfileInfoRow(label = "Tipo", value = user.userType.displayName)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showProfileDialog = false }
                            ) {
                                Text("Fechar")
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Cleanup do mapa quando a tela for destruída
        }
    }
}

@Composable
private fun LegendItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}
