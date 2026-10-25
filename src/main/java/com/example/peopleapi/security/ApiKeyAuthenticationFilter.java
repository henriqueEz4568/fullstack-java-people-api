package com.example.peopleapi.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-Key";

	private final byte[] apiKey;

	public ApiKeyAuthenticationFilter(String apiKey) {
		this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String providedKey = request.getHeader(HEADER);

		if (providedKey != null && MessageDigest.isEqual(apiKey, providedKey.getBytes(StandardCharsets.UTF_8))) {
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("api-key-client", null, List.of()));
			SecurityContextHolder.setContext(context);
		}

		chain.doFilter(request, response);
	}

}
