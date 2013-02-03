package com.tinkerpop.rexster.protocol.msg;

import com.tinkerpop.rexster.client.RexProException;
import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Blake Eggleston (bdeggleston.github.com)
 */
public class RexProMessageMetaFieldTest {

    /**
     * Tests the validation of various field permutations
     */
    @Test
    public void fieldValidation() {
        RexProMessageMeta meta = new RexProMessageMeta();

        try{
            meta.put("floatVal", 3.14f);
            new RexProMessageMetaField<Float>("floatVal", true, Float.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        try{
            meta.put("doubleVal", 3.14);
            new RexProMessageMetaField<Double>("doubleVal", true, Double.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        try{
            meta.put("intVal", 123);
            new RexProMessageMetaField<Integer>("intVal", true, Integer.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        try{
            meta.put("boolVal", true);
            new RexProMessageMetaField<Boolean>("boolVal", true, Boolean.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        try{
            meta.put("stringVal", "la la la");
            new RexProMessageMetaField<String>("stringVal", true, String.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        try{
            meta.put("wrongType", "should be int");
            new RexProMessageMetaField<Integer>("wrongType", true, Integer.class).validateMeta(meta);
            Assert.fail("expecting RexProException to be thrown");
        } catch (RexProException ex) {
            //exception is expected
        }
    }

    /**
     * Tests that default values are inserted if the key is missing
     */
    @Test
    public void defaultValueInsertion() {
        RexProMessageMeta meta = new RexProMessageMeta();

        try{
            new RexProMessageMetaField<Float>("floatVal", true, 1.23f, Float.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        Assert.assertTrue(meta.containsKey("floatVal"));
        Assert.assertEquals(1.23f, meta.get("floatVal"));
    }

    /**
     * Tests that missing meta values that are not required do not raise exceptions
     */
    @Test
    public void notRequiredTest() {
        RexProMessageMeta meta = new RexProMessageMeta();

        try{
            new RexProMessageMetaField<Float>("floatVal", false, Float.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        Assert.assertFalse(meta.containsKey("floatVal"));
        Assert.assertEquals(null, meta.get("floatVal"));

    }

    /**
     * Tests that missing meta values that are required raise exceptions
     */
    @Test
    public void requiredTest() {
        RexProMessageMeta meta = new RexProMessageMeta();

        try{
            new RexProMessageMetaField<Float>("floatVal", true, Float.class).validateMeta(meta);
            Assert.fail("expecting RexProException to be thrown");
        } catch (RexProException ex) {
            //exception is expected
        }

        //default value should override missing value
        try{
            new RexProMessageMetaField<Integer>("intVal", true, 1, Integer.class).validateMeta(meta);
        } catch (RexProException ex) {
            Assert.fail("unexpected exception thrown: " + ex.toString());
        }

        Assert.assertTrue(meta.containsKey("intVal"));
        Assert.assertEquals(1, meta.get("intVal"));

    }

}
