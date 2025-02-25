package dev.kurtyoon.pretest.core.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.kurtyoon.pretest.common.logging.LoggerUtils;
import dev.kurtyoon.pretest.core.dto.ResponseDto;
import dev.kurtyoon.pretest.core.exception.CommonException;
import dev.kurtyoon.pretest.core.exception.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ExceptionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerUtils.getLogger(ExceptionFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CommonException e) {
            log.error("FilterException throw CommonException : {}", e.getMessage());
            handleException(response, e.getErrorCode());
        } catch (Exception e) {
            log.error("filterException throw Exception : {}", e.getMessage());
            handleException(response, ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void handleException(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ResponseDto<Object> errorResponse = ResponseDto.fail(new CommonException(errorCode));
        String jsonResponse = new ObjectMapper().writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}
