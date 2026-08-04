# Estado atual — Eagle PBX Mobile

Atualizado em: 2026-08-04 14:33 -03

## Código e versão

- Branch: `codex/ringtone-corporativo`.
- Base confirmada antes do ajuste: `2bfd8a7`.
- Commit da correção: `aed0a75`.
- Versão candidata: `0.1.60` (`versionCode 61`).
- Motivo: manter uma identidade imutável durante todo o ciclo nativo da segunda
  chamada, restaurar o registro pelo caminho oficial de push do Liblinphone e
  impedir aceite contra uma referência já encerrada.

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
- Validação física da `0.1.58`: a primeira chamada foi atendida corretamente;
  na segunda, **Atender** chegou ao aplicativo e exibiu `Conectando...`, mas o
  estabelecimento SIP não prosseguiu.
- Causa encontrada: Liblinphone possui estados separados para o término e para
  a liberação do objeto. O aplicativo tratava `End` e `Released` como eventos
  globais;
  um evento tardio da primeira podia zerar a referência da segunda. A mesma
  janela fazia o Android Telecom descartar a nova sessão enquanto a anterior
  ainda estava sendo desmontada.
- A revisão `0.1.59` identifica cada chamada pelo `Call-ID` SIP, processa o
  encerramento uma única vez, preserva chamadas de outro identificador e mantém
  a segunda sessão Telecom em fila.
- Validação física da `0.1.59`: a primeira chamada foi atendida e encerrada
  corretamente; na segunda, **Atender** permaneceu em `Conectando...` e o modal
  não fechou nem depois do chamador encerrar.
- Diagnóstico confirmado no código: o `Call-ID` pode estar vazio em
  `IncomingReceived` e surgir antes de `End`/`Released`. A chave do evento
  terminal deixava então de coincidir com a chave que abriu a interface.
- A revisão `0.1.60` usa o `nativePointer` estável como proprietário do ciclo,
  conserva uma correlação externa separada, valida o estado antes de `accept()`
  e torna idempotente a confirmação `Connected`/`StreamsRunning`.
- O FCM passa a chamar `Core.processPushNotification(null)`. O valor enviado
  pelo PBX é o identificador Asterisk usado para cancelamento, e não pode ser
  entregue ao Liblinphone como se fosse o `Call-ID` do `INVITE`.

## Testes da 0.1.60

- `clean testDebugUnitTest assembleDebug`: aprovado, 46 tarefas.
- Testes unitários cobrem a mudança tardia do `Call-ID`, preservação da
  correlação e propriedade independente de chamadas consecutivas.
- `connectedDebugAndroidTest`: aprovado, 73 tarefas.
- Quatro testes instrumentados passaram no Android 16/API 36 equivalente ao
  Galaxy A25 5G, incluindo liberação atrasada da primeira durante a segunda.
- Pacote conferido: `versionName 0.1.60`, `versionCode 61`, `minSdk 28` e
  `targetSdk 36`.
- Assinatura: Android Debug, certificado SHA-256
  `74f558c6f85328521a419b2e32e35875640470d1b11644fae9d564fbcf8d5789`.
- APK: `Eagle-PBX-Mobile-0.1.60-debug.apk`.
- SHA-256: `73a651627febabafd483241b9b8936b6ff309df05060edca86df107d52ed943a`.
- Correção: commit `aed0a75`.
- Publicação: `https://eaglesistemas.com/pbx/download/Eagle-PBX-Mobile-0.1.60-debug.apk`.
- Portal: arquivo, hash, catálogo, contador, serviço e configuração Nginx
  validados; acesso público continua protegido por autenticação HTTP.

## Próximos passos

1. Instalar a `0.1.60` sobre a versão existente, sem desinstalar.
2. Bloquear o S25 Ultra e ligar para o ramal 101.
3. Atender e encerrar a primeira chamada e iniciar a segunda imediatamente.
4. Na segunda, tocar em **Atender** e confirmar a transição direta de
   `Conectando...` para **Chamada em andamento**.
5. Encerrar a segunda chamada pelo chamador e confirmar que a interface fecha.
6. Somente após homologação completa, criar a tag final e avançar para Chamadas
   ativas do Painel.

## Checkpoints

- `checkpoint/mobile-0.1.54-fullscreen`: primeira revisão que apresentou a
  chamada recebida em tela cheia no S25 Ultra bloqueado, inclusive em duas
  tentativas consecutivas; a latência variável permanece em observação.

## Dependências, defeitos e rollback

- Dependências: PBX `10.20.20.140`, API/App `10.20.20.147`, ambiente Android
  `10.20.20.148` e portal de downloads `10.20.20.116`.
- Defeito conhecido: o estabelecimento SIP e o áudio na segunda chamada
  consecutiva ainda dependem da validação física da revisão `0.1.60` no S25
  Ultra.
- Rollback operacional: `checkpoint/mobile-0.1.54-fullscreen`.
- Ponto estratégico de infraestrutura preservado:
  `_backup_pre_restruturacao_cores`.
