package com.hope.master_service.config;

import com.hope.master_service.constants.Constant;
import com.hope.master_service.service.AuditorAwareService;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.HandlerMethod;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AppConfig {

    private static String appVersion;

    @Value("${app.version}")
    private void setVersion(String version) {
        appVersion = version;
    }

    public static String getVersion() {
        return appVersion;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource
                = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:response/messages");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public AuditorAware<String> auditorAware() {
        return new AuditorAwareService();
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hope Master Service API")
                        .version(appVersion)
                        .description("Multi-tenant healthcare platform API with Keycloak authentication"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("opaque")
                                        .description("Paste the access_token from /api/v1/users/login response")));
    }

    @Bean
    public OperationCustomizer customizeOperations() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Parameter tenantHeader = new Parameter()
                    .in("header")
                    .schema(new Schema<String>().type("string"))
                    .name(Constant.TENANT_HEADER)
                    .description("Tenant schema name (e.g. new_beginning, hope_programme)")
                    .required(false);
            operation.addParametersItem(tenantHeader);
            return operation;
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
