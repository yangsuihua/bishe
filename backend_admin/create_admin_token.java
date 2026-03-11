import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateAdminToken {
    public static void main(String[] args) {
        // 1. 生成BCrypt加密密码（123456）
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String encodedPassword = encoder.encode(password);
        System.out.println("=== BCrypt加密后的密码 ===");
        System.out.println(encodedPassword);
        System.out.println();
        
        // 2. 生成JWT Token（假设用户ID为1）
        Long userId = 1L;
        String username = "admin";
        String secret = "video-platform-secret-key-2024-very-long-secret-abcdewdfdsfsdffgsd";
        Long expiration = 1209600L; // 14天（秒）
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        
        String token = Jwts.builder()
                .setClaims(claims)
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS512)
                .compact();
        
        System.out.println("=== JWT Token ===");
        System.out.println(token);
        System.out.println();
        
        // 3. 生成SQL插入语句
        System.out.println("=== SQL插入语句 ===");
        System.out.println("INSERT INTO `user` (username, password, nickname, role, status, created_at) ");
        System.out.println("VALUES ('admin', '" + encodedPassword + "', '管理员', 'admin', 1, NOW());");
    }
}
