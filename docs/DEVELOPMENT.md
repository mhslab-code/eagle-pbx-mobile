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

### 0.1.60

- propriedade do ciclo baseada no `nativePointer` estável do objeto Liblinphone;
- correlação externa congelada no primeiro evento, mesmo que o `Call-ID` só
  apareça posteriormente;
- `accept()` e `decline()` restritos aos estados reais de chamada recebida;
- confirmação de atendimento idempotente entre `Connected` e `StreamsRunning`;
- despertar FCM integrado a `Core.processPushNotification(null)` para recuperar
  o registro sem confundir o ID Asterisk com o `Call-ID` SIP;
- build limpo e testes unitários aprovados;
- quatro testes instrumentados aprovados no Android 16/API 36 equivalente ao
  Galaxy A25 5G;
- segunda chamada SIP real permanece como validação física decisiva no S25
  Ultra.

### 0.1.59

- chamadas correlacionadas pelo `Call-ID` real fornecido pelo Liblinphone;
- eventos terminais duplicados ou atrasados afetam somente a sessão de origem;
- notificação recebida e chamada em andamento mantêm identidades SIP separadas;
- segunda chamada preservada enquanto o Android Telecom desmonta a primeira;
- teste instrumentado reproduz a sequência: primeira atendida, segunda recebida
  e `Released` tardio da primeira;
- três baterias consecutivas e a suíte final no Android 16 passaram sem crash,
  ANR, erro de foreground ou timeout de `CallStyle`;
- estabelecimento SIP real permanece como validação física obrigatória no S25
  Ultra.

### 0.1.58

- controlador SIP movido da `MainActivity` para o ciclo de vida do processo;
- chamadas VoIP integradas ao `androidx.core:core-telecom` e registradas no
  Android Telecom;
- mesma chamada sinalizada por FCM e SIP processada uma única vez;
- `PendingIntent` exclusivo por ciclo de chamada e remoção do despertar manual
  anterior à intenção de tela cheia;
- atividade de bloqueio usa `finish()` e não encerra mais o serviço SIP ao
  atender ou recusar;
- notificação `CallStyle` usada como foreground durante toque e chamada ativa;
- teste instrumentado em Android 16 com PIN validou atendimento na primeira
  chamada, três chamadas bloqueadas consecutivas e atualização da notificação;
- bateria final: três processos novos, doze telas cheias, doze registros Telecom,
  nenhum timeout de `CallStyle`, crash ou ANR;
- estabelecimento SIP real permanece como validação física obrigatória no S25
  Ultra.

### 0.1.57

- comando **Atender** confirmado diretamente pelo controlador SIP;
- estado `Conectando...` bloqueado quando o aceite não foi executado nem
  enfileirado;
- transição completa permanece pendente de homologação física no S25 Ultra.

### 0.1.56

- removida a abertura antecipada da atividade principal durante o aceite;
- atendimento da tela cheia encaminhado pelo serviço e transição visual
  condicionada à confirmação `CONNECTED`;
- fluxo completo permanece pendente de homologação física no S25 Ultra.

### 0.1.55

- atendimento pela tela cheia não reapresenta as ações de chamada recebida na
  atividade principal;
- confirmação visual de chamada ativa continua condicionada ao estado SIP
  `CONNECTED`;
- atendimento e recusa permanecem pendentes de homologação física no S25
  Ultra.

### 0.1.54

- corrigida a regressão de encerramento do processo observada fisicamente na
  `0.1.53` no Galaxy S25 Ultra;
- opções do criador e do remetente do `PendingIntent` separadas conforme a API
  oficial do Android;
- rejeições específicas do fabricante não podem interromper a sinalização da
  chamada;
- tela bloqueada permanece pendente de nova homologação física.

### 0.1.53

- intenção de tela cheia compatível com as restrições de abertura em segundo
  plano dos Androids 15 e 16;
- criador e remetente do `PendingIntent` autorizados apenas no fluxo de chamada
  recebida;
- exibição real sobre o bloqueio permanece como teste físico obrigatório no
  Galaxy S25 Ultra.

### 0.1.52

- proteção contra eventos atrasados de notificação após o atendimento SIP;
- atividade dedicada mantida até confirmação real do atendimento;
- contingência de tela cheia condicionada a aparelho bloqueado, chamada ainda
  ativa e atividade ainda não visível;
- atualização sobre a revisão anterior deve ser validada com a mesma assinatura
  antes da publicação;
- tela bloqueada e atendimento permanecem como testes físicos obrigatórios no
  S25 Ultra.

### 0.1.51

- atividade de chamada preparada explicitamente para aparecer e acender sobre
  o bloqueio do Android;
- transição visual para chamada atendida condicionada à confirmação
  `CONNECTED` do motor SIP;
- testes unitários e APK de depuração gerados com sucesso na VM Android;
- exibição real sobre o bloqueio e atendimento no S25 Ultra permanecem como
  testes físicos obrigatórios desta revisão.

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
