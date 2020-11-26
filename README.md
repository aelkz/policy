# ip-rate-limit

1. Segue abaixo instruções para criação de certificado alto assinado: 

```
export SECRETS_KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)

keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias https-key -keystore keystore.jks -storepass ${SECRETS_KEYSTORE_PASSWORD

echo ${SECRETS_KEYSTORE_PASSWORD}
```
2. Instruções para teste do proxy
```
curl -vvv https://www.postman-echo.com/get?teste=fuse -H 'Accept: application/json' -x "http://localhost:8443"
```


keytool -import -trustcacerts -alias cax -file ca.crt -keystore truststore.jks -storepass password -noprompt  