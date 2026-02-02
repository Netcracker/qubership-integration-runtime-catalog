package org.qubership.integration.platform.runtime.catalog.cr.builders;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuildContext;
import org.qubership.integration.platform.runtime.catalog.cr.ResourceBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.naming.NamingStrategy;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.Chain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceMonitorBuilder implements ResourceBuilder<List<Chain>> {
    private static final String TEMPLATE_NAME = "service-monitor";

    @Data
    @Builder
    private static class TemplateData {
        private String name;
        private String integrationName;
        private String serviceName;
        private String interval;
    }

    private final Handlebars templates;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> serviceNamingStrategy;
    private final NamingStrategy<ResourceBuildContext<List<Chain>>> serviceMonitorNamingStrategy;

    @Autowired
    public ServiceMonitorBuilder(
            Handlebars templates,
            @Qualifier("integrationResourceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> integrationResourceNamingStrategy,
            @Qualifier("serviceNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> serviceNamingStrategy,
            @Qualifier("serviceMonitorNamingStrategy") NamingStrategy<ResourceBuildContext<List<Chain>>> serviceMonitorNamingStrategy
    ) {
        this.templates = templates;
        this.integrationResourceNamingStrategy = integrationResourceNamingStrategy;
        this.serviceNamingStrategy = serviceNamingStrategy;
        this.serviceMonitorNamingStrategy = serviceMonitorNamingStrategy;
    }

    @Override
    public boolean enabled(ResourceBuildContext<List<Chain>> context) {
        return context.getBuildInfo().getOptions().getMonitoring().isEnabled();
    }

    @Override
    public String build(ResourceBuildContext<List<Chain>> context) throws Exception {
        TemplateData templateData = buildTemplateData(context);
        Context templateContext = Context.newContext(templateData);
        Template template = templates.compile(TEMPLATE_NAME);
        return template.apply(templateContext);
    }

    private TemplateData buildTemplateData(ResourceBuildContext<List<Chain>> context) {
        return TemplateData.builder()
                .name(serviceMonitorNamingStrategy.getName(context))
                .integrationName(integrationResourceNamingStrategy.getName(context))
                .serviceName(serviceNamingStrategy.getName(context))
                .interval(getMetricsScrapeInterval(context))
                .build();
    }

    private String getMetricsScrapeInterval(ResourceBuildContext<List<Chain>> context) {
        String interval = context.getBuildInfo().getOptions().getMonitoring().getInterval();
        return StringUtils.isBlank(interval)
                ? "{{ .Values.monitoring.interval | default \"30s\" }}"
                : interval;
    }
}
