package de.jpaw.bonaparte.core.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.core.MapParser;
import de.jpaw.bonaparte.core.MimeTypes;
import de.jpaw.bonaparte.core.StaticMeta;
import de.jpaw.bonaparte.util.ToStringHelper;
import de.jpaw.json.JsonParser;

public class MapToBonaPortableTest {

    /** Demonstrates how a JSON String can be converted back to a BonaPortable. This is effectively a 2 step process. */
    @Test
    public void testJsonToBonaPortable() throws Exception {
        String json = "{ \"" + MimeTypes.JSON_FIELD_PQON + "\": \"meta.ParsedFoldingComponent\", \"fieldname\":\"foo\"   ,  \"index\": 42 }";
        Map<String,Object> testMap = new JsonParser(json, false).parseObject();
        BonaPortable test = MapParser.asBonaPortable(testMap, StaticMeta.OUTER_BONAPORTABLE);
        Assertions.assertNotNull(test);
        Assertions.assertEquals("ParsedFoldingComponent", test.getClass().getSimpleName());
        Assertions.assertEquals("foo", testMap.get("fieldname"));
        Assertions.assertEquals(Integer.valueOf(42), testMap.get("index"));
    }

    private void testNumber(Map<String,Object> testMap, String expected) {
        Assertions.assertEquals("Integer", testMap.get("i").getClass().getSimpleName());
        Assertions.assertEquals("Long", testMap.get("l").getClass().getSimpleName());
        Assertions.assertEquals(expected, testMap.get("d").getClass().getSimpleName());

    }

    /** Demonstrates how the parser converts numeric types, with choice on fractional numbers. */
    @Test
    public void testJsonToNumber() throws Exception {
        String json = "{ \"i\": -565656,   \"l\": 62626262628818818, \"d\":3.14 }";

        testNumber(new JsonParser(json, false).parseObject(), "BigDecimal");
        testNumber(new JsonParser(json, true).parseObject(), "Double");
    }

    /** Demonstrates how a Map (parsed JSON) can be converted back to a BonaPortable. */
    @Test
    public void testMapToBonaPortable() throws Exception {
        Map<String,Object> testMap = new HashMap<String,Object>(10);

        testMap.put(MimeTypes.JSON_FIELD_PQON, "meta.ParsedFoldingComponent");
        testMap.put("fieldname", "foo");
        testMap.put("index",     "42");     // show that converter also maps Strings to Integer where required!

        BonaPortable test = MapParser.asBonaPortable(testMap, StaticMeta.OUTER_BONAPORTABLE);
        Assertions.assertNotNull(test);
        Assertions.assertEquals("ParsedFoldingComponent", test.getClass().getSimpleName());
    }

    /** Demonstrates how a nested Map (parsed JSON) can be converted back to a BonaPortable. */
    @Test
    public void testMapsToBonaPortable() throws Exception {
        Map<String,Object> testMap = new HashMap<String,Object>(10);
        Map<String,Object> testMap2 = new HashMap<String,Object>(10);

        testMap2.put(MimeTypes.JSON_FIELD_PQON, "meta.ParsedFoldingComponent");
        testMap2.put("fieldname", "bar");
        testMap2.put("index",     999);

        testMap.put(MimeTypes.JSON_FIELD_PQON, "meta.ParsedFoldingComponent");
        testMap.put("fieldname", "foo");
        testMap.put("index",     42);
        testMap.put("alphaIndex", "EUR");
        // testMap.put("component", testMap2);          // fails due to self-reference (cyclic static initialization problem: meta$$component.lowerBound is null)

        System.out.println(ToStringHelper.toStringML(testMap));

        BonaPortable test = MapParser.asBonaPortable(testMap, StaticMeta.OUTER_BONAPORTABLE);
        Assertions.assertNotNull(test);
        Assertions.assertEquals("ParsedFoldingComponent", test.getClass().getSimpleName());
        System.out.println(ToStringHelper.toStringML(test));
    }
}
