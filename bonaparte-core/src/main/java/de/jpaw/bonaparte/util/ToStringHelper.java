package de.jpaw.bonaparte.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import de.jpaw.bonaparte.core.BonaPortable;

public class ToStringHelper {
    public static int maxSet = -1;
    public static int maxList = -1;
    public static int maxMap = -1;
    public static boolean showTransientFields = false;

    public static String toStringML(Object obj) {
        StringBuilder _buffer = new StringBuilder(1000);
        FieldOut(_buffer, new StringBuilder("\n"), true, obj);
        return _buffer.toString();
    }

    public static String toStringSL(Object obj) {
        StringBuilder _buffer = new StringBuilder(1000);
        FieldOut(_buffer, null, false, obj);
        return _buffer.toString();
    }

    private static void delimiter(StringBuilder _buffer, StringBuilder _currentIndent, boolean ofSuperClass) {
        if (_currentIndent == null) {
            // single line output
            _buffer.append(ofSuperClass ? "< " : ", ");
        } else {
            if (ofSuperClass) {
                _buffer.append(_currentIndent);
                _buffer.append("^^^");
                _buffer.append(_currentIndent);
            } else {
                _buffer.append(",");
                _buffer.append(_currentIndent);
            }
        }
    }


    public static void FieldOut(StringBuilder _buffer, StringBuilder _currentIndent, boolean showNulls, Object value) {
        int count = 0;
        if (value == null) {
            _buffer.append("null");
        } else if (value instanceof BonaPortable) {
            BonaPortable(_buffer, _currentIndent, showNulls, value);
        } else if (value instanceof java.util.List li) {
            // output a list of objects
            boolean firstInList = true;
            _buffer.append("[");
            for (Object e : li) {
                if (!firstInList)
                    _buffer.append(", ");
                firstInList = false;
                ++count;
                if (maxList >= 0 && count > maxList) {
                    // abort, list too long
                    _buffer.append("...");
                    break;
                }
                FieldOut(_buffer, _currentIndent, showNulls, e);
            }
            _buffer.append("]");
        } else if (value instanceof java.util.Set se) {
            // output a list of objects
            boolean firstInList = true;
            _buffer.append("{");
            for (Object e : se) {
                if (!firstInList)
                    _buffer.append(", ");
                firstInList = false;
                ++count;
                if (maxSet >= 0 && count > maxSet) {
                    // abort, list too long
                    _buffer.append("...");
                    break;
                }
                FieldOut(_buffer, _currentIndent, showNulls, e);
            }
            _buffer.append("}");
        } else if (value instanceof java.util.Map ma) {
            // output a map of objects
            boolean firstInList = true;
            _buffer.append("(");
            Map<?,?> m = ma;
            for (Map.Entry<?,?> e : m.entrySet()) {
                if (!firstInList)
                    _buffer.append(", ");
                firstInList = false;
                ++count;
                if (maxMap >= 0 && count > maxMap) {
                    // abort, list too long
                    _buffer.append("...");
                    break;
                }
                _buffer.append(e.getKey().toString());
                _buffer.append(":");
                FieldOut(_buffer, _currentIndent, showNulls, e.getValue());
            }
            _buffer.append(")");
        } else {
            _buffer.append(value.toString());
        }
    }

    public static void BonaPortable(StringBuilder _buffer, StringBuilder _currentIndent, boolean showNulls, Object obj) {
        _buffer.append(obj.getClass().getSimpleName());
        _buffer.append("(");
        if (_currentIndent != null) {
            _currentIndent.append("  ");                                // indent more
            _buffer.append(_currentIndent);
        }
        // object output
        // this is mainly used for debugging, so speed is not as relevant and reflection can be used instead of generated code
        toStringHelperClassOut(_buffer, _currentIndent, showNulls, obj, obj.getClass());
        // closure
        if (_currentIndent != null) {
            _currentIndent.setLength(_currentIndent.length() - 2);      // restore previous length
            _buffer.append(_currentIndent);                             // and add for closing parenthesis
        }
        _buffer.append(")");
    }

    // returns true if at least one field has been printed
    private static boolean toStringHelperClassOut(StringBuilder _buffer, StringBuilder _currentIndent, boolean showNulls, Object obj,
            Class<?> thisClass) {
        boolean firstField = true;
        boolean didSome = false;
        if (thisClass.getSuperclass() != Object.class) {
            // descend
            didSome = toStringHelperClassOut(_buffer, _currentIndent, showNulls, obj, thisClass.getSuperclass());
            if (didSome)
                delimiter(_buffer, _currentIndent, true);
        }
        for (Field field : thisClass.getDeclaredFields()) {
            if ((field.getModifiers() & Modifier.STATIC) != 0)
                continue;           // skip static fields
            if (!showTransientFields && (field.getModifiers() & Modifier.TRANSIENT) != 0)
                continue;           // skip transient fields
            if (!firstField)
                delimiter(_buffer, _currentIndent, false);
            firstField = false;
            field.setAccessible(true); // You might want to set modifier to public first.
            Object value = null;
            try {
                value = field.get(obj);
            } catch (IllegalArgumentException e) {
                _buffer.append(field.getName());
                _buffer.append(": ***Illegal argument exception***");
                firstField = false;
            } catch (IllegalAccessException e) {
                _buffer.append(field.getName());
                _buffer.append(": ***Illegal access exception***");
                firstField = false;
            }
            if (value != null || showNulls) {
                _buffer.append(field.getName());
                _buffer.append("=");
                 FieldOut(_buffer, _currentIndent, showNulls, value);
            }
        }
        return didSome || !firstField;
    }
}
