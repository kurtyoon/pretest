package dev.kurtyoon.pretest.core.config;

import dev.kurtyoon.pretest.core.filter.ExceptionFilter;
import dev.kurtyoon.pretest.core.filter.GlobalLoggerFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<GlobalLoggerFilter> globalLoggerFilter() {
        FilterRegistrationBean<GlobalLoggerFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new GlobalLoggerFilter());
        registrationBean.addUrlPatterns("/orders/*");
        registrationBean.setOrder(1);

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<ExceptionFilter> exceptionFilter() {
        FilterRegistrationBean<ExceptionFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new ExceptionFilter());
        registrationBean.addUrlPatterns("/orders/*");
        registrationBean.setOrder(2);

        return registrationBean;
    }
}
