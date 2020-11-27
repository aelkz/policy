# ip-rate-limit

1. Segue abaixo instruções para criação de certificado alto assinado: 

```
export KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)

keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}

echo ${KEYSTORE_PASSWORD}

```
2. Instruções para gerar keystore com certificados do bnb

O arquivo .cer foi extraído a partir do navegador após acesso a endpoint do backend do openbanking
```
keytool -importcert -file "bnb.cer" -keystore bnb.jks -alias openbanking
```

3. Instruções para teste do proxy

```
curl -vvv http://www.postman-echo.com/get&red=hat -H 'Accept: application/json' -x "http://localhost:8080" 
```


## Intrução para deploy em prod

1. Para esse procedimento será necessário acessar dois bastions (Dreads e CAPGV) por SSH. As informações de acesso estão disponíveis no [logbook](https://docs.google.com/document/d/1GWaGFo0WyniRjY_OLUif_-EtvyW_906J3WMJ9UujOKw/edit?skip_itp2_check=true&pli=1#heading=h.ip8fangishh9) do projeto.

    * Apenas o bastion do servidor DREADS possui acesso ao [gitlab](https://gitlab.consulting.redhat.com/consulting-brazil/banco-do-nordeste/) da consuloria

Procedimentos executados no Dreads  
```
$ git checkout {branch}
$ git pull
$ mvn clean package
$ docker build -t {image_name}:{tag} . --no-cache
$ docker save {image_name}:{tag} > {file_name}.tar
```
Procedimentos executados no CAPGV

```
$ scp -r {usuário}@{host}:/{folder}/{file_name}.tar . 
$ docker load < {file_name}.tar
$ docker login -u T003000 -p $(oc whoami -t) https://docker-registry-default.prd.ocp.capgv.intra.bnb
$ docker tag {imageid} docker-registry-default.prd.ocp.capgv.intra.bnb/{namespace}/{image_name}:{tag}
## Se você deseja criar um novo deployment
$ oc new-app --image-stream="{namespace}/{image_name}:{tag}"

```

## Ambiente de desenvolvimento

Este projeto possui dependência do Red Hat Dat Grid.

Usar o profile dev para executar em ambiente de desenvolvimento.
```
 $ mvn spring-boot:run -Drun.profiles=dev
```
