package com.codearena.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.codearena.entity.Role;
import com.codearena.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :nameSearch, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :emailSearch, '%'))")
    Page<User> findByNameOrEmailContaining(@Param("nameSearch") String nameSearch,
                                            @Param("emailSearch") String emailSearch,
                                            Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :nameSearch, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :emailSearch, '%')))")
    Page<User> findByRoleAndNameOrEmailContaining(@Param("role") Role role,
                                                   @Param("nameSearch") String nameSearch,
                                                   @Param("emailSearch") String emailSearch,
                                                   Pageable pageable);
}
