package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private boolean enabled;
    private Timestamp createdAt;
}