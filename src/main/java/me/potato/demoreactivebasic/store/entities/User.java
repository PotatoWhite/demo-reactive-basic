package me.potato.demoreactivebasic.store.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Table(value = "users")
public class User implements Serializable {
    @Setter
    @Id
    private Long id;
    private String name;
    private String email;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
