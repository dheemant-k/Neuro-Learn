package com.neurolearn.service;

import com.neurolearn.dto.AuthResponse;
import com.neurolearn.dto.LoginRequest;
import com.neurolearn.dto.RegisterRequest;
import com.neurolearn.dto.UserProfileResponse;
import com.neurolearn.entity.Role;
import com.neurolearn.entity.StudentProfile;
import com.neurolearn.entity.User;
import com.neurolearn.repository.StudentProfileRepository;
import com.neurolearn.repository.UserRepository;
import com.neurolearn.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       StudentProfileRepository studentProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email address is already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        Long studentProfileId = null;
        if (request.getRole() == Role.STUDENT) {
            StudentProfile profile = new StudentProfile();
            profile.setUser(savedUser);
            profile.setCurrentLevel(1);
            profile.setOverallAccuracy(0.0);
            profile.setTotalXp(0);
            StudentProfile savedProfile = studentProfileRepository.save(profile);
            studentProfileId = savedProfile.getId();
        }

        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                studentProfileId
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        Long studentProfileId = null;
        if (user.getRole() == Role.STUDENT) {
            studentProfileId = studentProfileRepository.findByUserId(user.getId())
                    .map(StudentProfile::getId)
                    .orElse(null);
        }

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                studentProfileId
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long studentProfileId = null;
        if (user.getRole() == Role.STUDENT) {
            studentProfileId = studentProfileRepository.findByUserId(user.getId())
                    .map(StudentProfile::getId)
                    .orElse(null);
        }

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                studentProfileId
        );
    }
}
