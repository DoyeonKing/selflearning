package com.example.springboot.security; // 包名调整

import com.example.springboot.entity.Admin;   // 导入路径调整
import com.example.springboot.entity.Doctor;   // 导入路径调整
import com.example.springboot.entity.Patient;  // 导入路径调整
import com.example.springboot.repository.AdminRepository;   // 导入路径调整
import com.example.springboot.repository.DoctorRepository;  // 导入路径调整
import com.example.springboot.repository.PatientRepository; // 导入路径调整
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public UserDetailsServiceImpl(PatientRepository patientRepository, DoctorRepository doctorRepository, AdminRepository adminRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 假设 username 可以是 patient 的 identifier, doctor 的 identifier, 或 admin 的 username
        // 实际应用中可能需要更明确的登录入口或用户类型区分

        // 尝试作为患者加载
        Optional<Patient> patient = patientRepository.findByIdentifier(username);
        if (patient.isPresent()) {
            return buildUserDetails(patient.get().getIdentifier(), patient.get().getPasswordHash(), "ROLE_PATIENT");
        }

        // 尝试作为医生加载
        Optional<Doctor> doctor = doctorRepository.findByIdentifier(username);
        if (doctor.isPresent()) {
            return buildUserDetails(doctor.get().getIdentifier(), doctor.get().getPasswordHash(), "ROLE_DOCTOR");
        }

        // 尝试作为管理员加载
        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            List<String> roles = new ArrayList<>();
            roles.add("ROLE_ADMIN");
            admin.get().getRoles().forEach(role -> roles.add("ROLE_" + role.getRoleName().toUpperCase()));
            return buildUserDetails(admin.get().getUsername(), admin.get().getPasswordHash(), roles.toArray(new String[0]));
        }

        throw new UsernameNotFoundException("User not found with username: " + username);
    }

    private UserDetails buildUserDetails(String username, String passwordHash, String... roles) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return new User(username, passwordHash, authorities);
    }
}
