package com.example.ums;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
public class UmsApplication {
    public static void main(String[] args) { SpringApplication.run(UmsApplication.class, args); }

    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService uds(UserRepo repo) {
        return name -> repo.findByUsername(name)
                .map(u -> User.withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRole().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException(name));
    }

    @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/admins/**").hasRole("ADMIN")
                .requestMatchers("/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
                .requestMatchers("/api/students/**").hasAnyRole("ADMIN", "STUDENT")
                .anyRequest().authenticated())
            .formLogin(f -> f.defaultSuccessUrl("/", true))
            .httpBasic(Customizer.withDefaults())
            .logout(l -> l.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean CommandLineRunner seed(UserRepo repo, PasswordEncoder enc) {
        return args -> {
            if (!repo.existsByUsername("admin")) {
                AppUser a = new AppUser();
                a.setUsername("admin"); a.setName("Super Admin"); a.setRole(Role.ADMIN);
                a.setPassword(enc.encode("admin123"));
                repo.save(a);
            }
        };
    }
}
