package me.potato.demoreactivebasic.store.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@ToString
@RequiredArgsConstructor
@Table(value = "users")
public class User {
    @Setter
    @Id
    private Long id;
    private final String name;
    private final String email;
}
