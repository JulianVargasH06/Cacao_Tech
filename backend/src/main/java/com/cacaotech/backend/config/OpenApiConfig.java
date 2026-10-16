package com.cacaotech.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger UI (/swagger-ui.html).
 * Declara el esquema JWT para que aparezca el botón "Authorize": se pega el token del login
 * y Swagger lo envía como "Authorization: Bearer <token>" en todas las peticiones.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Cacao Tech API", version = "v1", description = "Trazabilidad del cacao, del lote al producto"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
