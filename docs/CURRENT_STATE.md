# Estado atual — Eagle PBX Mobile

Atualizado em: 2026-08-04 06:41 -03

## Código e versão

- Branch: `codex/ringtone-corporativo`.
- Base confirmada antes do ajuste: `c4df0a3`.
- Commit da correção: `7fd6335`.
- Versão candidata: `0.1.54` (`versionCode 55`).
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

## Testes da 0.1.54

- `testDebugUnitTest`: aprovado.
- `assembleDebug`: aprovado.
- Gradle: 45 tarefas concluídas; build bem-sucedido.
- Pacote conferido: `versionName 0.1.54`, `versionCode 55`.
- Assinatura: compatível com o APK de depuração anterior disponível na VM.
- APK: `Eagle-PBX-Mobile-0.1.54-debug.apk`.
- SHA-256: `97fad47ed2eb51818e3e529e8903e8016f8f96a29d026a5f0ffd1cedddfe1e4c`.
- Correção: commit `873ad9b`.
- Publicação: `https://eaglesistemas.com/pbx/download/Eagle-PBX-Mobile-0.1.54-debug.apk`.
- Portal: contador cadastrado, redirecionamento `302`, arquivo visível e Nginx
  validados.

## Próximos passos

1. Instalar a `0.1.54` sobre a versão existente, sem desinstalar.
2. Bloquear o S25 Ultra e ligar para o ramal 101.
3. Confirmar que a tela acende e mostra a atividade personalizada sem
   duplicação.
4. Validar atendimento, recusa e chamada perdida.
5. Somente após homologação, criar tag/checkpoint e avançar para Chamadas
   ativas do Painel.

## Dependências, defeitos e rollback

- Dependências: PBX `10.20.20.140`, API/App `10.20.20.147`, ambiente Android
  `10.20.20.148` e portal de downloads `10.20.20.116`.
- Defeito conhecido: a exibição real da tela cheia ainda depende da validação
  física da revisão `0.1.54` no S25 Ultra.
- Rollback lógico: branch anterior em `c4df0a3`, versão `0.1.52`.
- Ponto estratégico de infraestrutura preservado:
  `_backup_pre_restruturacao_cores`.
