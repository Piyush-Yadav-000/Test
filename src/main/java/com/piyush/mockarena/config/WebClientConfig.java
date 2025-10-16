package com.piyush.mockarena.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // ✅ Enhanced HttpClient with connection pooling and timeouts
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 10s connect timeout
                .responseTimeout(Duration.ofSeconds(30)) // 30s response timeout
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)))
                .keepAlive(true)
                .compress(true);

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "MockArena/1.0")
                .filter(retryFilter()) // ✅ Add retry mechanism
                .filter(loggingFilter()) // ✅ Add request/response logging
                .build();
    }

    // ✅ Retry filter for connection issues
    private ExchangeFilterFunction retryFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest ->
                Mono.just(clientRequest)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(5))
                                .jitter(0.5)
                                .filter(throwable -> {
                                    // Retry on connection issues
                                    return throwable instanceof java.net.SocketException ||
                                            throwable instanceof java.net.ConnectException ||
                                            throwable instanceof java.util.concurrent.TimeoutException ||
                                            throwable.getMessage().contains("Connection reset") ||
                                            throwable.getMessage().contains("Connection refused");
                                }))
        );
    }

    // ✅ Logging filter for debugging
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            System.out.println("🔄 Request: " + clientRequest.method() + " " + clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
