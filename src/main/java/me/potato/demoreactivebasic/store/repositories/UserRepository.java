package me.potato.demoreactivebasic.store.repositories;

import me.potato.demoreactivebasic.store.entities.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
}
