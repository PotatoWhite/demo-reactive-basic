package me.potato.demoreactivebasic.logic.aggregates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.potato.demoreactivebasic.store.entities.User;
import me.potato.demoreactivebasic.store.repositories.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserAggregate {
    private final UserRepository userRepository;

    public Mono<User> findById(Long id){
        return userRepository.findById(id).cache().log();
    }

    public Mono<User> findByIdOnCache(Long id){
        return userRepository.findById(id).cache(Duration.of(30, ChronoUnit.SECONDS));
    }

    public Flux<User> findAll(){
        return userRepository.findAll();
    }

    public Flux<User> findAllOnCache(){
        return userRepository.findAll().log();
    }

    public Mono<User> create(User _create){
        return userRepository.save(_create);
    }

}
