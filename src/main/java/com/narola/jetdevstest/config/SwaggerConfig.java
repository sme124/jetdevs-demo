package com.narola.jetdevstest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.DefaultTypeNameProvider;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.schema.plugins.SchemaPluginsManager;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .securitySchemes(Collections.singletonList(basicAuthScheme()))
                .securityContexts(Collections.singletonList(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.narola.jetdevstest"))
                .paths(PathSelectors.any())
                .build();

    }
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("JetDevs File Operation API Documentation")
                .description("API documentation for the JetDevs Spring Boot application")
                .version("1.0.0")
                .build();
    }

    private SecurityScheme basicAuthScheme() {
        return new BasicAuth("basicAuth");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(Collections.singletonList(new SecurityReference("basicAuth", new AuthorizationScope[0])))
                .build();
    }

}

