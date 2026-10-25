import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/** Run with:  java scripts/GenerateRsaKey.java   (JDK 17+, nothing else needed) */
public class GenerateRsaKey {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        System.out.println("Copy the line below into the UPI_RSA_PRIVATE_KEY environment variable.");
        System.out.println("KEEP IT SECRET. Never commit it to git.\n");
        System.out.println(Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
    }
}
