# Eagle PBX Mobile

Aplicativo mobile nativo de telefonia corporativa da Eagle Sistemas.

## Estado

Projeto iniciado em 30 de julho de 2026. A primeira plataforma será Android,
com homologação inicial no Samsung Galaxy S25 Ultra com Android 16. O iOS será
desenvolvido depois da homologação do fluxo Android.

## Decisões aprovadas

- identificador Android: `com.eaglesistemas.eaglepbx`;
- distribuição inicial: APK assinado e instalado internamente;
- SDK SIP candidato: Liblinphone;
- licença do aplicativo: GNU AGPLv3;
- aproximadamente seis instalações Android e limite operacional inicial de dez
  ramais;
- Groundwire preservado como contingência durante a homologação;
- código-fonte público, sem credenciais ou configurações internas.

O Liblinphone ainda depende de protótipo técnico. Sua adoção definitiva somente
ocorrerá depois da validação de chamadas, push, segundo plano, Bluetooth e
compatibilidade com Android 16.

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

O projeto Android ainda não foi inicializado. A versão do Android Gradle
Plugin, Kotlin, SDK mínimo e SDK alvo serão fixadas na etapa de bootstrap,
depois da auditoria das versões estáveis e compatíveis com Android 16.
