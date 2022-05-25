package me.potato.demoreactivebasic.logic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.potato.demoreactivebasic.config.RedisConfig;
import me.potato.demoreactivebasic.logic.aggregates.UserAggregate;
import me.potato.demoreactivebasic.store.entities.User;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Service
public class L2CacheService {
    private final UserAggregate userAggregate;

    private final RedisConfig redisConfig;
    private ReactiveRedisTemplate reactiveRedisTemplate;

    @PostConstruct
    public void init() {
        reactiveRedisTemplate = redisConfig.createRedisTemplate(User.class);
    }

    public Flux<User> findAll() {
        return userAggregate.findAllOnCache();
    }

    public Mono<User> findById(Long id) {
        return reactiveRedisTemplate.opsForValue()
                .get(id.toString())
                .switchIfEmpty(
                        userAggregate.findById(id)
                                .doOnSuccess(user -> reactiveRedisTemplate.opsForValue().set(id.toString(), user, Duration.ofSeconds(30)).subscribe())
                ).cache().log();
    }
}

