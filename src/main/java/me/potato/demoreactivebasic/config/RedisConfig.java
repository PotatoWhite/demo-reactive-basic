package me.potato.demoreactivebasic.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@RequiredArgsConstructor
@Configuration
public class RedisConfig {

    private final ReactiveRedisConnectionFactory factory;

    public <T> ReactiveRedisTemplate createRedisTemplate(Class<T> type) {
        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new Jackson2JsonRedisSerializer<>(type);
        RedisSerializationContext.RedisSerializationContextBuilder<String, T> builder = RedisSerializationContext.newSerializationContext(keySerializer);
        RedisSerializationContext<String, T> context = builder.value(valueSerializer).build();
        return new ReactiveRedisTemplate<>(factory, context);
    }
}