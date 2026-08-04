# Estado atual — Eagle PBX Mobile

Atualizado em: 2026-08-04 06:22 -03

## Código e versão

- Branch: `codex/ringtone-corporativo`.
- Base confirmada antes do ajuste: `c4df0a3`.
- Versão candidata: `0.1.53` (`versionCode 54`).
- Motivo: permitir explicitamente a abertura da tela de chamada em segundo
  plano no Android 15/16, inclusive no Galaxy S25 Ultra bloqueado.

## Homologação

- Último item homologado: fluxos anteriores à revisão física de tela bloqueada.
- Item em teste: chamada recebida com o S25 Ultra bloqueado.
- A revisão `0.1.52` tocou e vibrou, mas não apresentou a interface.
- A permissão Android de alertas em tela cheia foi confirmada como ativa pelo
  usuário e pela tela de diagnóstico do aplicativo.
- A revisão `0.1.53` aguarda instalação sobre a versão anterior e homologação
  física.

## Testes da 0.1.53

- `testDebugUnitTest`: aprovado.
- `assembleDebug`: aprovado.
- Gradle: 45 tarefas concluídas; build bem-sucedido.
- Pacote conferido: `versionName 0.1.53`, `versionCode 54`.
- Assinatura: compatível com o APK de depuração anterior disponível na VM.
- APK: `Eagle-PBX-Mobile-0.1.53-debug.apk`.
- SHA-256: `d2ae8c96fc74b4043359bac986ac15f61df158798fe4cc3c3e693ae02adb8528`.

## Próximos passos

1. Instalar a `0.1.53` sobre a versão existente, sem desinstalar.
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
  física da revisão `0.1.53` no S25 Ultra.
- Rollback lógico: branch anterior em `c4df0a3`, versão `0.1.52`.
- Ponto estratégico de infraestrutura preservado:
  `_backup_pre_restruturacao_cores`.
