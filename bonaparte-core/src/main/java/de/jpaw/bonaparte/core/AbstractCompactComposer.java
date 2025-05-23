package de.jpaw.bonaparte.core;

import java.io.DataOutput;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import de.jpaw.bonaparte.enums.BonaNonTokenizableEnum;
import de.jpaw.bonaparte.enums.BonaTokenizableEnum;
import de.jpaw.bonaparte.pojos.meta.AlphanumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BasicNumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BinaryElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.ClassDefinition;
import de.jpaw.bonaparte.pojos.meta.EnumDataItem;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.MiscElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.NumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.ObjectReference;
import de.jpaw.bonaparte.pojos.meta.TemporalElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.XEnumDataItem;
import de.jpaw.bonaparte.util.DayTime;
import de.jpaw.enums.AbstractByteEnumSet;
import de.jpaw.enums.AbstractIntEnumSet;
import de.jpaw.enums.AbstractLongEnumSet;
import de.jpaw.enums.AbstractShortEnumSet;
import de.jpaw.enums.AbstractStringEnumSet;
import de.jpaw.enums.AbstractStringXEnumSet;
import de.jpaw.enums.AbstractXEnumBase;
import de.jpaw.enums.EnumSetMarker;
import de.jpaw.enums.TokenizableEnum;
import de.jpaw.enums.XEnum;
import de.jpaw.fixedpoint.FixedPointBase;
import de.jpaw.util.ByteArray;

public abstract class AbstractCompactComposer extends AbstractMessageComposer<IOException> implements CompactConstants {

    // remove unsafe access due to changes in Java 9
//    private static Field unsafeString = calculateUnsafe();
//    private static Field calculateUnsafe() {
//        try {
//            Field f = String.class.getDeclaredField("value");
//            f.setAccessible(true);
//            return f;
//        } catch (Exception e) {
//            return null;
//        }
//    }
    private final boolean useJsonForBonaCustomInElements;       // if true, the subobjects inside Element will be output as JSON. This kills enums on deserialization, but allows to deserialize objects not known on receiver side.
    private final boolean useCache;
    private final Map<BonaCustom, Integer> objectCache;
    private int numberOfObjectsSerialized;
    private int numberOfObjectReuses;
    // variables set by constructor
    protected final DataOutput out;
    protected final boolean recommendIdentifiable;              // if true, then factoryId and classId will be used to identify the object (requires prior registration of factories before parsing)
    protected boolean skipLowerBoundObjectDescription = true;   // if true and the object to serialize corresponds to its lower bound, then do not output the class description
    protected AbstractCompactComposer jsonComposer = null;      // derived composer on same DataOutput, which also creates field names. Will be initialized on demand.

    protected AbstractCompactComposer(final DataOutput out, final ObjectReuseStrategy reuseStrategy, final boolean recommendIdentifiable, final boolean useJsonForBonaCustomInElements) {
        switch (reuseStrategy) {
        case BY_CONTENTS:
            this.objectCache = new HashMap<BonaCustom, Integer>(250);
            this.useCache = true;
            break;
        case BY_REFERENCE:
            this.objectCache = new IdentityHashMap<BonaCustom, Integer>(250);
            this.useCache = true;
            break;
        default:
            this.objectCache = null;
            this.useCache = false;
            break;
        }
        this.out = out;
        this.recommendIdentifiable = recommendIdentifiable;
        this.useJsonForBonaCustomInElements = useJsonForBonaCustomInElements;
        numberOfObjectsSerialized = 0;
        numberOfObjectReuses = 0;
    }

    public boolean isSkipLowerBoundObjectDescription() {
        return skipLowerBoundObjectDescription;
    }

    public void setSkipLowerBoundObjectDescription(boolean skipLowerBoundObjectDescription) {
        this.skipLowerBoundObjectDescription = skipLowerBoundObjectDescription;
    }

    // must be overridden / called if caching / reuse is active!
    public void reset() {
        numberOfObjectsSerialized = 0;
        numberOfObjectReuses = 0;
        if (useCache)
            objectCache.clear();
    }

    // for statistics
    public int getNumberOfObjectReuses() {
        return numberOfObjectReuses;
    }



    protected void writeNull() throws IOException {
        out.writeByte(NULL_FIELD);
    }

    @Override
    public void writeNull(FieldDefinition di) throws IOException {
        out.writeByte(NULL_FIELD);
    }

    @Override
    public void writeNullCollection(FieldDefinition di) throws IOException {
        out.writeByte(NULL_FIELD);
    }



    // write a non-empty string (using charAt())
    protected void writeLongString(String s) throws IOException {
        char maxCode = 0;
        int numWith2Byte = 0;
        int len = s.length();
        for (int i = 0; i < len; ++ i) {
            char c = s.charAt(i);
            if (c > maxCode)
                maxCode = c;
            if (c > 127)
                ++numWith2Byte;
        }
        if (maxCode <= 255) {       // no reason not to support a full 8 bit character set - this may save a bit of space for some west european texts
            // pure ASCII String
            if (len <= 16) {
                out.writeByte(SHORT_ISO_STRING + len - 1);
            } else {
                out.writeByte(ISO_STRING);
                intOut(len);
            }
            out.writeBytes(s);
        } else if (maxCode < 2048) {
            // UTF-8 out, with max. 2 byte sequences...
            out.writeByte(UTF8_STRING);
            intOut(len + numWith2Byte);
            for (int i = 0; i < len; ++i) {
                int c = s.charAt(i);
                if (c < 128) {
                    out.writeByte(c);
                } else {
                    // out.writeShort(((0xC0 | (c >> 6)) << 8) | (0x80 | (c & 0x3F)));
                    out.writeShort(0xC080 | ((c & 0x7c0) << 2) | (c & 0x3F));  // should be the same, but shorter
                }
            }
        } else {
            // UTF-16, due to possible 3 byte sequences
            out.writeByte(UTF16_STRING);
            intOut(len);
            out.writeChars(s);
        }
    }

    // write a non-empty string (using char[])
    protected void writeLongStringArray(String s) throws IOException {
        char maxCode = 0;
        int numWith2Byte = 0;
        int len = s.length();
        char buff [] = s.toCharArray();
        for (int i = 0; i < len; ++ i) {
            char c = buff[i];
            if (c > maxCode)
                maxCode = c;
            if (c > 127)
                ++numWith2Byte;
        }
        if (maxCode <= 255) {
            // pure ASCII String
            if (len <= 16) {
                out.writeByte(SHORT_ISO_STRING + len - 1);
            } else {
                out.writeByte(ISO_STRING);
                intOut(len);
            }
            for (int i = 0; i < len; ++i)
                out.writeByte(buff[i]);
        } else if (maxCode < 2048) {
            // UTF-8 out, with max. 2 byte sequences...
            out.writeByte(UTF8_STRING);
            intOut(len + numWith2Byte);
            for (int i = 0; i < len; ++i) {
                int c = buff[i];
                if (c < 128) {
                    out.writeByte(c);
                } else {
                    // out.writeShort(((0xC0 | (c >> 6)) << 8) | (0x80 | (c & 0x3F)));
                    out.writeShort(0xC080 | ((c & 0x7c0) << 2) | (c & 0x3F));  // should be the same, but shorter
                }
            }
        } else {
            // UTF-16, due to possible 3 byte sequences
            out.writeByte(UTF16_STRING);
            intOut(len);
            for (int i = 0; i < len; ++i)
                out.writeShort(buff[i]);
        }
    }

//    // write a non-empty string (using char[])
//    protected void writeLongStringStealArray(String s) throws IOException {
//        if (unsafeString == null) {
//            writeLongStringArray(s);
//            return;
//        }
//        char buff[];
//        try {
//            buff = (char []) unsafeString.get(s);
//        } catch (Exception e) {
//            writeLongStringArray(s);
//            return;
//        }
//        char maxCode = 0;
//        int numWith2Byte = 0;
//        int len = buff.length;
//        for (int i = 0; i < len; ++ i) {
//            char c = buff[i];
//            if (c > maxCode)
//                maxCode = c;
//            if (c > 127)
//                ++numWith2Byte;
//        }
//        if (maxCode <= 255) {
//            // pure ASCII String
//            if (len <= 16) {
//                out.writeByte(SHORT_ISO_STRING + len - 1);
//            } else {
//                out.writeByte(ISO_STRING);
//                intOut(len);
//            }
//            for (int i = 0; i < len; ++i)
//                out.writeByte(buff[i]);
//        } else if (maxCode < 2048) {
//            // UTF-8 out, with max. 2 byte sequences...
//            out.writeByte(UTF8_STRING);
//            intOut(len + numWith2Byte);
//            for (int i = 0; i < len; ++i) {
//                int c = buff[i];
//                if (c < 128) {
//                    out.writeByte(c);
//                } else {
//                    // out.writeShort(((0xC0 | (c >> 6)) << 8) | (0x80 | (c & 0x3F)));
//                    out.writeShort(0xC080 | ((c & 0x7c0) << 2) | (c & 0x3F));  // should be the same, but shorter
//                }
//            }
//        } else {
//            // UTF-16, due to possible 3 byte sequences
//            out.writeByte(UTF16_STRING);
//            intOut(len);
//            for (int i = 0; i < len; ++i)
//                out.writeShort(buff[i]);
//        }
//    }

    // output an integral value
    protected void intOut(int n) throws IOException {
        if (n >= 0) {
            // bisect the cases...
            if (n <= 60) {
                // single byte
                if (n < 32)
                    out.writeByte(n);
                else
                    out.writeByte(0x80 - 32 + n);
            } else if (n <= 0x7fff) {
                if (n < 4096) {
                    // 2 byte integer
//                    out.writeByte(0xc0 + (n >> 8));
//                    out.writeByte(n);
                    out.writeShort(n | 0xc000);         // the same, but faster
                } else {
                    // 3 byte integer
                    out.writeByte(INT_2BYTE);
                    out.writeShort(n);
                }
            } else if (n <= 0x7fffff) {
//                out.writeByte(INT_3BYTE);
//                out.writeByte(n >> 16);
//                out.writeShort(n);
                out.writeInt(n | (INT_3BYTE << 24));    // the same, but faster
            } else {
                out.writeByte(INT_4BYTE);
                out.writeInt(n);
            }
        } else {
            if (n >= -32768) {
                if (n >= -10)
                    out.writeByte(0xa0 - n);
                else {
                    out.writeByte(INT_2BYTE);
                    out.writeShort(n);
                }
            } else if (n >= -0x800000) {
//                out.writeByte(INT_3BYTE);
//                out.writeByte(n >> 16);
//                out.writeShort(n);
                out.writeInt((n & 0xffffff) | (INT_3BYTE << 24));    // the same, but faster
            } else {
                out.writeByte(INT_4BYTE);
                out.writeInt(n);
            }
        }
    }

    protected void charOut(char c) throws IOException {
        // if it is a displayable ASCII character, there is a short form
        if ((c & ~0x7f) == 0 && c >= 0x20) {
            // 1:1 mapping! write it as a byte!
            out.writeByte(c);                       // 1 byte
        } else if ((c & 0xff00) == 0) {
            // ISO char
            out.writeByte(SHORT_ISO_STRING);        // 2 byte
            out.writeByte(c);
        } else {
            // something else. Write a single char
            out.writeByte(UNICODE_CHAR);            // 3 byte
            out.writeShort((short)c);
        }
    }

    // character
    @Override
    public void addField(MiscElementaryDataItem di, char c) throws IOException {
        charOut(c);
    }

    protected void stringOut(String s) throws IOException {
        if (s.length() == 0) {
            out.writeByte(EMPTY_FIELD);
        } else if (s.length() == 1) {
            charOut(s.charAt(0));
        } else if (s.length() > 8) {
            writeLongString(s);  // writeLongStringStealArray
        } else {
            writeLongString(s);
        }
    }

    @Override
    public void addField(AlphanumericElementaryDataItem di, String s) throws IOException {
        if (s == null) {
            writeNull();
        } else {
            stringOut(s);
        }
    }

    // n is not null
    protected void bigintOut(BigInteger n) throws IOException {
        int l = n.bitLength();
        // see if we fit into an int
        if (l <= 31) {
            // yes, then store as an int
            intOut(n.intValue());  // intValueExact() requires Java 1.8
        } else {
            out.writeByte(COMPACT_BIGINTEGER);
            byte [] tmp = n.toByteArray();      // TODO: do some dirty trick to avoid temporary array construction!
            intOut(tmp.length);
            out.write(tmp);
        }
    }

    // n is not null
    protected void bigdecimalOut(BigDecimal n) throws IOException {
        int sgn = n.signum();
        if (sgn == 0) {
            out.writeByte(0);       // special case, but no need to treat it separately while parsing, as general integers are accepted
        } else {
            int scale = n.scale();
            if (scale > 0 || scale < -3) {
                // is a fractional number
                if (scale > 0 && scale <= 9) {
                    out.writeByte(COMPACT_BIGDECIMAL + scale);
                } else {
                    out.writeByte(COMPACT_BIGDECIMAL);
                    intOut(scale);
                }
                bigintOut(n.unscaledValue());
            } else {
                // number is integral (and not with exponent HUGE)
                bigintOut(n.toBigInteger());
            }
        }
    }

    // decimal
    @Override
    public void addField(NumericElementaryDataItem di, BigDecimal n) throws IOException {
        if (n != null) {
            bigdecimalOut(n);
        } else {
            writeNull();
        }
    }

    // n is not null
    protected <F extends FixedPointBase<F>> void fixedpointOut(F n) throws IOException {
        if (n.isIntegralValue()) {
            longOut(n.longValue());
            return;
        }
        int scale = n.scale();
        // scale == 0 cannot happen because that would be caught by the integral check before
        // is a fractional number
        if (scale <= 9) {
            out.writeByte(COMPACT_BIGDECIMAL + scale);
        } else {
            out.writeByte(COMPACT_BIGDECIMAL);
            intOut(scale);
        }
        longOut(n.getMantissa());
    }

    @Override
    public <F extends FixedPointBase<F>> void addField(BasicNumericElementaryDataItem di, F n) throws IOException {
        if (n == null) {
            writeNull();
            return;
        }
        fixedpointOut(n);
    }

    // byte
    @Override
    public void addField(BasicNumericElementaryDataItem di, byte n) throws IOException {
        intOut(n);
    }

    // short
    @Override
    public void addField(BasicNumericElementaryDataItem di, short n) throws IOException {
        intOut(n);
    }

    // integer
    @Override
    public void addField(BasicNumericElementaryDataItem di, int n) throws IOException {
        intOut(n);
    }

    // int(n)
    @Override
    public void addField(BasicNumericElementaryDataItem di, BigInteger n) throws IOException {
        if (n != null) {
            bigintOut(n);
        } else {
            writeNull();
        }
    }

    // entry which does not need a reference
    protected void longOut(long n) throws IOException {
        int nn = (int)n;
        if (nn == n)
            intOut((int)n);
        else {
            if ((n & 0xffff0000L) == (n > 0 ? 0 : 0xffff0000L)) {
                out.writeByte(INT_6BYTE);
                out.writeShort((short)(n >> 32));
                out.writeInt((int)n);
                return;
            }
            // default
            out.writeByte(INT_8BYTE);  // TODO: optimize for 5 or 7 bytes here!
            out.writeLong(n);
        }
    }

    // long
    @Override
    public void addField(BasicNumericElementaryDataItem di, long n) throws IOException {
        longOut(n);
    }

    // boolean
    @Override
    public void addField(MiscElementaryDataItem di, boolean b) throws IOException {
        out.writeByte(b ? COMPACT_BOOLEAN_TRUE : COMPACT_BOOLEAN_FALSE);
    }

    // float
    @Override
    public void addField(BasicNumericElementaryDataItem di, float f) throws IOException {
        int i = (int)f;
        if (i == f) {
            // integral number, can be stored in compact form
            intOut(i);
        } else {
            out.writeByte(COMPACT_FLOAT);
            out.writeFloat(f);
        }
    }

    // double
    @Override
    public void addField(BasicNumericElementaryDataItem di, double d) throws IOException {
        int i = (int)d;
        if (i == d) {
            // integral number, can be stored in compact form
            intOut(i);
        } else {
            out.writeByte(COMPACT_DOUBLE);
            out.writeDouble(d);
        }
    }

    protected void uuidOut(UUID n) throws IOException {
        out.writeByte(COMPACT_UUID);
        out.writeLong(n.getMostSignificantBits());
        out.writeLong(n.getLeastSignificantBits());
    }

    // UUID
    @Override
    public void addField(MiscElementaryDataItem di, UUID n) throws IOException {
        if (n != null) {
            uuidOut(n);
        } else {
            writeNull();
        }
    }

    protected void bytearrayOut(ByteArray b) throws IOException {
        out.writeByte(COMPACT_BINARY);
        intOut(b.length());
        if (b.length() > 0) {
            b.writeToDataOutput(out);
        }
    }

    // ByteArray: initial quick & dirty implementation
    @Override
    public void addField(BinaryElementaryDataItem di, ByteArray b) throws IOException {
        if (b != null) {
            bytearrayOut(b);
        } else {
            writeNull();
        }
    }

    protected void bytesOut(byte [] b) throws IOException {
        out.writeByte(COMPACT_BINARY);
        intOut(b.length);
        if (b.length > 0) {
            out.write(b);
        }
    }

    // raw. Almost the same as ByteArray...
    @Override
    public void addField(BinaryElementaryDataItem di, byte[] b) throws IOException {
        if (b != null) {
            bytesOut(b);
        } else {
            writeNull();
        }
    }

    protected void localdateOut(LocalDate t) throws IOException {
        out.writeByte(COMPACT_DATE);
        intOut(t.getYear());
        intOut(t.getMonthValue());
        intOut(t.getDayOfMonth());
    }

    // converters for DAY und TIMESTAMP
    @Override
    public void addField(TemporalElementaryDataItem di, LocalDate t) throws IOException {
        if (t != null) {
            localdateOut(t);
        } else {
            writeNull();
        }
    }

    protected void localdatetimeOut(LocalDateTime t) throws IOException {
        int millis = t.getNano() / 1000000;
        boolean fractional = millis != 0;
        out.writeByte(!fractional ? COMPACT_DATETIME : COMPACT_DATETIME_MILLIS);
        intOut(t.getYear());
        intOut(t.getMonthValue());
        intOut(t.getDayOfMonth());
        if (fractional)
            intOut(DayTime.millisOfDay(t.toLocalTime()));
        else
            intOut(t.toLocalTime().toSecondOfDay());
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDateTime t) throws IOException {
        if (t != null) {
            localdatetimeOut(t);
        } else {
            writeNull();
        }
    }

    protected void localtimeOut(LocalTime t) throws IOException {
        int millis = t.getNano() / 1000000;
        if (millis != 0) {
            out.writeByte(COMPACT_TIME_MILLIS);
            intOut(DayTime.millisOfDay(t));
        } else {
            out.writeByte(COMPACT_TIME);
            intOut(t.toSecondOfDay());
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalTime t) throws IOException {
        if (t != null) {
            localtimeOut(t);
        } else {
            writeNull();
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, Instant t) throws IOException {
        if (t != null) {
            longOut(t.toEpochMilli());
        } else {
            writeNull();
        }
    }

    @Override
    public void startTransmission() throws IOException {
    }

    @Override
    public void terminateTransmission() throws IOException {
    }

    @Override
    public void terminateRecord() throws IOException {
    }

    @Override
    public void writeSuperclassSeparator() throws IOException {
        out.writeByte(PARENT_SEPARATOR);
    }

    @Override
    public void startRecord() throws IOException {
    }

    @Override
    public void startMap(FieldDefinition di, int currentMembers) throws IOException {
        out.writeByte(MAP_BEGIN);
        intOut(currentMembers);
    }

    @Override
    public void startArray(FieldDefinition di, int currentMembers, int sizeOfElement) throws IOException {
        out.writeByte(ARRAY_BEGIN);
        intOut(currentMembers);
    }

    // removed collections terminator because it conflicts with the intended use of parent / child serialization to separate tables
    @Override
    public void terminateArray() throws IOException {
//        out.out.writeByte(COLLECTIONS_TERMINATOR);
    }

    @Override
    public void terminateMap() throws IOException {
//        out.out.writeByte(COLLECTIONS_TERMINATOR);
    }

    @Override
    public void startObject(ObjectReference di, BonaCustom obj) throws IOException {
        ClassDefinition meta = obj.ret$MetaData();
        if (skipLowerBoundObjectDescription && di.getLowerBound() != null && di.getLowerBound().getName().equals(meta.getName())) {
            if (recommendIdentifiable) {
                out.writeByte(OBJECT_BEGIN_BASE);
            } else {
                out.writeByte(OBJECT_BEGIN_PQON);
                out.writeByte(EMPTY_FIELD);
                addField(REVISION_META, meta.getRevision());
            }
        } else {
            if (recommendIdentifiable && meta.getFactoryId() > 0 && meta.getId() > 0) {
                out.writeByte(OBJECT_BEGIN_ID);
                intOut(meta.getFactoryId());
                intOut(meta.getId());
            } else {
                out.writeByte(OBJECT_BEGIN_PQON);
                writeLongString(meta.getName());  // writeLongStringStealArray
                addField(REVISION_META, meta.getRevision());
            }
        }
    }

    @Override
    public void terminateObject(ObjectReference di, BonaCustom obj) throws IOException {
        out.writeByte(OBJECT_TERMINATOR);
    }

    @Override
    public void addField(ObjectReference di, BonaCustom obj) throws IOException {
        if (obj == null) {
            writeNull();
        } else {
            if (useCache) {
                Integer previousIndex = objectCache.get(obj);
                if (previousIndex != null) {
                    // reuse this instance
                    out.writeByte(OBJECT_AGAIN);
                    intOut(numberOfObjectsSerialized - previousIndex.intValue() - 1);
                    ++numberOfObjectReuses;
                    return;
                }
                // add the new object to the cache of known objects. This is
                // done despite we are not yet done with the object!
                objectCache.put(obj, Integer.valueOf(numberOfObjectsSerialized++));
                // fall through
            }
            // start a new object
            startObject(di, obj);
            // do all fields (now includes terminator)
            obj.serializeSub(this);
            // terminate the object
            terminateObject(di, obj);
        }
    }

    // enum with numeric expansion: delegate to Null/Int
    @Override
    public void addEnum(EnumDataItem di, BasicNumericElementaryDataItem ord, BonaNonTokenizableEnum n) throws IOException {
        if (n == null)
            writeNull(ord);
        else
            addField(ord, n.ordinal());
    }

    // enum with alphanumeric expansion: delegate to Null/String
    @Override
    public void addEnum(EnumDataItem di, AlphanumericElementaryDataItem token, BonaTokenizableEnum n) throws IOException {
        if (n == null)
            writeNull(token);
        else
            addField(token, n.getToken());
    }

    // xenum with alphanumeric expansion: delegate to Null/String
    @Override
    public void addEnum(XEnumDataItem di, AlphanumericElementaryDataItem token, XEnum<?> n) throws IOException {
        if (n == null)
            writeNull(token);
        else
            addField(token, n.getToken());
    }

    @Override
    public boolean addExternal(ObjectReference di, Object obj) throws IOException {
        return false;       // perform conversion by default
    }

    @Override
    public void addField(ObjectReference di, Map<String, Object> obj) throws IOException {
        if (obj == null) {
            writeNull(di);
            return;
        }
        elementOut(obj);
    }

    @Override
    public void addField(ObjectReference di, List<Object> l) throws IOException {
        if (l == null) {
            writeNull(di);
            return;
        }
        elementOut(l);
    }

    @Override
    public void addField(ObjectReference di, Object obj) throws IOException {
        if (obj == null) {
            writeNull(di);
            return;
        }
        elementOut(obj);
    }


    // output an Object array - anonymous subroutine
    protected void arrayOut(Object [] l) throws IOException {
        out.writeByte(ARRAY_BEGIN);
        intOut(l.length);
        for (Object o : l)
            elementOut(o);
    }

    // output a map - anonymous subroutine
    protected void mapOut(Map<String, Object> obj) throws IOException {
        out.writeByte(MAP_BEGIN);
        intOut(obj.size());  // number of members.  Note: this implies we cannot skip nulls! OK as we assume they would have been cleared before if it was the intention to do so.

        for (Map.Entry<String, Object> elem: obj.entrySet()) {
            stringOut(elem.getKey());
            elementOut(elem.getValue());
        }
    }

    // output a generic object - anonymous subroutine
    protected void elementOut(final Object obj) throws IOException {
        if (obj == null) {
            writeNull();
            return;
        }
        // check for types. here, we do not have primitive types
        if (obj instanceof Number) {
            // check numeric types
            if (obj instanceof Integer i) {
                intOut(i.intValue());
                return;
            }
            if (obj instanceof Long l) {
                longOut(l.longValue());
                return;
            }
            if (obj instanceof Byte b) {
                intOut(b.intValue());
                return;
            }
            if (obj instanceof Short s) {
                intOut(s.intValue());
                return;
            }
            if (obj instanceof BigInteger bi) {
                bigintOut(bi);
                return;
            }
            if (obj instanceof BigDecimal bd) {
                bigdecimalOut(bd);
                return;
            }
            if (obj instanceof Double d) {
                out.writeByte(COMPACT_DOUBLE);          // no check for integral type here, as we have no type information
                out.writeDouble(d.doubleValue());
                return;
            }
            if (obj instanceof Float f) {
                out.writeByte(COMPACT_FLOAT);          // no check for integral type here, as we have no type information
                out.writeFloat(f.floatValue());
                return;
            }
            if (obj instanceof FixedPointBase fp) {
                fixedpointOut(fp);
                return;
            }
            throw new RuntimeException("Unrecognized type " + obj.getClass().getSimpleName() + " for compact number");
        }
        if (obj instanceof String s) {
            stringOut(s);
            return;
        }
        if (obj instanceof Boolean bool) {
            out.writeByte(bool ? COMPACT_BOOLEAN_TRUE : COMPACT_BOOLEAN_FALSE);
            return;
        }
        if (obj instanceof UUID u) {
            uuidOut(u);
            return;
        }
        if (obj instanceof Character c) {
            charOut(c.charValue());
            return;
        }
        if (obj instanceof ByteArray ba) {
            bytearrayOut(ba);
            return;
        }
        if (obj instanceof Map ma) {
            mapOut(ma);
            return;
        }
        if (obj instanceof List li) {
            out.writeByte(ARRAY_BEGIN);
            intOut(li.size());
            for (Object o : li)
                elementOut(o);
            return;
        }
        if (obj instanceof EnumSetMarker) {
            if (obj instanceof AbstractStringEnumSet<?> ases) {
                stringOut(ases.getBitmap());
            } else if (obj instanceof AbstractStringXEnumSet<?> asxs) {
                stringOut(asxs.getBitmap());
            } else if (obj instanceof AbstractIntEnumSet<?> aies) {
                intOut(aies.getBitmap());
            } else if (obj instanceof AbstractLongEnumSet<?> ales) {
                longOut(ales.getBitmap());
            } else if (obj instanceof AbstractByteEnumSet<?> abes) {
                intOut(abes.getBitmap());
            } else if (obj instanceof AbstractShortEnumSet<?> ases) {
                intOut(ases.getBitmap());
            } else {
                throw new RuntimeException("Cannot transform enum set of type " + obj.getClass().getSimpleName() + " to JSON");
            }
            return;
        }
        if (obj instanceof Set<?> se) {
            out.writeByte(ARRAY_BEGIN);
            intOut(se.size());
            for (final Object o : se)
                elementOut(o);
            return;
        }

        if (obj instanceof Enum en) {
            // distinguish Tokenizable
            if (obj instanceof TokenizableEnum te) {
                stringOut(te.getToken());
            } else {
                intOut(en.ordinal());
            }
            return;
        }
        if (obj instanceof AbstractXEnumBase<?> axe) {
            stringOut(axe.getToken());
            return;
        }
        if (obj instanceof Temporal) {
            if (obj instanceof Instant in) {
                longOut(in.toEpochMilli());
                return;
            }
            if (obj instanceof LocalDate ld) {
                localdateOut(ld);
                return;
            }
            if (obj instanceof LocalTime lt) {
                localtimeOut(lt);
                return;
            }
            if (obj instanceof LocalDateTime ldt) {
                localdatetimeOut(ldt);
                return;
            }
            throw new RuntimeException("Cannot transform Java Temporal of type " + obj.getClass().getSimpleName() + " to JSON");
        }
        if (obj instanceof BonaCustom bc) {
            if (useJsonForBonaCustomInElements) {
                // create a special JSON composer on demand
                if (jsonComposer == null)
                    jsonComposer = new CompactJsonComposer(out);        // composer which adds ability to output field names
                jsonComposer.writeObject(bc);
            } else {
                addField(StaticMeta.INNER_BONAPORTABLE, bc);      // output objects as object, even within JSON! (catch issues during deserialize)
            }
            return;
        }
        if (obj instanceof Object [] oarr) {
            // any array of an object type (String, Boolean whatever...) except primitive arrays
            arrayOut(oarr);
            return;
        }
        if (obj.getClass().isArray()) {
            outputPrimitiveArrays(obj);
            return;
        }
        throw new RuntimeException("Cannot transform type " + obj.getClass().getSimpleName() + " to JSON");
    }

    // code moved out due to excessive length
    private void outputPrimitiveArrays(Object obj) throws IOException {
        if (obj instanceof byte [] array) {
            bytesOut(array);
            return;
        }
        out.writeByte(ARRAY_BEGIN);
        if (obj instanceof int [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                intOut(array[i]);
            }
            return;
        }
        if (obj instanceof boolean [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                out.writeByte(array[i] ? COMPACT_BOOLEAN_TRUE : COMPACT_BOOLEAN_FALSE);
            }
            return;
        }
        if (obj instanceof char [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                stringOut(Character.toString(array[i]));        // converts char [] to a string
            }
            return;
        }
        if (obj instanceof long [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                longOut(array[i]);
            }
            return;
        }
        if (obj instanceof short [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                intOut(array[i]);
            }
            return;
        }
        if (obj instanceof double [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                out.writeByte(COMPACT_DOUBLE);          // no check for integral type here, as we have no type information
                out.writeDouble(array[i]);
            }
            return;
        }
        if (obj instanceof float [] array) {
            final int len = array.length;
            intOut(len);
            for (int i = 0; i < len; ++i) {
                out.writeByte(COMPACT_FLOAT);          // no check for integral type here, as we have no type information
                out.writeFloat(array[i]);
            }
            return;
        }
        throw new RuntimeException("Not yet supported: primitive array " + obj.getClass().getSimpleName());
    }
}
