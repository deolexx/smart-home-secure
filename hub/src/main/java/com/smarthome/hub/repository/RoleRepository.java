package com.smarthome.hub.repository;

import com.smarthome.hub.domain.Role;
import com.smarthome.hub.domain.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
	Optional<Role> findByName(RoleName name);
}

