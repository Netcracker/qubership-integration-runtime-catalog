UPDATE catalog.elements e
SET properties =
        jsonb_set(
               e.properties,
               '{abacParameters, resourceType}',
               '"CHAIN"'
            )
WHERE e.type = 'http-trigger'
  AND e.accessControlType = 'ABAC'
  AND e.properties->'abacParameters'->>'resourceType' IS NULL;


UPDATE catalog.elements e
SET properties =
        jsonb_set(
                e.properties,
                '{abacParameters, operation}',
                '"CHAIN"'
            )
WHERE e.type = 'http-trigger'
  AND e.accessControlType = 'ABAC'
  AND e.properties->'abacParameters'->>'operation' IS NULL;


UPDATE catalog.elements e
SET properties =
        jsonb_set(
                e.properties,
                '{abacParameters, resourceDataType}',
                '"CHAIN"'
            )
WHERE e.type = 'http-trigger'
  AND e.accessControlType = 'ABAC'
  AND e.properties->'abacParameters'->>'resourceDataType' IS NULL;

