package de.jpaw.bonaparte.android;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.core.NoOpComposer;
import de.jpaw.bonaparte.pojos.meta.AlphanumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.BinaryElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.EnumDataItem;
import de.jpaw.bonaparte.pojos.meta.FieldDefinition;
import de.jpaw.bonaparte.pojos.meta.MiscElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.NumericElementaryDataItem;
import de.jpaw.bonaparte.pojos.meta.TemporalElementaryDataItem;
import de.jpaw.enums.TokenizableEnum;
import de.jpaw.util.ByteArray;

/** Composer which is designed to work as a delegate for a foldingComposer.
 * Therefore object output itself is not supported. */

public class LinearLayoutComposer extends NoOpComposer {
    protected final LinearLayout rowWidget;
    protected int column = 0;
    protected int rownum = -1;
    protected static final String [] BIGDECIMAL_FORMATS = {
        "#0",
        "#0.#",
        "#0.##",
        "#0.###",
        "#0.####",
        "#0.#####",
        "#0.######",
        "#0.#######",
        "#0.########",
        "#0.#########",
        "#0.##########",
        "#0.###########",
        "#0.############",
        "#0.#############",
        "#0.##############",
        "#0.###############",
        "#0.################",
        "#0.#################",
        "#0.##################"
    };

    public LinearLayoutComposer(final LinearLayout rowWidget) {
        this.rowWidget = rowWidget;
        column = 0;
    }

    /**************************************************************************************************
     * Serialization goes here
     **************************************************************************************************/

    protected void writeNull() {
        newTextView(null);
    }

    @Override
    public void writeNull(FieldDefinition di) {
        newTextView(null);
    }

    @Override
    public void writeNullCollection(FieldDefinition di) {
        ++column;   // no output for empty Views, but ensure that everything goes nicely into the correct column
    }

    @Override
    public void startTransmission() {
        rownum = -1;
    }
    @Override
    public void terminateTransmission() {
    }

    @Override
    public void terminateRecord() {
    }

    @Override
    public void writeSuperclassSeparator() {
    }

    @Override
    public void startRecord() {
        ++rownum;
        column = -1;
    }

    @Override
    public void writeRecord(BonaPortable o) {
        startRecord();
        addField(o);
        terminateRecord();
    }

    private View newView() {
        ++column;
        return rowWidget.getChildAt(column);
    }
    private void newTextView(String s) {
        ++column;
        ((TextView)rowWidget.getChildAt(column)).setText(s != null ? s : "");
    }

    // field type specific output functions

    // character
    @Override
    public void addField(char c) {
        newTextView(String.valueOf(c));
    }
    // ascii only (unicode uses different method)
    @Override
    public void addField(AlphanumericElementaryDataItem di, String s) {
        newTextView(s);
    }

    // decimal
    @Override
    public void addField(NumericElementaryDataItem di, BigDecimal n) {
        if (n != null) {
            int digs = di.getDecimalDigits();
            DecimalFormat df = new DecimalFormat(BIGDECIMAL_FORMATS[digs]);
            df.setMaximumFractionDigits(digs);
            df.setMinimumFractionDigits(digs);
            df.setGroupingUsed(true);
            newTextView(df.format(n));
        } else {
            writeNull();
        }
    }

    // byte
    @Override
    public void addField(byte n) {
        newTextView(Byte.toString(n));
    }
    // short
    @Override
    public void addField(short n) {
        newTextView(Short.toString(n));
    }
    // integer
    @Override
    public void addField(int n) {
        newTextView(Integer.toString(n));
    }

    // int(n)
    @Override
    public void addField(NumericElementaryDataItem di, Integer n) {
        if (n != null) {
            newTextView(Integer.toString(n));
        } else {
            writeNull();
        }
    }

    // long
    @Override
    public void addField(long n) {
        newTextView(Long.toString(n));
    }

    // boolean
    @Override
    public void addField(boolean b) {
        ((CheckBox)(newView())).setChecked(b);
    }

    // float
    @Override
    public void addField(float f) {
        newTextView(Float.toString(f));
    }

    // double
    @Override
    public void addField(double d) {
        newTextView(Double.toString(d));
    }

    // UUID
    @Override
    public void addField(MiscElementaryDataItem di, UUID n) {
        if (n != null) {
            newTextView(n.toString());
        } else {
            writeNull();
        }
    }

    // ByteArray: initial quick & dirty implementation
    @Override
    public void addField(BinaryElementaryDataItem di, ByteArray b) {
        if (b != null) {
            newTextView("(Binary)");
            //            ByteBuilder tmp = new ByteBuilder((b.length() * 2) + 4, null);
            //            Base64.encodeToByte(tmp, b.getBytes(), 0, b.length());
            //            newView().setViewValue(new String(tmp.getCurrentBuffer(), 0, tmp.length()));
        } else {
            writeNull();
        }
    }

    // raw
    @Override
    public void addField(BinaryElementaryDataItem di, byte[] b) {
        if (b != null) {
            newTextView("(Binary)");
            //            ByteBuilder tmp = new ByteBuilder((b.length * 2) + 4, null);
            //            Base64.encodeToByte(tmp, b, 0, b.length);
            //            newView().setViewValue(new String(tmp.getCurrentBuffer(), 0, tmp.length()));
        } else {
            writeNull();
        }
    }

    // converters for DAY und TIMESTAMP
    @Override
    public void addField(TemporalElementaryDataItem di, Calendar t) {
        if (t != null) {
            newTextView(t.toString());
        } else {
            writeNull();
        }
    }
    @Override
    public void addField(TemporalElementaryDataItem di, LocalDate t) {
        if (t != null) {
            newTextView(t.toString());
        } else {
            writeNull();
        }
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDateTime t) {
        if (t != null) {
            newTextView(t.toString());
        } else {
            writeNull();
        }
    }

    @Override
    public void startMap(int currentMembers, int indexID) {
    }

    @Override
    public void startArray(int currentMembers, int maxMembers, int sizeOfElement) {
    }

    @Override
    public void terminateArray() {
    }

    @Override
    public void terminateMap() {
    }

    @Override
    public void startObject(BonaPortable obj) {
    }

    /** Adding objects will lead to column misalignment if the objects itself are null. */
    @Override
    public void addField(BonaPortable obj) {
        if (obj != null) {
            // do all fields (now includes terminator)
            obj.serializeSub(this);
        }
    }

    // enum with numeric expansion: delegate to Null/Int
    @Override
    public void addEnum(EnumDataItem di, NumericElementaryDataItem ord, Enum<?> n) {
        if (n == null) {
            writeNull(ord);
        } else {
            addField(ord, n.ordinal());
        }
    }

    // enum with alphanumeric expansion: delegate to Null/String
    @Override
    public void addEnum(EnumDataItem di, AlphanumericElementaryDataItem token, TokenizableEnum n) {
        if (n == null) {
            writeNull(token);
        } else {
            addField(token, n.getToken());
        }
    }
}