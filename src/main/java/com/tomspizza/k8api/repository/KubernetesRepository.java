package com.tomspizza.k8api.repository;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class KubernetesRepository {

    private static final String APP_LABEL = "app";
    private static final String INGRESS_NAMESPACE = "default";
    private static final String INGRESS_NAME = "nginx-gateway";
    private static final String DEFAULT_PROTOCOL = "TCP";
    private static final int SERVICE_PORT = 80;

    private final DefaultKubernetesClient client;

    public List<Deployment> getDeployments() {
        return client.apps().deployments().inAnyNamespace().list().getItems();
    }

    public void register2Ingress(String serviceName, int servicePort) {
        IngressList ingressList = getIngressList();
        List<Ingress> ingresses = ingressList.getItems();
        if (ingresses == null || ingresses.isEmpty()) {
            log.info("Creating new ingress");
            createIngress(serviceName);
        } else {
            log.info("Found {} ingress", ingresses.size());
            log.info("Adding a new service to ingress");
            addService2Ingress(ingresses.get(0), serviceName);
        }
    }

    public void unregister2Ingress(String serviceName) {
        log.info("Remove service [{}] out of ingress", serviceName);
        IngressList ingressList = getIngressList();
        List<Ingress> ingresses = ingressList.getItems();
        if (ingresses != null && !ingresses.isEmpty()) {
            Ingress ingress = ingresses.get(0);
            List<IngressRule> rules = ingress.getSpec().getRules();
            List<HTTPIngressPath> paths = rules.get(0).getHttp().getPaths();
            paths.removeIf(p -> p.getPath().equals("/" + serviceName));
            if (paths.isEmpty()) {
                log.info("Empty path for ingress, delete ingress");
                client.network().ingress()
                        .inNamespace(INGRESS_NAMESPACE)
                        .delete(ingress);
            } else {
                client.network().ingress()
                        .inNamespace(INGRESS_NAMESPACE)
                        .createOrReplace(ingress);
            }
        }
    }

    private Ingress createIngress(String serviceName) {
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
                                    .withServicePort(new IntOrString(SERVICE_PORT))
                                .endBackend()
                            .endPath()
                        .endHttp()
                    .endRule()
                .endSpec()
                .build();
        return client.network().ingress().inNamespace(INGRESS_NAMESPACE).createOrReplace(ingress);
    }

    private Ingress addService2Ingress(Ingress ingress, String serviceName) {
        List<IngressRule> rules = ingress.getSpec().getRules();
        List<HTTPIngressPath> paths = rules.get(0).getHttp().getPaths();

        IngressBackend newBackend = new IngressBackend();
        newBackend.setServiceName(serviceName);
        newBackend.setServicePort(new IntOrString(SERVICE_PORT));

        HTTPIngressPath newPath = new HTTPIngressPath();
        newPath.setPath("/" + serviceName);
        newPath.setBackend(newBackend);
        paths.add(newPath);

        return client.network().ingress()
                .inNamespace(INGRESS_NAMESPACE)
                .createOrReplace(ingress);
    }

    public IngressList getIngressList() {
        return client.network().ingress()
                .inNamespace(INGRESS_NAMESPACE)
                .withLabel(APP_LABEL, INGRESS_NAME)
                .list();
    }

    public Deployment deployDeployment(String namespace, String serviceName, String image, int servicePort) {
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
                                    .withProtocol(DEFAULT_PROTOCOL)
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

    public void deployService(String namespace, String serviceName, int containerPort) {
        var servicePort = new ServicePort();
        servicePort.setProtocol(DEFAULT_PROTOCOL);
        servicePort.setPort(SERVICE_PORT);
        servicePort.setTargetPort(new IntOrString(containerPort));

        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(serviceName)
                    .addToLabels(APP_LABEL, serviceName)
                .endMetadata()
                .withNewSpec()
                    .withSelector(Map.of(APP_LABEL, serviceName))
                    .withPorts(servicePort)
                .endSpec()
                .build();
        client.services().inNamespace(namespace).create(service);
    }

    public Deployment scalePod(String namespace, String serviceName, int numberOfReplicas) {
        return client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceName).edit(d -> new DeploymentBuilder(d).editSpec()
                .withReplicas(numberOfReplicas)
                .endSpec().build());
    }

    public Boolean deleteDeployment(String namespace, String serviceName) {
        return client.apps().deployments()
                .inNamespace(namespace)
                .withName(serviceName).delete();
    }

    public Boolean deleteService(String namespace, String serviceName) {
        return client.services()
                .inNamespace(namespace)
                .withName(serviceName).delete();
    }
}
