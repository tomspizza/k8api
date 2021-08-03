package com.tomspizza.k8api.config;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KubernetesConfig {
    @Bean
    public DefaultKubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }
}