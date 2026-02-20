package de.jpaw.adapters.tests;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.jpaw.bonaparte.core.DataAndMeta;
import de.jpaw.bonaparte.core.ListMetaComposer;
import de.jpaw.bonaparte.pojos.adapters.fixedpoint.Millis;
import de.jpaw.bonaparte.pojos.adapters.fixedpointToBigDecimal.BigMillis;
import de.jpaw.bonaparte.pojos.adapters.tests.CustomMillis;
import de.jpaw.fixedpoint.types.MilliUnits;

public class TestListMetaWithAdapter {

    @Test
    public void testListMetaComposer() throws Exception {
        CustomMillis m1 = new CustomMillis("hello", MilliUnits.of(2718), MilliUnits.of(3142));
        ListMetaComposer c1 = new ListMetaComposer(false, false, false);
        c1.writeRecord(m1);
        List<DataAndMeta> dwm = c1.getStorage();

        Assertions.assertEquals(3, dwm.size());
        Assertions.assertEquals(Long.valueOf(2718), dwm.get(1).data);
        Assertions.assertEquals(Millis.meta$$mantissa, dwm.get(1).meta);
        Assertions.assertEquals(BigDecimal.valueOf(3142, 3), dwm.get(2).data);
        Assertions.assertEquals(BigMillis.meta$$mantissa, dwm.get(2).meta);
    }

    // see a different result with the last parameter of ListMetaComposer set to true
    @Test
    public void testListMetaComposerKeepExternals() throws Exception {
        CustomMillis m1 = new CustomMillis("hello", MilliUnits.of(2718), MilliUnits.of(3142));
        ListMetaComposer c1 = new ListMetaComposer(false, false, true);
        c1.writeRecord(m1);
        List<DataAndMeta> dwm = c1.getStorage();

        Assertions.assertEquals(3, dwm.size());
        Assertions.assertEquals(MilliUnits.of(2718), dwm.get(1).data);
        Assertions.assertEquals(CustomMillis.meta$$myIntegralMillis, dwm.get(1).meta);
        Assertions.assertEquals(MilliUnits.of(3142), dwm.get(2).data);
        Assertions.assertEquals(CustomMillis.meta$$myBigDecimalMillis, dwm.get(2).meta);
    }
}
