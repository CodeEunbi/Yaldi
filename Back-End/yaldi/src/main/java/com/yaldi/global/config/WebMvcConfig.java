package com.yaldi.global.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정
 *
 * <p><strong>주의:</strong> Jackson ObjectMapper 설정은 {@link JacksonConfig}에서 관리합니다.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Spring Security 필터가 실행되기 전에 DispatcherServlet이 404를 처리하도록 설정
     * 이렇게 하면 존재하지 않는 API 호출 시 OAuth 리다이렉트가 아닌 404 에러를 반환
     */
    @Bean
    public FilterRegistrationBean<org.springframework.web.filter.ForwardedHeaderFilter> forwardedHeaderFilter() {
        FilterRegistrationBean<org.springframework.web.filter.ForwardedHeaderFilter> bean =
            new FilterRegistrationBean<>(new org.springframework.web.filter.ForwardedHeaderFilter());
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
