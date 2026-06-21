package com.cropdeal.cropservice.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Custom principal stored in SecurityContext.
 * Controllers resolve the caller's userId from this instead of a DB lookup.
 */
@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final Long userId;
    private final String email;
    private final String role;
}
