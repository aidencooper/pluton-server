package net.aidencooper.pluton_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import net.aidencooper.pluton_server.security.jwt.config.RsaKeyProperties;

@EnableConfigurationProperties(RsaKeyProperties.class)
@SpringBootApplication
public class PlutonServerApplication {
	public static void main(String[] args) {
		SpringApplication.run(PlutonServerApplication.class, args);
	}
}
