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
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import de.jpaw.bonaparte.pojos.meta.AlphanumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BasicNumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BinaryElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.MiscElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.NumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.ObjectReference;
import de.jpaw.bonaparte.pojos.meta.TemporalElementaryDataItem;
import de.jpaw.fixedpoint.FixedPointBase;
import de.jpaw.util.ByteArray;
/**
 * The CSVComposer class.
 *
 * @author Michael Bischoff
 * @version $Revision$
 *
 *          Implements the serialization for the bonaparte format using a character Appendable, for CSV output
 */

public class CSVComposer extends AppendableComposer {

    protected boolean recordStart = true;

    //protected boolean shouldWarnWhenUsingFloat;
    protected final CSVConfiguration cfg;

    // derived data from CSVConfiguration
    protected final boolean replaceSeparator;               // true is a separator is specified and we use unquoted strings and the quote is null, but a quote replacement defined
    protected final String stringQuote;                     // quote character for strings, as a string
    protected final DateTimeFormatter dayFormat;            // day without time (Joda)
    protected final DateTimeFormatter timeFormat;           // time on second precision (Joda)
    protected final DateTimeFormatter time3Format;          // time on millisecond precision (Joda)
    protected final DateTimeFormatter timestampFormat;      // day and time on second precision (Joda)
    protected final DateTimeFormatter timestamp3Format;     // day and time on millisecond precision (Joda)
    protected final NumberFormat numberFormat;              // locale's default format for formatting float and double, covers decimal point and sign
    protected final NumberFormat fractionalFormat;          // format without grouping, otherwise close of numberFormat
    protected final NumberFormat bigDecimalFormat;          // locale's default format for formatting BigDecimal, covers decimal point and sign
    protected final DecimalFormatSymbols decimalFormatSymbols; // also used in DecimalFormat, which is a subclass of NumberFormat

    protected final DateTimeFormatter doDateTimeFormatter(DateTimeFormatter input) {
        return input.withLocale(cfg.locale);
    }

    public CSVComposer(Appendable work, CSVConfiguration cfg) {
        super(work, ObjectReuseStrategy.NONE);  // CSV does not know about object backreferences...
        this.cfg = cfg;
        this.stringQuote = (cfg.quote != null) ? String.valueOf(cfg.quote) : "";  // use this for cases where a String is required
        this.replaceSeparator = (cfg.quote == null) && cfg.quoteReplacement != null && cfg.separator != null && cfg.separator.length() > 0;
        //this.usesDefaultDecimalPoint = cfg.decimalPoint.equals(".");
        //this.shouldWarnWhenUsingFloat = cfg.decimalPoint.length() == 0;     // removing decimal points from float or double is a bad idea, because no scale is defined

        this.dayFormat          = doDateTimeFormatter(cfg.determineDayFormatter());
        this.timeFormat         = doDateTimeFormatter(cfg.determineTimeFormatter());
        this.time3Format        = doDateTimeFormatter(cfg.determineTime3Formatter());
        this.timestampFormat    = doDateTimeFormatter(cfg.determineTimestampFormatter());
        this.timestamp3Format   = doDateTimeFormatter(cfg.determineTimestamp3Formatter());

        // It is weird that we have to instantiate twice from cfg.locale, but Oracle's docs recommend to not instantiate DecimalFormat directly, and NumberFormat does not allow to retrieve
        // the instance of DecimalFormatSymbols.
        this.decimalFormatSymbols = new DecimalFormatSymbols(cfg.locale);
        this.numberFormat         = NumberFormat.getInstance(cfg.locale);
        this.numberFormat.setGroupingUsed(cfg.useGrouping);
        this.fractionalFormat     = (NumberFormat)this.numberFormat.clone();
        this.fractionalFormat.setGroupingUsed(false);
        this.bigDecimalFormat     = cfg.removePoint4BD ? null : (NumberFormat)this.numberFormat.clone();    // make a copy for BigDecimal, where we set fractional digits as required
    }

    protected void writeSeparator() throws IOException {   // nothing to do in the standard bonaparte format
        if (recordStart)
            recordStart = false;
        else
            addRawData(cfg.separator);
    }


    @Override
    protected void terminateField() {
    }

    @Override
    public void writeNull(FieldDefinition di) throws IOException {
        writeSeparator();
    }

    @Override
    public void writeNullCollection(FieldDefinition di) throws IOException {
        writeSeparator();
    }

    @Override
    public void startTransmission() {
    }

    @Override
    public void terminateTransmission() {
    }

    @Override
    public void writeSuperclassSeparator() {
    }

    @Override
    public void startRecord() {
        recordStart = true;
    }

    private void addCharSub(char c) throws IOException {
        addRawData(cfg.quote != null && c == cfg.quote ? stringQuote : c < 0x20 ? cfg.ctrlReplacement : String.valueOf(c));
    }

    // field type specific output functions

    // character
    @Override
    public void addField(MiscElementaryDataItem di, char c) throws IOException {
        writeSeparator();
        addCharSub(c);
    }

    protected void writeString(String s) throws IOException {
        writeSeparator();
        if (s != null) {
            addRawData(stringQuote);
            for (int i = 0; i < s.length(); ++i) {
                addCharSub(s.charAt(i));
            }
            addRawData(stringQuote);
        }
    }

    @Override
    public void addField(AlphanumericElementaryDataItem di, String s) throws IOException {
        // if quotes are specified, quote characters inside the string must be replaced.
        // else, separator characters (if they exist, i.e. not fixed width format) must be replaced
        if (s == null) {
            writeString(null);
        } else if (cfg.quote != null) {
            // quotes exist: replace any quotes inside the string
            writeString(s.replace(stringQuote, cfg.quoteReplacement));
        } else if (replaceSeparator) {
            // no quotes: replace any separator character
            writeString(s.replace(cfg.separator, cfg.quoteReplacement));
        } else {
            writeString(null);
        }
    }


    // int(n)
    @Override
    public void addField(BasicNumericElementaryDataItem di, BigInteger n) throws IOException {
        writeSeparator();
        if (n != null)
            super.addField(di, n);
    }

    // decimal
    @Override
    public void addField(NumericElementaryDataItem di, BigDecimal n) throws IOException {
        writeSeparator();
        if (n != null) {
            if (cfg.removePoint4BD) {
                // use standard BigDecimal formatter, and remove the "." from the output
                addRawData(n.setScale(di.getDecimalDigits()).toPlainString().replace(".", ""));
            } else {
                // use standard locale formatter to get the localized . or ,
                bigDecimalFormat.setMaximumFractionDigits(n.scale());
                bigDecimalFormat.setMinimumFractionDigits(n.scale());
                addRawData(bigDecimalFormat.format(n));
            }
        }
    }

    // output the integral part of a fixed point number. Overridden by fixed format output for padding
    protected <F extends FixedPointBase<F>> void fixedPointIntegralPart(BasicNumericElementaryDataItem di, F n, boolean sign, long integralPart) throws IOException {
        if (sign) {
            addSingleChar(decimalFormatSymbols.getMinusSign());
        }
        addRawData(numberFormat.format(integralPart));        // format using the locale's approach (potentially also using grouping)
    }

    @Override
    public <F extends FixedPointBase<F>> void addField(BasicNumericElementaryDataItem di, F n) throws IOException {
        writeSeparator();
        if (n != null) {
            // use the regular NumberFormat for the integral part, then do the decimal point and any fractional digits separately
            // the alternative way to convert to BigDecimal will be slower and also, not all methods of NumberFormat work with full precision
            long mantissa = n.getMantissa();
            final boolean sign = mantissa < 0;
            if (sign) {
                mantissa = -mantissa;
            }
            final long integralPart = mantissa / n.getUnitAsLong();
            fixedPointIntegralPart(di, n, sign, integralPart);
            if (n.scale() > 0) {
                // output a decimal point, unless it has been forbidden
                if (!cfg.removePoint4BD) {
                    addSingleChar(decimalFormatSymbols.getDecimalSeparator());
                }
                // determine the fractional digits (remove the sign!)
                final long fractional = mantissa - n.getUnitAsLong() * integralPart;
                lpad(fractionalFormat.format(fractional), n.scale(), decimalFormatSymbols.getZeroDigit());
            }
        }
    }

    // byte
    @Override
    public void addField(BasicNumericElementaryDataItem di, byte n) throws IOException {
        writeSeparator();
        super.addField(di, n);
    }
    // short
    @Override
    public void addField(BasicNumericElementaryDataItem di, short n) throws IOException {
        writeSeparator();
        super.addField(di, n);
    }
    // integer
    @Override
    public void addField(BasicNumericElementaryDataItem di, int n) throws IOException {
        writeSeparator();
        super.addField(di, n);
    }

    // long
    @Override
    public void addField(BasicNumericElementaryDataItem di, long n) throws IOException {
        writeSeparator();
        super.addField(di, n);
    }

    // boolean
    @Override
    public void addField(MiscElementaryDataItem di, boolean b) throws IOException {
        writeSeparator();
        super.addRawData(b ? cfg.booleanTrue : cfg.booleanFalse);
    }

    // float
    @Override
    public void addField(BasicNumericElementaryDataItem di, float f) throws IOException {
        writeSeparator();
        addRawData(numberFormat.format(f));            // format using the locale's approach
    }

    // double
    @Override
    public void addField(BasicNumericElementaryDataItem di, double d) throws IOException {
        writeSeparator();
        addRawData(numberFormat.format(d));            // format using the locale's approach
    }

    // UUID
    @Override
    public void addField(MiscElementaryDataItem di, UUID n) throws IOException {
        writeSeparator();
        if (n != null) {
            addRawData(stringQuote);
            super.addField(di, n);
            addRawData(stringQuote);
        }
    }

    // ByteArray: initial quick & dirty implementation
    @Override
    public void addField(BinaryElementaryDataItem di, ByteArray b) throws IOException {
        writeSeparator();
        if (b != null) {
            addRawData(stringQuote);
            super.addField(di, b);
            addRawData(stringQuote);
        }
    }

    // raw
    @Override
    public void addField(BinaryElementaryDataItem di, byte[] b) throws IOException {
        writeSeparator();
        if (b != null) {
            addRawData(stringQuote);
            super.addField(di, b);
            addRawData(stringQuote);
        }
    }

    // converters for DAY und TIMESTAMP
    @Override
    public void addField(TemporalElementaryDataItem di, LocalDate t) throws IOException {
        writeSeparator();
        if (t != null) {
            if (cfg.datesQuoted)
                addRawData(stringQuote);
            addRawData(t.format(dayFormat));
            if (cfg.datesQuoted)
                addRawData(stringQuote);
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalTime t) throws IOException {
        writeSeparator();
        if (t != null) {
            if (cfg.datesQuoted)
                addRawData(stringQuote);
            if (di.getFractionalSeconds() <= 0)
                addRawData(t.format(timeFormat));   // second precision
            else
                addRawData(t.format(time3Format));  // millisecond precision
            if (cfg.datesQuoted)
                addRawData(stringQuote);
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDateTime t) throws IOException {
        writeSeparator();
        if (t != null) {
            if (cfg.timeZone != null && cfg.timeZone != ZoneOffset.UTC) {
                // convert to other time zone
                t = t.atZone(ZoneOffset.UTC).withZoneSameInstant(cfg.timeZone).toLocalDateTime();
            }
            if (cfg.datesQuoted)
                addRawData(stringQuote);
            if (di.getFractionalSeconds() <= 0)
                addRawData(t.format(timestampFormat));   // second precision
            else
                addRawData(t.format(timestamp3Format));  // millisecond precision
            if (cfg.datesQuoted)
                addRawData(stringQuote);
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, Instant t) throws IOException {
        writeSeparator();
        if (t != null) {
            addRawData(Long.toString(t.toEpochMilli()));
        }
    }

    @Override
    public void startMap(FieldDefinition di, int currentMembers) throws IOException {
        if (cfg.mapStart != null && cfg.mapStart.length() > 0) {
            super.addRawData(cfg.mapStart);
            recordStart = true;
        }
    }

    @Override
    public void startArray(FieldDefinition di, int currentMembers, int sizeOfElement) throws IOException {
        if (cfg.arrayStart != null && cfg.arrayStart.length() > 0) {
            super.addRawData(cfg.arrayStart);
            recordStart = true;
        }
    }

    @Override
    public void terminateArray() throws IOException {
        if (cfg.arrayEnd != null && cfg.arrayEnd.length() > 0) {
            super.addRawData(cfg.arrayEnd);
            recordStart = true;
        }
    }

    @Override
    public void terminateMap() throws IOException {
        if (cfg.mapEnd != null && cfg.mapEnd.length() > 0) {
            super.addRawData(cfg.mapEnd);
            recordStart = true;
        }
    }

    @Override
    public void startObject(ObjectReference di, BonaCustom obj) throws IOException {
        if (cfg.objectStart != null && cfg.objectStart.length() > 0) {
            super.addRawData(cfg.objectStart);
            recordStart = true;
        }
    }

    @Override
    public void terminateObject(ObjectReference di, BonaCustom obj) throws IOException {
        if (cfg.objectEnd != null && cfg.objectEnd.length() > 0) {
            super.addRawData(cfg.objectEnd);
            recordStart = true;
        }
    }

    @Override
    public void addField(ObjectReference di, BonaCustom obj) throws IOException {
        if (obj != null) {
            startObject(di, obj);
            // do all fields
            obj.serializeSub(this);
            terminateObject(di, obj);
        }
    }
    @Override
    public void addField(ObjectReference di, Map<String, Object> obj) throws IOException {
        writeSeparator();
        if (obj != null) {
            addRawData(stringQuote);
            super.addField(di, obj);
            addRawData(stringQuote);
        }
    }

    @Override
    public void addField(ObjectReference di, Object obj) throws IOException {
        writeSeparator();
        if (obj != null) {
            addRawData(stringQuote);
            super.addField(di, obj);
            addRawData(stringQuote);
        }
    }
}
