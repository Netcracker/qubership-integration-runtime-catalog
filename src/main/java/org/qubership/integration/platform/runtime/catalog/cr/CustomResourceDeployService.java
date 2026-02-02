package org.qubership.integration.platform.runtime.catalog.cr;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.util.ModelMapper;
import io.kubernetes.client.util.Yaml;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.qubership.integration.platform.runtime.catalog.cr.k8s.CamelKIntegration;
import org.qubership.integration.platform.runtime.catalog.cr.k8s.CamelKIntegrationList;
import org.qubership.integration.platform.runtime.catalog.kubernetes.KubeOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CustomResourceDeployService {
    private final KubeOperator kubeOperator;

    @Autowired
    public CustomResourceDeployService(
            KubeOperator kubeOperator
    ) {
        this.kubeOperator = kubeOperator;
    }

    @PostConstruct
    public void init() {
        ModelMapper.addModelMap("camel.apache.org", "v1", "Integration", "Integrations", CamelKIntegration.class, CamelKIntegrationList.class);
    }

    public void deploy(String resourceText) throws CustomResourceDeployError {
        try {
            List<Object> resources = Yaml.loadAll(resourceText);
            for (Object resource : resources) {
                kubeOperator.createOrUpdateResource(resource);
            }
        } catch (ApiException e) {
            log.error("Failed to create or update resource: {}", e.getResponseBody());
            throw new CustomResourceDeployError("Failed to deploy resources", e);
        } catch (Exception exception) {
            log.error("Failed to create or update resource", exception);
            throw new CustomResourceDeployError("Failed to deploy resources", exception);
        }
    }
}
