/*
 * Copyright 2024 Michael Bischoff
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jpaw.bonaparte.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.jpaw.bonaparte.pojos.jsonschema.JsonSchema;
import de.jpaw.bonaparte.pojos.meta.AlphanumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BasicNumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BinaryElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.ClassDefinition;
import de.jpaw.bonaparte.pojos.meta.EnumDataItem;
import de.jpaw.bonaparte.pojos.meta.EnumDefinition;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.MiscElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.Multiplicity;
import de.jpaw.bonaparte.pojos.meta.ObjectReference;
import de.jpaw.bonaparte.pojos.meta.TemporalElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.XEnumDataItem;

/**
 * Generates a JSON Schema representation (as {@link JsonSchema} Bonaparte objects)
 * from Bonaparte class metadata ({@link BonaPortableClass} / {@link ClassDefinition}).
 *
 * <p>The generated schema follows JSON Schema Draft 2020-12 conventions.
 * Referenced sub-classes are placed in the {@code $defs} section of the root schema.</p>
 *
 * <p>Usage example:</p>
 * <pre>
 *     BonaPortableClass&lt;?&gt; bpc = FieldGetter.getBClass(MyClass.class);
 *     JsonSchema schema = JsonSchemaGenerator.generateSchema(bpc);
 * </pre>
 *
 * @author Michael Bischoff
 */
public class JsonSchemaGenerator {

    /** The definitions map, collecting sub-schemas keyed by their class name. */
    private final Map<String, JsonSchema> definitions = new LinkedHashMap<>();

    /**
     * Generates a JSON Schema for the given Bonaparte class.
     *
     * @param bpc the Bonaparte class descriptor
     * @return the root {@link JsonSchema} with {@code $defs} populated for all referenced sub-classes
     */
    public static JsonSchema generateSchema(BonaPortableClass<?> bpc) {
        return generateSchema(bpc.getMetaData());
    }

    /**
     * Generates a JSON Schema for the given Bonaparte class definition.
     *
     * @param classDef the Bonaparte class metadata
     * @return the root {@link JsonSchema} with {@code $defs} populated for all referenced sub-classes
     */
    public static JsonSchema generateSchema(ClassDefinition classDef) {
        JsonSchemaGenerator generator = new JsonSchemaGenerator();
        JsonSchema root = generator.processClass(classDef);
        if (!generator.definitions.isEmpty()) {
            root.setDefs(generator.definitions);
        }
        return root;
    }

    /**
     * Processes a class definition and returns its JSON Schema representation.
     * Any referenced sub-classes are added to {@link #definitions}.
     *
     * @param classDef the class metadata to process
     * @return the JSON Schema for the given class
     */
    private JsonSchema processClass(ClassDefinition classDef) {
        JsonSchema schema = new JsonSchema();
        schema.setTitle(classDef.getName());
        schema.setType("object");

        Map<String, JsonSchema> properties = new LinkedHashMap<>();
        List<String> requiredProperties = new ArrayList<>();

        // Collect fields from parent class first, then own fields
        collectFields(classDef, properties, requiredProperties);

        if (!properties.isEmpty()) {
            schema.setProperties(properties);
        }
        if (!requiredProperties.isEmpty()) {
            schema.setRequiredProperties(requiredProperties);
        }
        schema.setAdditionalProperties(Boolean.FALSE);

        return schema;
    }

    /**
     * Recursively collects fields from the class hierarchy (parent first) into the properties map.
     *
     * @param classDef            the class whose fields to collect
     * @param properties          accumulator for field schemas, keyed by field name
     * @param requiredProperties  accumulator for required field names
     */
    private void collectFields(ClassDefinition classDef, Map<String, JsonSchema> properties, List<String> requiredProperties) {
        if (classDef == null) {
            return;
        }
        // Include parent fields first
        if (classDef.getParentMeta() != null) {
            collectFields(classDef.getParentMeta(), properties, requiredProperties);
        }
        // Then own fields
        for (FieldDefinition field : classDef.getFields()) {
            JsonSchema fieldSchema = processField(field);
            properties.put(field.getName(), fieldSchema);
            if (field.getIsRequired()) {
                requiredProperties.add(field.getName());
            }
        }
    }

    /**
     * Processes a single field definition and returns its JSON Schema.
     * Handles multiplicity (arrays, lists, sets, maps).
     *
     * @param field the field metadata
     * @return the JSON Schema for this field
     */
    private JsonSchema processField(FieldDefinition field) {
        JsonSchema baseSchema = processFieldBaseType(field);

        switch (field.getMultiplicity()) {
        case ARRAY:
        case LIST:
        case SET:
            JsonSchema arraySchema = new JsonSchema();
            arraySchema.setType("array");
            arraySchema.setItems(baseSchema);
            if (field.getMinCount() != null && field.getMinCount() > 0) {
                arraySchema.setMinItems(field.getMinCount());
            }
            if (field.getMaxCount() != null && field.getMaxCount() > 0) {
                arraySchema.setMaxItems(field.getMaxCount());
            }
            if (field.getMultiplicity() == Multiplicity.SET) {
                arraySchema.setUniqueItems(Boolean.TRUE);
            }
            return arraySchema;
        case MAP:
            JsonSchema mapSchema = new JsonSchema();
            mapSchema.setType("object");
            mapSchema.setAdditionalProperties(Boolean.TRUE);
            return mapSchema;
        default:
            // SCALAR - return as-is
            return baseSchema;
        }
    }

    /**
     * Determines the JSON Schema for the base (scalar) type of a field.
     * The instanceof checks are ordered from most-specific to least-specific
     * subclass to ensure correct dispatch.
     *
     * @param field the field metadata
     * @return the JSON Schema representing the scalar type
     */
    private JsonSchema processFieldBaseType(FieldDefinition field) {
        JsonSchema schema = new JsonSchema();

        if (field instanceof ObjectReference) {
            ObjectReference ref = (ObjectReference) field;
            ClassDefinition bound = ref.getLowerBound();
            if (bound != null) {
                String refName = bound.getName();
                schema.setRef("#/$defs/" + refName);
                // Add sub-class to definitions if not already present (prevents infinite recursion)
                if (!definitions.containsKey(refName)) {
                    // Mark as in-progress before recursing to handle circular references
                    definitions.put(refName, new JsonSchema());
                    JsonSchema subSchema = processClass(bound);
                    definitions.put(refName, subSchema);
                }
            } else {
                schema.setType("object");
            }
            return schema;
        }

        if (field instanceof EnumDataItem) {
            EnumDataItem enumField = (EnumDataItem) field;
            schema.setType("string");
            List<String> enumValues = buildEnumValues(enumField.getBaseEnum());
            if (!enumValues.isEmpty()) {
                schema.setEnumValues(enumValues);
            }
            return schema;
        }

        if (field instanceof XEnumDataItem) {
            XEnumDataItem xenumField = (XEnumDataItem) field;
            schema.setType("string");
            List<String> enumValues = buildEnumValues(xenumField.getBaseXEnum().getBaseEnum());
            if (!enumValues.isEmpty()) {
                schema.setEnumValues(enumValues);
            }
            return schema;
        }

        // AlphanumericElementaryDataItem also covers AlphanumericEnumSetDataItem and XEnumSetDataItem
        if (field instanceof AlphanumericElementaryDataItem) {
            AlphanumericElementaryDataItem alphaField = (AlphanumericElementaryDataItem) field;
            schema.setType("string");
            if (alphaField.getLength() > 0) {
                schema.setMaxLength(alphaField.getLength());
            }
            if (alphaField.getMinLength() > 0) {
                schema.setMinLength(alphaField.getMinLength());
            }
            if (alphaField.getRegexp() != null && !alphaField.getRegexp().isEmpty()) {
                schema.setPattern(alphaField.getRegexp());
            }
            return schema;
        }

        if (field instanceof TemporalElementaryDataItem) {
            schema.setType("string");
            switch (field.getBonaparteType()) {
            case "day":
                schema.setFormat("date");
                break;
            case "time":
                schema.setFormat("time");
                break;
            default:
                // timestamp and instant both map to date-time
                schema.setFormat("date-time");
                break;
            }
            return schema;
        }

        // BasicNumericElementaryDataItem also covers NumericElementaryDataItem and NumericEnumSetDataItem
        if (field instanceof BasicNumericElementaryDataItem) {
            BasicNumericElementaryDataItem numField = (BasicNumericElementaryDataItem) field;
            schema.setType(numField.getDecimalDigits() == 0 ? "integer" : "number");
            return schema;
        }

        if (field instanceof MiscElementaryDataItem) {
            switch (field.getBonaparteType()) {
            case "boolean":
                schema.setType("boolean");
                break;
            case "uuid":
                schema.setType("string");
                schema.setFormat("uuid");
                break;
            case "char":
                schema.setType("string");
                schema.setMaxLength(1);
                schema.setMinLength(1);
                break;
            default:
                schema.setType("string");
                break;
            }
            return schema;
        }

        if (field instanceof BinaryElementaryDataItem) {
            schema.setType("string");
            schema.setFormat("byte");
            return schema;
        }

        // Fallback for any unhandled types
        schema.setType("string");
        return schema;
    }

    /**
     * Builds the list of enum value strings from an {@link EnumDefinition}.
     * Uses tokens if available and non-empty, otherwise falls back to IDs.
     *
     * @param enumDef the enum metadata
     * @return list of enum value strings
     */
    private static List<String> buildEnumValues(EnumDefinition enumDef) {
        List<String> result = new ArrayList<>();
        List<String> tokens = enumDef.getTokens();
        if (tokens != null) {
            for (String token : tokens) {
                if (token != null && !token.isEmpty()) {
                    result.add(token);
                }
            }
        }
        if (result.isEmpty()) {
            // Fall back to symbolic IDs
            List<String> ids = enumDef.getIds();
            if (ids != null) {
                result.addAll(ids);
            }
        }
        return result;
    }
}
