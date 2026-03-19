package com.major.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.repository.cdi.Eager;

@Getter
@AllArgsConstructor
public class UserDto {
    private Long id;
}
