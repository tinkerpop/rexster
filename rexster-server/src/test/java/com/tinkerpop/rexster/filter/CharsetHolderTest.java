package com.tinkerpop.rexster.filter;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CharsetHolderTest {
    @Test
    public void isSupportedValid() {
        CharsetHolder charset = new CharsetHolder("UTF-8", 1, 1);
        Assert.assertTrue(charset.isSupported());
    }

    @Test
    public void isSupportedInvalid() {
        CharsetHolder charset = new CharsetHolder("junk", 1, 1);
        Assert.assertFalse(charset.isSupported());
    }

    @Test
    public void getAcceptableCharsetsSingleNoQuality() {
        List<CharsetHolder> charsets = CharsetHolder.getAcceptableCharsets("UTF-8");

        // first item will be UTF-8
        CharsetHolder charset = charsets.get(0);

        Assert.assertEquals("UTF-8", charset.getCharset());
        Assert.assertEquals(1f, charset.getQuality(), 0);
        Assert.assertEquals(0, charset.getOrder());
    }

    @Test
    public void getAcceptableCharsetsSingleWithQuality() {
        List<CharsetHolder> charsets = CharsetHolder.getAcceptableCharsets("UTF-8;q=0.5");

        // first item will be ISO-8859-1
        CharsetHolder charset = charsets.get(0);

        Assert.assertEquals("ISO-8859-1", charset.getCharset());
        Assert.assertEquals(1f, charset.getQuality(), 0);
        Assert.assertEquals(Integer.MAX_VALUE, charset.getOrder());

        // check parsing of UTF-8
        for (CharsetHolder cs : charsets) {
            if (cs.getCharset().equals("UTF-8")) {
                charset = cs;
                break;
            }
        }

        Assert.assertEquals("UTF-8", charset.getCharset());
        Assert.assertEquals(0.5f, charset.getQuality(), 0);
        Assert.assertEquals(0, charset.getOrder());
    }

    @Test
    public void getAcceptableCharsetsSingleWithQualityAndAsterisk() {
        List<CharsetHolder> charsets = CharsetHolder.getAcceptableCharsets("UTF-8;q=0.5,*;q=0.1");

        // first item is UTF-8
        CharsetHolder charset = charsets.get(0);

        Assert.assertEquals("UTF-8", charset.getCharset());
        Assert.assertEquals(0.5f, charset.getQuality(), 0);
        Assert.assertEquals(0, charset.getOrder());

        // everything else should be 0.1 with no more UTF-8 references
        boolean noUtf8 = true;

        for (CharsetHolder cs : charsets.subList(1, charsets.size())) {
            if (cs.getCharset().equals("UTF-8")) {
                noUtf8 = false;
            }

            Assert.assertEquals(0.1f, cs.getQuality(), 0);
        }

        Assert.assertTrue(noUtf8);
    }

    @Test
    public void getAcceptableCharsetsMultipleCharsetsSameQualityUseOrder() {
        List<CharsetHolder> charsets = CharsetHolder.getAcceptableCharsets("ISO-8859-1;q=0.1,ISO-8859-2;q=0.5,UTF-8;q=0.5,*;q=0");

        // first item is ISO-8859-2
        CharsetHolder charset = charsets.get(0);
        Assert.assertEquals("ISO-8859-2", charset.getCharset());
        Assert.assertEquals(0.5f, charset.getQuality(), 0);

        // second item is UTF-8
        charset = charsets.get(1);
        Assert.assertEquals("UTF-8", charset.getCharset());
        Assert.assertEquals(0.5f, charset.getQuality(), 0);

        // third item is ISO-8859-1
        charset = charsets.get(2);
        Assert.assertEquals("ISO-8859-1", charset.getCharset());
        Assert.assertEquals(0.1f, charset.getQuality(), 0);
    }

    @Test
    public void getAcceptableCharsetsMultipleCharsetsNoAsterisk() {
        List<CharsetHolder> charsets = CharsetHolder.getAcceptableCharsets("ISO-8859-2;q=0.5,UTF-8;q=0.5");

        // third item is ISO-8859-1 - gets a q=1 by default
        CharsetHolder charset = charsets.get(0);
        Assert.assertEquals("ISO-8859-1", charset.getCharset());
        Assert.assertEquals(1f, charset.getQuality(), 0);

        // first item is ISO-8859-2
        charset = charsets.get(1);
        Assert.assertEquals("ISO-8859-2", charset.getCharset());
        Assert.assertEquals(0.5f, charset.getQuality(), 0);

        // second item is UTF-8
        charset = charsets.get(2);
        Assert.assertEquals("UTF-8", charset.getCharset());
        Assert.assertEquals(0.5f, charset.getQuality(), 0);

    }
}
