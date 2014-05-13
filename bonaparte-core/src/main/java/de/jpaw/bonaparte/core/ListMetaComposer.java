package de.jpaw.bonaparte.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

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
import de.jpaw.enums.TokenizableEnum;
import de.jpaw.enums.XEnum;
import de.jpaw.util.ByteArray;

/** Represents composer which does not serialize, but instead appends all objects into a list.
 * Only the clear() and add() methods of the List interface are used by this implementation.
 * This implementation is not ideal, since it may unbox/rebox objects of the BonaPortables.
 * To improve it, the BonaPortable interface would need to be changed. */
public class ListMetaComposer extends NoOpComposer implements MessageComposer<RuntimeException> {
    final List<DataAndMeta<Object,FieldDefinition>> storage;
    final boolean doDeepCopies;
    
    /** Creates a new ListMetaComposer for a given preallocated external storage. */
    public ListMetaComposer(final List<DataAndMeta<Object,FieldDefinition>> storage, boolean doDeepCopies) {
        this.storage = storage;
        this.doDeepCopies = doDeepCopies;
    }
    /** Creates a new ListMetaComposer, creating an own internal storage. */
    public ListMetaComposer(boolean doDeepCopies) {
        this.storage = new ArrayList<DataAndMeta<Object,FieldDefinition>>();
        this.doDeepCopies = doDeepCopies;
    }
    
    protected void add(FieldDefinition di, Object o) {
    	storage.add(new DataAndMeta<Object,FieldDefinition>(di, o));
    }
    
    public List<DataAndMeta<Object,FieldDefinition>> getStorage() {
        return storage;
    }
    public boolean getDoDeepCopies() {
        return doDeepCopies;
    }
    
    public void reset() {
        storage.clear();
    }

    @Override
    public void writeNull(FieldDefinition di) {
        add(di,null);
    }

    @Override
    public void writeNullCollection(FieldDefinition di) {
        add(di,null);
    }


    @Override
    public void writeRecord(BonaPortable o) {
        startRecord();  // noop in the base implementation
        addField(StaticMeta.OUTER_BONAPORTABLE, o);
    }

    @Override
    public void addField(AlphanumericElementaryDataItem di, String s) {
        add(di, s);
    }

    @Override
    public void addField(MiscElementaryDataItem di, boolean b) {
        add(di, Boolean.valueOf(b));
    }

    @Override
    public void addField(MiscElementaryDataItem di, char c) {
        add(di, Character.valueOf(c));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, double d) {
        add(di, Double.valueOf(d));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, float f) {
        add(di, Float.valueOf(f));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, byte n) {
        add(di, Byte.valueOf(n));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, short n) {
        add(di, Short.valueOf(n));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, int n) {
        add(di, Integer.valueOf(n));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, long n) {
        add(di, Long.valueOf(n));
    }

    @Override
    public void addField(BasicNumericElementaryDataItem di, Integer n) {
        add(di, n);
    }

    @Override
    public void addField(NumericElementaryDataItem di, BigDecimal n) {
        add(di, n);
    }

    @Override
    public void addField(MiscElementaryDataItem di, UUID n) {
        add(di, n);
    }

    @Override
    public void addField(BinaryElementaryDataItem di, ByteArray b) {
        add(di, b);
    }

    @Override
    public void addField(BinaryElementaryDataItem di, byte[] b) {
        if (b == null)
            writeNull(di);
        add(di, doDeepCopies ? Arrays.copyOf(b, b.length) : b);
    }

    @Override
    public void addField(TemporalElementaryDataItem di, Calendar t) {
        if (t == null)
            writeNull(di);
        add(di, doDeepCopies ? (Calendar)t.clone() : t);
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDate t) {
        add(di, t);
    }

    @Override
    public void addField(TemporalElementaryDataItem di, LocalDateTime t) {
        add(di, t);
    }

    @Override
    public void addField(ObjectReference di, BonaPortable obj) {
        if (obj == null) {
            writeNull(null);
        } else {
            startObject(di, obj);
            obj.serializeSub(this);
        }
    }
    
    @Override
    public void addEnum(EnumDataItem di, BasicNumericElementaryDataItem ord, Enum<?> n) {
        add(di, n);
    }

    @Override
    public void addEnum(EnumDataItem di, AlphanumericElementaryDataItem token, TokenizableEnum n) {
        add(di, n);
    }

	@Override
	public void addEnum(XEnumDataItem di, AlphanumericElementaryDataItem token, XEnum<?> n) {
        add(di, n);
	}
}