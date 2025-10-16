package com.piyush.mockarena.repository;

import com.piyush.mockarena.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN'")
    List<User> findAllAdmins();

    @Query("SELECT u FROM User u WHERE u.role = 'USER'")
    List<User> findAllUsers();

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}