import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

        String password = "Admin@12345";
        String hash = encoder.encode(password);

        System.out.println("PASSWORD = " + password);
        System.out.println("HASH = " + hash);
        System.out.println("MATCHES = " + encoder.matches(password, hash));
    }
}