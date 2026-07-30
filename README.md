# Eagle PBX Mobile

Aplicativo mobile nativo de telefonia corporativa da Eagle Sistemas.

## Estado

Projeto iniciado em 30 de julho de 2026. A primeira plataforma será Android,
com homologação operacional nos Samsung Galaxy A25 5G e validação complementar
no Samsung Galaxy S25 Ultra. O iOS será desenvolvido depois da homologação do
fluxo Android.

Versão de desenvolvimento atual: `0.1.6`. A identidade visual, o login nativo,
a restauração segura da sessão, o logoff, a navegação principal e a integração
inicial de presença foram validados no emulador equivalente ao Galaxy A25 5G.
A agenda corporativa, o histórico e o player autenticado de gravações também
estão integrados. O motor SIP Liblinphone inicializa no aplicativo e cada
instalação registra uma identidade individual na API. O endpoint e a credencial
SIP ainda não são provisionados.

## Decisões aprovadas

- identificador Android: `com.eaglesistemas.eaglepbx`;
- distribuição inicial: APK assinado e instalado internamente;
- SDK SIP candidato: Liblinphone;
- licença do aplicativo: GNU AGPLv3;
- aproximadamente seis instalações Android e limite operacional inicial de dez
  ramais;
- Groundwire preservado como contingência durante a homologação;
- código-fonte público, sem credenciais ou configurações internas.

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
