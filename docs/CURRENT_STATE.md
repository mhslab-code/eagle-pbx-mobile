# Estado atual — Eagle PBX Mobile

Atualizado em: 2026-08-04 16:40 -03

## Código e versão

- Branch: `codex/ringtone-corporativo`.
- Base confirmada antes do ajuste: `b89244b`.
- Commit da correção: `a139fd8`.
- Versão candidata: `0.1.63` (`versionCode 64`).
- Motivo: garantir o atendimento da segunda chamada enquanto o áudio da
  primeira ainda é liberado e remover a tela assim que a sessão SIP terminar.

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
- Validação física da `0.1.60`: a primeira chamada já permaneceu em
  `Conectando...` e caiu.
- Evidência dos serviços: o PBX enviou o push às 14:50:28 e encerrou o ciclo às
  14:50:43, enquanto o APK só consultou e recebeu a configuração SIP às
  14:51:03. A interface veio do FCM, mas não existiu `INVITE` atendível no S25.
- A revisão `0.1.61` restaura a configuração SIP de cache cifrado no início do
  processo. Na ausência de cache, o push dispara a consulta de configuração
  imediatamente, sem aguardar a restauração completa do aplicativo.
- A reconciliação posterior tornou-se idempotente: a mesma configuração renova
  o registro sem remover a conta que já está processando a chamada.
- Validação física monitorada da `0.1.61`: a primeira chamada recebeu `ANSWER`,
  entrou na ponte e terminou normalmente. Na segunda, o PBX manteve o endpoint
  em `Ringing` por aproximadamente nove segundos e recebeu `CANCEL`, sem nenhum
  `ANSWER`; mesmo depois do término, o S25 permaneceu em `Conectando...`.
- A captura comprova que a interface considerava um atendimento enfileirado
  como sucesso visual apesar de o motor não confirmar `accept()`. O encerramento
  da atividade também dependia de callbacks/cancelamento, sem reconciliação com
  a lista nativa.
- A revisão `0.1.62` resolve a chamada novamente em `Core.currentCall` e
  `Core.calls`, traz o motor para primeiro plano, reativa a rede e libera os
  recursos de mídia antes do aceite.
- O enfileiramento agora só é permitido enquanto existe push sem identificador
  SIP. Se o `INVITE` já existe e `accept()` falha, a tela não muda falsamente
  para `Conectando...`.
- Uma guarda da mesma geração remove a interface após três ausências
  consecutivas do objeto SIP nativo.
- Na primeira tentativa física com a `0.1.62`, o S25 Ultra não tocou porque o
  `INVITE` SIP não chegou ao aparelho: o PBX iniciou o ciclo às 16:07:19,
  encerrou a janela às 16:07:35 e o registro móvel só reapareceu às 16:07:38.
- A causa estava no canal `eagle-mobile-push`, que deixava de procurar o
  contato após 14 segundos, junto do tempo padrão de 15 segundos do ramal.
- A infraestrutura foi corrigida no commit `d774491`: o contato passa a ser
  reavaliado por até 30 segundos e ramais com dispositivo mobile mantêm pelo
  menos 45 segundos de toque. O APK permanece na versão `0.1.62`.
- Validação física após a correção do PBX: a primeira chamada recebeu `ANSWER`
  às 16:23:44 e entrou na ponte. A segunda recebeu `INVITE`, respondeu
  `Ringing`, mas não gerou `ANSWER`; o chamador cancelou às 16:24:10 e a tela
  permaneceu em `Conectando...`.
- A revisão `0.1.63` remove a preempção manual de áudio. O atendimento passa a
  seguir o cliente Android oficial do Linphone, usando parâmetros criados a
  partir do `INVITE`; se o clique ocorrer antes de o objeto estar pronto ou
  durante a liberação da chamada anterior, o comando é repetido enquanto a
  mesma chamada continua válida.
- A guarda agora considera `End`, `Error` e `Released` como estados terminais,
  mesmo que o objeto ainda permaneça temporariamente em `Core.calls`.

## Testes da 0.1.63

- `clean testDebugUnitTest assembleDebug connectedDebugAndroidTest`: aprovado.
- Gradle: 79 tarefas executadas em build limpo.
- Trinta e seis testes unitários passaram sem falhas; a nova cobertura valida
  a repetição condicionada do aceite e os três estados terminais nativos.
- Seis testes instrumentados passaram no Android 16/API 36 equivalente ao
  Galaxy A25 5G.
- O teste instrumentado remove o objeto SIP durante a apresentação e confirma
  que alerta e atividade deixam o estado órfão automaticamente.
- Pacote conferido: `versionName 0.1.63`, `versionCode 64`, `minSdk 28` e
  `targetSdk 36`.
- Assinatura: Android Debug, certificado SHA-256
  `74f558c6f85328521a419b2e32e35875640470d1b11644fae9d564fbcf8d5789`.
- APK: `Eagle-PBX-Mobile-0.1.63-debug.apk`.
- SHA-256: `b96fb1c8657ae5dc22169b7af7ba15336dc0a49cea3466997a9df3bd3c6bcb51`.
- Correção: commit `a139fd8`.
- Publicação: `https://eaglesistemas.com/pbx/download/Eagle-PBX-Mobile-0.1.63-debug.apk`.
- Portal: arquivo, hash, catálogo, contador, serviço e configuração Nginx
  validados; acesso público continua protegido por autenticação HTTP.

## Próximos passos

1. Instalar a `0.1.63` sobre a versão existente e aguardar **Online**.
2. Bloquear o S25 Ultra e ligar para o ramal 101.
3. Atender, manter por alguns segundos e encerrar a primeira chamada.
4. Iniciar a segunda imediatamente, tocar em **Atender** e confirmar a
   transição de `Conectando...` para **Chamada em andamento**.
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
- Defeito conhecido: o estabelecimento SIP e o áudio em duas chamadas
  consecutivas ainda dependem da validação física da revisão `0.1.63` no S25
  Ultra; repetição do aceite e remoção terminal possuem cobertura unitária e a
  interface órfã possui cobertura instrumentada.
- Rollback operacional: `checkpoint/mobile-0.1.54-fullscreen`.
- Ponto estratégico de infraestrutura preservado:
  `_backup_pre_restruturacao_cores`.
