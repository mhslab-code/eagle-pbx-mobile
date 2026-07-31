# Eagle PBX Mobile

Aplicativo mobile nativo de telefonia corporativa da Eagle Sistemas.

## Estado

Projeto iniciado em 30 de julho de 2026. A primeira plataforma será Android,
com homologação operacional nos Samsung Galaxy A25 5G e validação complementar
no Samsung Galaxy S25 Ultra. O iOS será desenvolvido depois da homologação do
fluxo Android.

Versão de desenvolvimento atual: `0.1.27`. A identidade visual, o login nativo,
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
transporte SIP ativo e renova o registro ao retornar. Push e recuperação após
encerramento do processo permanecem nas fases seguintes.
Em chamadas recebidas, o serviço nativo do Liblinphone mantém o ciclo da
chamada e uma notificação de alta prioridade alerta e abre a interface de
atendimento quando o aplicativo está em segundo plano.
Ao encerrar uma chamada que despertou a tela, o aplicativo retorna ao segundo
plano sem interromper o serviço SIP. Em aparelho sem bloqueio seguro, como o
emulador atual, a tela permanece desbloqueada; o retorno ao PIN ou à biometria
será confirmado no Galaxy A25 físico.

## Decisões aprovadas

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

A revisão `0.1.27` integra ao APK o plugin Google Services e o módulo principal
do Firebase Cloud Messaging, sem Analytics e sem outros produtos Firebase. O
arquivo de configuração real foi validado apenas na VM Android e continua
ignorado pelo Git. Registro do token no backend e despertar da chamada serão
implementados nas próximas etapas.

O protótipo utiliza Liblinphone `5.5.13`. Sua adoção definitiva somente
ocorrerá depois da validação de registro, chamadas, push, segundo plano,
Bluetooth e compatibilidade em aparelho físico com Android 16.

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
