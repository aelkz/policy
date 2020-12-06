FROM openjdk:8
EXPOSE 8088
EXPOSE 8443
ARG JAR_FILE=target/*.jar
COPY keystore.jks keystore.jks
COPY ${JAR_FILE} fuse-proxy.jar

USER 0
COPY ../../src/main/resources/keystore/certs /tmp
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias client -keystore  /usr/local/openjdk-8/jre/lib/security/cacerts -file /tmp/client01.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias client -keystore  keystore.jks -file /tmp/client02.cer
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias client -keystore keystore.jks -file /tmp/client01.pem
RUN /usr/local/openjdk-8/bin/keytool -import -storepass changeit -noprompt -trustcacerts -alias client -keystore keystore.jks -file /tmp/client03.cer
USER 1001

ENTRYPOINT ["java","-jar","/fuse-proxy.jar"]
