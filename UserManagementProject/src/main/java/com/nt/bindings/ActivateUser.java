package com.nt.bindings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivateUser {
private String email;
private String tempPassword;
private String newPassword;
private String confirmPassword;
}
