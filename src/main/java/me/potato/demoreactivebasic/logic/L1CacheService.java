package me.potato.demoreactivebasic.logic;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.potato.demoreactivebasic.store.entities.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class L1CacheService {
    private final L2CacheService l2CacheService;

    // L1 Cache
    private final Cache<String, User> L1_CACHED_USER = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .build();


    public Mono<User> findById(Long id) {
        Optional<User> cachedUser = Optional.ofNullable(L1_CACHED_USER.getIfPresent(id.toString()));
        return Mono
                .justOrEmpty(cachedUser)
                .switchIfEmpty( l2CacheService
                        .findById(id)
                        .doOnSuccess(user -> L1_CACHED_USER.put(id.toString(), user))
                ).cache().log();
    }
}
