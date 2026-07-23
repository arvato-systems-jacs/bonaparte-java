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
package de.jpaw.bonaparte.core.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.jpaw.bonaparte.core.JsonSchemaGenerator;
import de.jpaw.bonaparte.pojos.jsonschema.JsonSchema;
import de.jpaw.bonaparte.pojos.jsonschema.JsonSchemaType;
import de.jpaw.bonaparte.pojos.meta.BundleInformation;

public class JsonSchemaGeneratorTest {

    @Test
    public void testGenerateSchemaNotNull() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        Assertions.assertNotNull(schema, "Generated schema must not be null");
    }

    @Test
    public void testGenerateSchemaTypeIsObject() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        Assertions.assertEquals(JsonSchemaType.OBJECT, schema.getType(), "Root schema type must be 'object'");
    }

    @Test
    public void testGenerateSchemaTitleMatchesClassName() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        Assertions.assertEquals("BundleInformation", schema.getTitle(), "Root schema title must match class name");
    }

    @Test
    public void testGenerateSchemaAdditionalPropertiesFalse() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        Assertions.assertEquals(Boolean.FALSE, schema.getAdditionalProperties(),
                "additionalProperties must be false");
    }

    @Test
    public void testGenerateSchemaRequiredFields() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        Assertions.assertNotNull(schema.getRequiredProperties(), "Required properties list must not be null");
        Assertions.assertTrue(schema.getRequiredProperties().contains("name"), "'name' must be required");
        Assertions.assertTrue(schema.getRequiredProperties().contains("packages"), "'packages' must be required");
        Assertions.assertTrue(schema.getRequiredProperties().contains("classPath"), "'classPath' must be required");
        Assertions.assertTrue(schema.getRequiredProperties().contains("bundleStatus"), "'bundleStatus' must be required");
        Assertions.assertFalse(schema.getRequiredProperties().contains("whenStatusChanged"),
                "'whenStatusChanged' must not be required (it is optional)");
    }

    @Test
    public void testGenerateSchemaPropertiesExist() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        Assertions.assertNotNull(schema.getProperties(), "Properties map must not be null");
        Assertions.assertTrue(schema.getProperties().containsKey("name"), "Properties must include 'name'");
        Assertions.assertTrue(schema.getProperties().containsKey("bundleStatus"), "Properties must include 'bundleStatus'");
        Assertions.assertTrue(schema.getProperties().containsKey("whenStatusChanged"),
                "Properties must include 'whenStatusChanged'");
    }

    @Test
    public void testGenerateSchemaStringTypeForName() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        JsonSchema nameSchema = schema.getProperties().get("name");
        Assertions.assertNotNull(nameSchema, "Schema for 'name' must not be null");
        Assertions.assertEquals(JsonSchemaType.STRING, nameSchema.getType(), "Type of 'name' must be 'string'");
    }

    @Test
    public void testGenerateSchemaArrayTypeForPackages() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        JsonSchema packagesSchema = schema.getProperties().get("packages");
        Assertions.assertNotNull(packagesSchema, "Schema for 'packages' must not be null");
        Assertions.assertEquals(JsonSchemaType.ARRAY, packagesSchema.getType(), "Type of 'packages' must be 'array'");
        Assertions.assertNotNull(packagesSchema.getItems(), "Array items schema must not be null");
        Assertions.assertEquals(JsonSchemaType.STRING, packagesSchema.getItems().getType(),
                "Type of array items for 'packages' must be 'string'");
    }

    @Test
    public void testGenerateSchemaDateTimeFormatForInstant() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        JsonSchema instantSchema = schema.getProperties().get("whenStatusChanged");
        Assertions.assertNotNull(instantSchema, "Schema for 'whenStatusChanged' must not be null");
        Assertions.assertEquals(JsonSchemaType.STRING, instantSchema.getType(),
                "Type of 'whenStatusChanged' must be 'string'");
        Assertions.assertEquals("date-time", instantSchema.getFormat(),
                "Format of 'whenStatusChanged' must be 'date-time'");
    }

    @Test
    public void testGenerateSchemaEnumValues() {
        JsonSchema schema = JsonSchemaGenerator.generateSchema(BundleInformation.BClass.INSTANCE);
        JsonSchema enumSchema = schema.getProperties().get("bundleStatus");
        Assertions.assertNotNull(enumSchema, "Schema for 'bundleStatus' must not be null");
        Assertions.assertEquals(JsonSchemaType.STRING, enumSchema.getType(), "Type of 'bundleStatus' must be 'string'");
        Assertions.assertNotNull(enumSchema.getEnumValues(),
                "Enum values for 'bundleStatus' must not be null");
        Assertions.assertFalse(enumSchema.getEnumValues().isEmpty(),
                "Enum values for 'bundleStatus' must not be empty");
    }
}
