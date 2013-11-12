package org.telegram.android.core.model;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.misc.SqlExceptionUtil;
import com.j256.ormlite.support.DatabaseResults;
import org.telegram.tl.TLObject;

import java.lang.reflect.Field;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 11.11.13
 * Time: 8:01
 */
public class TlDataType extends BaseDataType {

    private static final TlDataType singleTon = new TlDataType();

    public static TlDataType getSingleton() {
        return singleTon;
    }

    public TlDataType() {
        super(SqlType.BYTE_ARRAY, new Class<?>[]{TLObject.class});
    }

    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
        throw new SQLException("Default values for serializable types are not supported");
    }

    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        return results.getBytes(columnPos);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
        byte[] bytes = (byte[]) sqlArg;
        if (bytes.length == 0) {
            return null;
        }
        try {
            return TLLocalContext.getInstance().deserializeMessage(bytes);
        } catch (Exception e) {
            throw SqlExceptionUtil.create("Could not read serialized tl-object from byte array", e);
        }
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object obj) throws SQLException {
        if (obj == null) {
            return new byte[0];
        }
        if (!TLLocalContext.getInstance().isSupportedObject((TLObject) obj)) {
            throw SqlExceptionUtil.create("Unsupported tl object for serialization: " + obj, null);
        }
        try {

            return ((TLObject) obj).serialize();
        } catch (Exception e) {
            throw SqlExceptionUtil.create("Could not write serialized object to byte array: " + obj, e);
        }
    }

    @Override
    public boolean isValidForField(Field field) {
        return TLObject.class.isAssignableFrom(field.getType());
    }

    @Override
    public boolean isStreamType() {
        // can't do a getObject call beforehand so we have to check for nulls
        return true;
    }

    @Override
    public boolean isComparable() {
        return false;
    }

    @Override
    public boolean isAppropriateId() {
        return false;
    }

    @Override
    public boolean isArgumentHolderRequired() {
        return true;
    }

    @Override
    public Object resultStringToJava(FieldType fieldType, String stringValue, int columnPos) throws SQLException {
        throw new SQLException("Serializable type cannot be converted from string to Java");
    }

    @Override
    public Class<?> getPrimaryClass() {
        return TLObject.class;
    }
}
