package com.tomspizza.k8api.service;

import com.tomspizza.k8api.dto.DeployDto;
import com.tomspizza.k8api.dto.DeploymentDto;
import com.tomspizza.k8api.dto.K8sDto;
import com.tomspizza.k8api.dto.ScaleDto;
import com.tomspizza.k8api.exception.ServiceException;
import com.tomspizza.k8api.repository.KubernetesRepository;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class KubernetesService {

    private static final int MIN_POD = 1;
    private static final int MAX_POD = 10;

    @Value("${app.urlSchema}")
    private String urlSchema;

    private final KubernetesRepository kubernetesRepository;

    public List<DeploymentDto> getAllDeployments() {
        List<Deployment> deployments = kubernetesRepository.getDeployments();
        String url = kubernetesRepository.getIngressPublicUrl();
        String uri = (url == null ? null : String.format("%s://%s", urlSchema, url));
        return deployments.stream().map(d -> new DeploymentDto(d, uri)).collect(Collectors.toList());
    }

    public void deploy(DeployDto deployDto) {
        log.info("Deploying pod");
        kubernetesRepository.getOrCreateNamespace(deployDto.getNamespace());

        kubernetesRepository.deployDeployment(deployDto.getNamespace(),
                deployDto.getServiceName(),
                deployDto.getImage());

        log.info("Exposing to service");
        kubernetesRepository.deployService(deployDto.getNamespace(),
                deployDto.getServiceName());

        log.info("Exposing to ingress");
        kubernetesRepository.register2Ingress(deployDto.getServiceName());
    }

    public void scale(ScaleDto scaleDto) {
        int numberOfReplicas = scaleDto.getNumberOfReplicas();
        if (numberOfReplicas < MIN_POD || numberOfReplicas > MAX_POD) {
            throw new ServiceException(String.format("Number of replicas should be in %s and %s", MIN_POD, MAX_POD));
        }
        kubernetesRepository.scalePod(scaleDto.getNamespace(), scaleDto.getServiceName(), numberOfReplicas);
    }

    public void delete(K8sDto k8sDto) {
        log.info("Remove deployment");
        kubernetesRepository.deleteDeployment(k8sDto.getNamespace(), k8sDto.getServiceName());

        log.info("Remove service");
        kubernetesRepository.deleteService(k8sDto.getNamespace(), k8sDto.getServiceName());

        log.info("Unregister of ingress");
        kubernetesRepository.unregister2Ingress(k8sDto.getServiceName());
    }
}
