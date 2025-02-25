package dev.kurtyoon.pretest.core.filter;

import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class GlobalLoggerFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerUtils.getLogger(GlobalLoggerFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        log.info("[Global] HTTP Request Received! ({} {} {})",
                request.getHeader("X-FORWARDED_FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr(),
                request.getMethod(),
                request.getRequestURI()
        );

        request.setAttribute("INTERCEPTOR_PRE_HANDLE_TIME", System.currentTimeMillis());

        filterChain.doFilter(request, response);

        Long preHandleTime = (Long) request.getAttribute("INTERCEPTOR_PRE_HANDLE_TIME");
        Long postHandleTime = System.currentTimeMillis();

        log.info("[Global] HTTP Request Has Been Processed! It Took {}ms. ({} {} {})",
                postHandleTime - preHandleTime,
                request.getHeader("X-FORWARDED-FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr(),
                request.getMethod(),
                request.getRequestURI());
    }
}
