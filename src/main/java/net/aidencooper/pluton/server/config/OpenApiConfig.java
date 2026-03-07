package net.aidencooper.pluton.server.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "Pluton Server",
                contact = @Contact(
                        name = "Aiden",
                        email = "aidencooper23@gmail.com"
                ),
                license = @License(
                        name = "GNU Affero General Public License v3.0 or later",
                        identifier = "AGPL-3.0-or-later",
                        url = "https://www.gnu.org/licenses/agpl-3.0.en.html"
                )
        ),
        servers = {
                @Server(
                        description = "Pluton Server Prod Environment",
                        url = "https://pluton.aidencooper.net"
                ),
                @Server(
                        description = "Pluton Server Dev Environment",
                        url = "http://localhost:8080"
                ),
                @Server(
                        description = "Pluton Media Server Dev Environment",
                        url = "http://localhost:8081"
                ),
                @Server(
                        description = "Pluton App Dev Environment"
                )
        }
)
public class OpenApiConfig {
}
