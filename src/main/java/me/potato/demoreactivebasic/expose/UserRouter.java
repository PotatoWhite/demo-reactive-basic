package me.potato.demoreactivebasic.expose;

import lombok.extern.slf4j.Slf4j;
import me.potato.demoreactivebasic.store.entities.User;
import me.potato.demoreactivebasic.store.repositories.UserRepository;
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
    RouterFunction<ServerResponse> userRouterConfig(UserRepository userRepository) {
        return route()
                .GET("/users", request -> ok().body(userRepository.findAll(), User.class))
                .GET("/users/{id}", request -> ok().body(userRepository.findById(Long.parseLong(request.pathVariable("id"))), User.class))
                .POST("/users",
                        request -> request.bodyToMono(User.class)
                                .flatMap(userRepository::save)
                                .flatMap(_saved -> created(URI.create(format("%s%s/%s", getBaseUrl(request), "/users", _saved.getId()))).bodyValue(_saved)))
                .build();
    }


    private String getBaseUrl(final ServerRequest request) {
        final var uri = request.uri();
        final var last = uri.toString().length() - request.path().length();
        return uri.toString().substring(0, last);
    }

}
