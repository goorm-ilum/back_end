package com.talktrip.talktrip.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talktrip.talktrip.global.util.JWTUtil;
import com.talktrip.talktrip.domain.member.entity.Member;
import com.talktrip.talktrip.domain.member.repository.MemberRepository;
import com.talktrip.talktrip.global.security.CustomMemberDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class JWTCheckFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final MemberRepository memberRepository;

    public JWTCheckFilter(JWTUtil jwtUtil, MemberRepository memberRepository) {
        this.jwtUtil = jwtUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("[JWTCheckFilter] 실행됨");

        String authHeader = request.getHeader("Authorization");
        String accessToken = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        } else {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals("accessToken")) {
                        accessToken = cookie.getValue();
                        break;
                    }
                    if (cookie.getName().equals("member")) {
                        try {
                            // URL 디코딩
                            String decoded = URLDecoder.decode(cookie.getValue(), "UTF-8");
                            ObjectMapper mapper = new ObjectMapper();
                            Map<String, Object> memberMap = mapper.readValue(decoded, Map.class);
                            if (memberMap.containsKey("accessToken")) {
                                accessToken = (String) memberMap.get("accessToken");
                                break;
                            }
                        } catch (Exception e) {
                            log.error("member 쿠키 파싱 실패: {}", e.getMessage());
                        }
                    }
                }
            }
        }

        if (accessToken == null) {
            log.warn("Authorization 헤더 없음 + 쿠키에서도 토큰 없음");
            respondWithError(response, "MISSING_AUTH_HEADER");
            return;
        }

        try {
            Map<String, Object> claims = jwtUtil.validateToken(accessToken);

            String email = claims.get("email").toString();

            Optional<Member> memberOptional = memberRepository.findByAccountEmail(email);
            if (memberOptional.isEmpty()) {
                respondWithError(response, "MEMBER_NOT_FOUND");
                return;
            }

            Member member = memberOptional.get();
            CustomMemberDetails customMemberDetails = new CustomMemberDetails(member);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(customMemberDetails, null, customMemberDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            respondWithError(response, "INVALID_OR_EXPIRED_TOKEN");
        }
    }

    private void respondWithError(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        PrintWriter writer = response.getWriter();
        new ObjectMapper().writeValue(writer, Map.of("error", message));
        writer.flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/api/member/kakao") ||
                uri.equals("/api/member/kakao-login-url");
    }
}