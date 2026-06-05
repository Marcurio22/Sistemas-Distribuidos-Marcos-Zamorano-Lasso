/**
 * El Plantío 360 - Configuración robusta de multipart.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */
package com.marcos.plantio360.config;

import jakarta.servlet.MultipartConfigElement;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Refuerza los límites de subida por código, además de application.properties.
 *
 * La regla de negocio sigue estando en AdminController: imágenes <= 64MB.
 * Aquí se configura un límite técnico mayor para que Spring pueda leer la
 * petición y mostrar un mensaje de validación específico cuando sea posible.
 */
@Slf4j
@Configuration
public class MultipartUploadConfig {

    private static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(128);
    private static final DataSize MAX_REQUEST_SIZE = DataSize.ofMegabytes(130);
    private static final DataSize FILE_SIZE_THRESHOLD = DataSize.ofMegabytes(2);
    private static final int MAX_REQUEST_BYTES = Math.toIntExact(MAX_REQUEST_SIZE.toBytes());
    private static final int MAX_PART_COUNT = 100;
    private static final int MAX_PART_HEADER_SIZE_BYTES = 32 * 1024;

    /**
     * Fuerza resolución perezosa para que los controladores puedan capturar
     * errores multipart dentro de su propio flujo de formulario.
     */
    @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setResolveLazily(true);
        return resolver;
    }

    /**
     * Configuración servlet real que usa request.getParts() en multipart/form-data.
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(MAX_FILE_SIZE);
        factory.setMaxRequestSize(MAX_REQUEST_SIZE);
        factory.setFileSizeThreshold(FILE_SIZE_THRESHOLD);
        return factory.createMultipartConfig();
    }

    /**
     * Refuerza límites del conector Tomcat embebido para evitar 413 prematuros.
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatUploadCustomizer() {
        return factory -> factory.addConnectorCustomizers(this::customizeConnector);
    }

    private void customizeConnector(Connector connector) {
        connector.setMaxPostSize(MAX_REQUEST_BYTES);
        connector.setMaxSavePostSize(MAX_REQUEST_BYTES);

        // Importante: maxPartCount y maxPartHeaderSize pertenecen al Connector,
        // no al ProtocolHandler. setProperty("maxPartCount", ...) puede no aplicar
        // el límite real y Tomcat seguiría rechazando formularios con 10+ partes.
        connector.setMaxPartCount(MAX_PART_COUNT);
        connector.setMaxPartHeaderSize(MAX_PART_HEADER_SIZE_BYTES);

        ProtocolHandler protocolHandler = connector.getProtocolHandler();
        if (protocolHandler instanceof AbstractHttp11Protocol<?> protocol) {
            protocol.setMaxSwallowSize(-1);
        }

        log.info(
            "Tomcat Connector multipart configurado: maxPostSize={} bytes, maxSavePostSize={} bytes, maxPartCount={}, maxPartHeaderSize={} bytes, maxSwallowSize=-1",
            connector.getMaxPostSize(),
            connector.getMaxSavePostSize(),
            connector.getMaxPartCount(),
            connector.getMaxPartHeaderSize()
        );
    }

    /**
     * Log de arranque para demostrar qué límites se están cargando dentro del contenedor.
     */
    @Bean
    public ApplicationRunner uploadLimitLogger(Environment environment) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                log.info("==== CONFIGURACIÓN MULTIPART EFECTIVA ====");
                log.info("spring.servlet.multipart.enabled = {}", environment.getProperty("spring.servlet.multipart.enabled"));
                log.info("spring.servlet.multipart.resolve-lazily = {}", environment.getProperty("spring.servlet.multipart.resolve-lazily"));
                log.info("spring.servlet.multipart.max-file-size = {}", environment.getProperty("spring.servlet.multipart.max-file-size"));
                log.info("spring.servlet.multipart.max-request-size = {}", environment.getProperty("spring.servlet.multipart.max-request-size"));
                log.info("server.tomcat.max-http-form-post-size = {}", environment.getProperty("server.tomcat.max-http-form-post-size"));
                log.info("server.tomcat.max-part-count = {}", environment.getProperty("server.tomcat.max-part-count"));
                log.info("server.tomcat.max-part-header-size = {}", environment.getProperty("server.tomcat.max-part-header-size"));
                log.info("server.tomcat.max-swallow-size = {}", environment.getProperty("server.tomcat.max-swallow-size"));
                log.info("Límite técnico reforzado por código: file={}, request={}, partCount={}, partHeaderSize={}B", MAX_FILE_SIZE, MAX_REQUEST_SIZE, MAX_PART_COUNT, MAX_PART_HEADER_SIZE_BYTES);
                log.info("==========================================");
            }
        };
    }
}
