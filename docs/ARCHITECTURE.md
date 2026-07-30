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

- FCM;
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
