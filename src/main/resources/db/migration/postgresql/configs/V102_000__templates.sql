CREATE TABLE templates
    (
        dtype               VARCHAR(31),
        id                  VARCHAR(255)          NOT NULL
            CONSTRAINT pk_templates
                PRIMARY KEY,
        created_when        TIMESTAMP,
        description         VARCHAR(255),
        modified_when       TIMESTAMP,
        name                VARCHAR(255),
        properties          JSONB,
        type                VARCHAR(255),
        created_by_id       VARCHAR(255),
        created_by_name     VARCHAR(255),
        modified_by_id      VARCHAR(255),
        modified_by_name    VARCHAR(255),
        CONSTRAINT uk_templates
            UNIQUE (name)
    );