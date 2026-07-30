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

### 0.1.8

- botão de chamada habilitado somente com registro SIP e destino preenchido;
- chamada interna originada pelo endpoint exclusivo do dispositivo;
- estados de início, toque, conexão, encerramento e falha integrados à interface;
- botão muda para encerramento durante a tentativa ou chamada ativa;
- chamada para o ramal 102 e encerramento pelo Android homologados no emulador;
- testes unitários, compilação e instalação no emulador aprovados;
- áudio bidirecional e chamada recebida permanecem como próximas etapas.

### 0.1.7

- configuração SIP solicitada somente depois do registro autenticado do
  dispositivo;
- credencial individual mantida apenas em memória e removida no logoff;
- transporte PJSIP TLS e mídia SRTP obrigatória configurados no Liblinphone;
- endpoint mobile exclusivo registrado no Asterisk;
- contato disponível validado sem reinício e sem alterar PJSIP 101–105,
  Groundwire, PWA ou Tauri;
- testes unitários, compilação e instalação no emulador aprovados;
- primeira chamada interna e recebimento permaneciam como próximos testes.

### 0.1.6

- identidade aleatória e persistente criada por instalação;
- identidade mantida separada da sessão e excluída do backup do Android;
- registro autenticado e idempotente na API do Eagle PBX;
- servidor armazena somente o hash do identificador da instalação;
- estado `Dispositivo registrado · SIP pendente` integrado ao Discador;
- nenhuma credencial SIP criada, transmitida ou persistida nesta etapa;
- testes unitários, compilação, instalação e registro no emulador aprovados.

### 0.1.5

- Liblinphone `5.5.13` integrado a partir do repositório Maven oficial;
- núcleo SIP inicializado sem credenciais ou conta provisionada;
- estado `Motor SIP inicializado` validado no emulador;
- APK limitado a `arm64-v8a` para aparelhos e `x86_64` para o emulador;
- empacotamento e 28 bibliotecas nativas validados para páginas de 16 KB;
- testes unitários, compilação e inicialização sem falhas;
- registro SIP permanece bloqueado até existir provisionamento individual e
  revogável por dispositivo.

### 0.1.4

- histórico pessoal carregado pela API autenticada;
- fotos, nomes, direção, resultado, data, horário e duração validados;
- filtros Todas e Perdidas;
- indicação de gravação disponível ou indisponível;
- player autenticado com reprodução, pausa, avanço e contador;
- arquivo autorizado mantido somente no cache temporário;
- continuidade e retomada do áudio pendentes de confirmação em aparelho físico,
  pois o encaminhamento de áudio por RDP apresentou estalos.

### 0.1.3

- agenda corporativa carregada pela API autenticada;
- contatos ordenados alfabeticamente e consolidados em um card por pessoa;
- fotografias CardDAV e iniciais de contingência validadas;
- pesquisa por nome, número e rótulo;
- modal com todos os números de cada contato;
- atualização manual da agenda sem duplicação de cards.

### 0.1.2

- navegação nativa entre Discador, Contatos e Histórico;
- cabeçalho com identidade do usuário e estado de presença;
- modal Minha conta integrado ao logoff seguro;
- alteração de presença integrada à API existente;
- DND global ao ramal documentado como comportamento temporário;
- DND independente por dispositivo planejado para a revisão 1.0.4.
- Discador nativo homologado visualmente no emulador;
- teclado, backspace, limpeza prolongada e inserção prolongada de `+` validados;
- retorno sonoro das teclas pendente de validação em aparelho físico.

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
