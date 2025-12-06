package de.jpaw.bonaparte.core;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jpaw.bonaparte.enums.BonaByteEnumSet;
import de.jpaw.bonaparte.enums.BonaIntEnumSet;
import de.jpaw.bonaparte.enums.BonaLongEnumSet;
import de.jpaw.bonaparte.enums.BonaNonTokenizableEnum;
import de.jpaw.bonaparte.enums.BonaShortEnumSet;
import de.jpaw.bonaparte.enums.BonaStringEnumSet;
import de.jpaw.bonaparte.enums.BonaTokenizableEnum;
import de.jpaw.bonaparte.pojos.meta.AlphanumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.AlphanumericEnumSetDataItem;
import de.jpaw.bonaparte.pojos.meta.BasicNumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BinaryElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.EnumDataItem;
import de.jpaw.bonaparte.pojos.meta.EnumDefinition;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.MiscElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.NumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.NumericEnumSetDataItem;
import de.jpaw.bonaparte.pojos.meta.ObjectReference;
import de.jpaw.bonaparte.pojos.meta.TemporalElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.XEnumDataItem;
import de.jpaw.bonaparte.pojos.meta.XEnumSetDataItem;
import de.jpaw.enums.TokenizableEnum;
import de.jpaw.enums.XEnum;
import de.jpaw.fixedpoint.FixedPointBase;
import de.jpaw.json.JsonEscaper;
import de.jpaw.util.Base64;
import de.jpaw.util.ByteArray;
import de.jpaw.util.ByteBuilder;

/**
 * This class generates TOON (Token-Oriented Object Notation) output.
 * TOON is a line-oriented, indentation-based text format that encodes the JSON data model
 * with explicit structure and minimal quoting.
 *
 * See https://github.com/toon-format/toon for the specification, as well as https://github.com/toon-format/spec
 *
 * Key features of this implementation:
 * - Strings are always quoted (as per requirement)
 * - Objects use indentation instead of braces
 * - Arrays declare their length with bracket notation
 * - Numbers are output in plain decimal form (no scientific notation)
 * - Date/time types are formatted as ISO-8601 strings
 *
 * @author github copilot and Michael Bischoff (jpaw.de)
 */
public class BonaparteToonComposer extends AbstractMessageComposer<IOException> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BonaparteToonComposer.class);

    // ISO-8601 formatters for temporal types
    protected static final DateTimeFormatter LOCAL_DATE_ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    protected static final DateTimeFormatter LOCAL_TIME_ISO = DateTimeFormatter.ISO_LOCAL_TIME;
    protected static final DateTimeFormatter LOCAL_DATETIME_ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    protected static final DateTimeFormatter INSTANT_ISO = DateTimeFormatter.ISO_INSTANT;

    protected final Appendable out;
    protected final JsonEscaper jsonEscaper;
    protected int indentLevel = 0;
    protected int indentSize = 2;  // Default indent size in spaces
    protected String currentClass = "N/A";
    protected boolean writeEnumOrdinals = true;
    protected boolean writeEnumTokens = true;

    // Track whether we're in an array context
    protected boolean inArray = false;
    protected int arrayElementCount = 0;
    protected int expectedArrayElements = 0;

    public BonaparteToonComposer(Appendable out) {
        this(out, 2);
    }

    public BonaparteToonComposer(Appendable out, int indentSize) {
        this.out = out;
        this.indentSize = indentSize;
        this.jsonEscaper = new BonaparteJsonEscaper(out);
    }

    public BonaparteToonComposer(Appendable out, JsonEscaper jsonEscaper) {
        this(out, 2, jsonEscaper);
    }

    public BonaparteToonComposer(Appendable out, int indentSize, JsonEscaper jsonEscaper) {
        this.out = out;
        this.indentSize = indentSize;
        this.jsonEscaper = jsonEscaper;
    }

    /**
     * Converts a BonaCustom object to TOON string.
     */
    public static String toToonString(BonaCustom obj) {
        if (obj == null)
            return null;
        StringBuilder buff = new StringBuilder(4000);
        BonaparteToonComposer composer = new BonaparteToonComposer(buff);
        try {
            composer.writeRecord(obj);
        } catch (IOException e) {
            LOGGER.error("Serialization exception: ", e);
            throw new RuntimeException(e);
        }
        return buff.toString();
    }

    /**
     * Writes indentation for the current level.
     */
    protected void writeIndent() throws IOException {
        for (int i = 0; i < indentLevel * indentSize; i++) {
            out.append(' ');
        }
    }

    /**
     * Writes a newline.
     */
    protected void newLine() throws IOException {
        out.append('\n');
    }

    /**
     * Writes a quoted string, always using quotes as per specification.
     * Escapes special characters: \, ", newline, carriage return, tab.
     */
    protected void writeQuotedString(String s) throws IOException {
        if (s == null) {
            out.append("null");
            return;
        }
        jsonEscaper.outputUnicodeWithControls(s);
    }

    /**
     * Helper method to write a string value (always quoted).
     * Handles both array and non-array contexts.
     */
    protected void writeStringValue(FieldDefinition di, String value) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            writeQuotedString(value);
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(' ');
            writeQuotedString(value);
            newLine();
        }
    }

    /**
     * Helper method to write a string value (always unquoted).
     * Handles both array and non-array contexts.
     */
    protected void writeUnquotedStringValue(FieldDefinition di, String value) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append(value);
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(' ');
            out.append(value);
            newLine();
        }
    }

    /**
     * Writes a key followed by colon.
     */
    protected void writeKey(String key) throws IOException {
        // Keys matching ^[A-Za-z_][A-Za-z0-9_.]*$ can be unquoted, but we'll keep it simple
        // and quote keys that need it
        if (needsQuoting(key)) {
            writeQuotedString(key);
        } else {
            out.append(key);
        }
        out.append(':');
    }

    /**
     * Check if a key needs quoting.
     */
    protected boolean needsQuoting(String key) {
        if (key == null || key.isEmpty()) {
            return true;
        }
        char first = key.charAt(0);
        if (!Character.isLetter(first) && first != '_') {
            return true;
        }
        for (int i = 1; i < key.length(); i++) {
            char c = key.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats a number in plain decimal format (no scientific notation).
     * Removes trailing zeros after decimal point.
     */
    protected String formatNumber(BigDecimal value) {
        return stripTrailingZero(value.stripTrailingZeros().toPlainString());
    }

    /**
     * Strips trailing ".0" from number strings.
     */
    protected String stripTrailingZero(String value) {
        if (value.endsWith(".0")) {
            return value.substring(0, value.length() - 2);
        }
        return value;
    }

    @Override
    public void writeObject(BonaCustom o) throws IOException {
        objectOutSub(StaticMeta.OUTER_BONAPORTABLE, o);
    }

    @Override
    public void startTransmission() throws IOException {
        // Root array format: [N]:
        // We'll track this in writeTransmission
    }

    @Override
    public void startRecord() throws IOException {
        // No-op for individual records
    }

    @Override
    public void startObject(ObjectReference di, BonaCustom obj) throws IOException {
        // In TOON, objects don't have explicit delimiters, just indentation
        // The field name has already been written by addField
    }

    @Override
    public void writeSuperclassSeparator() throws IOException {
        // No separator needed in TOON
    }

    @Override
    public void terminateObject(ObjectReference di, BonaCustom obj) throws IOException {
        // No closing delimiter needed
    }

    @Override
    public void startArray(FieldDefinition di, int currentMembers, int sizeOfElement) throws IOException {
        // Write array header: key[N]:
        writeIndent();
        writeKey(di.getName());
        out.append('[');
        out.append(Integer.toString(currentMembers));
        out.append(']');
        out.append(':');

        if (currentMembers > 0) {
            // For inline primitive arrays
            out.append(' ');
        } else {
            newLine();
        }

        inArray = true;
        arrayElementCount = 0;
        expectedArrayElements = currentMembers;
    }

    @Override
    public void startMap(FieldDefinition di, int currentMembers) throws IOException {
        // Maps are treated as objects in TOON
        writeIndent();
        writeKey(di.getName());
        newLine();
        indentLevel++;
    }

    @Override
    public void terminateMap() throws IOException {
        indentLevel--;
    }

    @Override
    public void terminateArray() throws IOException {
        if (arrayElementCount > 0 && expectedArrayElements > 0) {
            // Only write newline if we had inline elements
            newLine();
        }
        inArray = false;
        arrayElementCount = 0;
        expectedArrayElements = 0;
    }

    @Override
    public void terminateRecord() throws IOException {
        // TOON documents don't have a trailing newline
    }

    @Override
    public void terminateTransmission() throws IOException {
        // No closing delimiter for root array
    }

    @Override
    public void writeRecord(BonaCustom o) throws IOException {
        startRecord();
        addField(StaticMeta.OUTER_BONAPORTABLE, o);
        terminateRecord();
    }

    @Override
    public void writeNull(FieldDefinition di) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append("null");
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(" null");
            newLine();
        }
    }

    @Override
    public void writeNullCollection(FieldDefinition di) throws IOException {
        writeNull(di);
    }

    @Override
    public void addField(MiscElementaryDataItem di, boolean b) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append(b ? "true" : "false");
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(' ');
            out.append(b ? "true" : "false");
            newLine();
        }
    }

    @Override
    public void addField(MiscElementaryDataItem di, char c) throws IOException {
        writeStringValue(di, String.valueOf(c));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, double d) throws IOException {
        String value = formatNumber(BigDecimal.valueOf(d));
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append(value);
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(' ');
            out.append(value);
            newLine();
        }
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, float f) throws IOException {
        // Use BigDecimal to ensure plain format without scientific notation
        String value = formatNumber(BigDecimal.valueOf(f));
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append(value);
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(' ');
            out.append(value);
            newLine();
        }
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, byte n) throws IOException {
        addIntField(di.getName(), n);
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, short n) throws IOException {
        addIntField(di.getName(), n);
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, int n) throws IOException {
        addIntField(di.getName(), n);
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, long n) throws IOException {
        addLongField(di.getName(), n);
    }

    protected void addIntField(String name, int value) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append(Integer.toString(value));
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(name);
            out.append(' ');
            out.append(Integer.toString(value));
            newLine();
        }
    }

    protected void addLongField(String name, long value) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            out.append(Long.toString(value));
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(name);
            out.append(' ');
            out.append(Long.toString(value));
            newLine();
        }
    }

    @Override
    public void addField(AlphanumericElementaryDataItem di, String s) throws IOException {
        if (inArray) {
            if (arrayElementCount > 0) {
                out.append(',');
            }
            writeQuotedString(s);
            arrayElementCount++;
        } else {
            writeIndent();
            writeKey(di.getName());
            out.append(' ');
            writeQuotedString(s);
            newLine();
        }
    }

    protected void objectOutSub(ObjectReference di, BonaCustom obj) throws IOException {
        String previousClass = currentClass;
        currentClass = di.getName();
        boolean wasInArray = inArray;
        inArray = false;

        startObject(di, obj);
        indentLevel++;
        obj.serializeSub(this);
        indentLevel--;
        terminateObject(di, obj);

        currentClass = previousClass;
        inArray = wasInArray;
    }

    @Override
    public void addField(ObjectReference di, BonaCustom obj) throws IOException {
        if (obj == null) {
            writeNull(di);
            return;
        }

        if (inArray) {
            // Arrays of objects not supported - would require list item format with "- " prefix
            throw new UnsupportedOperationException("Arrays of objects not supported in TOON format");
        } else {
            writeIndent();
            writeKey(di.getName());
            newLine();
            objectOutSub(di, obj);
        }
    }

    @Override
    public void addField(MiscElementaryDataItem di, UUID n) throws IOException {
        writeUnquotedStringValue(di, n == null ? null : n.toString());
    }

    @Override
    public void addField(BinaryElementaryDataItem di, ByteArray b) throws IOException {
        if (b == null) {
            writeNull(di);
        } else {
            ByteBuilder tmp = new ByteBuilder((b.length() * 2) + 4, null);
            Base64.encodeToByte(tmp, b.getBytes(), 0, b.length());
            String s = new String(tmp.getCurrentBuffer(), 0, tmp.length());
            writeUnquotedStringValue(di, s);
        }
    }

    @Override
    public void addField(BinaryElementaryDataItem di, byte[] b) throws IOException {
        if (b == null) {
            writeNull(di);
        } else {
            ByteBuilder tmp = new ByteBuilder((b.length * 2) + 4, null);
            Base64.encodeToByte(tmp, b, 0, b.length);
            String s = new String(tmp.getCurrentBuffer(), 0, tmp.length());
            writeUnquotedStringValue(di, s);
        }
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, BigInteger n) throws IOException {
        writeUnquotedStringValue(di, n == null ? null : n.toString());
    }

    @Override
    public void addField(NumericElementaryDataItem di, BigDecimal n) throws IOException {
        writeUnquotedStringValue(di, n == null ? null : formatNumber(n));
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDate t) throws IOException {
        writeUnquotedStringValue(di, t == null ? null : t.format(LOCAL_DATE_ISO));
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDateTime t) throws IOException {
        writeUnquotedStringValue(di, t == null ? null : t.format(LOCAL_DATETIME_ISO));
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalTime t) throws IOException {
        writeUnquotedStringValue(di, t == null ? null : t.format(LOCAL_TIME_ISO));
    }

    @Override
    public void addField(TemporalElementaryDataItem di, Instant t) throws IOException {
        writeUnquotedStringValue(di, t == null ? null : t.toString());  // Uses ISO_INSTANT format
    }

    @Override
    public void addEnum(EnumDataItem di, BasicNumericElementaryDataItem ord, BonaNonTokenizableEnum n) throws IOException {
        String value = n == null ? null : writeEnumOrdinals ? Integer.toString(n.ordinal()) : n.name();
        writeUnquotedStringValue(di, value);
    }

    @Override
    public void addEnum(EnumDataItem di, AlphanumericElementaryDataItem token, BonaTokenizableEnum n) throws IOException {
        writeUnquotedStringValue(di, n == null ? null : (writeEnumTokens ? n.getToken() : n.name()));
    }

    @Override
    public void addEnum(XEnumDataItem di, AlphanumericElementaryDataItem token, XEnum<?> n) throws IOException {
        writeUnquotedStringValue(di, n == null ? null : (writeEnumTokens ? n.getToken() : n.name()));
    }

    @Override
    public boolean addExternal(ObjectReference di, Object obj) throws IOException {
        return false; // perform conversion by default
    }

    @Override
    public void addField(ObjectReference di, Map<String, Object> obj) throws IOException {
        if (obj == null) {
            writeNull(di);
            return;
        }
        throw new UnsupportedOperationException("Direct Map serialization not supported in TOON format");
    }

    @Override
    public void addField(ObjectReference di, List<Object> obj) throws IOException {
        if (obj == null) {
            writeNull(di);
            return;
        }

        // Serialize List similar to array format: key[N]: value1,value2,...
        writeIndent();
        writeKey(di.getName());
        out.append('[');
        out.append(Integer.toString(obj.size()));
        out.append(']');
        out.append(':');

        if (obj.size() > 0) {
            out.append(' ');
            boolean first = true;
            for (Object element : obj) {
                if (!first) {
                    out.append(',');
                }
                writeListElement(element);
                first = false;
            }
        }
        newLine();
    }

    /**
     * Writes a single element from a List.
     * Handles primitives, strings, numbers, booleans, characters, and null.
     */
    protected void writeListElement(Object element) throws IOException {
        if (element == null) {
            out.append("null");
        } else if (element instanceof String) {
            writeQuotedString((String) element);
        } else if (element instanceof Boolean) {
            out.append(element.toString());
        } else if (element instanceof Number) {
            // Format numbers properly
            if (element instanceof Float) {
                out.append(formatNumber(BigDecimal.valueOf(((Float) element).floatValue())));
            } else if (element instanceof Double) {
                out.append(formatNumber(BigDecimal.valueOf(((Double) element).doubleValue())));
            } else {
                out.append(element.toString());
            }
        } else if (element instanceof Character) {
            writeQuotedString(element.toString());
        } else {
            // For other objects, convert to string and quote
            writeQuotedString(element.toString());
        }
    }

    @Override
    public void addField(ObjectReference di, Object obj) throws IOException {
        if (obj == null) {
            writeNull(di);
            return;
        }
        throw new UnsupportedOperationException("Direct Object serialization not supported in TOON format");
    }

    @Override
    public <S extends Enum<S>> void addField(NumericEnumSetDataItem di, BonaByteEnumSet<S> n) throws IOException {
        if (n == null) {
            writeNull(di);
        } else if (writeEnumOrdinals) {
            addIntField(di.getName(), n.getBitmap());
        } else {
            writeEnumSetAsArray(di, n.getBitmap(), di.getBaseEnumset().getBaseEnum());
        }
    }

    @Override
    public <S extends Enum<S>> void addField(NumericEnumSetDataItem di, BonaShortEnumSet<S> n) throws IOException {
        if (n == null) {
            writeNull(di);
        } else if (writeEnumOrdinals) {
            addIntField(di.getName(), n.getBitmap());
        } else {
            writeEnumSetAsArray(di, n.getBitmap(), di.getBaseEnumset().getBaseEnum());
        }
    }

    @Override
    public <S extends Enum<S>> void addField(NumericEnumSetDataItem di, BonaIntEnumSet<S> n) throws IOException {
        if (n == null) {
            writeNull(di);
        } else if (writeEnumOrdinals) {
            addIntField(di.getName(), n.getBitmap());
        } else {
            writeEnumSetAsArray(di, n.getBitmap(), di.getBaseEnumset().getBaseEnum());
        }
    }

    @Override
    public <S extends Enum<S>> void addField(NumericEnumSetDataItem di, BonaLongEnumSet<S> n) throws IOException {
        if (n == null) {
            writeNull(di);
        } else if (writeEnumOrdinals) {
            addLongField(di.getName(), n.getBitmap());
        } else {
            writeEnumSetAsArray(di, n.getBitmap(), di.getBaseEnumset().getBaseEnum());
        }
    }

    protected void writeEnumSetAsArray(FieldDefinition di, long bitmap, EnumDefinition edi) throws IOException {
        // Count set bits
        int count = Long.bitCount(bitmap);

        writeIndent();
        writeKey(di.getName());
        out.append('[');
        out.append(Integer.toString(count));
        out.append(']');
        out.append(':');

        if (count > 0) {
            out.append(' ');
            List<String> ids = edi.getIds();
            int ordinal = 0;
            boolean first = true;
            long n = bitmap;
            while (n != 0L) {
                if ((n & 1L) != 0L) {
                    if (!first) {
                        out.append(',');
                    }
                    out.append(ids.get(ordinal));
                    first = false;
                }
                ++ordinal;
                n >>>= 1;
            }
        }
        newLine();
    }

    @Override
    public <S extends TokenizableEnum> void addField(AlphanumericEnumSetDataItem di, BonaStringEnumSet<S> e) throws IOException {
        if (e == null) {
            writeNull(di);
        } else if (writeEnumTokens) {
            // Write bitmap as a string
            writeStringValue(di, e.getBitmap());
        } else {
            // Write as array of enum names
            writeIndent();
            writeKey(di.getName());
            out.append('[');
            out.append(Integer.toString(e.size()));
            out.append(']');
            out.append(':');

            if (e.size() > 0) {
                out.append(' ');
                boolean first = true;
                for (TokenizableEnum t : e) {
                    if (!first) {
                        out.append(',');
                    }
                    out.append(t.name());
                    first = false;
                }
            }
            newLine();
        }
    }

    @Override
    public <S extends TokenizableEnum> void addField(XEnumSetDataItem di, BonaStringEnumSet<S> e) throws IOException {
        if (e == null) {
            writeNull(di);
            return;
        }
        throw new UnsupportedOperationException("XEnumSetDataItem not supported in TOON format");
    }

    @Override
    public <F extends FixedPointBase<F>> void addField(BasicNumericElementaryDataItem di, F n) throws IOException {
        writeUnquotedStringValue(di, n == null ? null : n.toString());
    }

    // Getters and setters for configuration
    public boolean isWriteEnumOrdinals() {
        return writeEnumOrdinals;
    }

    public void setWriteEnumOrdinals(boolean writeEnumOrdinals) {
        this.writeEnumOrdinals = writeEnumOrdinals;
    }

    public boolean isWriteEnumTokens() {
        return writeEnumTokens;
    }

    public void setWriteEnumTokens(boolean writeEnumTokens) {
        this.writeEnumTokens = writeEnumTokens;
    }
}
