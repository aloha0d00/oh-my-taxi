package com.carpool.repository;

import com.carpool.entity.User;
import com.carpool.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByPhoneAndRole(String phone, UserRole role);

    boolean existsByPhone(String phone);

    boolean existsByStudentId(String studentId);

    long count();

    @Query("SELECT COUNT(u) FROM User u WHERE DATE(u.createdAt) = CURRENT_DATE")
    long countTodayNewUsers();
}
