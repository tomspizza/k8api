package com.tomspizza.k8api.controller;

import com.tomspizza.k8api.service.KubernetesService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/k8s")
public class KubernetesController {

    private final KubernetesService kubernetesService;

    public KubernetesController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }
}
