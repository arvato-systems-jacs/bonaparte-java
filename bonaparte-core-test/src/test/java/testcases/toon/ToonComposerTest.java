package testcases.toon;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.jpaw.bonaparte.core.BonaparteToonComposer;
import de.jpaw.bonaparte.pojos.jsonTest.ColorAlnum;
import de.jpaw.bonaparte.pojos.jsonTest.ColorNum;
import de.jpaw.bonaparte.pojos.jsonTest.JsonEnumAndList;
import de.jpaw.bonaparte.pojos.jsonTest.JsonFieldTest;
import de.jpaw.bonaparte.pojos.jsonTest.XColor;

/**
 * Test cases for BonaparteToonComposer.
 * Tests serialization of various data types to TOON format.
 */
public class ToonComposerTest {
    
    @Test
    public void testSimpleObject() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("mytext");
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Simple object result:");
        System.out.println(result);
        
        // Verify it contains the field
        Assertions.assertTrue(result.contains("text:"));
        Assertions.assertTrue(result.contains("\"mytext\""));
    }
    
    @Test
    public void testWithDateField() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("mytext");
        t.setVarField(LocalDate.of(2015, 10, 31));
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Date field result:");
        System.out.println(result);
        
        // Verify date is formatted correctly
        Assertions.assertTrue(result.contains("2015-10-31"));
    }
    
    @Test
    public void testWithList() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("mytext");
        
        List<Object> l = new ArrayList<Object>(20);
        t.setVarList(l);
        l.add(42);
        l.add(3.14);
        l.add('x');
        l.add("Hello, world");
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("List field result:");
        System.out.println(result);
        
        // Verify TOON format: varList[4]: value1,value2,...
        Assertions.assertTrue(result.contains("text:"));
        Assertions.assertTrue(result.contains("varList[4]:"));
        Assertions.assertTrue(result.contains("42"));
        Assertions.assertTrue(result.contains("3.14"));
        Assertions.assertTrue(result.contains("\"x\""));
        Assertions.assertTrue(result.contains("\"Hello, world\""));
    }
    
    @Test
    public void testEnumSerialization() throws Exception {
        JsonEnumAndList t = new JsonEnumAndList();
        t.setCn(ColorNum.GREEN);
        t.setCa(ColorAlnum.GREEN);
        t.setCx(XColor.myFactory.getByName("RED"));
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Enum serialization result:");
        System.out.println(result);
        
        // Verify enums are present (with ordinals by default)
        Assertions.assertTrue(result.contains("cn:"));
        Assertions.assertTrue(result.contains("ca:"));
        Assertions.assertTrue(result.contains("cx:"));
    }
    
    @Test
    public void testEnumWithTokens() throws Exception {
        JsonEnumAndList t = new JsonEnumAndList();
        t.setCn(ColorNum.GREEN);
        t.setCa(ColorAlnum.GREEN);
        t.setCx(XColor.myFactory.getByName("RED"));
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.setWriteEnumOrdinals(false);
        composer.setWriteEnumTokens(true);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Enum with tokens result:");
        System.out.println(result);
        
        // Verify enum names/tokens are quoted strings
        Assertions.assertTrue(result.contains("\""));
    }
    
    @Test
    public void testTemporalTypes() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("test");
        
        // Test with various temporal types stored in varField
        LocalDate date = LocalDate.of(2025, 1, 15);
        t.setVarField(date);
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Temporal types result:");
        System.out.println(result);
        
        // Verify ISO-8601 date format
        Assertions.assertTrue(result.contains("2025-01-15"));
    }
    
    @Test
    public void testNumberFormatting() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("test");
        
        // Create a list with various number types
        List<Object> l = new ArrayList<Object>();
        l.add(42);           // integer
        l.add(3.14);         // double
        l.add(1.0);          // should be formatted as "1" not "1.0"
        l.add(0.000001);     // small decimal
        t.setVarList(l);
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Number formatting result:");
        System.out.println(result);
        
        // Verify numbers are in plain format (no scientific notation)
        Assertions.assertFalse(result.contains("e"), "Should not contain scientific notation");
        Assertions.assertFalse(result.contains("E"), "Should not contain scientific notation");
    }
    
    @Test
    public void testStringQuoting() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        // Strings should always be quoted in TOON format
        t.setText("simple text");
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("String quoting result:");
        System.out.println(result);
        
        // Verify strings are always quoted
        Assertions.assertTrue(result.contains("\"simple text\""));
    }
    
    @Test
    public void testNullHandling() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText(null);  // null string
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Null handling result:");
        System.out.println(result);
        
        // Verify null is represented correctly
        Assertions.assertTrue(result.contains("null"));
    }
    
    @Test
    public void testBooleanSerialization() throws Exception {
        JsonEnumAndList t = new JsonEnumAndList();
        
        List<Object> l = new ArrayList<Object>();
        l.add(true);
        l.add(false);
        l.add(true);
        t.setAny(l);
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Boolean serialization result:");
        System.out.println(result);
        
        // Verify booleans are lowercase
        Assertions.assertTrue(result.contains("true"));
        Assertions.assertTrue(result.contains("false"));
    }
    
    @Test
    public void testToToonStringHelper() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("test via helper");
        
        String result = BonaparteToonComposer.toToonString(t);
        System.out.println("Helper method result:");
        System.out.println(result);
        
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("\"test via helper\""));
    }
    
    @Test
    public void testEmptyList() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("test");
        t.setVarList(new ArrayList<Object>());
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("Empty list result:");
        System.out.println(result);
        
        // Verify empty list format: varList[0]:
        Assertions.assertTrue(result.contains("varList[0]:"));
    }
    
    @Test
    public void testListWithNull() throws Exception {
        JsonFieldTest t = new JsonFieldTest();
        t.setText("test");
        
        List<Object> l = new ArrayList<Object>();
        l.add("first");
        l.add(null);
        l.add("last");
        t.setVarList(l);
        
        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);
        
        String result = sb.toString();
        System.out.println("List with null result:");
        System.out.println(result);
        
        // Verify list contains null
        Assertions.assertTrue(result.contains("varList[3]:"));
        Assertions.assertTrue(result.contains("null"));
        Assertions.assertTrue(result.contains("\"first\""));
        Assertions.assertTrue(result.contains("\"last\""));
    }
    
    @Test
    public void testNullObjectToString() {
        String result = BonaparteToonComposer.toToonString(null);
        Assertions.assertNull(result);
    }
}
