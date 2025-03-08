package com.example.alarms.config;

import io.r2dbc.mssql.MssqlConnectionConfiguration;
import io.r2dbc.mssql.MssqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.lang.NonNull;

@Configuration
@Profile(value = "!test")
@EnableR2dbcRepositories("com.example.alarms.repositories")
public class R2dbcConfiguration extends AbstractR2dbcConfiguration {

    @Value("${database.name}")
    private String database;

    @Value("${database.host}")
    private String host;

    @Value("${database.port:1433}")
    private int port;

    @Value("${database.username}")
    private String username;

    @Value("${database.password}")
    private String password;

    @Override
    @Bean
    @NonNull
    public ConnectionFactory connectionFactory() {
        return new MssqlConnectionFactory(MssqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .database(database)
                .trustServerCertificate(true) // For development - disable in production
                .build());
    }
}