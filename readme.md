# Spring Reactive 및 layered caching을 이용한 Restful API 
- Spring에서 제공하는 R2DBC, data-redis-reactive와 caffeine 이용한 Restful API 개발 방법
---
## 0. 사전준비
- docker를 이용한 postgres sql 설치
- docker를 이용한 redis 설치  

## 0.1 docker를 이용한 postgres sql설치
- postgres sql 설치
```shell
❯ docker run -d -p 5432:5432 -e POSTGRES_USER=root -e POSTGRES_PASSWORD='potato' --name local_postgres postgres
```

- postgres user 및 database 생성
```shell
❯ docker exec -it local_postgres psql -U root
psql (14.2 (Debian 14.2-1.pgdg110+1))
Type "help" for help.

root=# create user potato password 'test1234' superuser;
CREATE ROLE
root=# create database reactive_basic owner potato;
CREATE DATABASE
root=# 
```

- table 생성
```shell
❯ docker exec -it local_postgres psql -U potato reactive_basic
psql (14.2 (Debian 14.2-1.pgdg110+1))
Type "help" for help.

reactive_basic=# create table public.users(id SERIAL PRIMARY KEY, name VARCHAR(255), email VARCHAR(255));
CREATE TABLE
reactive_basic=# 
```

### 0.2 docker를 이용한 redis설치
```shell
❯ docker run -d -p 6379:6379 --name local_redis redis
```
---
## 1. 프로젝트 생성
### 1.1 Spring Initializr 이용한 프로젝트 생성
- 필요 Package 목록
```groovy
   implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
   implementation 'org.springframework.boot:spring-boot-starter-webflux'

   /*caching*/
   implementation 'com.github.ben-manes.caffeine:caffeine'
   implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'

   /*driver*/
   runtimeOnly 'io.r2dbc:r2dbc-postgresql'
```

- build.gradle
```groovy
plugins {
    id 'org.springframework.boot' version '2.6.8'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'java'
}

group = 'me.potato'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    /*reactive*/
    implementation 'org.springframework.boot:spring-boot-starter-data-r2dbc'
    implementation 'org.springframework.boot:spring-boot-starter-webflux'

    /*caching*/
    implementation 'com.github.ben-manes.caffeine:caffeine'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'

    /*driver*/
    runtimeOnly 'io.r2dbc:r2dbc-postgresql'

    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'

}

tasks.named('test') {
    useJUnitPlatform()
}
```

### 1.2 Application 환경설정
- application.yml : PostgresSql 과 Redis를 접속하기 위한 Connection 설정 
```yaml
spring:
  r2dbc:
    url: r2dbc:pool:postgres://localhost:5432/reactive_basic
    username: potato
    password: test1234
  redis:
    host: localhost
    port: 6379
```

- config.RedisConfig : 여러 Type의 RedisTemplate을 사용하기 위한 설정
- 필요한 곳에서는 createRedisTemplate를 통해 ReactiveRedisTemplate을 생성한다.
```java
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
```

- expose.UserRouter : 외부에서 인입되는 Call 을 받아줄 Router 설정
```java
@Slf4j
@Configuration
public class UserRouter {
    @Bean
    RouterFunction<ServerResponse> userRouterConfig(UserAggregate userAggregate, L1CacheService l1CacheService, L2CacheService l2CacheService) {
        return route()
                .GET("/users", request -> ok().body(userAggregate.findAll(), User.class))
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
```
 
# 2. Caching 구조

## 2.1 L1 Cache : Caffeine(logic.L1CacheService)
- Caffeine을 이용한 L1 Cache, Redis를 이용한 L2 Cache 를 구성한다.
- Router를 통해 외부의 요청 "/cached/users/{id}"을 받게 되면 L1 Cache를 먼저 찾아 본다.
```java
 public Mono<User> findById(Long id) {
        Optional<User> cachedUser = Optional.ofNullable(L1_CACHED_USER.getIfPresent(id.toString()));
        return Mono
                .justOrEmpty(cachedUser)
                .switchIfEmpty( l2CacheService
                        .findById(id)
                        .doOnSuccess(user -> L1_CACHED_USER.put(id.toString(), user))
                );
    }
```
- 이 때 호출되는 것이 l1CacheService.findById 인데 먼저 Caffeine을 통해 Application과 같은 공간에 있는 메모리를 조회한다.
- Caffeine이 Async 하게 조회되는 지는 찾아 보지 않았지만, Application과 같은 공간의 Memory를 뒤져보는 작업이라 크게 걱정할 만한 blocking 코드는 아닐 것이라 생각한다.
- 아무튼, l1 cache에서 hit가 되지 않는 경우 l2 cache를 이용해 다시 조회해 보고 정상적으로 완료가 되면 l1 cache에 적재한다.

## 2.2 L2 Cache : Redis(logic.L2CacheService)
- spring data redis 외에 spring data redis reactive가 추가 되어 사용해봤다.
- reactive가 붙는 경우 결과가 Mono, Flux등을 반환 됨을 알 수 있고, Reactor Pipeline을 구설 할수 있다.
```java
    @PostConstruct
    public void init() {
        reactiveRedisTemplate = redisConfig.createRedisTemplate(User.class);
        }
        
    public Mono<User> findById(Long id) {
        return reactiveRedisTemplate.opsForValue()
                .get(id.toString())
                .switchIfEmpty(
                        userAggregate.findById(id)
                                .doOnSuccess(user -> reactiveRedisTemplate.opsForValue().set(id.toString(), user, Duration.ofSeconds(30)).subscribe())
                );
    }
```
- 먼저 RedisTemplate을 사용하기 위해 redisConfig.createRedisTemplate(User.class) 를 사용했다.
- reactiveRedisTemplate는 redis 조회 결과를 Mono나 Flux로 반환 해주는 부분을 잘 볼 필요가 있다.
- Redis의 경우 네트워크를 타고 Application 외부의 저장소에서 정보를 획득하기 때문에 blocking으로 동작하면 wait되는 thread가 증가해 대용량 트랜잭션에 적합하지 않다.
- 이런 이슈를 해결하기 위해 reactiveRedisTemplate응 non-blocking 기능을 제공해 준다. 그 결과가 Mono, Flux의 반환이다.
- 동일한 사유로 R2DBC 또한 결과를 Mono 또는 Flux로 제공해준다.
- 어쨋거나 Redis에서 Data를 가져오면 반환 없을 경우 userAggregate를 통해 DBMS에서 조회한다.

끝

# 기타.
- 기존에 Redis등을 Cache로 사용할 떄 cacheManager등을 이용해서 특정 key에 대한 ttl을 설정 했는데, opsForValue().set()을 이용하니 넣는 item들 마다 ttl을 설정 할 수 있어 좋음.
- TODO : WebFlux에서도 cache()를 통해 caching 기능을 제공하는데, caffeine 과 동일하게 사용할 수 있겠지만, 추후 공부를 더해서 eviction mechanism 을 익히고 적용해 봐야겠다.
