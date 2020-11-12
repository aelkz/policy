# ip-rate-limit

1. Segue abaixo instruções para criação de certificado alto assinado: 

```
export SECRETS_KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)

keytool -genkeypair -keyalg RSA -keysize 2048 -dname "CN=0.0.0.0" -alias https-key -keystore keystore.jks -storepass ${SECRETS_KEYSTORE_PASSWORD}
keytool -certreq -keyalg rsa -alias https-key -keystore keystore.jks -file camel.csr -storepass ${SECRETS_KEYSTORE_PASSWORD}

echo ${SECRETS_KEYSTORE_PASSWORD}
```
2. Instruções para teste do proxy
```
curl -k -vvv http://localhost:9081/actuator/info -H 'Accept: application/json' -x "https://localhost:8443" --proxy-insecure
```