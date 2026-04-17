package com.stockpro.api_gateway.util;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Service
public class JwtService {
	@Value("${app.jwt.secret}")
	private String SECRET;
	@Value("${app.jwt.expiration-ms}")
	private long expirationTime;
	
	private Key signingKey;
	
	@PostConstruct
	public void init() {
		this.signingKey = Keys.hmacShaKeyFor(SECRET.getBytes());
	}
	
	public String genrateToken(String email) {
		return Jwts.builder()
				.setSubject(email)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis()+1000*60*60))
				.signWith(signingKey, SignatureAlgorithm.HS256)
				.compact();
	}
	
	public String extractEmail(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(signingKey).build()
				.parseClaimsJws(token)
				.getBody()
				.getSubject();
	}
	
	public boolean validateToken(String token, String email) {
		return extractEmail(token).equals(email) && !isTokenExpired(token);
	}
	
	public boolean isTokenExpired(String token) {
		return Jwts.parserBuilder().setSigningKey(signingKey).build()
				.parseClaimsJws(token).getBody().getExpiration().before(new Date());
	}
}