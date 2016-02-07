package de.jpaw.bonaparte.util.impl;

import de.jpaw.bonaparte.core.BonaPortable;
import de.jpaw.bonaparte.core.ByteArrayParser;
import de.jpaw.bonaparte.core.JsonComposer;
import de.jpaw.bonaparte.core.MimeTypes;
import de.jpaw.bonaparte.core.StaticMeta;
import de.jpaw.bonaparte.util.IMarshaller;
import de.jpaw.util.ApplicationException;
import de.jpaw.util.ByteArray;
import de.jpaw.util.ByteBuilder;

public class RecordMarshallerJson implements IMarshaller {
    
    @Override
    public String getContentType() {
        return MimeTypes.MIME_TYPE_JSON;
    }

    @Override
    public ByteArray marshal(BonaPortable request) {
        return ByteArray.fromString(JsonComposer.toJsonString(request));
    }

    @Override
    public BonaPortable unmarshal(ByteBuilder buffer) throws ApplicationException {
        return (new ByteArrayParser(buffer.getCurrentBuffer(), 0, buffer.length())).readObject(StaticMeta.OUTER_BONAPORTABLE, BonaPortable.class);
    }
}
