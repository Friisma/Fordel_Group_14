package dk.utd.fordel.view;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PasswordExpiryInterceptor passwordExpiryInterceptor;

    public WebConfig(PasswordExpiryInterceptor passwordExpiryInterceptor) {
        this.passwordExpiryInterceptor = passwordExpiryInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(passwordExpiryInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/js/**", "/css/**", "/images/**", "/webjars/**", "/style.css", "/favicon.ico");
    }
}