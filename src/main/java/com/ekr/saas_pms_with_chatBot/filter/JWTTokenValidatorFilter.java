package com.ekr.saas_pms_with_chatBot.filter;

import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.ENTITY.Account;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.REPOSITORY.AccountRepository;
import com.ekr.saas_pms_with_chatBot.SAAS_MANAGEMENT.Service.AccountService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

@Component
public class JWTTokenValidatorFilter extends OncePerRequestFilter {

    @Value("${jwt.secret:saasPmsJwtSecretKey2026ProductionSecureKey_9fK3mX7qL2vN8cR5}")
    private String secret; // Để dùng @Value thì class này phải là Bean --> Phải dùng @Component

    @Value("${acc.user}")
    String admin_u;

    @Autowired
    AccountRepository accountRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Lấy JWT Token Font-end gửi lên từ Header của http req
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Xóa Prefix Bearer trong Chuỗi chỉ lấy token
        token = token.substring(7);

        // Kiểm tra token
        try {
            // Tạo chữ ký số
            byte[] secret_b = secret.getBytes();
            SecretKey key = Keys.hmacShaKeyFor(secret_b);
            //Xác thực token
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Xác định ROLE
            String Role= "ROLE_ANYMOS";
            // Nếu email giống AMIN --> ADMIN
            // Nếu tìm được trong bảng Account --> Owner (Hard code
            // Nếu tìm được trong bảng User --> Các nhân viên khác theo bảng ROLE

            if(admin_u.equals(claims.getSubject())){
                Role = "ROLE_ADMIN";
            }else{
                // Tìm trong bảng Account
                // claims.getSubject() là email
                Account account = accountRepository.findByEmail(claims.getSubject());
                if(account != null){
                    Role = "ROLE_OWNER";
                }else{
                    // Tìm trong bảng User (nhân viên để xác định) Role
                    // Làm sau
                }
            }

            // LOG ĐỂ KIỂM TRA
            System.out.println("ROLE: " + Role);



            // Tạo Authentication đã xác thực
            UserDetails u = new User(
                    claims.getSubject(),
                    null,
                    List.of(
                            new SimpleGrantedAuthority(Role)
                    ) // Tạm Hard Code
            );


            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    u,
                    null,
                    u.getAuthorities()
            );
            // Set authentication vào Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);


            System.out.println("JWT Token is valid: Bạn đã xác thực thành công !!!!");
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException("401: Auth" + "\n" + e.getMessage());
        }

        // Tiếp tục filter kế tiếp
        filterChain.doFilter(request, response);
    }

    // Các API không cần kiểm tra JWT
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath(); // lấy path

        //Public
        // "/api/v1/auth/**", "/api/v1/contacts/**"
        if(path.equals("/api/v1/saas/user/login")) return true;
        if(path.equals("/api/v1/saas/user/register")) return true;


        if(path.equals("/api/v1/saas/user/Plan/List")) return true;
        if(path.equals("/api/v1/saas/user/Plan/Detail")) return true;
//        if(path.startsWith("/api/v1/auth/")) return true;
//        if(path.startsWith("/api/v1/contacts/")) return true;

        return false;
    }
}
