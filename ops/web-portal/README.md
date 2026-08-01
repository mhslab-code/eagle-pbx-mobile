# Portal de distribuição do Eagle PBX Mobile

## Endereço

```text
https://eaglesistemas.com/pbx/
```

Toda a árvore `/pbx/` é protegida por autenticação HTTP Basic sobre HTTPS. O
usuário inicial é `eagle`. Nenhuma senha ou hash real é versionado.

## Servidor

- host interno: `10.20.20.116`;
- container: `web-files-server` (`nginx:alpine`);
- projeto Docker: `/home/dsmiranda/web-projects`;
- raiz pública: `/home/dsmiranda/web-projects/html/eaglesistemas/pbx`;
- arquivo de hashes: `/home/dsmiranda/web-projects/auth/eagle-pbx.htpasswd`.

O arquivo de hashes pode ser legível pelo processo do Nginx, mas contém apenas
o hash APR1. A senha em texto puro não deve permanecer nesse diretório nem ser
registrada em comandos, logs ou commits.

## Arquivos versionados

- `index.html`: página de download para colaboradores;
- `vhost.conf`: configuração completa dos hosts estáticos, incluindo `/pbx/`;
- `docker-compose.yml`: serviço e volume de autenticação.

O logotipo e o APK são copiados dos artefatos do projeto durante a publicação.
Atualize juntos a versão, o nome, o tamanho e o SHA-256 mostrados na página.
Versões anteriores homologadas podem permanecer no catálogo para contingência,
desde que cada APK tenha nome inequívoco e integridade validada separadamente.

## Validação obrigatória

Depois da publicação, confirme:

1. `https://eaglesistemas.com/` continua respondendo `200`;
2. `/pbx/` e o APK respondem `401` sem credenciais;
3. `/pbx/` e o APK respondem `200` com credenciais válidas;
4. o SHA-256 do arquivo baixado por HTTPS é idêntico ao artefato homologado;
5. `docker exec web-files-server nginx -t` não apresenta erros.

## Troca de senha

Gere a nova senha fora do histórico do shell e atualize o arquivo com
`openssl passwd -apr1`. Entregue a senha por canal seguro e descarte qualquer
arquivo temporário em texto puro depois da confirmação. Recarregue o Nginx
com:

```bash
sudo docker exec web-files-server nginx -s reload
```

Hashes não são reversíveis. Se uma senha for perdida, crie outra; não tente
consultá-la no arquivo `eagle-pbx.htpasswd`.
