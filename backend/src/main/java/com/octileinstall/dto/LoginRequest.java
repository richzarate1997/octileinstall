package com.octileinstall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    /** Keeps the password out of logs and error messages that stringify this object. */
    @Override
    public String toString() {
        return "LoginRequest(email=" + email + ", password=****)";
    }
}
