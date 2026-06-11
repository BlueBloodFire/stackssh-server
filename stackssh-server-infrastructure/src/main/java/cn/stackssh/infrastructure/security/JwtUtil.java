package cn.stackssh.infrastructure.security;

import cn.stackssh.domain.auth.adapter.port.ITokenGeneratorPort;
import cn.stackssh.domain.auth.adapter.port.ITokenVerifierPort;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JwtUtil implements ITokenGeneratorPort, ITokenVerifierPort {

    private static final long EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;

    private final Algorithm algorithm;

    public JwtUtil(@Value("${jwt.secret:StackSSH2026JwtSecret!!PleaseChange}") String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    @Override
    public String generate(String userId, String username) {
        return JWT.create()
                .withSubject(username)
                .withClaim("userId", userId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .sign(algorithm);
    }

    public DecodedJWT decodeToken(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm).build();
        return verifier.verify(token);
    }

    @Override
    public void verify(String token) {
        JWT.require(algorithm).build().verify(token);
    }

    @Override
    public boolean isValid(String token) {
        try {
            verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return decodeToken(token).getClaim("userId").asString();
    }

    public String getUsername(String token) {
        return decodeToken(token).getSubject();
    }
}
