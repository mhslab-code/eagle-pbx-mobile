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
