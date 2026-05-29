package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.model.User;
import com.example.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.user_service.dto.UserResponseDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public UserResponseDTO register(@Valid @RequestBody User user){User savedUser = userService.register(user);

        return new UserResponseDTO(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/hello")
    public String hello() {
        return "JWT valide";
    }

    @GetMapping("/secure")
    public String secure() {
        return "Route sécurisée";
    }

    @GetMapping("/admin")
    public String admin() {
        return "Bienvenue ADMIN ";
    }

    @GetMapping("/client")
    public String client() {
        return "Bienvenue CLIENT ";
    }

    @GetMapping("/livreur")
    public String livreur() {
        return "Bienvenue LIVREUR ";
    }

    @GetMapping("/me")
    public UserResponseDTO me(Authentication authentication) {

        User user = userService.getCurrentUser(authentication.getName());

        return new UserResponseDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}