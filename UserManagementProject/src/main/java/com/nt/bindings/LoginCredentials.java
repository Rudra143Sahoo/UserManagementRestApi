package com.nt.bindings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class LoginCredentials {
	private String email;
	private String password;

}
