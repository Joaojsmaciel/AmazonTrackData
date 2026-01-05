package com.example.capdex.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.capdex.data.model.Boat
import com.example.capdex.ui.viewmodel.BoatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoatListScreen(
    boatViewModel: BoatViewModel = viewModel(),
    onBoatClick: (Boat) -> Unit,
    onAddBoat: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by boatViewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Boat?>(null) }
    
    LaunchedEffect(Unit) {
        android.util.Log.d("BoatListScreen", "Iniciando carregamento de embarcações")
        boatViewModel.loadUserBoats()
    }
    
    // Log do estado para debug
    LaunchedEffect(uiState) {
        android.util.Log.d("BoatListScreen", "Estado atualizado: isLoading=${uiState.isLoading}, boats=${uiState.boats.size}, error=${uiState.errorMessage}")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Embarcações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        android.util.Log.d("BoatListScreen", "Botão recarregar clicado")
                        boatViewModel.loadUserBoats() 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBoat,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar embarcação")
            }
        }
    ) { padding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Carregando embarcações...")
                        }
                    }
                }
                
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Erro ao carregar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage ?: "Erro desconhecido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { boatViewModel.loadUserBoats() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tentar Novamente")
                        }
                    }
                }
                
                uiState.boats.isEmpty() -> {
                    EmptyBoatsMessage(onAddBoat = onAddBoat)
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.boats) { boat ->
                            BoatCard(
                                boat = boat,
                                onClick = { onBoatClick(boat) },
                                onDelete = { showDeleteDialog = boat }
                            )
                        }
                    }
                }
            }
            
            // Mensagem de erro
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
    
    // Dialog de confirmação de exclusão
    showDeleteDialog?.let { boat ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Excluir Embarcação") },
            text = { Text("Tem certeza que deseja excluir ${boat.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        boatViewModel.deactivateBoat(boat.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BoatCard(
    boat: Boat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto da embarcação
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                if (boat.photoUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(boat.photoUrl),
                        contentDescription = "Foto do barco",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.DirectionsBoat,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Informações da embarcação
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = boat.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = boat.type,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                boat.capacity?.let {
                    Text(
                        text = "$it passageiros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${boat.routes.size} rota(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Botões de ação
            Column(
                horizontalAlignment = Alignment.End
            ) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyBoatsMessage(onAddBoat: () -> Unit) {
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
            text = "Adicione sua primeira embarcação",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onAddBoat) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar Embarcação")
        }
    }
}
