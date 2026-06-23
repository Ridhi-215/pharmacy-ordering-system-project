package com.pharmacy.ordering.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

@Configuration
@Slf4j
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Configuring DataSource for URL: {}", dbUrl);

        if (dbUrl != null && dbUrl.startsWith("jdbc:mysql://")) {
            try {
                // Register MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Set short login timeout to detect failure quickly (3 seconds)
                DriverManager.setLoginTimeout(3);
                
                log.info("Testing connection to MySQL database at {}...", dbUrl);
                try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword)) {
                    log.info("Successfully connected to MySQL database!");
                    return DataSourceBuilder.create()
                            .url(dbUrl)
                            .username(dbUsername)
                            .password(dbPassword)
                            .driverClassName("com.mysql.cj.jdbc.Driver")
                            .build();
                }
            } catch (Exception e) {
                log.error("Failed to connect to MySQL database at {}. Error: {}. Falling back to in-memory H2 database.", dbUrl, e.getMessage());
            }
        } else {
            log.warn("MySQL URL is either missing or invalid: {}. Falling back to in-memory H2 database.", dbUrl);
        }

        // Fallback: H2 Database
        log.info("Initializing fallback H2 database in MySQL compatibility mode...");
        return DataSourceBuilder.create()
                .url("jdbc:h2:mem:pharmacy_db;DB_CLOSE_DELAY=-1;MODE=MySQL")
                .username("sa")
                .password("")
                .driverClassName("org.h2.Driver")
                .build();
    }
}
