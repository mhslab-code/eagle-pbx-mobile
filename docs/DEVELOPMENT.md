# Ambiente de desenvolvimento Android

## Requisitos

- Android Studio Quail 3 ou compatível;
- JDK integrado ao Android Studio;
- Android SDK Platform 36;
- Android SDK Build-Tools;
- Android SDK Platform-Tools;
- Android Emulator com aceleração KVM;
- Git.

O Android Studio pode manter plataformas adicionais para testes, mas a
compilação e o alvo do aplicativo permanecem fixados no API 36.

## Projeto

Abra no Android Studio o diretório:

```text
android/
```

O projeto usa o Gradle Wrapper próprio. Não instale nem fixe uma versão global
do Gradle.

## Validação local

Com `JAVA_HOME` e `ANDROID_HOME` configurados:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
```

O APK de depuração é gerado somente para desenvolvimento local. A chave
definitiva de assinatura e suas senhas não pertencem ao repositório.

## Emuladores iniciais

- Galaxy A25 5G equivalente: 1080 × 2340, 420 dpi, Android 16/API 36;
- Android 15/API 35 será usado na matriz de compatibilidade;
- uma imagem com páginas de memória de 16 KB será validada antes da adoção
  definitiva de bibliotecas nativas, especialmente o Liblinphone.

## Marcos validados

### 0.1.2

- navegação nativa entre Discador, Contatos e Histórico;
- cabeçalho com identidade do usuário e estado de presença;
- modal Minha conta integrado ao logoff seguro;
- alteração de presença integrada à API existente;
- DND global ao ramal documentado como comportamento temporário;
- DND independente por dispositivo planejado para a revisão 1.0.4.

### 0.1.1

- login real no endpoint HTTPS homologado;
- senha mantida somente em memória durante o envio;
- cookie de sessão cifrado por chave do Android Keystore;
- restauração após encerramento e abertura do processo;
- logoff remoto e limpeza local;
- nenhum marcador de sessão observado no Logcat;
- conta administrativa recusada no aplicativo mobile.

### 0.1.0

- projeto Kotlin/Jetpack Compose compilado com API 36;
- testes unitários e geração do APK de depuração aprovados;
- instalação e inicialização no emulador Galaxy A25 5G aprovadas;
- identidade visual e tela inicial de login aprovadas;
- campos ainda sem integração com a API.
