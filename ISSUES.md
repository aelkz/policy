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
6. Após o período de refresh, o cliente sempre recebe o erro 429 na primeira requisição. As demais são processadas normalmente
7. O cliente não pode ficar chamando sempre quando houver 429, pois o cache nunca será esvaziado. O cliente precisa interromper o consumo pelo período estipulado da janela

8. max-idle da entry do cache
