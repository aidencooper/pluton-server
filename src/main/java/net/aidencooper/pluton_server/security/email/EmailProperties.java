package net.aidencooper.pluton_server.security.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties (prefix = "pluton.mail")
public record EmailProperties(String fromAddress) {
    
}
