# 🗺️ Sistema de Viagens - Consulta de Rotas

## 📊 Estrutura no Firebase

Cada localização agora contém um `tripId` único por viagem:

```
Firestore:
  └─ locations/
      └─ {userId}/
          └─ tracks/
              ├─ doc1: {
              │   userId: "abc123",
              │   tripId: "abc123_1704398400000",  ← VIAGEM 1
              │   latitude: -3.1190,
              │   longitude: -60.0217,
              │   timestamp: 1704398400000,
              │   speed: 5.2,
              │   ...
              │ }
              ├─ doc2: {
              │   tripId: "abc123_1704398400000",  ← VIAGEM 1 (mesmo ID)
              │   ...
              │ }
              ├─ doc3: {
              │   tripId: "abc123_1704410000000",  ← VIAGEM 2 (novo ID)
              │   ...
              │ }
```

## 🚀 Exemplos de Consultas Web

### 1️⃣ Listar Todas as Viagens de um Motorista

```javascript
// Firebase Web SDK v9+
import { collection, query, where, getDocs, orderBy } from 'firebase/firestore';

async function listarViagens(userId) {
    const locationsRef = collection(db, `locations/${userId}/tracks`);
    const snapshot = await getDocs(locationsRef);
    
    // Agrupar por tripId
    const viagens = {};
    snapshot.forEach(doc => {
        const data = doc.data();
        if (!viagens[data.tripId]) {
            viagens[data.tripId] = [];
        }
        viagens[data.tripId].push(data);
    });
    
    // Retornar lista de viagens
    return Object.entries(viagens).map(([tripId, pontos]) => ({
        tripId,
        pontos: pontos.sort((a, b) => a.timestamp - b.timestamp),
        inicio: new Date(Math.min(...pontos.map(p => p.timestamp))),
        fim: new Date(Math.max(...pontos.map(p => p.timestamp))),
        totalPontos: pontos.length
    }));
}

// Uso
const viagens = await listarViagens('userId_do_mototaxi');
console.log(`Total de viagens: ${viagens.length}`);
viagens.forEach(viagem => {
    console.log(`Viagem ${viagem.tripId}: ${viagem.totalPontos} pontos`);
});
```

### 2️⃣ Desenhar Rota de uma Viagem Específica (Google Maps)

```javascript
async function desenharRota(userId, tripId, map) {
    const locationsRef = collection(db, `locations/${userId}/tracks`);
    const q = query(
        locationsRef,
        where('tripId', '==', tripId),
        orderBy('timestamp', 'asc')
    );
    
    const snapshot = await getDocs(q);
    const pontos = [];
    
    snapshot.forEach(doc => {
        const data = doc.data();
        pontos.push({
            lat: data.latitude,
            lng: data.longitude,
            speed: data.speed,
            timestamp: data.timestamp
        });
    });
    
    // Desenhar polyline no Google Maps
    const routePath = new google.maps.Polyline({
        path: pontos,
        geodesic: true,
        strokeColor: '#2196F3',
        strokeOpacity: 1.0,
        strokeWeight: 4
    });
    routePath.setMap(map);
    
    // Marcador de início
    new google.maps.Marker({
        position: pontos[0],
        map: map,
        label: 'I',
        title: 'Início'
    });
    
    // Marcador de fim
    new google.maps.Marker({
        position: pontos[pontos.length - 1],
        map: map,
        label: 'F',
        title: 'Fim'
    });
    
    // Ajustar zoom para mostrar toda a rota
    const bounds = new google.maps.LatLngBounds();
    pontos.forEach(p => bounds.extend(p));
    map.fitBounds(bounds);
    
    return pontos;
}
```

### 3️⃣ Desenhar Rota com OpenStreetMap (Leaflet)

```javascript
async function desenharRotaLeaflet(userId, tripId, map) {
    const locationsRef = collection(db, `locations/${userId}/tracks`);
    const q = query(
        locationsRef,
        where('tripId', '==', tripId),
        orderBy('timestamp', 'asc')
    );
    
    const snapshot = await getDocs(q);
    const pontos = [];
    
    snapshot.forEach(doc => {
        const data = doc.data();
        pontos.push([data.latitude, data.longitude]);
    });
    
    // Desenhar polyline
    const polyline = L.polyline(pontos, {
        color: '#2196F3',
        weight: 4,
        opacity: 0.8
    }).addTo(map);
    
    // Marcadores
    L.marker(pontos[0])
        .bindPopup('Início da Viagem')
        .addTo(map);
    
    L.marker(pontos[pontos.length - 1])
        .bindPopup('Fim da Viagem')
        .addTo(map);
    
    // Ajustar zoom
    map.fitBounds(polyline.getBounds());
}
```

### 4️⃣ Buscar Viagens por Período

```javascript
async function buscarViagensPorPeriodo(userId, dataInicio, dataFim) {
    const locationsRef = collection(db, `locations/${userId}/tracks`);
    const q = query(
        locationsRef,
        where('timestamp', '>=', dataInicio.getTime()),
        where('timestamp', '<=', dataFim.getTime()),
        orderBy('timestamp', 'asc')
    );
    
    const snapshot = await getDocs(q);
    const viagens = {};
    
    snapshot.forEach(doc => {
        const data = doc.data();
        if (!viagens[data.tripId]) {
            viagens[data.tripId] = {
                pontos: [],
                userType: data.userType,
                userName: data.userName
            };
        }
        viagens[data.tripId].pontos.push(data);
    });
    
    return viagens;
}

// Uso: Buscar viagens de hoje
const hoje = new Date();
hoje.setHours(0, 0, 0, 0);
const amanha = new Date(hoje);
amanha.setDate(amanha.getDate() + 1);

const viagensHoje = await buscarViagensPorPeriodo(userId, hoje, amanha);
```

### 5️⃣ Estatísticas de uma Viagem

```javascript
async function calcularEstatisticas(userId, tripId) {
    const locationsRef = collection(db, `locations/${userId}/tracks`);
    const q = query(
        locationsRef,
        where('tripId', '==', tripId),
        orderBy('timestamp', 'asc')
    );
    
    const snapshot = await getDocs(q);
    const pontos = [];
    
    snapshot.forEach(doc => {
        pontos.push(doc.data());
    });
    
    if (pontos.length === 0) return null;
    
    // Calcular distância total (aproximada)
    let distanciaTotal = 0;
    for (let i = 1; i < pontos.length; i++) {
        const dist = calcularDistancia(
            pontos[i-1].latitude,
            pontos[i-1].longitude,
            pontos[i].latitude,
            pontos[i].longitude
        );
        distanciaTotal += dist;
    }
    
    // Velocidade média
    const velocidades = pontos.map(p => p.speed * 3.6); // m/s para km/h
    const velocidadeMedia = velocidades.reduce((a, b) => a + b, 0) / velocidades.length;
    const velocidadeMaxima = Math.max(...velocidades);
    
    // Duração
    const inicio = pontos[0].timestamp;
    const fim = pontos[pontos.length - 1].timestamp;
    const duracao = (fim - inicio) / 1000 / 60; // minutos
    
    return {
        tripId,
        distanciaKm: distanciaTotal.toFixed(2),
        duracaoMinutos: duracao.toFixed(0),
        velocidadeMedia: velocidadeMedia.toFixed(1),
        velocidadeMaxima: velocidadeMaxima.toFixed(1),
        totalPontos: pontos.length,
        inicio: new Date(inicio),
        fim: new Date(fim),
        userType: pontos[0].userType,
        userName: pontos[0].userName
    };
}

// Função auxiliar para calcular distância (fórmula de Haversine)
function calcularDistancia(lat1, lon1, lat2, lon2) {
    const R = 6371; // Raio da Terra em km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
}
```

### 6️⃣ Listar Todas as Viagens de Todos os Motoristas (Admin)

```javascript
async function listarTodasViagens(tipoUsuario = null) {
    // Buscar todos os usuários
    const usersRef = collection(db, 'users');
    let q = query(usersRef);
    
    if (tipoUsuario) {
        q = query(usersRef, where('userType', '==', tipoUsuario));
    }
    
    const usersSnapshot = await getDocs(q);
    const todasViagens = [];
    
    for (const userDoc of usersSnapshot.docs) {
        const userId = userDoc.id;
        const userData = userDoc.data();
        
        const locationsRef = collection(db, `locations/${userId}/tracks`);
        const locSnapshot = await getDocs(locationsRef);
        
        const viagensUsuario = {};
        locSnapshot.forEach(doc => {
            const data = doc.data();
            if (!viagensUsuario[data.tripId]) {
                viagensUsuario[data.tripId] = {
                    tripId: data.tripId,
                    userId: userId,
                    userName: userData.fullName,
                    userType: data.userType,
                    pontos: []
                };
            }
            viagensUsuario[data.tripId].pontos.push(data);
        });
        
        Object.values(viagensUsuario).forEach(viagem => {
            viagem.pontos.sort((a, b) => a.timestamp - b.timestamp);
            todasViagens.push(viagem);
        });
    }
    
    return todasViagens;
}

// Uso: Listar todas as viagens de mototaxis
const viagensMototaxis = await listarTodasViagens('MOTOTAXI');
```

## 🎨 Interface Web - Exemplo Completo HTML

```html
<!DOCTYPE html>
<html>
<head>
    <title>Rotas - Amazon Track Data</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        #map { height: 500px; }
        .viagem-item { 
            padding: 10px; 
            border: 1px solid #ddd; 
            margin: 5px; 
            cursor: pointer; 
        }
        .viagem-item:hover { background: #f0f0f0; }
    </style>
</head>
<body>
    <h1>Histórico de Viagens</h1>
    
    <div>
        <label>Motorista:</label>
        <select id="motorista"></select>
        
        <button onclick="carregarViagens()">Carregar Viagens</button>
    </div>
    
    <div id="viagens-lista"></div>
    
    <div id="map"></div>
    
    <script type="module">
        import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js';
        import { getFirestore, collection, query, where, getDocs, orderBy } from 'https://www.gstatic.com/firebasejs/10.7.1/firebase-firestore.js';
        
        // Configuração do Firebase
        const firebaseConfig = { /* sua config */ };
        const app = initializeApp(firebaseConfig);
        const db = getFirestore(app);
        
        // Inicializar mapa
        const map = L.map('map').setView([-3.1190, -60.0217], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
        
        let currentPolyline = null;
        
        window.carregarViagens = async function() {
            const userId = document.getElementById('motorista').value;
            const viagens = await listarViagens(userId);
            
            const lista = document.getElementById('viagens-lista');
            lista.innerHTML = viagens.map(v => `
                <div class="viagem-item" onclick="desenharViagem('${userId}', '${v.tripId}')">
                    <strong>Viagem ${v.tripId}</strong><br>
                    Início: ${v.inicio.toLocaleString()}<br>
                    Fim: ${v.fim.toLocaleString()}<br>
                    Pontos: ${v.totalPontos}
                </div>
            `).join('');
        };
        
        window.desenharViagem = async function(userId, tripId) {
            if (currentPolyline) {
                map.removeLayer(currentPolyline);
            }
            
            const pontos = await buscarPontos(userId, tripId);
            currentPolyline = L.polyline(pontos, {color: '#2196F3'}).addTo(map);
            map.fitBounds(currentPolyline.getBounds());
        };
    </script>
</body>
</html>
```

## ✅ Vantagens do Sistema de Viagens

- ✅ **Organização**: Cada trajeto fica separado
- ✅ **Análise Individual**: Estatísticas por viagem
- ✅ **Histórico Completo**: Fácil visualizar todas as corridas
- ✅ **Performance**: Queries mais rápidas com tripId
- ✅ **Relatórios**: Gerar relatórios diários, semanais, mensais
