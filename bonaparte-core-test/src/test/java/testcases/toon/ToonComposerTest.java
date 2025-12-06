package testcases.toon;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.jpaw.bonaparte.core.BonaparteToonComposer;
import de.jpaw.bonaparte.pojos.jsonTest.ColorAlnum;
import de.jpaw.bonaparte.pojos.jsonTest.ColorNum;
import de.jpaw.bonaparte.pojos.jsonTest.JsonEnumAndList;
import de.jpaw.bonaparte.pojos.jsonTest.JsonFieldTest;
import de.jpaw.bonaparte.pojos.jsonTest.TestD;
import de.jpaw.bonaparte.pojos.jsonTest.TestObj;
import de.jpaw.bonaparte.pojos.jsonTest.TestSimple;
import de.jpaw.bonaparte.pojos.jsonTest.TestT;
import de.jpaw.bonaparte.pojos.jsonTest.TestTS;
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
        TestD t = new TestD();
        t.setText("mytext");
        t.setD0(LocalDate.of(2015, 10, 31));

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(t);

        String result = sb.toString();
        System.out.println("Date field result:");
        System.out.println(result);

        // Verify date is formatted correctly
        Assertions.assertTrue(result.contains("2015-10-31"));
    }

//    @Test
//    public void testWithList() throws Exception {
//        JsonFieldTest t = new JsonFieldTest();
//        t.setText("mytext");
//
//        List<Object> l = new ArrayList<Object>(20);
//        t.setVarList(l);
//        l.add(42);
//        l.add(3.14);
//        l.add('x');
//        l.add("Hello, world");
//
//        StringBuilder sb = new StringBuilder();
//        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
//        composer.writeRecord(t);
//
//        String result = sb.toString();
//        System.out.println("List field result:");
//        System.out.println(result);
//
//        // Verify TOON format: varList[4]: value1,value2,...
//        Assertions.assertTrue(result.contains("text:"));
//        Assertions.assertTrue(result.contains("varList[4]:"));
//        Assertions.assertTrue(result.contains("42"));
//        Assertions.assertTrue(result.contains("3.14"));
//        Assertions.assertTrue(result.contains("\"x\""));
//        Assertions.assertTrue(result.contains("\"Hello, world\""));
//    }

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

        // Verify enum names/tokens are unquoted strings
        Assertions.assertTrue(!result.contains("\""));
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
        Assertions.assertTrue(result.contains("varList:[0]:"));
    }

    @Test
    public void testNullObjectToString() {
        String result = BonaparteToonComposer.toToonString(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testTestObjMarshalling() throws Exception {
        TestObj obj = new TestObj();
        obj.setPrimitiveInt(42);
        obj.setWrappedLong(1234567890L);
        obj.setMyDay(LocalDate.of(2025, 1, 15));
        obj.setMyInstant(Instant.parse("2025-01-15T10:30:45Z"));
        obj.setMyText("Hello Bonaparte");
        obj.setMyBoolean(true);
        obj.setMyNull(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(obj);

        String result = sb.toString();
        System.out.println("TestObj marshalling result:");
        System.out.println(result);

        // Verify all fields are present
        Assertions.assertTrue(result.contains("primitiveInt:"));
        Assertions.assertTrue(result.contains("42"));
        Assertions.assertTrue(result.contains("wrappedLong:"));
        Assertions.assertTrue(result.contains("1234567890"));
        Assertions.assertTrue(result.contains("myDay:"));
        Assertions.assertTrue(result.contains("2025-01-15"));
        Assertions.assertTrue(result.contains("myInstant:"));
        Assertions.assertTrue(result.contains("2025-01-15T10:30:45Z"));
        Assertions.assertTrue(result.contains("myText:"));
        Assertions.assertTrue(result.contains("\"Hello Bonaparte\""));
        Assertions.assertTrue(result.contains("myBoolean:"));
        Assertions.assertTrue(result.contains("true"));
        Assertions.assertTrue(result.contains("myNull:"));
        Assertions.assertTrue(result.contains("123e4567-e89b-12d3-a456-426614174000"));
    }

    @Test
    public void testTestObjWithNulls() throws Exception {
        TestObj obj = new TestObj();
        obj.setPrimitiveInt(0);
        // Leave all optional fields as null

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(obj);

        String result = sb.toString();
        System.out.println("TestObj with nulls result:");
        System.out.println(result);

        // Required field should be present
        Assertions.assertTrue(result.contains("primitiveInt:"));
        Assertions.assertTrue(result.contains("0"));
        // Null fields should show as null
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testTestTSMarshalling() throws Exception {
        TestTS obj = new TestTS();
        obj.setTs0(LocalDateTime.of(2025, 1, 15, 14, 30, 45));
        obj.setTs3(LocalDateTime.of(2025, 1, 15, 14, 30, 45, 123000000));

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(obj);

        String result = sb.toString();
        System.out.println("TestTS marshalling result:");
        System.out.println(result);

        // Verify timestamp fields are present
        Assertions.assertTrue(result.contains("ts0:"));
        Assertions.assertTrue(result.contains("ts3:"));
        Assertions.assertTrue(result.contains("2025-01-15T14:30:45"));
        // ts3 should include milliseconds
        Assertions.assertTrue(result.contains("2025-01-15T14:30:45.123"));
    }

    @Test
    public void testTestTMarshalling() throws Exception {
        TestT obj = new TestT();
        obj.setT0(LocalTime.of(14, 30, 45));
        obj.setT3(LocalTime.of(14, 30, 45, 123000000));

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(obj);

        String result = sb.toString();
        System.out.println("TestT marshalling result:");
        System.out.println(result);

        // Verify time fields are present
        Assertions.assertTrue(result.contains("t0:"));
        Assertions.assertTrue(result.contains("t3:"));
        Assertions.assertTrue(result.contains("14:30:45"));
        // t3 should include milliseconds
        Assertions.assertTrue(result.contains("14:30:45.123"));
    }

    @Test
    public void testTestSimpleMarshalling() throws Exception {
        TestSimple obj = new TestSimple();
        obj.setNum(999);
        obj.setText("Simple test text");

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(obj);

        String result = sb.toString();
        System.out.println("TestSimple marshalling result:");
        System.out.println(result);

        // Verify fields are present
        Assertions.assertTrue(result.contains("num:"));
        Assertions.assertTrue(result.contains("999"));
        Assertions.assertTrue(result.contains("text:"));
        Assertions.assertTrue(result.contains("\"Simple test text\""));
    }

    @Test
    public void testTestSimpleWithNullText() throws Exception {
        TestSimple obj = new TestSimple();
        obj.setNum(123);
        obj.setText(null);

        StringBuilder sb = new StringBuilder();
        BonaparteToonComposer composer = new BonaparteToonComposer(sb);
        composer.writeRecord(obj);

        String result = sb.toString();
        System.out.println("TestSimple with null text result:");
        System.out.println(result);

        // Verify required num field
        Assertions.assertTrue(result.contains("num:"));
        Assertions.assertTrue(result.contains("123"));
        // Verify null text
        Assertions.assertTrue(result.contains("text:"));
        Assertions.assertTrue(result.contains("null"));
    }

    @Test
    public void testAllFourClassesToToonString() throws Exception {
        // Test using the helper method for all 4 classes

        TestObj testObj = new TestObj();
        testObj.setPrimitiveInt(1);
        testObj.setMyText("obj test");
        String objResult = BonaparteToonComposer.toToonString(testObj);

        TestTS testTS = new TestTS();
        testTS.setTs0(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        String tsResult = BonaparteToonComposer.toToonString(testTS);

        TestT testT = new TestT();
        testT.setT0(LocalTime.of(12, 0, 0));
        String tResult = BonaparteToonComposer.toToonString(testT);

        TestSimple testSimple = new TestSimple();
        testSimple.setNum(100);
        testSimple.setText("helper test");
        String simpleResult = BonaparteToonComposer.toToonString(testSimple);

        System.out.println("All four classes helper method results:");
        System.out.println("TestObj: " + objResult);
        System.out.println("TestTS: " + tsResult);
        System.out.println("TestT: " + tResult);
        System.out.println("TestSimple: " + simpleResult);

        // Verify all return valid results
        Assertions.assertNotNull(objResult);
        Assertions.assertNotNull(tsResult);
        Assertions.assertNotNull(tResult);
        Assertions.assertNotNull(simpleResult);

        // Basic verification of content
        Assertions.assertTrue(objResult.contains("\"obj test\""));
        Assertions.assertTrue(tsResult.contains("2025-01-01T00:00:00"));
        Assertions.assertTrue(tResult.contains("12:00:00"));
        Assertions.assertTrue(simpleResult.contains("\"helper test\""));
    }
}