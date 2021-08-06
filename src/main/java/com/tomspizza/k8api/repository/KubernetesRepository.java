package com.tomspizza.k8api.repository;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class KubernetesRepository {

    @Value("${app.containerPort}")
    private int containerPort;

    @Value("${app.ingressNamespace}")
    private String ingressNamespace;

    @Value("${app.ingressName}")
    private String ingressName;

    @Value("${app.servicePort}")
    private int servicePort;

    @Value("${app.nginx.annotationKey}")
    private String nginxAnnotationKey;

    @Value("${app.nginx.annotationValue}")
    private String nginxAnnotationValue;

    private static final String APP_LABEL = "app";
    private static final String DEFAULT_PROTOCOL = "TCP";
    private static final String PATH_TYPE = "Prefix";
    private static final String DEFAULT_INGRESS_CON_NAME = "ingress-nginx-controller";

    private final DefaultKubernetesClient client;

    public String getIngressPublicUrl() {
        List<Service> services = client.services().inAnyNamespace().list().getItems();
        Optional<Service> optional = services.stream().filter(s -> DEFAULT_INGRESS_CON_NAME.equals(s.getMetadata().getName())).findAny();
        if (!optional.isPresent()) {
            return null;
        }

        Service service = optional.get();
        return service.getStatus().getLoadBalancer().getIngress().get(0).getIp();
    }

    public Namespace getOrCreateNamespace(String name) {
        List<Namespace> namespaces = client.namespaces().list().getItems();
        Optional<Namespace> optional = namespaces.stream().filter(n -> n.getMetadata().getName().equals(name)).findAny();
        if (optional.isPresent()) {
            return optional.get();
        }

        log.info("Create a new namespace: {}", name);
        Namespace newNamespace = new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build();
        client.namespaces().createOrReplace(newNamespace);
        return newNamespace;
    }

    public List<Deployment> getDeployments() {
        return client.apps().deployments().inAnyNamespace().list().getItems();
    }

    public void register2Ingress(String serviceName) {
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
                client.network().v1().ingresses()
                        .inNamespace(ingressNamespace)
                        .delete(ingress);
            } else {
                client.network().v1().ingresses()
                        .inNamespace(ingressNamespace)
                        .createOrReplace(ingress);
            }
        }
    }

    private Ingress createIngress(String serviceName) {
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                .withName(ingressName)
                .addToLabels(APP_LABEL, ingressName)
                .addToAnnotations(nginxAnnotationKey, nginxAnnotationValue)
                .endMetadata()
                .withNewSpec()
                .addNewRule()
                .withNewHttp()
                .addNewPath()
                .withPath("/" + serviceName + "(/|$)(.*)")
                .withPathType(PATH_TYPE)
                .withNewBackend()
                .withNewService()
                .withName(serviceName)
                .withNewPort()
                .withNumber(servicePort)
                .endPort()
                .endService()
                .endBackend()
                .endPath()
                .endHttp()
                .endRule()
                .endSpec()
                .build();
        return client.network().v1().ingresses().inNamespace(ingressNamespace).createOrReplace(ingress);
    }

    private Ingress addService2Ingress(Ingress ingress, String serviceName) {
        List<IngressRule> rules = ingress.getSpec().getRules();
        List<HTTPIngressPath> paths = rules.get(0).getHttp().getPaths();

        ServiceBackendPort serviceBackendPort = new ServiceBackendPort();
        serviceBackendPort.setNumber(servicePort);

        IngressServiceBackend ingressServiceBackend = new IngressServiceBackend();
        ingressServiceBackend.setName(serviceName);
        ingressServiceBackend.setPort(serviceBackendPort);

        IngressBackend ingressBackend = new IngressBackend();
        ingressBackend.setService(ingressServiceBackend);

        HTTPIngressPath newPath = new HTTPIngressPath();
        newPath.setPath("/" + serviceName);
        newPath.setPathType(PATH_TYPE);
        newPath.setBackend(ingressBackend);
        paths.add(newPath);

        return client.network().v1().ingresses()
                .inNamespace(ingressNamespace)
                .createOrReplace(ingress);
    }

    public IngressList getIngressList() {
        return client.network().v1().ingresses()
                .inNamespace(ingressNamespace)
                .withLabel(APP_LABEL, ingressName)
                .list();
    }

    public Deployment deployDeployment(String namespace, String serviceName, String image) {
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
                .withContainerPort(containerPort)
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

    public void deployService(String namespace, String serviceName) {
        ServicePort servicePort = new ServicePort();
        servicePort.setProtocol(DEFAULT_PROTOCOL);
        servicePort.setPort(this.servicePort);
        servicePort.setTargetPort(new IntOrString(this.containerPort));

        Map<String, String> selectors = new HashMap<>();
        selectors.put(APP_LABEL, serviceName);

        Service service = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .addToLabels(APP_LABEL, serviceName)
                .endMetadata()
                .withNewSpec()
                .withSelector(selectors)
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
