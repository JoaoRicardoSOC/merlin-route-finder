package br.com.jence.backend.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /*
     * Origens vem de propriedade em vez de ficarem fixas aqui: as URLs do Totem e do Mobile
     * publicados so serao conhecidas no momento do deploy, e mudar origem nao deveria exigir
     * recompilar a aplicacao. O padrao cobre o desenvolvimento local.
     */
    @Value("${merlin.cors.allowed-origins}")
    private String[] origensPermitidas;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origensPermitidas)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }
}
