package com.tomspizza.k8api.dto;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class DeploymentDto extends K8sDto {
    //    private String serviceUrl;
    private String image;
    private String deployed;
    private int replicas;


    public DeploymentDto(Deployment deployment) {
        var metadata = deployment.getMetadata();
        setNamespace(metadata.getNamespace());
        setServiceName(metadata.getName());

        var spec = deployment.getSpec();
        var template = spec.getTemplate();
        var containers = template.getSpec().getContainers();
        var container = containers.get(0);
        this.image = container.getImage();

        this.deployed = metadata.getCreationTimestamp();

        var status = deployment.getStatus();
        this.replicas = Objects.isNull(status.getReplicas()) ? 0 : status.getReplicas();
    }
}
