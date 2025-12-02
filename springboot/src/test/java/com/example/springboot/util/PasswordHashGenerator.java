package com.example.springboot.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        // 请确保您的项目依赖了 spring-security-core，否则这行会报错
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 这是我们要测试的初始密码
        String password = "Xmr@6511";

        // 生成哈希值
        String hashedPassword = encoder.encode(password);

        System.out.println("原始密码: " + password);
        System.out.println("生成的哈希值: " + hashedPassword);

        // 生成第二个密码的哈希值 (用户B)
        String activePassword = "ActivePass";
        String hashedActivePassword = encoder.encode(activePassword);
        System.out.println("原始密码: " + activePassword);
        System.out.println("生成的哈希值: " + hashedActivePassword);
    }
}