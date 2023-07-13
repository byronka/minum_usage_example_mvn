package com.renomad.auth;

public record LoginResult(LoginResultStatus status, User user) {
}
