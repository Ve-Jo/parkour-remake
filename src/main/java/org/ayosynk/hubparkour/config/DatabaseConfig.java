package org.ayosynk.hubparkour.config;

public record DatabaseConfig(
        boolean enabled,
        String host,
        int port,
        String name,
        String user,
        String password,
        int poolSize,
        boolean useSsl,
        boolean createDatabase
) {}
