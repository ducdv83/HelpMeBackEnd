// repository/OtpRepository.java
package com.helpme.backend.repository;

import com.helpme.backend.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpRepository extends JpaRepository<Otp, UUID> {

    Optional<Otp> findByPhoneAndCode(String phone, String code);

    void deleteByPhone(String phone);
}