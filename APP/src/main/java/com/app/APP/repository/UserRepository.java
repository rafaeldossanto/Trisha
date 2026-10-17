package com.app.APP.repository;

import com.app.APP.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUserCode(String userCode);

    @Query("SELECT u FROM User u WHERE LOWER(u.userCode) LIKE LOWER(CONCAT(:term, '%'))")
    List<User> findByUserCodePrefix(@Param("term") String term);
}
