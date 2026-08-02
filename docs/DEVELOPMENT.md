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

### 0.1.46

- abertura pelo heads-up mantém o aplicativo em primeiro plano em aparelho
  desbloqueado;
- o retorno automático após a chamada fica restrito ao fluxo iniciado com o
  `Keyguard` realmente bloqueado.

### 0.1.45

- proteção contra `IDLE` transitório ao abrir uma chamada pelo heads-up;
- permanência do modal no primeiro plano enquanto a chamada ainda existe na
  interface ou no serviço SIP;
- teste unitário da política de retorno ao segundo plano;
- ringtone corporativo preservado sem alterações.

### 0.1.44

- toque interno do Liblinphone explicitamente desativado após a criação e a
  inicialização do core;
- ringtone corporativo mantido como fonte única da chamada recebida, sem
  alterar o ringback das chamadas efetuadas;
- testes automatizados e APK devem ser gerados na VM Android antes da
  homologação física.

### 0.1.43

- novo canal de chamada silencioso, sem reaproveitar preferências persistidas
  pelo Android no canal anterior;
- aplicativo visível apresenta apenas modal e toque corporativo, sem heads-up
  duplicado;
- segundo plano mantém heads-up e toque controlado exclusivamente pelo serviço
  SIP;
- abertura e minimização durante a chamada não reiniciam o `MediaPlayer`;
- testes automatizados e APK devem ser gerados na VM Android antes da
  homologação física.

### 0.1.42

- toque corporativo fornecido pela Eagle incorporado em `res/raw`;
- reprodução de chamada centralizada no serviço SIP, sem toque nativo paralelo
  do Liblinphone;
- eventos repetidos de exibição da chamada não reiniciam o `MediaPlayer`;
- testes automatizados e APK devem ser gerados na VM Android antes da
  homologação física.

### 0.1.41

- abertura imediata do modal de chamada ao tocar na notificação nativa;
- identidade do chamador recuperada do evento e do cache local enquanto a
  sessão e o SIP são restaurados em segundo plano;
- atendimento solicitado antes do registro SIP mantido em espera até o motor
  estar pronto;
- testes automatizados e geração do APK aprovados na VM Android;
- comportamento completo em processo encerrado será homologado no aparelho
  físico após a instalação desta revisão.

### 0.1.27

- projeto Firebase exclusivo associado ao identificador Android aprovado;
- plugin Google Services e módulo principal do Firebase Cloud Messaging
  integrados sem Analytics;
- `google-services.json` real mantido fora do Git;
- testes unitários e geração do APK de depuração aprovados na VM Android;
- registro do token no backend e recebimento de push permanecem como próximas
  etapas.

### 0.1.10

- teclado numérico envia DTMF somente durante chamada conectada;
- dígitos `1`, `2` e `#` recebidos e encaminhados pelo Asterisk;
- validação feita pelo canal temporário de diagnóstico DTMF, removido depois do
  teste;
- testes unitários, compilação e instalação no emulador aprovados.

### 0.1.9

- chamada interna recebida em primeiro plano;
- modal com nome, ramal e fotografia da agenda autorizada;
- recusa integrada à API homologada do Eagle PBX, encerrando a chamada em todos
  os dispositivos;
- atendimento pelo Android interrompendo o toque no Groundwire;
- encerramento pelo Android refletido imediatamente na origem;
- testes unitários, compilação e instalação no emulador aprovados;
- áudio bidirecional permanece pendente de aparelho físico.

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
