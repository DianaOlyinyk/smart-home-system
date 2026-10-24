package server;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.function.Predicate;

@Component
class ApiPathConfiguration implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        Predicate<Class<?>> restController = type -> AnnotatedElementUtils.hasAnnotation(type, RestController.class);
        configurer.addPathPrefix(ApiPaths.PREFIX, HandlerTypePredicate.forBasePackage("server").and(restController));
    }
}
