# Estado atual — Eagle PBX Mobile

Atualizado em: 2026-08-04 10:55 -03

## Código e versão

- Branch: `codex/ringtone-corporativo`.
- Base confirmada antes do ajuste: `461e160`.
- Commit da correção: `bdf5576`.
- Versão candidata: `0.1.58` (`versionCode 59`).
- Motivo: retirar o controle SIP do ciclo de vida da `MainActivity`, integrar
  as chamadas ao Android Telecom e impedir que a atividade de bloqueio encerre
  o serviço de telefonia.

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
- A validação física da `0.1.56` mostrou `Conectando...` sem aceite SIP real;
  a chamada seguiu para a caixa postal e a atividade fechou.
- A revisão `0.1.57` só confirma `Conectando...` quando o controlador SIP aceita
  ou enfileira efetivamente o comando.
- A validação física da `0.1.57` mostrou que o modal podia aparecer somente na
  primeira chamada e que **Atender** podia não responder.
- Causa confirmada em código e log: o handler de atendimento dependia da
  `MainActivity`; FCM e SIP podiam reapresentar a mesma chamada; e
  `finishAndRemoveTask()` disparava `onTaskRemoved`, encerrando explicitamente
  o serviço SIP após fechar a tela dedicada.
- A revisão `0.1.58` mantém o controlador no processo, registra a chamada com
  Core-Telecom, consolida FCM/SIP, usa um `PendingIntent` por ciclo e não encerra
  mais o serviço ao atender ou recusar.

## Testes da 0.1.58

- `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest`: aprovado.
- Gradle: 78 tarefas concluídas; build limpo bem-sucedido.
- Teste instrumentado com Android 16/API 36 e bloqueio por PIN: aprovado.
- Cada bateria confirma atendimento na primeira chamada, três chamadas
  bloqueadas consecutivas e atualização da mesma notificação `CallStyle` para
  “Chamada em andamento”.
- Três processos novos: 12 telas cheias, 12 registros reconhecidos pelo Android
  Telecom, zero encerramentos do serviço pela tarefa, zero timeout de
  `CallStyle`, zero crash e zero ANR.
- Pacote conferido: `versionName 0.1.58`, `versionCode 59`.
- Assinatura: compatível com o APK de depuração anterior disponível na VM.
- APK: `Eagle-PBX-Mobile-0.1.58-debug.apk`.
- SHA-256: `455e1aa164e9a02187f7036a1a66e4a892b3788c145a670c2b06bba2b895b029`.
- Correção: commit `bdf5576`.
- Publicação: `https://eaglesistemas.com/pbx/download/Eagle-PBX-Mobile-0.1.58-debug.apk`.
- Portal: contador cadastrado, redirecionamento `302`, arquivo visível e Nginx
  validados.

## Próximos passos

1. Instalar a `0.1.58` sobre a versão existente, sem desinstalar.
2. Bloquear o S25 Ultra e ligar para o ramal 101.
3. Fazer três chamadas completas consecutivas, confirmando tela cheia em todas.
4. Na primeira, atender e confirmar áudio bidirecional e estado “Chamada em
   andamento”; na segunda, recusar; na terceira, deixar virar chamada perdida.
5. Somente após homologação completa, criar a tag final e avançar para Chamadas
   ativas do Painel.

## Checkpoints

- `checkpoint/mobile-0.1.54-fullscreen`: primeira revisão que apresentou a
  chamada recebida em tela cheia no S25 Ultra bloqueado, inclusive em duas
  tentativas consecutivas; a latência variável permanece em observação.

## Dependências, defeitos e rollback

- Dependências: PBX `10.20.20.140`, API/App `10.20.20.147`, ambiente Android
  `10.20.20.148` e portal de downloads `10.20.20.116`.
- Defeito conhecido: o estabelecimento SIP e o áudio após **Atender** ainda
  dependem da validação física da revisão `0.1.58` no S25 Ultra.
- Rollback operacional: `checkpoint/mobile-0.1.54-fullscreen`.
- Ponto estratégico de infraestrutura preservado:
  `_backup_pre_restruturacao_cores`.
