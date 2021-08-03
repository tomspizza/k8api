package com.tomspizza.k8api.repository;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KubernetesRepository {

    private final DefaultKubernetesClient client;

    public List<Deployment> getDeployments() {
        return client.apps().deployments().inAnyNamespace().list().getItems();
    }

    public void deploy(String namespace, String serviceName, String image) {
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", serviceName)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withName(serviceName)
                .withImage(image)
                .withCommand("sleep", "36000")
                .endContainer()
                .endSpec()
                .endTemplate()
                .withNewSelector()
                .addToMatchLabels("app", serviceName)
                .endSelector()
                .endSpec()
                .build();
        client.apps().deployments().inNamespace(namespace).create(deployment);
    }

    public void scale(String namespace, String serviceName, int numberOfReplicas) {
        client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceName).edit(d -> new DeploymentBuilder(d).editSpec()
                .withReplicas(numberOfReplicas)
                .endSpec().build());
    }

    public void delete(String namespace, String serviceName) {
        client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceName).delete();
    }
}
