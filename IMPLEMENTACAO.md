# Amazon Track Data - Sistema de Autenticação

## 🎯 Funcionalidades Implementadas

### Sistema completo de autenticação com Firebase Authentication e Firestore

✅ **Cadastro de Usuários**
- Nome completo
- Email
- CPF (com validação e formatação automática)
- Senha (mínimo 6 caracteres)
- Confirmação de senha
- Seleção de tipo de usuário:
  - Passageiro
  - Mototaxi
  - Barqueiro

✅ **Login Automático**
- Após o cadastro, o usuário é automaticamente logado

✅ **Validações Implementadas**
- Email válido
- CPF válido (algoritmo completo de validação)
- Nome completo (mínimo 2 palavras)
- Senha mínima de 6 caracteres
- Confirmação de senha

## 🏗️ Arquitetura

O projeto segue a arquitetura **MVVM (Model-View-ViewModel)** com as melhores práticas:

```
app/src/main/java/com/example/amazontrackdata/
├── data/
│   ├── model/
│   │   ├── User.kt              # Modelo de dados do usuário
│   │   ├── UserType.kt          # Enum dos tipos de usuário
│   │   └── AuthResult.kt        # Sealed class para resultados
│   └── repository/
│       └── AuthRepository.kt    # Repository para operações de autenticação
├── ui/
│   ├── screens/
│   │   ├── LoginScreen.kt       # Tela de login
│   │   ├── SignUpScreen.kt      # Tela de cadastro
│   │   └── HomeScreen.kt        # Tela inicial (após login)
│   └── viewmodel/
│       └── AuthViewModel.kt     # ViewModel de autenticação
├── navigation/
│   ├── Screen.kt                # Rotas de navegação
│   └── AppNavigation.kt         # Configuração de navegação
├── utils/
│   └── ValidationUtils.kt       # Utilitários de validação
└── MainActivity.kt              # Activity principal
```

## 🔥 Firebase Services Utilizados

1. **Firebase Authentication** - Autenticação de usuários
2. **Firebase Firestore** - Armazenamento de dados dos usuários

## 📱 Fluxo da Aplicação

1. **Primeira vez**: Usuário vê a tela de Login
2. **Cadastro**: Clica em "Cadastre-se" e preenche os dados
3. **Login Automático**: Após cadastro, é automaticamente logado
4. **Home**: Vê suas informações e pode fazer logout
5. **Persistência**: Login persiste entre sessões

## 🎨 UI/UX

- **100% Jetpack Compose** (nenhum XML foi usado)
- Material Design 3
- Validação em tempo real
- Feedback visual de erros
- Loading states
- Formatação automática de CPF
- Visibilidade de senha toggleável

## 🔒 Segurança

- Senhas nunca são armazenadas no Firestore
- Firebase Authentication gerencia senhas de forma segura
- Validação no cliente e no servidor
- CPF validado com algoritmo oficial

## 🚀 Como Usar

1. **Sincronize o projeto** no Android Studio
2. **Aguarde o download** das dependências do Firebase
3. **Execute o app** em um dispositivo ou emulador
4. **Cadastre-se** como Passageiro, Mototaxi ou Barqueiro
5. **Explore** as funcionalidades

## 📦 Dependências Adicionadas

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
implementation("com.google.firebase:firebase-analytics")
implementation("com.google.firebase:firebase-auth")
implementation("com.google.firebase:firebase-firestore")

// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.8.5")

// ViewModel Compose
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
```

## 🎓 Boas Práticas Aplicadas

✅ Separação de responsabilidades (MVVM)
✅ Single Source of Truth (StateFlow)
✅ Coroutines para operações assíncronas
✅ Sealed classes para estados
✅ Repository pattern
✅ Validação centralizada
✅ Navegação declarativa
✅ UI reativa com Compose
✅ Error handling adequado
✅ Loading states
✅ Código organizado e escalável

## 📝 Próximos Passos Sugeridos

- [ ] Recuperação de senha
- [ ] Edição de perfil
- [ ] Upload de foto de perfil
- [ ] Verificação de email
- [ ] Autenticação com Google/Facebook
- [ ] Implementar funcionalidades específicas por tipo de usuário
