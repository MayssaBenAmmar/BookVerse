package com.alibou.book.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Méthodes existantes
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findAllByUsernameIsNull();

    // ✅ MÉTHODES OBLIGATOIRES : Retournent des listes pour gérer les doublons
    List<User> findAllByUsername(String username);
    List<User> findAllByEmail(String email);

    // ✅ Méthodes utiles supplémentaires
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // ✅ Alternative robuste pour chercher par username OU email
    @Query("SELECT u FROM User u WHERE u.username = :identifier OR u.email = :identifier")
    List<User> findByUsernameOrEmail(@Param("identifier") String identifier);
}