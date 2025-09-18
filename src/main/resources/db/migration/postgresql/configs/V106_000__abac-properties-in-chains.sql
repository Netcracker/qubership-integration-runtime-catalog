UPDATE catalog.elements
SET properties = jsonb_set(properties, '{abacResourceType}', '"CHAIN"')
WHERE type = 'http-trigger' AND properties->>'abacResourceType' IS NULL;


UPDATE catalog.elements
SET properties = jsonb_set(properties, '{abacOperation}', '"ALL"')
WHERE type = 'http-trigger' AND properties->>'abacOperation' IS NULL;

UPDATE catalog.elements
SET properties = jsonb_set(properties, '{abacResourceDataType}', '"String"')
WHERE type = 'http-trigger' AND properties->>'abacResourceDataType' IS NULL;
