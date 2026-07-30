# Segurança

## Relato de vulnerabilidades

Não publique vulnerabilidades que revelem infraestrutura, credenciais ou dados
da Eagle em issues públicas. Comunique-as diretamente à equipe responsável
pela infraestrutura corporativa.

## Segredos proibidos

Nunca faça commit de:

- credenciais SIP ou tokens de provisionamento;
- senhas, cookies, chaves privadas ou arquivos `.env`;
- `google-services.json` real;
- certificados APNs ou arquivos de assinatura Android;
- keystore, alias ou senha de assinatura;
- endereços internos acompanhados de credenciais;
- dados CardDAV, AMI, SMTP ou banco.

## Assinatura Android

A versão de desenvolvimento usa somente a assinatura de depuração gerada pelo
ambiente local. A keystore definitiva:

- será criada sob custódia da Eagle;
- ficará fora do Git;
- não será enviada a workflows públicos;
- terá cópia de segurança controlada;
- não será reutilizada para outros produtos.

## Provisionamento

O aplicativo recebe somente credenciais temporárias ou específicas do próprio
dispositivo por canal autenticado. Bloqueio ou revogação deve invalidar a
sessão do App e o endpoint correspondente sem afetar telefones físicos,
Groundwire ou outros dispositivos do ramal.

## Sessão Android

- a senha do usuário é transmitida somente por HTTPS e nunca é persistida;
- o cookie de sessão é cifrado com AES-GCM por chave não exportável do Android
  Keystore;
- backups do aplicativo estão desativados;
- tráfego HTTP sem TLS está bloqueado no manifesto;
- logoff remoto e respostas de bloqueio ou sessão inválida removem a cópia
  local;
- cookies, senhas e tokens não podem ser escritos em logs.
