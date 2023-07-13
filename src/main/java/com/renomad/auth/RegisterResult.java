package com.renomad.auth;

public record RegisterResult(RegisterResultStatus status, User newUser) {}
