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
- motor SIP Liblinphone, condicionado ao protótipo;
- integração Telecom Framework/`ConnectionService`;
- serviço de push FCM;
- gerenciamento de áudio e Bluetooth;
- cache local limitado de contatos e histórico.

## Identidade por dispositivo

Cada instalação deve possuir:

- identificador revogável;
- endpoint SIP próprio;
- credencial diferente dos demais aparelhos;
- token de push associado;
- registro de criação, uso e revogação.

Um ramal pode possuir múltiplos dispositivos, mas uma credencial não deve ser
compartilhada entre eles.

## Fases

### 1. Fundação

- repositório público e AGPLv3;
- projeto Android;
- identidade visual;
- proteção contra segredos;
- pipeline de validação sem chave de produção.

### 2. API

- login;
- perfil e DND;
- contatos e fotografias;
- histórico e gravações autorizadas;
- cadastro e revogação do dispositivo.

### 3. Telefonia em primeiro plano

- protótipo Liblinphone;
- registro SIP;
- chamadas internas e externas;
- áudio bidirecional;
- DTMF;
- rotas de áudio e Bluetooth.

### 4. Telefonia em segundo plano

- FCM;
- proxy SIP/push;
- Telecom Framework e `ConnectionService`;
- chamada recebida com aplicativo suspenso e encerrado.

### 5. Homologação

- Samsung Galaxy S25 Ultra com Android 16;
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
