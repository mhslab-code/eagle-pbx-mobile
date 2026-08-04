# Arquitetura mobile

## Visão geral

```text
Android / iOS
      |
      | HTTPS, push, SIP e mídia segura
      v
Eagle PBX App API e serviço de provisionamento
      |
      +-- autenticação, usuários e dispositivos
      +-- contatos e fotografias autorizados
      +-- histórico e gravações autorizadas
      +-- push FCM/APNs
      +-- proxy SIP/push
      |
      +------------+-------------+
      v                          v
 Issabel PBX                  CardDAV
```

O mobile não acessa diretamente o banco, AMI, arquivos de gravação ou
credenciais técnicas. Todos os dados passam pelas APIs autorizadas.

## Componentes Android

- interface nativa;
- camada autenticada de API;
- armazenamento seguro de sessão e provisionamento;
- motor SIP Liblinphone `5.5.13`, com conta individual provisionada pela API;
- integração Telecom Framework/`ConnectionService`;
- serviço de push FCM;
- gerenciamento de áudio e Bluetooth;
- cache local limitado de contatos e histórico.

## Firebase Cloud Messaging

Foi aprovado um projeto Firebase exclusivo, **Eagle PBX Mobile**, para o
identificador Android `com.eaglesistemas.eaglepbx`.

O FCM será usado somente como sinalização para despertar o aplicativo quando o
processo não estiver disponível. A chamada, a autenticação, o provisionamento e
a mídia continuam pertencendo à infraestrutura Eagle PBX; nenhuma credencial
SIP será transportada no payload do push.

```text
PBX detecta a chamada
        |
        v
Backend Eagle PBX -- FCM HTTP v1 --> Android
        |                              |
        |                              v
        +----------------------> serviço de chamada
                                       |
                                       v
                              SIP/TLS e mídia segura
```

Decisões operacionais:

- Firebase Cloud Messaging é utilizado na modalidade sem custo;
- o backend existente envia o push, sem Cloud Functions ou Cloud Run;
- cada instalação associa seu Firebase Installation ID (FID) à identidade
  revogável do dispositivo;
- a API nunca devolve credenciais Firebase ao cliente;
- a credencial de serviço e `google-services.json` real não são versionados;
- registros substituídos, inválidos ou pertencentes a dispositivos revogados são
  removidos do cadastro;
- o payload contém apenas identificadores mínimos da chamada, sem senha SIP,
  cookie de sessão ou outro segredo.

## Identidade por dispositivo

Cada instalação deve possuir:

- identificador aleatório persistente e revogável;
- endpoint SIP próprio;
- credencial diferente dos demais aparelhos;
- token de push associado;
- registro de criação, uso e revogação.

Um ramal pode possuir múltiplos dispositivos, mas uma credencial não deve ser
compartilhada entre eles.

Desde a versão 0.1.6, o identificador é criado localmente e mantido em armazenamento
privado separado da sessão. A API recebe esse valor por HTTPS, armazena somente
seu hash e cria um registro idempotente com estado `pending`. Nenhuma credencial
SIP é criada ou devolvida nessa etapa.

Na versão 0.1.7, um dispositivo marcado como `ready` pode solicitar sua
configuração SIP. A API valida conjuntamente sessão, usuário, hash da instalação,
estado e nome do endpoint. A credencial é mantida apenas em memória pelo
aplicativo, entregue por HTTPS e usada para registrar PJSIP TLS com SRTP.

Na versão 0.1.8, o discador origina chamadas internas pelo endpoint SIP
exclusivo do dispositivo. Os estados de tentativa, toque e conexão são
acompanhados pelo Liblinphone, e o aplicativo pode encerrar a chamada durante a
tentativa ou depois do atendimento. O áudio permanece como etapa separada de
homologação.

Na versão 0.1.9, chamadas recebidas em primeiro plano exibem identidade,
ramal e foto obtidos da agenda autorizada. O atendimento ocorre diretamente
pelo Liblinphone e faz os demais dispositivos pararem de tocar. A recusa usa
também a API de telefonia já homologada no desktop, encerrando a chamada em
todos os dispositivos do ramal.

Na versão 0.1.10, as teclas do discador enviam DTMF pelo Liblinphone durante
uma chamada conectada. O Asterisk confirmou a recepção e o encaminhamento dos
eventos por RFC4733, independentemente do áudio redirecionado pelo RDP.

Na versão 0.1.41, a abertura originada pela notificação não depende da conclusão
da restauração autenticada. O evento fornece a identidade mínima da chamada, o
cache local complementa nome e fotografia e o modal é apresentado imediatamente.
Sessão, provisionamento e registro SIP avançam em segundo plano; a ação de
atender pode ficar enfileirada até o telefone estar apto a processá-la.

Na versão 0.1.42, o serviço SIP é o único proprietário do toque de chamada. O
áudio corporativo é empacotado no APK, o toque nativo do Liblinphone permanece
desativado e novas sinalizações da mesma chamada reutilizam a reprodução já
ativa. Assim, abrir o aplicativo pelo heads-up não cria uma segunda camada de
áudio. O serviço encerra a reprodução ao atender, recusar, receber cancelamento
da origem ou finalizar a chamada.

Na versão 0.1.43, a apresentação acompanha a visibilidade do aplicativo. Em
primeiro plano há somente o modal interno; em segundo plano há notificação de
alta prioridade. Ambos reutilizam o toque único do serviço SIP. O canal Android
`eagle_pbx_incoming_calls_v3` não possui som próprio e substitui o canal anterior,
cuja configuração o sistema operacional poderia ter preservado entre versões.

Na versão 0.1.44, além de desabilitar o toque nativo do sistema, o caminho de
toque interno do Liblinphone é explicitamente limpo antes e depois da
inicialização do core. Isso evita que o toque padrão do telefone seja reproduzido
em paralelo com o MP3 corporativo, sem interferir no ringback das chamadas
efetuadas.

Na versão 0.1.45, o retorno automático ao segundo plano exige confirmação
conjunta da interface e do serviço SIP de que não existe mais chamada recebida.
Um estado `IDLE` intermediário, comum durante a restauração disparada pelo
heads-up, não pode minimizar a Activity nem fazer a notificação reaparecer.

Na versão 0.1.46, esse retorno também depende do estado real do `Keyguard` no
momento em que a notificação é aberta. Em aparelho desbloqueado, o modal de
chamada permanece em primeiro plano; o retorno automático fica reservado às
chamadas abertas sobre a tela efetivamente bloqueada.

Na versão 0.1.51, a atividade dedicada recebe flags de janela para aparecer e
acender sobre o bloqueio, e o serviço SIP pode iniciá-la diretamente quando o
`Keyguard` estiver ativo. A ação **Atender** interrompe a sinalização local, mas
o estado visual somente passa a atendido após o Liblinphone confirmar
`CONNECTED`; assim, a interface não confunde uma solicitação de atendimento
com uma chamada já estabelecida.

Na versão 0.1.52, a restauração deixa de aceitar um evento atrasado da
notificação quando o motor já está em `CONNECTED` ou `HELD`. A atividade de
chamada sinaliza sua própria visibilidade ao serviço; se o Android não a exibir
após aceitar a intenção de tela cheia, uma contingência temporizada pode
apresentá-la, desde que o aparelho continue bloqueado e a chamada ainda exista.

Na versão 0.1.53, o criador e o remetente do `PendingIntent` da chamada
concedem explicitamente a autorização de abertura em segundo plano exigida a
partir do Android 15. Essa autorização fica limitada à atividade dedicada de
chamada recebida e à sua contingência no aparelho bloqueado.

Na versão 0.1.54, a autorização do criador segue o modo compatível documentado
pelo Android e a autorização do remetente é aplicada apenas no envio manual da
contingência. A criação das opções possui fallback seguro para preservar toque
e vibração mesmo que uma implementação do fabricante rejeite a opção.

Na versão 0.1.55, a solicitação de atendimento originada na atividade dedicada
remove imediatamente as ações de chamada recebida do estado da interface
principal. A atividade dedicada acompanha `Conectando...` até a confirmação
SIP, evitando um segundo modal antes da tela de chamada ativa.

Na versão 0.1.56, a atividade dedicada não abre a `MainActivity` para solicitar
o atendimento. A ação segue pelo serviço; a `MainActivity` só volta ao primeiro
plano depois que o motor SIP confirma `CONNECTED`, já no estado de chamada em
andamento.

Na versão 0.1.57, a solicitação entre atividade dedicada e controlador SIP tem
retorno síncrono. O estado visual `Conectando...` depende de aceite efetivo ou
fila confirmada; ausência do handler mantém as ações disponíveis em vez de
simular atendimento.

Na versão 0.1.58, `EaglePbxApplication` passa a possuir o `LoginViewModel` e o
controlador de telefonia durante todo o processo. Assim, FCM, serviço foreground
e `IncomingCallActivity` acessam o mesmo motor SIP mesmo quando a
`MainActivity` nunca foi criada. `EagleTelecomController` registra cada chamada
no Android Telecom por `CallsManager.addCall`, serializa as transições de
atendimento, ativação e desligamento e mantém o fluxo personalizado como
contingência caso uma implementação OEM recuse a integração.

A notificação recebida e a notificação em andamento usam o mesmo identificador
foreground. A atividade de chamada usa somente `finish()`: remover sua tela não
remove a tarefa nem encerra `SipForegroundService`. Eventos FCM e SIP com o
mesmo identificador ou número são idempotentes, enquanto chamadas posteriores
com novos identificadores permanecem independentes.

Na versão 0.1.59, o ciclo deixa de depender de uma referência global sem
identidade. `LinphoneEngine` extrai o `Call-ID` do `CallLog`, ignora a repetição
terminal `End`/`Released` e só limpa o estado ativo quando o evento pertence ao
mesmo `Call-ID`. `SipForegroundService` mantém separadamente o identificador SIP
da chamada recebida e o da chamada em andamento. Assim, a liberação tardia de
uma sessão já encerrada não cancela um novo alerta ou um novo aceite. O bridge
do Android Telecom conserva uma segunda sessão pendente até o fechamento da
primeira e vincula push, SIP e Telecom sem usar apenas o número do chamador.

O estado de presença já utiliza a API existente do Eagle PBX. Temporariamente,
o DND continua sendo aplicado ao ramal inteiro no Asterisk. A separação do DND
por instalação — Android, PWA, desktop e softphones — fica prevista para a
revisão 1.0.4 do ecossistema e exigirá identidade de dispositivo também nessa
operação.

## Fases

### 1. Fundação

- repositório público e AGPLv3;
- projeto Android em Kotlin e Jetpack Compose;
- SDK mínimo 28 e SDK alvo 36;
- identidade visual;
- proteção contra segredos;
- pipeline de validação sem chave de produção.

### 2. API

- login;
- perfil e DND;
- contatos e fotografias;
- histórico e gravações autorizadas;
- cadastro do dispositivo concluído;
- provisionamento SIP individual concluído; revogação administrativa pendente.

### 3. Telefonia em primeiro plano

- protótipo Liblinphone e compatibilidade de 16 KB validados;
- registro SIP individual validado;
- chamadas internas originadas e recebidas, atendimento, recusa global e
  encerramento validados; chamadas externas pendentes;
- DTMF durante chamada validado;
- áudio bidirecional;
- DTMF;
- rotas de áudio e Bluetooth.

### 4. Telefonia em segundo plano

- projeto Firebase exclusivo e FCM sem custo definidos; integração pendente;
- proxy SIP/push;
- Telecom Framework e `ConnectionService`;
- chamada recebida com aplicativo suspenso e encerrado.

### 5. Homologação

- Samsung Galaxy A25 5G com Android 15 e Android 16;
- Samsung Galaxy S25 Ultra com Android 16 como validação complementar;
- Wi-Fi e rede móvel;
- troca de rede durante chamada;
- suspensão e encerramento;
- Bluetooth;
- DND;
- múltiplos dispositivos;
- operação paralela com Groundwire.

### 6. iOS

Somente após homologar o Android: PushKit, APNs, CallKit e processo de
distribuição Apple.
