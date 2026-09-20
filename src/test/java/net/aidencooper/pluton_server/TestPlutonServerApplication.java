package net.aidencooper.pluton_server;

import org.springframework.boot.SpringApplication;

public class TestPlutonServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(PlutonServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
