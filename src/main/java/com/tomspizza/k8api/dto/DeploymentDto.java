package com.tomspizza.k8api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tomspizza.k8api.util.DateUtil;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentDto extends K8sDto {
    private String image;
    private String deployed;
    private String uptime;
    private int replicas;
    private String url;

    public DeploymentDto(Deployment deployment, String url) {
        ObjectMeta metadata = deployment.getMetadata();
        setNamespace(metadata.getNamespace());
        setServiceName(metadata.getName());

        DeploymentSpec spec = deployment.getSpec();
        PodTemplateSpec template = spec.getTemplate();
        List<Container> containers = template.getSpec().getContainers();
        Container container = containers.get(0);
        this.image = container.getImage();
        this.deployed = metadata.getCreationTimestamp();
        this.url = (url == null ? null : String.join("/", url, metadata.getName()));

        DeploymentStatus status = deployment.getStatus();
        this.replicas = Objects.isNull(status.getReplicas()) ? 0 : status.getReplicas();

        this.uptime = DateUtil.getAge(metadata.getCreationTimestamp());
    }
}
