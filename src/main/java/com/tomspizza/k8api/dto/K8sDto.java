package com.tomspizza.k8api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class K8sDto {
    private String namespace;
    private String serviceName;
}
