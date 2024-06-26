/*
 * Copyright 2012 Michael Bischoff
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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jpaw.bonaparte.enums.BonaNonTokenizableEnum;
import de.jpaw.bonaparte.enums.BonaTokenizableEnum;
import de.jpaw.bonaparte.pojos.meta.AlphanumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BasicNumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BinaryElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.EnumDataItem;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.MiscElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.NumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.ObjectReference;
import de.jpaw.bonaparte.pojos.meta.TemporalElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.XEnumDataItem;
import de.jpaw.bonaparte.util.DayTime;
import de.jpaw.bonaparte.util.FixASCII;
import de.jpaw.enums.XEnum;
import de.jpaw.fixedpoint.FixedPointBase;
import de.jpaw.util.Base64;
import de.jpaw.util.ByteArray;
import de.jpaw.util.ByteBuilder;
/**
 * Implements the serialization for the bonaparte format into byte arrays, using the {@link de.jpaw.util.ByteBuilder ByteBuilder} class, which is similar to the well known {@link java.lang.StringBuilder StringBuilder}.
 *
 * @author Michael Bischoff
 *
 */

public class ByteArrayComposer extends AbstractMessageComposer<RuntimeException> implements BufferedMessageComposer<RuntimeException>, ByteArrayConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteArrayComposer.class);
    private final boolean useCache;
    private final Map<BonaCustom,Integer> objectCache;
    private int numberOfObjectsSerialized;
    private int numberOfObjectReuses;

    // variables for serialization
    private ByteBuilder work;

    /** Quick conversion utility method, for use by code generators. (null safe) */
    public static byte [] marshal(ObjectReference di, BonaPortable x) {
        if (x == null)
            return null;
        ByteArrayComposer bac = new ByteArrayComposer();
        bac.addField(di, x);
        return bac.getBytes();
    }

    /** Quick conversion utility method, for use by code generators. (null safe, avoids double copying of the result) */
    public static ByteArray marshalAsByteArray(ObjectReference di, BonaPortable x) {
        if (x == null)
            return null; // consistent with the other methods: f(null) = null  //  ByteArray.ZERO_BYTE_ARRAY;
        ByteArrayComposer bac = new ByteArrayComposer();
        bac.addField(di, x);
        return new ByteArray(bac.getBuffer(), 0, bac.getLength());
    }

    /** Creates a new ByteArrayComposer, using this classes static default Charset **/
    public ByteArrayComposer() {
        this(ObjectReuseStrategy.defaultStrategy);
    }

    /** Creates a new ByteArrayComposer, using this classes static default Charset **/
    public ByteArrayComposer(ObjectReuseStrategy reuseStrategy) {
        switch (reuseStrategy) {
        case BY_CONTENTS:
            this.objectCache = new HashMap<BonaCustom,Integer>(250);
            this.useCache = true;
            break;
        case BY_REFERENCE:
            this.objectCache = new IdentityHashMap<BonaCustom,Integer>(250);
            this.useCache = true;
            break;
        default:
            this.objectCache = null;
            this.useCache = false;
            break;
        }
        this.work = new ByteBuilder(0, getDefaultCharset());
        numberOfObjectsSerialized = 0;
        numberOfObjectReuses = 0;
    }

    protected int getNumberOfObjectsSerialized() {
        return numberOfObjectsSerialized;
    }

    @Override
    public void setCharset(Charset charset) {
        super.setCharset(charset);
        // also tell the ByteBuilder!
        work.setCharset(charset);
    }

    /** Sets the current length to 0, allowing reuse of the allocated output buffer for a new message. */
    @Override
    public void reset() {
        work.setLength(0);
        numberOfObjectsSerialized = 0;
        numberOfObjectReuses = 0;
        if (useCache)
            objectCache.clear();
    }

    /** Returns the number of bytes written. */
    @Override
    public int getLength() {    // obtain the number of written bytes (composer)
        return work.length();
    }

    /** Returns the current buffer as a Java byte array. Only the first <code>getLength()</code> bytes of this buffer are valid. */
    @Override
    public byte[] getBuffer() {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Buffer retrieved, {} bytes written, {} object reuses", getLength(), numberOfObjectReuses);
        return work.getCurrentBuffer();
    }

    /** returns the result as a deep copy byte array of precise length of the result. */
    @Override
    public byte[] getBytes() {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Bytes retrieved, {} bytes written, {} object reuses", getLength(), numberOfObjectReuses);
        return work.getBytes();  // slow!
    }

    /** allows to add raw data to the produced byte array. Use this for protocol support at beginning or end of a message */
    public void addRawData(byte [] data) {
        work.write(data);
    }

    /* *************************************************************************************************
     * Serialization goes here
     **************************************************************************************************/

    // the following two methods are provided as separate methods instead of
    // code the single command each time,
    // with the intention that they max become extended or redefined and reused
    // for CSV output to files with
    // customized separators.
    // Because this class is defined as final, I hope the JIT will inline them
    // for better performance
    // THIS IS REQUIRED ONLY LOCALLY
    private void terminateField() {
        work.append(FIELD_TERMINATOR);
    }
    protected void writeNull() {
        work.append(NULL_FIELD);
    }
    @Override
    public void writeNull(FieldDefinition di) {
        work.append(NULL_FIELD);
    }

    @Override
    public void writeNullCollection(FieldDefinition di) {
        work.append(NULL_FIELD);
    }

    @Override
    public void startTransmission() {
        work.append(TRANSMISSION_BEGIN);
        writeNull();    // blank version number
    }
    @Override
    public void terminateTransmission() {
        work.append(TRANSMISSION_TERMINATOR);
        work.append(TRANSMISSION_TERMINATOR2);
    }

    @Override
    public void terminateRecord() {
        if (getWriteCRs()) {
            work.append(RECORD_OPT_TERMINATOR);
        }
        work.append(RECORD_TERMINATOR);
    }

    @Override
    public void writeSuperclassSeparator() {
        work.append(PARENT_SEPARATOR);
    }

    @Override
    public void startRecord() {
        work.append(RECORD_BEGIN);
        writeNull();  // blank version number
    }

    private void addCharSub(int c) {
        if ((c < ' ') && (c != '\t')) {
            work.append(ESCAPE_CHAR);
            work.append((byte)(c + '@'));
        } else if (c <= 127) {
            // ASCII character: this is faster
            work.append((byte)c);
        } else {
            work.appendUnicode(c);
        }
    }
    // field type specific output functions

    // character
    @Override
    public void addField(MiscElementaryDataItem di, char c) {
        addCharSub(c);
        terminateField();
    }

    protected void unicodeOut(String s) {
        // take care not to break multi-Sequences
        for (int i = 0; i < s.length();) {
            int c = s.codePointAt(i);
            addCharSub(c);
            i += Character.charCount(c);
        }
        terminateField();
    }

    // ascii only (unicode uses different method). This one does a validity check now.
    @Override
    public void addField(AlphanumericElementaryDataItem di, String s) {
        if (s != null) {
            if (di.getRestrictToAscii()) {
                // don't trust them!
                work.append(FixASCII.checkAsciiAndFixIfRequired(s, di.getLength(), di.getName()));
                terminateField();
            } else {
                unicodeOut(s);
            }
        } else {
            writeNull();
        }
    }

    // decimal
    @Override
    public void addField(NumericElementaryDataItem di, BigDecimal n) {
        if (n != null) {
            work.appendAscii(n.toPlainString());
            terminateField();
        } else {
            writeNull();
        }
    }

    // FixedPoint
    @Override
    public <F extends FixedPointBase<F>> void addField(BasicNumericElementaryDataItem di, F n) {
        if (n != null) {
            work.appendAscii(n.toString());
            terminateField();
        } else {
            writeNull();
        }
    }

    // byte
    @Override
    public void addField(BasicNumericElementaryDataItem di, byte n) {
        work.appendAscii(Byte.toString(n));
        terminateField();
    }
    // short
    @Override
    public void addField(BasicNumericElementaryDataItem di, short n) {
        work.appendAscii(Short.toString(n));
        terminateField();
    }
    // integer
    @Override
    public void addField(BasicNumericElementaryDataItem di, int n) {
        work.appendAscii(Integer.toString(n));
        terminateField();
    }

    // int(n)
    @Override
    public void addField(BasicNumericElementaryDataItem di, BigInteger n) {
        if (n != null) {
            work.appendAscii(n.toString());
            terminateField();
        } else {
            writeNull();
        }
    }

    // long
    @Override
    public void addField(BasicNumericElementaryDataItem di, long n) {
        work.appendAscii(Long.toString(n));
        terminateField();
    }

    // boolean
    @Override
    public void addField(MiscElementaryDataItem di, boolean b) {
        if (b) {
            work.append((byte) '1');
        } else {
            work.append((byte) '0');
        }
        terminateField();
    }


    // UUID
    @Override
    public void addField(MiscElementaryDataItem di, UUID n) {
        if (n != null) {
            work.appendAscii(n.toString());
            terminateField();
        } else {
            writeNull();
        }
    }

    // float
    @Override
    public void addField(BasicNumericElementaryDataItem di, float f) {
        work.appendAscii(Float.toString(f));
        terminateField();
    }

    // double
    @Override
    public void addField(BasicNumericElementaryDataItem di, double d) {
        work.appendAscii(Double.toString(d));
        terminateField();
    }

    // ByteArray: initial quick & dirty implementation
    @Override
    public void addField(BinaryElementaryDataItem di, ByteArray b) {
        if (b != null) {
            b.appendBase64(work);
            terminateField();
        } else {
            writeNull();
        }
    }

    // raw
    @Override
    public void addField(BinaryElementaryDataItem di, byte[] b) {
        if (b != null) {
            Base64.encodeToByte(work, b, 0, b.length);
            terminateField();
        } else {
            writeNull();
        }
    }

    // append a left padded ASCII String
    private void lpad(String s, int length, byte padCharacter) {
        int l = s.length();
        while (l++ < length) {
            work.append(padCharacter);
        }
        work.appendAscii(s);
    }

    // converters for DAY und TIMESTAMP
    @Override
    public void addField(TemporalElementaryDataItem di, LocalDate t) {
        if (t != null) {
            work.appendAscii(Integer.toString(DayTime.dayAsInt(t)));
            terminateField();
        } else {
            writeNull();
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDateTime t) {
        if (t != null) {
            work.append(Integer.toString(10000 * t.getYear() + 100 * t.getMonthValue() + t.getDayOfMonth()));
            int length = di.getFractionalSeconds();
            if (length >= 0) {
                // not only day, but also time
                final LocalTime tm = t.toLocalTime();
                final int seconds = tm.toSecondOfDay();
                final int milliSeconds = tm.getNano() / 1000000;
                //tmpValue = 10000 * t.getHourOfDay() + 100 * t.getMinuteOfHour() + t.getSecondOfMinute();
                if (seconds != 0 || length > 0 && milliSeconds != 0) {
                    work.append((byte) '.');
                    if (di.getHhmmss()) {
                        lpad(Integer.toString(10000 * tm.getHour() + 100 * tm.getMinute() + tm.getSecond()), 6, (byte)'0');
                    } else {
                        lpad(Integer.toString(tm.toSecondOfDay()), 6, (byte)'0');
                    }
                    if (length > 0 && milliSeconds != 0) {
                        // add milliseconds
                        lpad(Integer.toString(milliSeconds), 3, (byte)'0');
                    }
                }
            }
            terminateField();
        } else {
            writeNull();
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, Instant t) {
        if (t != null) {
            long millis = t.toEpochMilli();
            work.appendAscii(Long.toString(millis / 1000L));
            int length = di.getFractionalSeconds();
            int millisecs = (int)(millis % 1000L);
            if (length > 0 && millisecs != 0) {
                work.append((byte)'.');
                lpad(Integer.toString(millisecs), 3, (byte)'0');
            }
            terminateField();
        } else {
            writeNull();
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalTime t) {
        if (t != null) {
            int length = di.getFractionalSeconds();
            int seconds = t.toSecondOfDay();
            if (di.getHhmmss()) {
                int tmpValue = seconds / 60; // minutes and hours
                tmpValue = (100 * (tmpValue / 60)) + (tmpValue % 60);
                work.appendAscii(Integer.toString((tmpValue * 100) + (seconds % 60)));
            } else {
                work.appendAscii(Integer.toString(seconds));
            }
            if (length > 0) {
                int millis = t.getNano() / 1000000;
                if (millis  != 0) {
                   // add milliseconds
                   work.append((byte)'.');
                   lpad(Integer.toString(millis), 3, (byte)'0');
                }
            }
            terminateField();
        } else {
            writeNull();
        }
    }

    @Override
    public void startMap(FieldDefinition di, int currentMembers) {
        work.append(MAP_BEGIN);
        addField(StaticMeta.INTERNAL_INTEGER, di.getMapIndexType().ordinal());
        addField(StaticMeta.INTERNAL_INTEGER, currentMembers);
    }

    @Override
    public void startArray(FieldDefinition di, int currentMembers, int sizeOfElement) {
        work.append(ARRAY_BEGIN);
        addField(StaticMeta.INTERNAL_INTEGER, currentMembers);
    }

    @Override
    public void terminateArray() {
        work.append(ARRAY_TERMINATOR);
    }

    @Override
    public void terminateMap() {
        work.append(ARRAY_TERMINATOR);
    }

    @Override
    public void startObject(ObjectReference di, BonaCustom obj) {
        work.append(OBJECT_BEGIN);
        addField(OBJECT_CLASS, obj.ret$PQON());
        addField(REVISION_META, obj.ret$MetaData().getRevision());
    }

    @Override
    public void terminateObject(ObjectReference di, BonaCustom obj) {
        work.append(OBJECT_TERMINATOR);
    }

    // hook for inherited classes
    protected void notifyReuse(int referencedIndex) {
    }


    @Override
    public void addField(ObjectReference di, BonaCustom obj) {
        if (obj == null) {
            writeNull();
        } else {
            if (useCache) {
                Integer previousIndex = objectCache.get(obj);
                if (previousIndex != null) {
                    // reuse this instance
                    work.append(OBJECT_AGAIN);
                    addField(StaticMeta.INTERNAL_INTEGER, numberOfObjectsSerialized - previousIndex.intValue() - 1);  // 0 is same object as previous, 1 = the one before etc...
                    ++numberOfObjectReuses;
                    notifyReuse(previousIndex);
                    return;
                }
                // add the new object to the cache of known objects
                objectCache.put(obj, Integer.valueOf(numberOfObjectsSerialized++));
                // fall through
            }
            // start a new object
            startObject(di, obj);

            // do all fields (now includes terminator)
            obj.serializeSub(this);

            // terminate the new object
            terminateObject(di, obj);
        }
    }

    // enum with numeric expansion: delegate to Null/Int
    @Override
    public void addEnum(EnumDataItem di, BasicNumericElementaryDataItem ord, BonaNonTokenizableEnum n) {
        if (n == null)
            writeNull(ord);
        else
            addField(ord, n.ordinal());
    }

    // enum with alphanumeric expansion: delegate to Null/String
    @Override
    public void addEnum(EnumDataItem di, AlphanumericElementaryDataItem token, BonaTokenizableEnum n) {
        if (n == null)
            writeNull(token);
        else
            addField(token, n.getToken());
    }

    // xenum with alphanumeric expansion: delegate to Null/String
    @Override
    public void addEnum(XEnumDataItem di, AlphanumericElementaryDataItem token, XEnum<?> n) {
        if (n == null)
            writeNull(token);
        else
            addField(token, n.getToken());
    }

    @Override
    public boolean addExternal(ObjectReference di, Object obj) {
        return false;       // perform conversion by default
    }

    @Override
    public void addField(ObjectReference di, Map<String, Object> obj) {
        if (obj == null)
            writeNull(di);
        else
            unicodeOut(BonaparteJsonEscaper.asJson(obj));
    }

    @Override
    public void addField(ObjectReference di, List<Object> obj) throws RuntimeException {
        if (obj == null)
            writeNull(di);
        else
            unicodeOut(BonaparteJsonEscaper.asJson(obj));
    }

    @Override
    public void addField(ObjectReference di, Object obj) {
        if (obj == null)
            writeNull(di);
        else
            unicodeOut(BonaparteJsonEscaper.asJson(obj));
    }
}
