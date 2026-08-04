# Estado atual — Eagle PBX Mobile

Atualizado em: 2026-08-04 09:15 -03

## Código e versão

- Branch: `codex/ringtone-corporativo`.
- Base confirmada antes do ajuste: `c4df0a3`.
- Commit da correção: `7fd6335`.
- Versão candidata: `0.1.56` (`versionCode 57`).
- Motivo: permitir explicitamente a abertura da tela de chamada em segundo
  plano no Android 15/16, inclusive no Galaxy S25 Ultra bloqueado.

## Homologação

- Último item homologado: fluxos anteriores à revisão física de tela bloqueada.
- Item em teste: chamada recebida com o S25 Ultra bloqueado.
- A revisão `0.1.52` tocou e vibrou, mas não apresentou a interface.
- A permissão Android de alertas em tela cheia foi confirmada como ativa pelo
  usuário e pela tela de diagnóstico do aplicativo.
- A revisão `0.1.53` foi rejeitada: o processo encerrou ao iniciar o alerta e a
  vibração ocorreu somente uma vez.
- A revisão `0.1.54` corrige essa regressão e aguarda homologação física.
- Resultado parcial da `0.1.54` no Galaxy S25 Ultra: a chamada abriu em tela
  cheia imediatamente no primeiro teste e abriu novamente no segundo teste,
  com atraso de alguns segundos.
- Estado: checkpoint funcional aprovado; homologação final ainda pendente dos
  fluxos de atendimento, recusa, término e chamada perdida.
- Defeito encontrado após o checkpoint: ao atender pela tela cheia, a
  `MainActivity` reapresentava o modal com **Recusar** e **Atender**.
- A revisão `0.1.55` corrige essa transição e aguarda homologação física.
- A validação física mostrou que a `0.1.55` ainda abria a atividade principal
  em estado `INCOMING` antes da confirmação SIP, exibindo “Chamada recebida”.
- A revisão `0.1.56` mantém a tela dedicada até `CONNECTED` e só então apresenta
  a interface principal como “Chamada em andamento”.

## Testes da 0.1.56

- `testDebugUnitTest`: aprovado.
- `assembleDebug`: aprovado.
- Gradle: 45 tarefas concluídas; build bem-sucedido.
- Pacote conferido: `versionName 0.1.56`, `versionCode 57`.
- Assinatura: compatível com o APK de depuração anterior disponível na VM.
- APK: `Eagle-PBX-Mobile-0.1.56-debug.apk`.
- SHA-256: `4bc52574d5a2fd8ba3913a42c6128772ea012fd2f833bcc1df3f22ac1538a1e3`.
- Correção: commit `0e52542`.
- Publicação: `https://eaglesistemas.com/pbx/download/Eagle-PBX-Mobile-0.1.56-debug.apk`.
- Portal: contador cadastrado, redirecionamento `302`, arquivo visível e Nginx
  validados.

## Próximos passos

1. Instalar a `0.1.56` sobre a versão existente, sem desinstalar.
2. Bloquear o S25 Ultra e ligar para o ramal 101.
3. Confirmar que a tela acende e mostra a atividade personalizada sem
   duplicação.
4. Validar atendimento, recusa e chamada perdida.
5. Somente após homologação completa, criar a tag final e avançar para Chamadas
   ativas do Painel.

## Checkpoints

- `checkpoint/mobile-0.1.54-fullscreen`: primeira revisão que apresentou a
  chamada recebida em tela cheia no S25 Ultra bloqueado, inclusive em duas
  tentativas consecutivas; a latência variável permanece em observação.

## Dependências, defeitos e rollback

- Dependências: PBX `10.20.20.140`, API/App `10.20.20.147`, ambiente Android
  `10.20.20.148` e portal de downloads `10.20.20.116`.
- Defeito conhecido: a transição direta da tela cheia para a chamada ativa
  ainda depende da validação física da revisão `0.1.56` no S25 Ultra.
- Rollback lógico: branch anterior em `c4df0a3`, versão `0.1.52`.
- Ponto estratégico de infraestrutura preservado:
  `_backup_pre_restruturacao_cores`.
