package com.cropdeal.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Order(-1)
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Map.of(
                    "status", status.value(),
                    "error", status.getReasonPhrase(),
                    "message", message,
                    "path", exchange.getRequest().getPath().value()
            ));
        } catch (JsonProcessingException e) {
            bytes = ("{\"error\":\"Internal error\"}").getBytes();
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value()) != null
                    ? HttpStatus.resolve(rse.getStatusCode().value())
                    : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (ex.getMessage() != null && ex.getMessage().contains("Connection refused")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED -> "Authentication required";
            case FORBIDDEN -> "Access denied";
            case NOT_FOUND -> "Resource not found";
            case SERVICE_UNAVAILABLE -> "Service temporarily unavailable";
            default -> ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        };
    }
}
