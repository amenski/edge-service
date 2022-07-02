package it.aman.gateway.filter;

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
import it.aman.common.util.ERPConstants;
import it.aman.common.util.StringUtils;
import it.aman.gateway.config.ApplicationProperties;
import it.aman.gateway.util.ResponseBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Authentication filter + enhance response with transactionId
 * 
 * @author Aman
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final Tracer tracer;

    private final ObjectMapper objectMapper;

    private final WebClient.Builder webClientBuilder;

    private final ApplicationProperties applicationProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String subject = request.getHeaders().getFirst(ERPConstants.X_REQUESTED_URL_SUBJECT);
        if(StringUtils.isBlank(subject)) {
            subject = ERPConstants.ANONYMOUS_USER;
        }
        // add spanId as a transactionCode on each response
        enhanceResponseWithTransactionId(exchange);
        
        if (isSecured(request)) {
            return webClientBuilder.build().get().uri(applicationProperties.getTokenValidationUrl())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken)
                    .header(ERPConstants.X_REQUESTED_URL_SUBJECT, subject)
                    .header(ERPConstants.X_REQUESTED_URL_HTTP_METHOD, exchange.getRequest().getMethodValue())
                    .header(ERPConstants.X_REQUESTED_URL, exchange.getRequest().getURI().getPath())
                    .retrieve()
                    .bodyToMono(ResponseBase.class)
                    .map(response -> exchange)
                    .flatMap(chain::filter).onErrorResume(error -> {
                        log.error("Error validating authorization token: {}", error);
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
        return -1;
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

    private boolean isSecured(final ServerHttpRequest request) {
        return applicationProperties.getExcludedUrls().stream().noneMatch(request.getURI().getPath()::equals);
    }
    
    // would have been better on a separate class. But, ordering is a bit annoying, so this ended up here.
    private void enhanceResponseWithTransactionId(ServerWebExchange exchange) {
        brave.Span span = tracer.currentSpan();
        String traceId = span != null ? span.context().traceIdString() : "no_spanId_found";
        exchange.getResponse().getHeaders().set(ERPConstants.TRANSACTION_ID_KEY, traceId);
    }
}
