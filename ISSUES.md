1. Evitar consumir o datagrid quando não houver atualização do bucket para o remote address
2. Avaliar a utilização do block synchronized vs. estratégias de leak bucket para rate-limit
    https://www.baeldung.com/spring-bucket4j
    https://medium.com/smyte/rate-limiter-df3408325846
    https://nordicapis.com/everything-you-need-to-know-about-api-rate-limiting/
    https://camel.apache.org/components/latest/eips/throttle-eip.html
    https://stackoverflow.com/questions/1671089/why-are-synchronize-expensive-in-java
    https://developer.github.com/v3/#rate-limiting
3. Ausência de ?bridgeEndpoint=true&throwExceptionOnFailure=false no https consumer
4. JaegerTagProcesso sem anotacação @Component (avaliar necessidade)
5. concat de ips do metodo ipfilter c.r.a.p.route.external.ProxyRoute - clientIp "10.6.128.23:200.164.107.55":
