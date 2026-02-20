package de.jpaw.bonaparte.core.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.jpaw.bonaparte.core.BonaPortableFactory;
import de.jpaw.bonaparte.pojos.meta.ClassDefinition;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.XEnumSetDefinition;
import de.jpaw.bonaparte.util.FieldGetter;

public class PathResolverTest {
    static {
        BonaPortableFactory.init();       // required here, tests will fail if omitted
    }

    private void info() {
        System.out.println(ClassDefinition.meta$$fields.getLowerBound() == null ? "NULL!!!!!" : "ok");
    }

    @Test
    public void testPathResolving1() throws Exception {
        ClassDefinition obj = XEnumSetDefinition.BClass.INSTANCE.getMetaData();

        FieldDefinition f = FieldGetter.getFieldDefinitionForPathname(obj, "baseXEnum");
        Assertions.assertEquals(XEnumSetDefinition.meta$$baseXEnum, f);
    }

    @Test
    public void testPathResolving2() throws Exception {
        ClassDefinition obj = ClassDefinition.BClass.INSTANCE.getMetaData();

        FieldDefinition f = FieldGetter.getFieldDefinitionForPathname(obj, "fields[2].dataCategory");
        Assertions.assertEquals(FieldDefinition.meta$$dataCategory, f);
    }

    @Test
    public void testPathResolving12() throws Exception {
        info();
        ClassDefinition obj = XEnumSetDefinition.BClass.INSTANCE.getMetaData();
        FieldDefinition f = FieldGetter.getFieldDefinitionForPathname(obj, "baseXEnum");
        Assertions.assertEquals(XEnumSetDefinition.meta$$baseXEnum, f);

        ClassDefinition obj2 = ClassDefinition.BClass.INSTANCE.getMetaData();

        FieldDefinition f2 = FieldGetter.getFieldDefinitionForPathname(obj2, "fields[2].dataCategory");
        Assertions.assertEquals(FieldDefinition.meta$$dataCategory, f2);
    }

}
