UPDATE catalog.elements e
SET properties = jsonb_set(
        jsonb_set(
                jsonb_set(
                        e.properties,
                        '{abacParameters, resourceType}',
                        '"CHAIN"',
                        true
                    ),
                '{abacParameters, operation}',
                '"ALL"',
                true
            ),
        '{abacParameters, resourceDataType}',
        '"String"',
        true
    )
WHERE e.type = 'trigger'
  AND e.properties->>'accessControlType' = 'ABAC'
  AND (
    e.properties->'abacParameters'->>'resourceType' IS NULL
   OR e.properties->'abacParameters'->>'operation' IS NULL
   OR e.properties->'abacParameters'->>'resourceDataType' IS NULL
    );

