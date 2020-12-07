# 3scale custom policy
**features:** `ip-rate limit with JBoss Data Grid` , `opentracing with jaeger` , `actuator metrics` , `prometheus metrics`<br>
**tags:** `opentracing` `jaeger` `fuse77` `prometheus` `actuator` `datagrid` `java8`

## PRE-REQUISITES

## CONFIGURING 3SCALE

## CONFIGURE APICAST PRODUCTION CACHE REFRESH INTERVAL
```
oc set dc/apicast-production .... APICAST_CONFIGURATION_CACHE # 300 for default value
```

## TESTING W/ 3SCALE
```
# use production route (as jaeger will be configured only for apicast-production gtw)
curl -k -vvv "https://openbanking-production.apps.raphael.lab.upshift.rdu2.redhat.com/get?user_key=2695b1d2bae74f8006a6d91c2fc4d407" # if using key as query param
curl -k -vvv "https://sample-production.apps.raphael.lab.upshift.rdu2.redhat.com/get" -H 'user_key: 2695b1d2bae74f8006a6d91c2fc4d407' # if using key as http header
```

## TESTING LOCALLY (WITHOUT 3SCALE)
```
docker run -d --name jaeger \
  -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
  -p 5775:5775/udp \
  -p 6831:6831/udp \
  -p 6832:6832/udp \
  -p 5778:5778 \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 14250:14250 \
  -p 9411:9411 \
  jaegertracing/all-in-one:1.20
```

Then, you can then navigate to `http://localhost:16686` to access the Jaeger UI.
Then, you can start the container instance: `docker start <CONTAINER-ID>`

```
curl -vvv "http://www.postman-echo.com/get" -H 'Accept: application/json'

curl -k -vvv https://www.postman-echo.com/get -H 'Accept: application/json' -x "https://0.0.0.0:8443" --proxy-insecure
curl -vvv "http://www.postman-echo.com/get" -H 'Accept: application/json' -x "http://0.0.0.0:8080"

print "GET https://<backend url>. HTTP/1.1\nHost: <backend>\nAccept: */*\n\n" | ncat --no-shutdown --ssl <camel proxy app> <camel proxy port>
print "GET http://www.postman-echo.com/get HTTP/1.1\nHost: http://www.postman-echo.com/get\nAccept: */*\n\n" | ncat --no-shutdown 0.0.0.0 8080
```

## DATAGRID DEPLOYMENT
```
IMAGESTREAM_NS=microservices

oc create -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/7.3-v1.7/templates/datagrid73-image-stream.json -n $IMAGESTREAM_NS
oc import-image jboss-datagrid73-openshift --from='registry.redhat.io/jboss-datagrid-7/datagrid73-openshift:1.7’ -n $IMAGESTREAM_NS

oc new-app --name=datagrid-fuse-policy \
 --image-stream=jboss-datagrid73-openshift:1.7 \
 -e INFINISPAN_CONNECTORS=hotrod \
 -e CACHE_NAMES=default \
 -e HOTROD_SERVICE_NAME=policy-hotrod \
 -e AB_PROMETHEUS_ENABLE=true
```

## APPLICATION DEPLOYMENT
```
export MSA_PROJECT_NAMESPACE=microservices
oc delete all,bc,secret,is,svc,cm -lapp=proxy-policy-api -n ${MSA_PROJECT_NAMESPACE}

export NEXUS_NAMESPACE=${MSA_PROJECT_NAMESPACE} # where nexus3 is located
export PROJECT_NAMESPACE=${MSA_PROJECT_NAMESPACE}
export TEMPLATE_NAMESPACE=${MSA_PROJECT_NAMESPACE}
export APP=proxy-policy-api
export APP_GIT=https://github.com/aelkz/policy.git
export APP_GIT_BRANCH=quicklab
export CUSTOM_TEMPLATE=$(oc get template -n ${TEMPLATE_NAMESPACE} | grep s2i-fuse77-spring-boot-2-camel | awk 'NR==1 {print $1}')
export OCP_DOMAIN=apps.raphael.lab.upshift.rdu2.redhat.com

# change mirror url using your nexus openshift route

export MAVEN_URL=http://$(oc get route -lapp=nexus3 -n $MSA_PROJECT_NAMESPACE | awk 'NR==2 {print $2}')/repository/maven-public/
export MAVEN_URL_RELEASES=http://$(oc get route -lapp=nexus3 -n $MSA_PROJECT_NAMESPACE | awk 'NR==2 {print $2}')/repository/maven-releases/
export MAVEN_URL_SNAPSHOTS=http://$(oc get route -lapp=nexus3 -n $MSA_PROJECT_NAMESPACE | awk 'NR==2 {print $2}')/repository/maven-snapshots/

# using a template
# if pom.xml is at root path of git repository
oc new-app --template=${CUSTOM_TEMPLATE} \
 --name=${APP} \
 -e MAVEN_MIRROR_URL=${MAVEN_URL} \
 --build-env='MAVEN_MIRROR_URL='${MAVEN_URL} \
 --build-env='GIT_REF='${APP_GIT_BRANCH} \
 --param GIT_REPO=${APP_GIT} \
 --param APP_NAME=${APP} \
 --param GIT_REF=${APP_GIT_BRANCH} \
 --param IMAGE_STREAM_NAMESPACE=${MSA_PROJECT_NAMESPACE} \
 -n ${MSA_PROJECT_NAMESPACE} > installation_details.log ; cat installation_details.log

echo "
  apiVersion: v1
  kind: Service
  metadata:
    name: "${APP}"
    annotations:
      prometheus.io/scrape: \"true\"
      prometheus.io/port: \"9779\"
    labels:
      expose: \"true\"
      app: "${APP}"
  spec:
    ports:
      - name: proxy-http
        port: 8080
        protocol: TCP
        targetPort: 8080
      - name: proxy-https
        port: 8443
        protocol: TCP
        targetPort: 8443
      - name: http
        port: 8090
        protocol: TCP
        targetPort: 8090
      - name: metrics
        port: 8081
        protocol: TCP
        targetPort: 8081
    selector:
      app: "${APP}"
    type: ClusterIP
    sessionAffinity: \"None\"
" | oc create -f - -n ${MSA_PROJECT_NAMESPACE}

# there's no need to expose the policy service (it will be used as internal cluster communication)
```

## DEFAULT METRIC ENDPOINTS
```
http://localhost:8081/actuator/metrics
http://localhost:8081/actuator/metrics/process.uptime
http://localhost:8081/actuator/prometheus
```

## DEPLOYMENT CONFIG. PROBES (FIX FOR SPRINGBOOT1)
If using the newest version (7.7.0.fuse-770012-redhat-00003) of fuse, actuator 8081 port is not exposed as it will be expected by default fuse 7.7 template,<br>
so all probes from template needs to be updated to reflect new actuator endpoints.

```
oc rollout pause dc ${APP} -n ${MSA_PROJECT_NAMESPACE}
oc set probe dc ${APP} --remove --liveness --readiness -n $MSA_PROJECT_NAMESPACE

oc set probe dc ${APP} --readiness --failure-threshold 3 --initial-delay-seconds 10 --get-url=http://:8090/camel/health/check -n ${MSA_PROJECT_NAMESPACE}
oc set probe dc ${APP} --liveness --failure-threshold 3 --initial-delay-seconds 180 --get-url=http://:8090/camel/health/check -n ${MSA_PROJECT_NAMESPACE}
oc rollout resume dc ${APP} -n ${MSA_PROJECT_NAMESPACE}
```

Test it using the following:
```
http://localhost:8090/metrics/
http://localhost:8090/trace
http://localhost:8090/beans
http://localhost:8090/camel/health/check
```

## DEPLOYMENT CONFIG. VARIABLES SETUP
```
KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)
keytool -genkeypair -keyalg RSA -keysize 2048 -dname CN="*.$OCP_DOMAIN" -alias https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
echo ${KEYSTORE_PASSWORD}

sudo mkdir -p /deployments/data
sudo chown -R raphael: /deployments/
sudo cp keystore.jks /deployments/data/keystore.jks

oc rollout pause dc ${APP} -n ${MSA_PROJECT_NAMESPACE}
oc create configmap policy-api-keystore-config --from-file=./keystore.jks -n ${MSA_PROJECT_NAMESPACE}
oc set volume dc/${APP} --add --overwrite --name=policy-api-config-volume -m /deployments/data -t configmap --configmap-name=policy-api-keystore-config -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite FUSE_PROXY_KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD} -n ${MSA_PROJECT_NAMESPACE}

oc set env dc/${APP} --overwrite OPENSHIFT_APP_NAME=${APP} -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_HOST_NAME=${OCP_DOMAIN} -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_JAEGER_TRACE_HOST=jaeger-collector.microservices.svc.cluster.local -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_JAEGER_TRACE_PORT=14268 -n ${MSA_PROJECT_NAMESPACE}

oc set env dc/${APP} --overwrite INFINISPAN_SERVICE_NAMESPACE=microservices -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite INFINISPAN_APP_NAME=datagrid-fuse-policy -n ${MSA_PROJECT_NAMESPACE}

oc set env dc/${APP} --overwrite FUSE_PROXY_DEBUG_HEADERS=true -n ${MSA_PROJECT_NAMESPACE}

oc set env dc/${APP} --overwrite POLICY_IP_RATE_LIMIT_MAX_HIT_COUNT=10 -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite POLICY_IP_RATE_LIMIT_TIME_WINDOW=60000 -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite POLICY_IP_RATE_LIMIT_X_FORWARDED_FOR="10.6.128.23,200.164.107.55" -n ${MSA_PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite POLICY_IP_RATE_LIMIT_WHITE_LIST_IPS= -n ${MSA_PROJECT_NAMESPACE}

oc rollout resume dc ${APP} -n ${MSA_PROJECT_NAMESPACE}

curl -k -vvv "https://sample-production.apps.raphael.lab.upshift.rdu2.redhat.com/get" -H 'user_key: 38c6657cd5dee0a4a5c1dd5805dd482a'
```

### REFERENCES
