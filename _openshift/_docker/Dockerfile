FROM openjdk:8
EXPOSE 8088
EXPOSE 8443
ARG JAR_FILE=target/*.jar
COPY keystore.jks keystore.jks
COPY ${JAR_FILE} fuse-proxy.jar

USER 0
COPY ../../src/main/resources/keystore/certs /tmp
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust -keystore  /usr/local/openjdk-8/jre/lib/security/cacerts -file /tmp/BNB_Aut_Cert_Intermediaria.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust1 -keystore /usr/local/openjdk-8/jre/lib/security/cacerts -file /tmp/BNB_Aut_Cert_Raiz.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust2 -keystore /usr/local/openjdk-8/jre/lib/security/cacerts -file /tmp/CertificadoFirewall.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust3 -keystore /usr/local/openjdk-8/jre/lib/security/cacerts -file /tmp/katello-server-ca.pem
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust4 -keystore /usr/local/openjdk-8/jre/lib/security/cacerts -file /tmp/S1FCWP01.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust -keystore  keystore.jks -file /tmp/BNB_Aut_Cert_Intermediaria.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust1 -keystore keystore.jks -file /tmp/BNB_Aut_Cert_Raiz.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust2 -keystore keystore.jks -file /tmp/CertificadoFirewall.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust3 -keystore keystore.jks -file /tmp/katello-server-ca.pem
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust4 -keystore keystore.jks -file /tmp/S1FCWP01.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias bnbtrust5 -keystore keystore.jks -file /tmp/S1FCWP02.cer

USER 1001

ENTRYPOINT ["java","-jar","/fuse-proxy.jar"]
