package com.example.JobFinder.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes.
 * Run this main method to generate hashes for your existing passwords.
 */
public class PasswordHashUtil {

    public static void main(String[] args) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Common test passwords
        System.out.println("Password: '123456' -> Hash: " + encoder.encode("123456"));
        System.out.println("Password: 'admin123' -> Hash: " + encoder.encode("admin123"));
        System.out.println("Password: 'password' -> Hash: " + encoder.encode("password"));
        
        // Add your actual passwords here
        // System.out.println("Password: 'your_password' -> Hash: " + encoder.encode("your_password"));
    }
}
