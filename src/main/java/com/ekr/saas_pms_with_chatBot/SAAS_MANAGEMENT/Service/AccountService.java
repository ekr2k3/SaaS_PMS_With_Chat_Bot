package com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.LoginRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.REQ.RegisterRequestDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.LoginResponseDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.DTO.RES.RegisterResponseDTO;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.filter.JwtHelper;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ResponseEntity<?> register(RegisterRequestDTO request) {
        // Kiểm tra Email đã tồn tại
        if (accountRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(409).body(
                    new  RegisterResponseDTO(request.getEmail() + " already exists")
            );
        }
        System.out.println("Register email request: " + request.getEmail());

        // Hash Password (BCrypt)
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Lưu account
        Account account = new Account();
        account.setEmail(request.getEmail());
        account.setName(request.getName());
        account.setPassword(hashedPassword);
        account.setStatus(Account.AccountStatus.ACTIVE); // Lúc tạo luôn bằng ACTIVE
        account = accountRepository.save(account);

        // Sinh JWT
        // String token = jwtHelper.generateToken(account.getEmail());

        // Trả ResRegisterDTO
        return ResponseEntity.status(200).body(
                new  RegisterResponseDTO("Registration successful")
        );
    }

    // LOGIN

    @Value("${acc.user}")
    String admin_u;
    @Value("${acc.pass}")
    String admin_p;


    public ResponseEntity<?> login(LoginRequestDTO request) {

        if(request.getEmail().equals(admin_u) && request.getPassword().equals(admin_p)) {
            var token = jwtHelper.generateToken(request.getEmail());
            return ResponseEntity.ok(new LoginResponseDTO("Hello ADMIN SAAS you just Login success", token));
        }

        // Tìm Account
        Account account;
        try{
            account = accountRepository.findByEmail(request.getEmail());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(
                    new LoginResponseDTO("Không thể tìm được user" + e.getMessage(), null)
            );
        }
        // Kiểm tra password
        // Hash mật khẩu người dùng gửi lên
        var hasingPassword = passwordEncoder.encode(request.getPassword());
        // So sánh
        // ✅ Đúng
            if (!passwordEncoder.matches(
                    request.getPassword(),
                    account.getPassword()
            )) {
                return ResponseEntity.status(400).body(
                        new LoginResponseDTO("Mật khẩu không đúng", null)
                );
            }


        var token = jwtHelper.generateToken(request.getEmail());

        return ResponseEntity.ok(new LoginResponseDTO("Login success", token));

    }
}
