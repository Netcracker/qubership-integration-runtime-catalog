/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.integration.platform.runtime.catalog.configuration.datasource;

import org.qubership.integration.platform.runtime.catalog.configuration.datasource.properties.HikariConfigProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;

@Configuration
@ConditionalOnMissingBean(value = DataSource.class, name = "configsDataSource")
@EnableConfigurationProperties(HikariConfigProperties.class)
@EnableJpaRepositories(basePackages = {
        "org.qubership.integration.platform.catalog.persistence.configs.repository",
        "org.qubership.integration.platform.runtime.catalog.persistence.configs.repository"
})
@EntityScan(basePackages = {
        "org.qubership.integration.platform.catalog.persistence.configs.entity",
        "org.qubership.integration.platform.runtime.catalog.persistence.configs.entity"
})
public class PersistenceConfiguration {

    private final HikariConfigProperties properties;

    @Autowired
    public PersistenceConfiguration(HikariConfigProperties properties) {
        this.properties = properties;
    }

    @Primary
    @Bean("configsDataSource")
    public DataSource configsDataSource() {
        return new HikariDataSource(properties.getDatasource("configs-datasource"));
    }
}
