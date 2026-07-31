# CODEX — Eagle PBX Mobile

## Revisão 0.1.38

- Contatos e histórico passam a abrir imediatamente pelo cache local cifrado,
  com atualização posterior pela API autorizada.
- A fotografia do cabeçalho vem do perfil do usuário; a agenda corporativa é
  usada para identificar destinos e chamadores.
- Chamadas iniciadas pelos contatos exibem o contato correspondente no
  Discador durante todo o fluxo.
- A notificação de chamada recebida apresenta ações nativas de atender e
  recusar. Quando não há atendimento, uma notificação persistente registra a
  chamada perdida com nome, número e fotografia disponíveis.
- O ícone oficial do Eagle PBX foi aplicado ao launcher Android. Escala e
  legibilidade finais serão conferidas no Galaxy A25 físico, pois a gaveta do
  emulador aplica máscara e cache próprios.
- O comportamento funcional da chamada perdida foi homologado no emulador. O
  acabamento visual da notificação permanece sujeito a revisão futura no
  aparelho físico.
- A compilação limpa da revisão foi validada com o recurso vetorial de telefone
  utilizado nos contatos, evitando dependência de artefatos residuais do Gradle.

## Revisão 0.1.37

- Removida de Minha conta a opção de limpar ou ocultar o histórico de chamadas.
- O Android não envia mais solicitações de limpeza e sempre consulta o
  histórico corporativo autorizado pelo servidor.
- A política vale para todos os colaboradores: registros de chamadas não podem
  ser ocultados ou apagados pelo aplicativo.

## Revisão 0.1.34

- Iniciado o acabamento visual nativo com base na identidade homologada do
  PWA e do aplicativo desktop.
- O cabeçalho separa marca, presença e perfil do usuário em áreas próprias.
- O Discador mantém os controles já validados e oculta mensagens técnicas de
  sucesso, exibindo somente falhas que exigem atenção.
- Esta revisão é exclusivamente visual; autenticação, SIP, FCM, contatos,
  histórico e gravações mantêm o comportamento da revisão anterior.

## Revisão 0.1.33

- indisponibilidade de rede não apaga mais a sessão local nem retorna o usuário
  para a tela de login;
- a identidade da sessão é mantida cifrada pelo Android Keystore e a interface
  informa `Sem conexão` enquanto tenta restaurar a comunicação a cada 3 segundos;
- somente respostas explícitas `401/403` invalidam a sessão armazenada.

## Revisão 0.1.32

- o estado inicial vazio do motor SIP durante o despertar não encerra mais o
  alerta preliminar recebido por FCM;
- o encerramento pelo motor SIP ocorre somente depois de uma chamada SIP real
  ter sido observada, preservando toque e notificação durante o registro.

## Revisão 0.1.31

- o toque de chamada recebida usa reprodução assíncrona em loop e permanece
  ativo até atender, recusar, receber o cancelamento correlacionado ou expirar;
- a preparação do áudio não bloqueia a thread principal do aplicativo.

## Revisão 0.1.30

- o push de cancelamento correlacionado encerra imediatamente toque e
  notificação quando a origem abandona a chamada antes do registro SIP;
- eventos de cancelamento recebidos antes do alerta também são lembrados por
  uma janela curta, evitando toque órfão por inversão de entrega do FCM.

## Revisão 0.1.29

- o serviço FCM processa somente eventos `incoming_call` em sessão autenticada;
- o evento apresenta a notificação nativa de chamada e traz a interface ao
  primeiro plano para restaurar o motor SIP;
- o registro FCM é renovado durante o despertar e o alerta preliminar possui
  limite de 45 segundos para não permanecer ativo sem um `INVITE`;
- nome e número do chamador são limitados antes de chegar à interface;
- a homologação do processo encerrado depende da tentativa SIP retardada no
  PBX e não deve ser declarada apenas com o teste FCM.

## Revisão 0.1.28

- o Firebase Installation ID (FID) é enviado após autenticação e registro do
  dispositivo e renovado pelo serviço oficial do Firebase;
- o FID nunca é exibido na interface ou escrito em logs;
- esta revisão prepara o endereçamento do push, mas ainda não envia nem processa
  eventos de chamada recebida via FCM.

## Revisão 0.1.27

- O projeto Android passou a usar o plugin Google Services e o módulo principal
  do Firebase Cloud Messaging, controlado pelo Firebase BoM.
- Analytics e demais produtos Firebase não foram adicionados.
- A configuração real foi validada exclusivamente no ambiente Android e
  permanece ignorada pelo Git.
- Esta revisão prepara o recebimento FCM; registro do token e despertar SIP
  continuam nas etapas seguintes.

## Decisão de infraestrutura push — 2026-07-31

- O Android usará um projeto Firebase exclusivo denominado `Eagle PBX Mobile`.
- O FCM será usado sem custo somente para despertar o fluxo de chamada quando
  o processo tiver sido encerrado.
- O backend existente enviará as mensagens; não dependeremos de serviços
  computacionais pagos do Firebase.
- Configurações e credenciais reais do Firebase permanecem fora do Git.
- O planejamento anterior previa push, mas não explicitava o provedor, a
  dependência operacional nem o custo; esta decisão corrige essa lacuna.

## Revisão 0.1.26

- O retorno ao segundo plano é associado à abertura provocada pela notificação,
  inclusive em emuladores ou aparelhos sem PIN, cujo teclado não é reportado
  pelo Android como bloqueado.

## Revisão 0.1.25

- Quando uma chamada abre o aplicativo sobre a tela bloqueada, recusar ou
  encerrar devolve o Eagle PBX para trás da tela de bloqueio. A tarefa apenas
  volta ao segundo plano; o serviço SIP permanece ativo.

## Revisão 0.1.24

- Atender diretamente pela notificação também traz o Eagle PBX ao primeiro
  plano e exibe os controles da chamada ativa. Recusar continua encerrando a
  chamada sem abrir o aplicativo.

## Revisão 0.1.23

- O toque de chamada recebida permanece em repetição até atender, recusar ou a
  origem encerrar. A recusa SIP agora responde como `Declined`, evitando a
  indicação incorreta de `Forbidden` no telefone originador.

## Revisão 0.1.22

- A notificação nativa de chamada recebida oferece as ações `Atender` e
  `Recusar`, sem exigir a abertura prévia do aplicativo. O toque no corpo da
  notificação continua abrindo o modal completo da chamada.

## Revisão 0.1.21

- O serviço interno opcional do Linphone deixou de ser declarado pelo aplicativo.
  O registro SIP em segundo plano permanece sob responsabilidade do serviço
  foreground do Eagle PBX, evitando o encerramento do processo pelo Android após
  a primeira chamada recebida.

## Revisão 0.1.20

- o registro SIP ativo inicia um serviço em primeiro plano de uso especial;
- o serviço impede que o Android 16 congele o processo ao enviar a atividade
  para segundo plano;
- o Liblinphone entra explicitamente em modo de segundo plano, mantém o
  transporte SIP ativo e renova o registro ao retornar à interface;
- o serviço Android fornecido pelo SDK Liblinphone está declarado no manifesto;
- chamadas recebidas em segundo plano geram alerta nativo de alta prioridade e
  abrem a interface de atendimento;
- a notificação persistente abre o Eagle PBX e desaparece no logoff ou quando
  a tarefa é removida;
- o motor SIP permanece no `LoginViewModel` nesta fase; recuperação após morte
  do processo e chamadas com o aplicativo encerrado dependem da etapa de push.

## Escopo

Este repositório contém exclusivamente os aplicativos mobile nativos do Eagle
PBX. Não documentar aqui configurações internas do Issabel, CardDAV, Painel
administrativo ou aplicativo desktop.

## Decisões permanentes

1. Android é a primeira plataforma; iOS vem após sua homologação.
2. O identificador Android é `com.eaglesistemas.eaglepbx`.
3. O código do aplicativo é público sob GNU AGPLv3.
4. Liblinphone é candidato técnico, ainda não homologado.
5. O APK inicial será distribuído internamente, fora da Play Store.
6. Cada dispositivo terá credencial SIP própria, individual e revogável.
7. Nenhuma credencial pode ser embutida no APK ou versionada.
8. A chave de assinatura pertence exclusivamente à Eagle e permanece fora do
   Git e do ambiente de CI público.
9. Groundwire permanece como contingência até homologação prolongada.
10. Toda alteração deve ser testada, documentada e publicada em commit próprio.

## Ambiente inicial de homologação

- aparelhos operacionais: Samsung Galaxy A25 5G;
- aparelho complementar: Samsung Galaxy S25 Ultra;
- sistemas de homologação: Android 15 e Android 16;
- instalações previstas: aproximadamente seis;
- limite operacional inicial: dez ramais.

## Licenciamento

O aplicativo será compatível com as condições da AGPLv3. Caso o Liblinphone
seja adotado:

- o código correspondente da versão distribuída deve permanecer disponível;
- modificações no SDK devem ser publicadas;
- avisos de copyright e licença devem ser preservados;
- scripts e instruções de compilação devem acompanhar o código;
- módulos proprietários incompatíveis não podem ser incorporados ao mesmo
  aplicativo.

Uma licença comercial somente será reconsiderada se futuramente for necessário
fechar o código.

## Regras de implementação

- usar APIs autenticadas do Eagle PBX; não acessar AMI ou banco diretamente;
- não reutilizar credenciais do Groundwire, MicroSIP ou PWA;
- usar TLS e mídia segura;
- minimizar dados persistidos no aparelho;
- revogar a sessão e o endpoint do dispositivo quando o acesso for bloqueado;
- registrar eventos de provisionamento e revogação sem registrar segredos;
- implementar push e telefonia nativa antes de considerar o mobile homologado.

## Revisão 0.1.11

- As teclas DTMF aceitas durante uma chamada são exibidas no formato
  `DTMF: 12#`.
- O destino originalmente discado permanece inalterado.
- A sequência é limpa no início ou no encerramento de outra chamada.
- O comportamento foi homologado no emulador equivalente ao Galaxy A25 5G e
  comparado com o frontend compartilhado do PWA/Tauri.

## Revisão 0.1.12

- O botão Microfone fica habilitado somente durante uma chamada conectada.
- A interface alterna entre `Microfone` e `Mudo`, com destaque visual no estado
  silenciado, e retorna ao estado normal ao encerrar.
- O estado é aplicado à chamada ativa do Liblinphone.
- Somente a interface foi homologada no emulador. A confirmação do
  silenciamento real exige aparelho físico, pois a sessão RDP não encaminha o
  microfone ao emulador.

## Revisão 0.1.13

- O botão Áudio fica habilitado somente durante chamada conectada.
- Um modal lista as saídas de reprodução oferecidas pelo Android/Liblinphone e
  marca em verde a saída atualmente selecionada.
- Selecionar uma saída aplica o dispositivo à chamada ativa e fecha o modal
  sem encerrar a ligação.
- Listagem, seleção e continuidade da chamada foram homologadas no emulador.
  O roteamento acústico real exige aparelho físico.

## Revisão 0.1.14

- O botão Espera utiliza `pause()` na chamada ativa do Liblinphone.
- Durante a espera, a interface informa `Chamada em espera` e oferece a ação
  `Retomar`.
- A retomada utiliza `resume()` e restaura o estado conectado.
- Música de espera no ramal remoto, transições visuais e continuidade da
  chamada foram homologadas.

## Revisão 0.1.15

- O botão Transferir abre um modal com visor, teclado, backspace e confirmação
  explícita.
- A transferência direta usa `transferTo()` na chamada ativa.
- Depois que o PBX aceita a operação, o Android sai da sessão e origem e
  destino permanecem conectados.
- O fluxo completo foi homologado entre os ramais 104 e 105.

## Revisão 0.1.16

- O modal de transferência oferece as ações direta e assistida.
- A transferência assistida coloca a chamada original em espera e estabelece
  uma segunda chamada de consulta pelo Liblinphone.
- `Cancelar consulta` encerra somente o segundo destino e retoma a chamada
  original.
- `Concluir transferência` usa `transferToAnother()`, conecta os dois ramais e
  remove o Android da sessão.
- A interface preserva sua estrutura durante as transições para evitar
  travamento de recomposição.
- Cancelamento, retomada e conclusão foram homologados entre os ramais 104 e
  105.

## Revisão 0.1.17

- `Adicionar chamada` mantém o primeiro participante em espera e somente
  oferece a conferência após o segundo destino atender.
- As consultas aos ramais 101–105 usam a rota interna sem caixa postal; quando
  não há atendimento, a chamada original é retomada e o modal fecha após uma
  confirmação visual breve.
- A formação da conferência é executada fora da thread visual, evitando ANR
  durante a persistência interna do Liblinphone.
- Cliques repetidos são bloqueados assim que a formação começa.
- A conferência 101 + 104 + 105 e o encerramento simultâneo dos dois
  participantes pelo Android foram homologados.
- A transferência assistida sem atendimento retoma a chamada original, mostra
  uma confirmação visual neutra e fecha o modal automaticamente.
- O estado de falha anterior não reaparece quando o modal de transferência é
  aberto novamente.
- A atualização manual do histórico propaga a recarga forçada até a API e
  desabilita cache HTTP para a requisição.

## Revisão 0.1.35

- O Discador Android deve seguir a composição homologada do Eagle PBX desktop,
  preservando a adaptação nativa para a tela do aparelho.
- A marca usa o ativo oficial do projeto; não usar emojis ou representações
  genéricas para ações telefônicas.
- A fotografia do usuário é resolvida pela agenda autorizada com base no ramal,
  com iniciais apenas como fallback.
- Cabeçalho, presença, visor, teclado, ações e navegação inferior devem manter
  a mesma hierarquia visual do PWA/Tauri.
- A revisão é exclusivamente visual e não altera regras SIP, chamadas ou push.

## Revisão 0.1.36

- O cabeçalho autenticado possui um seletor de aparência à esquerda do menu da
  conta, seguindo a mesma posição e comportamento do PWA/Tauri.
- A alternância é cíclica: `Claro` → `Escuro` → `Sistema` → `Claro`, indicada
  respectivamente pelos ícones de sol, lua e monitor.
- A preferência é persistida localmente no aparelho. `Sistema` acompanha as
  mudanças de aparência do Android sem exigir novo login.
- Cabeçalho e navegação inferior permanecem escuros nos três modos; superfícies,
  textos, bordas, abas e modais usam a paleta correspondente.
- Login, restauração de sessão, regras SIP, push e chamadas não foram alterados.
