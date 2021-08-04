package com.tomspizza.k8api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeployDto extends K8sDto {
    private String image;
    private int servicePort;
}
