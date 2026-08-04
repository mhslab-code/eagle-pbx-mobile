# Eagle PBX Mobile

Aplicativo mobile nativo de telefonia corporativa da Eagle Sistemas.

## Estado

Projeto iniciado em 30 de julho de 2026. A primeira plataforma será Android,
com homologação operacional nos Samsung Galaxy A25 5G e validação complementar
no Samsung Galaxy S25 Ultra. O iOS será desenvolvido depois da homologação do
fluxo Android.

Versão de desenvolvimento atual: `0.1.53`. A identidade visual, o login nativo,
a restauração segura da sessão, o logoff, a navegação principal e a integração
inicial de presença foram validados no emulador equivalente ao Galaxy A25 5G.
A agenda corporativa, o histórico e o player autenticado de gravações também
estão integrados. Cada instalação registra uma identidade individual na API,
recebe exclusivamente sua configuração SIP e estabelece registro PJSIP TLS
com o Liblinphone. Chamadas internas originadas e recebidas, atendimento,
recusa global e encerramento pelo aplicativo foram homologados. O áudio
permanece pendente de homologação em aparelho físico. O envio de DTMF durante
a chamada foi validado diretamente no Asterisk. As teclas aceitas também são
exibidas visualmente em uma linha separada, sem alterar o destino discado.
O controle de microfone já alterna visualmente entre `Microfone` e `Mudo` e
atua na sessão Liblinphone. A interface foi validada no emulador; a confirmação
do silenciamento real permanece pendente de teste em aparelho físico, pois o
microfone não é encaminhado ao emulador pela sessão RDP.
O controle de saída de áudio lista os dispositivos de reprodução oferecidos
pelo Android, identifica a saída atual e permite selecioná-la sem interromper
a chamada. O fluxo visual foi homologado no emulador; o roteamento acústico
real permanece pendente de aparelho físico.
Espera e retomada também estão operacionais: o interlocutor recebe a música
do PBX, o botão muda para `Retomar` e a chamada volta ao estado conectado sem
ser encerrada.

O toque de chamada recebido é um recurso corporativo incorporado ao APK. O
aplicativo mantém uma única reprodução contínua por chamada, mesmo quando a
notificação abre a interface, e desativa o toque nativo paralelo do motor SIP.
O alerta respeita o perfil sonoro do Android: em modo Som reproduz o toque
corporativo, em modo Vibrar utiliza somente a vibração nativa contínua durante a chamada
e em modo Silencioso não produz som nem vibração. A Activity de atendimento
pode acender e aparecer sobre a tela bloqueada enquanto a chamada estiver ativa.
Na revisão `0.1.50`, a chamada recebida usa uma atividade nativa exclusiva em
tela cheia: mostra foto, nome e número do chamador e oferece **Recusar** e
**Atender** sem passar pela restauração da sessão ou pela tela do discador.
Na revisão `0.1.51`, essa atividade recebe também as sinalizações explícitas de
tela bloqueada e o atendimento só muda para o estado conectado depois da
confirmação real do SIP. Isso evita a falsa tela de espera após tocar em
**Atender**.
Na revisão `0.1.52`, notificações atrasadas deixam de sobrescrever uma chamada
já atendida. A atividade dedicada permanece em `Conectando...` até a confirmação
real do SIP e o serviço usa uma contingência curta caso o Android aceite a
intenção de tela cheia, mas não apresente a tela personalizada no bloqueio.
No Android 14 ou superior, o card de notificações em `Minha conta` também
identifica e abre a autorização específica de chamadas em tela cheia quando ela
ainda não foi concedida pelo sistema.
Na revisão `0.1.53`, a intenção de tela cheia e sua contingência passam a
autorizar explicitamente a abertura da atividade em segundo plano, conforme as
restrições dos Androids 15 e 16 usados pelo Galaxy S25 Ultra.
Com o aplicativo visível, somente o modal interno e esse toque são apresentados;
o aviso nativo fica reservado ao segundo plano. O canal de chamadas do Android
é silencioso e não disputa a reprodução controlada pelo serviço de telefonia.
A transferência direta utiliza um modal com visor e teclado, entrega a chamada
ao destino escolhido e remove o Android da sessão depois da aceitação pelo
PBX. O fluxo completo foi homologado entre os ramais 104 e 105.
A transferência assistida mantém a chamada original em espera enquanto o
usuário consulta o segundo destino. É possível cancelar a consulta e retomar a
chamada original ou concluir a transferência, conectando os dois ramais e
retirando o Android da sessão. Os dois caminhos foram homologados entre os
ramais 104 e 105. Quando o destino não atende, a chamada original é retomada,
uma confirmação visual breve é exibida e o modal fecha automaticamente.
Adicionar chamada mantém o primeiro participante em espera, consulta o segundo
destino e somente libera a formação da conferência depois do atendimento real.
Destinos internos sem atendimento não seguem para caixa postal nesse fluxo; a
chamada original é retomada automaticamente. A conferência entre os ramais
101, 104 e 105, bem como seu encerramento simultâneo pelo Android, foi
homologada.
O botão `Atualizar` do histórico ignora respostas HTTP armazenadas e consulta
novamente o servidor, mantendo as chamadas recentes sincronizadas.
Enquanto o aplicativo permanece aberto em segundo plano, um serviço de
telefonia mantém o processo SIP elegível para execução. O ciclo de vida do
Liblinphone acompanha a passagem entre primeiro e segundo plano, mantém o
transporte SIP ativo e renova o registro ao retornar. O serviço FCM já recebe
o evento `incoming_call` em alta prioridade, restaura a interface autenticada,
apresenta a notificação de chamada e inicia o ciclo de registro SIP. A
homologação após encerramento do processo depende agora do canal móvel
retardado no PBX.
Em chamadas recebidas, o serviço nativo do Liblinphone mantém o ciclo da
chamada e uma notificação de alta prioridade alerta e abre a interface de
atendimento quando o aplicativo está em segundo plano.
Ao tocar nessa notificação, a identificação mínima recebida pelo push e os
contatos mantidos no cache abrem imediatamente o modal de chamada recebida,
com número, nome e fotografia quando disponíveis. A restauração da sessão e do
registro SIP ocorre em segundo plano; se o usuário escolher atender antes de o
telefone SIP ficar pronto, a ação permanece aguardando e é executada assim que
o registro for concluído.
Ao encerrar uma chamada que despertou a tela, o aplicativo retorna ao segundo
plano sem interromper o serviço SIP. Em aparelho sem bloqueio seguro, como o
emulador atual, a tela permanece desbloqueada; o retorno ao PIN ou à biometria
será confirmado no Galaxy A25 físico.

A revisão `0.1.38` acrescenta cache local cifrado para contatos e histórico,
identificação do contato no Discador e notificação persistente para chamadas
perdidas. O ícone oficial também foi aplicado ao launcher Android. O fluxo de
chamada perdida foi homologado no emulador; escala do ícone e acabamento visual
da notificação serão reavaliados no Galaxy A25 físico, sem bloquear esta
homologação funcional.

## Decisões aprovadas

- colaboradores não podem limpar nem ocultar o histórico de chamadas pelo
  aplicativo;
- identificador Android: `com.eaglesistemas.eaglepbx`;
- distribuição inicial: APK assinado e instalado internamente;
- SDK SIP candidato: Liblinphone;
- licença do aplicativo: GNU AGPLv3;
- aproximadamente seis instalações Android e limite operacional inicial de dez
  ramais;
- Groundwire preservado como contingência durante a homologação;
- código-fonte público, sem credenciais ou configurações internas.

## Push Android

O recebimento após o Android encerrar o processo utilizará um projeto Firebase
exclusivo do produto, denominado **Eagle PBX Mobile**. O aplicativo usará
somente o Firebase Cloud Messaging (FCM), serviço sem custo, para receber o
evento que desperta o fluxo nativo de chamada.

O backend já existente do Eagle PBX enviará as mensagens pelo protocolo HTTP v1
do FCM. Não serão contratados Cloud Functions, Cloud Run, banco, hospedagem ou
outros serviços pagos do Firebase para esse fluxo.

O arquivo Android `google-services.json` de produção e a credencial de serviço
usada pelo backend são segredos operacionais: permanecem fora do Git e devem ser
armazenados exclusivamente nos respectivos servidores e ambientes autorizados.

A revisão `0.1.27` integrou ao APK o plugin Google Services e o módulo principal
do Firebase Cloud Messaging, sem Analytics e sem outros produtos Firebase. O
arquivo de configuração real foi validado apenas na VM Android e continua
ignorado pelo Git. As revisões `0.1.28` e `0.1.29` acrescentaram,
respectivamente, o registro revogável do FID no backend e o processamento do
evento de chamada que desperta a sessão autenticada e inicia o registro SIP.
Nenhum identificador de instalação é exibido ou gravado em log.

O protótipo utiliza Liblinphone `5.5.13`. Sua adoção definitiva somente
ocorrerá depois da validação de registro, chamadas, push, segundo plano,
Bluetooth e compatibilidade em aparelho físico com Android 16.

A revisão `0.1.39` habilita no Android as permissões nativas de microfone e de
controle de áudio usadas pelo motor SIP. O Discador também fornece retorno DTMF
local em cada tecla. A homologação desta revisão exige teste em aparelho físico
do som das teclas e do áudio bidirecional durante uma chamada.

A revisão `0.1.34` iniciou o acabamento visual nativo homologável. O cabeçalho
agora separa a marca Eagle Sistemas do perfil do usuário, preserva o seletor de
presença e aproxima a hierarquia visual do PWA/desktop. No Discador, mensagens
técnicas de provisionamento bem-sucedido deixaram de ocupar a interface; apenas
falhas acionáveis são apresentadas ao usuário. Nenhuma regra SIP ou fluxo de
chamada foi alterado nesta revisão.

A revisão `0.1.35` substituiu a aproximação inicial pela composição visual do
Eagle PBX desktop: logotipo oficial monocromático, fotografia do usuário obtida
da agenda autorizada, presença contornada, cabeçalho em dois níveis, Discador
sem cartão externo, ícones nativos nas ações e navegação inferior com ícones e
legendas. Esta revisão é exclusivamente visual e não altera o motor SIP.

A revisão `0.1.36` adicionou ao cabeçalho o seletor de aparência homologado no
PWA/Tauri. O botão alterna sequencialmente entre `Claro`, `Escuro` e `Sistema`,
mantém a escolha no aparelho e, no modo `Sistema`, acompanha a configuração do
Android. Cabeçalho, navegação inferior, login e restauração da sessão preservam
a identidade escura da marca; o conteúdo das abas e os modais adotam o tema
selecionado. Não houve alteração no motor SIP nem nos fluxos de chamada.

## Comportamento temporário do DND

Na API atual, `Não perturbe` é aplicado globalmente ao ramal no Asterisk. Assim,
ao ativá-lo no Android, os demais dispositivos registrados no mesmo ramal
também deixam de tocar. Este comportamento foi mantido nesta fase para não
divergir do PWA e do aplicativo desktop.

O DND independente por dispositivo está planejado para a revisão `1.0.4` do
ecossistema Eagle PBX. Até essa revisão, o teste de DND deve considerar seu
efeito global sobre o ramal.

## Escopo Android

- autenticação na API do Eagle PBX;
- provisionamento individual e revogável por dispositivo;
- chamadas SIP originadas e recebidas;
- integração com Telecom Framework e `ConnectionService`;
- recebimento por push com o aplicativo suspenso ou encerrado;
- microfone, alto-falante, Bluetooth e dispositivos de áudio;
- contatos, fotografias, histórico, gravações autorizadas, presença e DND;
- interface nativa baseada na identidade visual homologada do Eagle PBX.

## Segurança

Este repositório nunca deve conter:

- senhas ou credenciais SIP;
- tokens, cookies ou chaves privadas;
- `.env` ou configurações reais de produção;
- credenciais CardDAV, AMI, SMTP ou banco;
- certificados e chave definitiva de assinatura do APK;
- dados pessoais ou exportações de usuários.

Consulte [SECURITY.md](SECURITY.md) antes de contribuir.

## Documentação

- [CODEX.md](CODEX.md): regras permanentes do projeto;
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): arquitetura e fases;
- [LICENSE](LICENSE): GNU Affero General Public License v3.

## Compilação

O projeto Android está em [`android/`](android/) e utiliza:

- identificador `com.eaglesistemas.eaglepbx`;
- Kotlin e Jetpack Compose;
- SDK mínimo 28;
- SDK de compilação e alvo 36 (Android 16);
- Gradle Wrapper versionado no repositório.

Consulte [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) para preparar o ambiente e
executar a validação local.
