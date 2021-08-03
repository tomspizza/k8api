package com.tomspizza.k8api.service;

import com.tomspizza.k8api.dto.DeployDto;
import com.tomspizza.k8api.dto.DeploymentDto;
import com.tomspizza.k8api.dto.K8sDto;
import com.tomspizza.k8api.dto.ScaleDto;
import com.tomspizza.k8api.exception.ServiceException;
import com.tomspizza.k8api.repository.KubernetesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class KubernetesService {

    private static final int MIN_POD = 0;
    private static final int MAX_POD = 10;

    private final KubernetesRepository kubernetesRepository;

    public List<DeploymentDto> getAllServices() {
        var deployments = kubernetesRepository.getDeployments();
        return deployments.stream().map(DeploymentDto::new).collect(Collectors.toList());
    }

    public void deploy(DeployDto deployDto) {
        kubernetesRepository.deploy(deployDto.getNamespace(), deployDto.getServiceName(), deployDto.getImage());
    }

    public void scale(ScaleDto scaleDto) {
        var numberOfReplicas = scaleDto.getNumberOfReplicas();
        if (numberOfReplicas < MIN_POD || numberOfReplicas > MAX_POD) {
            throw new ServiceException(String.format("Number of replicas should be in %s and %s", MIN_POD, MAX_POD));
        }
        kubernetesRepository.scale(scaleDto.getNamespace(), scaleDto.getServiceName(), numberOfReplicas);
    }

    public void delete(K8sDto k8sDto) {
        kubernetesRepository.delete(k8sDto.getNamespace(), k8sDto.getServiceName());
    }
}
