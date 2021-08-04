package com.tomspizza.k8api.repository;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KubernetesRepository {

    private static final String APP_LABEL = "app";
    private static final String INGRESS_NAMESPACE = "default";
    private static final String INGRESS_NAME = "ingress-gateway";

    private final DefaultKubernetesClient client;

    public List<Deployment> getDeployments() {
        return client.apps().deployments().inAnyNamespace().list().getItems();
    }

    public void register2Ingress(String serviceName, int servicePort) {
        IngressList ingressList = getIngressList();
        List<Ingress> ingresses = ingressList.getItems();
        if (ingresses == null || ingresses.isEmpty()) {
            createIngress(serviceName, servicePort);
        } else {
            addService2Ingress(ingresses.get(0), serviceName, servicePort);
        }
    }

    public void unregister2Ingress(String serviceName) {
        IngressList ingressList = getIngressList();
        List<Ingress> ingresses = ingressList.getItems();
        if (ingresses != null && !ingresses.isEmpty()) {
            Ingress ingress = ingresses.get(0);
            List<IngressRule> rules = ingress.getSpec().getRules();
            List<HTTPIngressPath> paths = rules.get(0).getHttp().getPaths();
            paths.removeIf(p -> p.getPath().equals("/" + serviceName));
            client.network().ingress()
                    .inNamespace(INGRESS_NAMESPACE)
                    .createOrReplace(ingress);
        }
    }

    private Ingress createIngress(String serviceName, int servicePort) {
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                    .withName(INGRESS_NAME)
                    .addToAnnotations("nginx.ingress.kubernetes.io/rewrite-target", "/")
                    .addToLabels(APP_LABEL, INGRESS_NAME)
                .endMetadata()
                .withNewSpec()
                    .addNewRule()
                        .withNewHttp()
                            .addNewPath()
                                .withPath("/" + serviceName)
                                .withNewBackend()
                                    .withServiceName(serviceName)
                                    .withServicePort(new IntOrString(servicePort))
                                .endBackend()
                            .endPath()
                        .endHttp()
                    .endRule()
                .endSpec()
                .build();
        return client.network().ingress().inNamespace(INGRESS_NAMESPACE).createOrReplace(ingress);
    }

    private Ingress addService2Ingress(Ingress ingress, String serviceName, int servicePort) {
        List<IngressRule> rules = ingress.getSpec().getRules();
        List<HTTPIngressPath> paths = rules.get(0).getHttp().getPaths();

        IngressBackend newBackend = new IngressBackend();
        newBackend.setServiceName(serviceName);
        newBackend.setServicePort(new IntOrString(servicePort));

        HTTPIngressPath newPath = new HTTPIngressPath();
        newPath.setPath("/" + serviceName);
        newPath.setBackend(newBackend);
        paths.add(newPath);

        return client.network().ingress()
                .inNamespace(INGRESS_NAMESPACE)
                .createOrReplace(ingress);
    }

    private IngressList getIngressList() {
        return client.network().ingress()
                .inNamespace(INGRESS_NAMESPACE)
                .withLabel(APP_LABEL, INGRESS_NAME)
                .list();
    }

    public Deployment deploy(String namespace, String serviceName, String image, int servicePort) {
        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                    .addToLabels(APP_LABEL, serviceName)
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewTemplate()
                        .withNewMetadata()
                        .addToLabels(APP_LABEL, serviceName)
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName(serviceName)
                                .withImage(image)
                                .addNewPort()
                                    .withContainerPort(servicePort)
                                .endPort()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                    .withNewSelector()
                        .addToMatchLabels(APP_LABEL, serviceName)
                    .endSelector()
                .endSpec()
                .build();
        return client.apps().deployments().inNamespace(namespace).create(deployment);
    }

    public Deployment scale(String namespace, String serviceName, int numberOfReplicas) {
        return client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceName).edit(d -> new DeploymentBuilder(d).editSpec()
                .withReplicas(numberOfReplicas)
                .endSpec().build());
    }

    public Boolean delete(String namespace, String serviceName) {
        return client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceName).delete();
    }
}
