package model.entity;

import jdk.jshell.Snippet;
import lombok.*;
import model.entity.enums.Role;

import java.time.LocalDateTime;
@NoArgsConstructor
@Setter
@Getter
@AllArgsConstructor
@ToString

public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private String salt;
    private String fullName;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
