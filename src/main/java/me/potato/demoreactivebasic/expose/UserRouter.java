package me.potato.demoreactivebasic.expose;

import lombok.extern.slf4j.Slf4j;
import me.potato.demoreactivebasic.logic.L1CacheService;
import me.potato.demoreactivebasic.logic.L2CacheService;
import me.potato.demoreactivebasic.logic.aggregates.UserAggregate;
import me.potato.demoreactivebasic.store.entities.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.net.URI;

import static java.lang.String.format;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.created;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Slf4j
@Configuration
public class UserRouter {
    @Bean
    RouterFunction<ServerResponse> userRouterConfig(UserAggregate userAggregate, L1CacheService l1CacheService, L2CacheService l2CacheService) {
        return route()
                .GET("/users", request -> ok().body(userAggregate.findAll(), User.class))
                .GET("/cached/users", request -> ok().body(l2CacheService.findAll(), User.class))
                .GET("/users/{id}", request -> ok().body(userAggregate.findById(Long.parseLong(request.pathVariable("id"))), User.class))
                .GET("/cached/users/{id}", request -> ok().body(l1CacheService.findById(Long.parseLong(request.pathVariable("id"))), User.class))
                .POST("/users",
                        request -> request.bodyToMono(User.class)
                                .flatMap(userAggregate::create)
                                .flatMap(_saved -> created(URI.create(format("%s%s/%s", getBaseUrl(request), "/users", _saved.getId()))).bodyValue(_saved)))
                .build();
    }


    private String getBaseUrl(final ServerRequest request) {
        final var uri = request.uri();
        final var last = uri.toString().length() - request.path().length();
        return uri.toString().substring(0, last);
    }

}
