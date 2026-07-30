# CODEX — Eagle PBX Mobile

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
