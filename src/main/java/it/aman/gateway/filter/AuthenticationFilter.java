package it.aman.gateway.filter;

import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import brave.Tracer;
import it.aman.gateway.util.ResponseBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

//https://medium.com/javarevisited/spring-boot-authorization-creating-an-authorization-server-for-your-microservices-50a3aefd6ce8

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final WebClient.Builder webClientBuilder;

    private final ObjectMapper objectMapper;

    private static final String TX_HEADER = "X-Transaction-Id";

    @Resource
    private Tracer tracer;

    List<String> excludedUrls = List.of("/login"); // fetch this from property file
    
    @Value("${token-validation-url:lb://account-service/api/v1/validateToken}")
    private String tokenValidationUrl;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        // add spanId as a transactionCode on each response
        enhanceResponseWithTransactionId(exchange);
        
        if (isSecured.test(request)) {
            return webClientBuilder.build().get().uri(tokenValidationUrl)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .retrieve().bodyToMono(ResponseBase.class)
                    .map(response ->  exchange)
                    .flatMap(chain::filter).onErrorResume(error -> {
                        log.info("Error validating authorization token.");
                        HttpStatus httpStatus = HttpStatus.BAD_GATEWAY;
                        String errorMsg = HttpStatus.BAD_GATEWAY.getReasonPhrase();
                        if (error instanceof WebClientResponseException) {
                            WebClientResponseException webCLientException = (WebClientResponseException) error;
                            httpStatus = webCLientException.getStatusCode();
                            errorMsg = webCLientException.getStatusText();
                        }
                        return onError(exchange, httpStatus, errorMsg, "Authentication Failed");
                    });
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus, String err, String errDetails) {
        DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        try {
            response.getHeaders().add("Content-Type", "application/json");
            ResponseBase data = new ResponseBase()
                                    .success(Boolean.FALSE)
                                    .addErrorsItem(err)
                                    .resultCode(httpStatus.value())
                                    .message(errDetails);
            byte[] byteData = objectMapper.writeValueAsBytes(data);
            return response.writeWith(Mono.just(byteData).map(dataBufferFactory::wrap));

        } catch (JsonProcessingException e) {
            log.error("Error parsing: {}", e);
        }
        return response.setComplete();
    }

    private final Predicate<ServerHttpRequest> isSecured = request -> excludedUrls.stream().noneMatch(uri -> request.getURI().getPath().contains(uri));

    // would have been better on a separate class. But, ordering might be a bit annoying, so this ended up here.
    private void enhanceResponseWithTransactionId(ServerWebExchange exchange) {
        brave.Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceIdString() : "no_spanId_found";
        log.info("traceId: {}", traceId);
        exchange.getResponse().getHeaders().set(TX_HEADER, traceId);
    }
}
