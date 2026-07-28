package com.team05.petmeeting.global.security.test;

import com.team05.petmeeting.global.security.userdetails.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;

public class WithCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithCustomUser annotation) {

        SecurityContext context = SecurityContextHolder.createEmptyContext();

        List<GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(annotation.role()));
        CustomUserDetails userDetails = new CustomUserDetails(
                annotation.userId(),
                authorities
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        context.setAuthentication(auth);
        return context;
    }
}