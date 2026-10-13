package com.pintu.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(min = 1, max = 64)
    private String name;

    @Size(max = 255)
    private String avatarUrl;

    @Pattern(regexp = "male|female|other|undisclosed")
    private String gender;
}

