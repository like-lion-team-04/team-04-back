package com.likelion.firstbite.firstbiteserver.auth.security;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.member.domain.MemberStatus;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService jwtTokenService;
    private final MemberRepository memberRepository;
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header=request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UUID id=jwtTokenService.parseMemberId(header.substring(7));
                if (memberRepository.findById(id).filter(member -> member.getStatus() == MemberStatus.ACTIVE).isPresent()) {
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
                }
            } catch (RuntimeException ignored) { SecurityContextHolder.clearContext(); }
        }
        chain.doFilter(request,response);
    }
}
