package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;

/**
 * Defines the key, type, default, and whether or not a field is required
 *
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProMessageMetaField<FieldType> {
    protected String key;
    protected Boolean required;
    protected Object defaultValue;
    protected Class fieldType;

    public RexProMessageMetaField(String key, Boolean required, FieldType defaultValue, Class<FieldType> fieldType) {
        this.key = key;
        this.required = required;
        this.defaultValue = defaultValue;
        this.fieldType = fieldType;
    }

    public RexProMessageMetaField(String key, Boolean required, Class<FieldType> fieldType) {
        this(key, required, null, fieldType);
    }

    /**
     * Validates this field in the given meta object
     *
     * @param meta: the meta object to validate
     */
    public void validateMeta(RexProMessageMeta meta) throws RexProException{
        //handle missing / null values
        if (meta.get(key) == null){
            if (defaultValue != null) {
                meta.put(key, defaultValue);
                return;
            } else if (required){
                throw new RexProException("meta value is required for " + key);
            }
            //otherwise, bail out
            return;
        }

        //handle improperly typed values
        Object val = meta.get(key);
        if (!fieldType.isInstance(val)) {
            throw new RexProException(this.fieldType.toString() + " type required for " + key + ", " + val.getClass().toString() + " found");
        }

    }
}
