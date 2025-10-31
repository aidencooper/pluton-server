package net.aidencooper.pluton.server.repository;

import net.aidencooper.pluton.server.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

}
