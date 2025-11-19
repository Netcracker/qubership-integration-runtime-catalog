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
import static org.qubership.integration.platform.runtime.catalog.model.constant.CamelNames.OPERATION_PROTOCOL_TYPE_PROP;

@Component
public class KafkaBeansBinder implements ElementBeansBuilder {
    private static final Set<String> KAFKA_ELEMENTS = Set.of(
            CamelNames.KAFKA_SENDER_COMPONENT,
            CamelNames.KAFKA_SENDER_2_COMPONENT,
            CamelNames.KAFKA_TRIGGER_COMPONENT,
            CamelNames.KAFKA_TRIGGER_2_COMPONENT
    );

    private final MaasClassifierGetterHelper maasClassifierGetterHelper;

    @Autowired
    public KafkaBeansBinder(
            MaasClassifierGetterHelper maasClassifierGetterHelper
    ) {
        this.maasClassifierGetterHelper = maasClassifierGetterHelper;
    }

    @Override
    public boolean applicableTo(ChainElement element) {
        String type = element.getType();
        return KAFKA_ELEMENTS.contains(type)
                || (
                Set.of(ASYNC_API_TRIGGER_COMPONENT, SERVICE_CALL_COMPONENT).contains(type)
                        && OPERATION_PROTOCOL_TYPE_KAFKA.equals(
                        element.getProperties().get(OPERATION_PROTOCOL_TYPE_PROP))
        );
    }

    @Override
    public void build(XMLStreamWriter2 streamWriter, ChainElement element, SourceBuilderContext context) throws Exception {
        addKafkaClientFactoryBean(streamWriter, element.getId(), element);
        addKafkaClientFactoryBean(streamWriter, element.getId() + "-v2", element);
    }

    private void addKafkaClientFactoryBean(XMLStreamWriter2 streamWriter, String name, ChainElement element) throws Exception {
        streamWriter.writeStartElement("bean");
        streamWriter.writeAttribute("name", name);
        streamWriter.writeAttribute("type", "org.apache.camel.component.kafka.KafkaClientFactory");
        streamWriter.writeAttribute("builderClass", "org.qubership.integration.platform.engine.util.builders.KafkaClientFactoryBuilder");
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
        return KAFKA_ELEMENTS.contains(element.getType())
                ? getMaasClassifierForAmpqElement(element)
                : maasClassifierGetterHelper.getMaasClassifierForServiceCallOrAsyncApiElement(element);
    }

    private static String getMaasClassifierForAmpqElement(ChainElement element) {
        String sourceType = element.getPropertyAsString(CamelOptions.CONNECTION_SOURCE_TYPE_PROP);
        return ConnectionSourceType.MAAS.toString().equals(sourceType)
                || EnvironmentSourceType.MAAS_BY_CLASSIFIER.toString().equals(sourceType)
                ? Optional.ofNullable(element.getPropertyAsString(CamelOptions.MAAS_TOPICS_CLASSIFIER_NAME_PROP))
                .orElse("")
                : "";
    }
}
