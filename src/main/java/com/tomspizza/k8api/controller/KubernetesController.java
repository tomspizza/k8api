package com.tomspizza.k8api.controller;

import com.tomspizza.k8api.dto.DeployDto;
import com.tomspizza.k8api.dto.DeploymentDto;
import com.tomspizza.k8api.dto.K8sDto;
import com.tomspizza.k8api.dto.ScaleDto;
import com.tomspizza.k8api.service.KubernetesService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/k8s")
public class KubernetesController {

    private final KubernetesService kubernetesService;

    public KubernetesController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    @ResponseBody
    @GetMapping("/list")
    public List<DeploymentDto> list() {
        return kubernetesService.getAllDeployments();
    }

    @ResponseBody
    @PostMapping("/deploy")
    public String deploy(@RequestBody DeployDto deployDto) {
        kubernetesService.deploy(deployDto);
        return "Success";
    }

    @ResponseBody
    @PostMapping("/scale")
    public String scale(@RequestBody ScaleDto scaleDto) {
        kubernetesService.scale(scaleDto);
        return "Success";
    }

    @ResponseBody
    @PostMapping("/delete")
    public String delete(@RequestBody K8sDto k8sDto) {
        kubernetesService.delete(k8sDto);
        return "Success";
    }
}
