package com.honeytong.user.repository;

import com.honeytong.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
