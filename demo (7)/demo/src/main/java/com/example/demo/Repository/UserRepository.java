package com.example.demo.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;

import jakarta.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.phone = ?1")
    Optional<User> findByPhone(String phone);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Users SET is_online = ?2 WHERE id = ?1", nativeQuery = true)
    void updateOnline(Long id, boolean active);
}
