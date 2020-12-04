# 3scale custom policy
**features:** `ip-rate limit with JBoss Data Grid` , `opentracing with jaeger` , `actuator metrics` , `prometheus metrics`<br>
**tags:** `opentracing` `jaeger` `fuse77` `prometheus` `actuator` `datagrid` `java8`

## PRE-REQUISITES

## CONFIGURING 3SCALE

## TESTING W/ 3SCALE

## TESTING LOCALLY (WITHOUT 3SCALE)

## OPENSHIFT DEPLOYMENT
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
 --param IMAGE_STREAM_NAMESPACE=${PROJECT_NAMESPACE} \
 -n ${PROJECT_NAMESPACE} > installation_details.log ; cat installation_details.log

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
" | oc create -f - -n ${PROJECT_NAMESPACE}

# there's no need to expose the policy service (it will be used as internal cluster communication)

KEYSTORE_PASSWORD=$(openssl rand -base64 512 | tr -dc A-Z-a-z-0-9 | head -c 25)
keytool -genkeypair -keyalg RSA -keysize 2048 -dname CN="*.$OCP_DOMAIN" -alias https-key -keystore keystore.jks -storepass ${KEYSTORE_PASSWORD}
echo ${KEYSTORE_PASSWORD}

sudo mkdir -p /deployments/data
sudo chown -R raphael: /deployments/
sudo cp keystore.jks /deployments/data/keystore.jks

oc rollout pause dc ${APP} -n ${PROJECT_NAMESPACE}
oc create configmap policy-api-keystore-config --from-file=./keystore.jks -n ${PROJECT_NAMESPACE}
oc set volume dc/${APP} --add --overwrite --name=policy-api-config-volume -m /deployments/data -t configmap --configmap-name=policy-api-keystore-config -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD} -n ${PROJECT_NAMESPACE}

oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_SCHEMA=netty4-http -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_DESTINATION_SCHEMA=netty4-http -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_PROXY_PORT=8443 -n ${PROJECT_NAMESPACE}
oc set env dc/${APP} --overwrite OPENSHIFT_JETTY_KEYSTORE_PASSWORD=77z9SYEGhSovlsBkALpko0BUb -n ${PROJECT_NAMESPACE}
oc rollout resume dc ${APP} -n ${PROJECT_NAMESPACE}

curl -k -vvv "https://sample-production.apps.raphael.lab.upshift.rdu2.redhat.com/get" -H 'user_key: 38c6657cd5dee0a4a5c1dd5805dd482a'
```

## OPENSHIFT DEPLOYMENT CONFIG. VARIABLES SETUP

### REFERENCES
