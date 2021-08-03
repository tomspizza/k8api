package com.tomspizza.k8api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScaleDto extends K8sDto {
    private int numberOfReplicas;
}
