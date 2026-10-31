package com.releaseguard.security;

public record CsrfResponse(
        String token,
        String parameterName,
        String headerName
) {}

