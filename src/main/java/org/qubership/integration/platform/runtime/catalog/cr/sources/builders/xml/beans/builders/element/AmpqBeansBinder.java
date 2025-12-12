package org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders.element;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.stax2.XMLStreamWriter2;
import org.qubership.integration.platform.runtime.catalog.cr.sources.SourceBuilderContext;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.ElementBeansBuilder;
import org.qubership.integration.platform.runtime.catalog.cr.sources.builders.xml.beans.builders.element.helpers.MaasClassifierGetterHelper;
import org.qubership.integration.platform.runtime.catalog.model.constant.CamelNames;
import org.qubership.integration.platform.runtime.catalog.model.constant.CamelOptions;
import org.qubership.integration.platform.runtime.catalog.model.constant.ConnectionSourceType;
import org.qubership.integration.platform.runtime.catalog.model.system.EnvironmentSourceType;
import org.qubership.integration.platform.runtime.catalog.persistence.configs.entity.chain.element.ChainElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

import static org.qubership.integration.platform.runtime.catalog.model.constant.CamelNames.*;
import static org.qubership.integration.platform.runtime.catalog.model.constant.CamelOptions.*;

@Component
public class AmpqBeansBinder implements ElementBeansBuilder {
    private static final Set<String> RABBITMQ_ELEMENTS = Set.of(
                CamelNames.RABBITMQ_SENDER_COMPONENT,
                CamelNames.RABBITMQ_SENDER_2_COMPONENT,
                CamelNames.RABBITMQ_TRIGGER_COMPONENT,
                CamelNames.RABBITMQ_TRIGGER_2_COMPONENT
        );

    private final MaasClassifierGetterHelper maasClassifierGetterHelper;

    @Autowired
    public AmpqBeansBinder(
            MaasClassifierGetterHelper maasClassifierGetterHelper
    ) {
        this.maasClassifierGetterHelper = maasClassifierGetterHelper;
    }

    @Override
    public boolean applicableTo(ChainElement element) {
        String type = element.getType();
        return RABBITMQ_ELEMENTS.contains(type)
                || (
                        Set.of(ASYNC_API_TRIGGER_COMPONENT, SERVICE_CALL_COMPONENT).contains(type)
                        && OPERATION_PROTOCOL_TYPE_AMQP.equals(
                                element.getProperties().get(OPERATION_PROTOCOL_TYPE_PROP))
        );
    }

    @Override
    public void build(XMLStreamWriter2 streamWriter, ChainElement element, SourceBuilderContext context) throws Exception {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", element.getId());
        streamWriter.writeAttribute("type", "com.rabbitmq.client.MetricsCollector");
        streamWriter.writeAttribute("builderClass", "org.qubership.integration.platform.engine.util.builders.RabbitMQMetricsCollectorBuilder");
        streamWriter.writeAttribute("builderMethod", "build");

        streamWriter.writeStartElement("properties");

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "chainId");
        streamWriter.writeAttribute("value", element.getChain().getId());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "chainName");
        streamWriter.writeAttribute("value", element.getChain().getName());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "elementId");
        streamWriter.writeAttribute("value", element.getId());

        streamWriter.writeEmptyElement("property");
        streamWriter.writeAttribute("key", "elementName");
        streamWriter.writeAttribute("value", element.getName());

        String maasClassifier = getMaasClassifier(element);
        if (StringUtils.isNotBlank(maasClassifier)) {
            streamWriter.writeEmptyElement("property");
            streamWriter.writeAttribute("key", "maasClassifier");
            streamWriter.writeAttribute("value", maasClassifier);
        }

        streamWriter.writeEndElement();
        streamWriter.writeEndElement();
    }

    private String getMaasClassifier(ChainElement element) {
        return RABBITMQ_ELEMENTS.contains(element.getType())
            ? getMaasClassifierForAmpqElement(element)
            : maasClassifierGetterHelper.getMaasClassifierForServiceCallOrAsyncApiElement(element);
    }

    private static String getMaasClassifierForAmpqElement(ChainElement element) {
        String sourceType = element.getPropertyAsString(CamelOptions.CONNECTION_SOURCE_TYPE_PROP);
        return ConnectionSourceType.MAAS.toString().equals(sourceType)
                || EnvironmentSourceType.MAAS_BY_CLASSIFIER.toString().equals(sourceType)
                ? Optional.ofNullable(element.getPropertyAsString(CamelOptions.MAAS_VHOST_CLASSIFIER_NAME_PROP))
                .orElse(DEFAULT_VHOST_CLASSIFIER_NAME)
                : "";
    }
}
