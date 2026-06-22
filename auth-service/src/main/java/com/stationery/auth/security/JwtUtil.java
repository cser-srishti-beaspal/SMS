package com.stationery.auth.security;

// JWT ke alag-alag parts handle karne ke liye
import java.nio.charset.StandardCharsets;                    // Token ka payload (saara data) represent karta hai
import java.util.Date;       // Token ki expiry date nikal gayi

import javax.crypto.SecretKey;                      // JWT banane aur parse karne ki main factory class

import org.slf4j.Logger;     // Token ka format galat hai (tampered/broken)
import org.slf4j.LoggerFactory;   // Token ka type supported nahi (e.g. unsigned token aaya)
import org.springframework.beans.factory.annotation.Value;             // Secret string se SecretKey object banata hai
import org.springframework.stereotype.Component;// Signature match nahi ki — token fake hai

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts; // application.properties se value inject karne ke liye
import io.jsonwebtoken.MalformedJwtException;           // Spring ko batata hai — "is class ka bean banao"
import io.jsonwebtoken.UnsupportedJwtException;          // HMAC signing ke liye cryptographic key type
import io.jsonwebtoken.security.Keys; // String → bytes convert karne ke liye (UTF-8 encoding)
import io.jsonwebtoken.security.SecurityException;                  // Token ki issuedAt aur expiration dates ke liye

/**
 * JwtUtil — JWT token ka poora lifecycle yahan manage hota hai:
 *   1. Token Generate karna  (login ke baad)
 *   2. Token Validate karna  (har request pe)
 *   3. Token se Data nikalna (username, role)
 *
 * INTERVIEW TIP: JWT = Header.Payload.Signature — teen parts hote hain
 *   Header    → algorithm info  (HS256)
 *   Payload   → actual data     (username, role, expiry)
 *   Signature → tamper-proof seal (secret key se bana)
 */
@Component // Spring is class ko automatically inject kar sakta hai (@Autowired se)
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // application.properties se inject hota hai → jwt.secret=mySecretKey123
    // @Value matlab — hardcode mat karo, config file se lo
    @Value("${jwt.secret}")
    private String secret;

    // Token kitni der valid rahega — milliseconds mein (e.g. 86400000 = 24 ghante)
    @Value("${jwt.expiration}")
    private Long expiration;


    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER: getSigningKey()
    // Secret string ko cryptographic SecretKey mein convert karta hai
    // Yeh key token sign karne aur verify karne dono kaam aati hai
    // ─────────────────────────────────────────────────────────────
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8); // "mySecret" → [109, 121, 83...] bytes
        return Keys.hmacShaKeyFor(keyBytes); // Bytes se HMAC-SHA SecretKey banao
        // HMAC = Hash-based Message Authentication Code
        //                Yeh ensure karta hai ki token sirf hamara server hi bana sakta hai
    }


    // ─────────────────────────────────────────────────────────────
    // 1. TOKEN GENERATE KARNA
    // Login success ke baad yeh method call hoti hai
    // Username aur Role leke signed JWT string return karta hai
    // ─────────────────────────────────────────────────────────────
    public String generateToken(String username, String role) {
        Date now = new Date();                                // Abhi ka time — issuedAt ke liye
        Date expiryDate = new Date(now.getTime() + expiration); // now + 24hr = expiry time

        return Jwts.builder()
                .subject(username)          // Token kiska hai? → "john@gmail.com"
                .claim("role", role)        // Extra data add karo → "role": "STUDENT"
                .issuedAt(now)              // Kab issue hua? → current time
                .expiration(expiryDate)     // Kab expire hoga? → now + expiration
                .signWith(getSigningKey())  // SecretKey se sign karo → Signature part banta hai
                .compact();                 // Saara build karo aur ek string mein compress karo
                                            // Result: "eyJhbGci....eyJ1c2VyI....SflKxwRJ"
    }


    // ─────────────────────────────────────────────────────────────
    // 2. TOKEN SE DATA NIKALNA
    // extractAllClaims() ek private helper hai jo token parse karta hai
    // Uske upar yeh public methods specific values nikalte hain
    // ─────────────────────────────────────────────────────────────

    // Token se sirf username nikalo (JWT mein "subject" field hota hai)
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject(); // Payload ka "sub" field → "john@gmail.com"
    }

    // Token se sirf role nikalo (humne khud "role" claim add kiya tha generateToken mein)
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class); // "role" key ki value → "STUDENT"
    }


    // ─────────────────────────────────────────────────────────────
    // 3. TOKEN VALIDATE KARNA
    // Har incoming request pe yeh check hota hai
    // Agar koi bhi check fail ho → false return karo, request reject karo
    //
    // INTERVIEW TIP: Alag-alag catch kyun? → Precise logging ke liye
    //                Har exception ka matlab alag hai — client ko sahi error batana zaroori hai
    // ─────────────────────────────────────────────────────────────
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey()) // Signature verify karo — key match honi chahiye
                    .build()
                    .parseSignedClaims(token);   // Token parse karo — agar kuch bhi galat hua toh exception
            return true; // Sab theek → token valid hai ✅

        } catch (SecurityException e) {
            // Token ki signature match nahi ki → token fake ya tampered hai
            logger.error("Invalid JWT signature: {}", e.getMessage());

        } catch (MalformedJwtException e) {
            // Token ka structure hi galat hai → "abc.def" jaisa kuch aaya (3 parts nahi)
            logger.error("Invalid JWT token: {}", e.getMessage());

        } catch (ExpiredJwtException e) {
            // Token sahi tha, lekin expire ho gaya → user ko dobara login karna hoga
            logger.error("JWT token is expired: {}", e.getMessage());

        } catch (UnsupportedJwtException e) {
            // Token type support nahi → jaise unsigned (plain) token bheja
            logger.error("JWT token is unsupported: {}", e.getMessage());

        } catch (IllegalArgumentException e) {
            // Token string null/empty hai → Authorization header khaali tha
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false; // Koi bhi exception aaya → token invalid ❌
    }


    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPER: extractAllClaims()
    // Token ko fully parse karta hai aur poora payload (Claims) return karta hai
    // Private hai kyunki bahar sirf specific values chahiye (username, role)
    // ─────────────────────────────────────────────────────────────
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // Pehle signature verify karo
                .build()
                .parseSignedClaims(token)     // Token ko teen parts mein todke verify karo
                .getPayload();                // Sirf Payload (Claims) part nikalo aur return karo
                // Claims = Map jaisa object → .getSubject(), .get("role") etc. call kar sakte ho
    }
}