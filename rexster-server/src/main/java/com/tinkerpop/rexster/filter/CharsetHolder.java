package com.tinkerpop.rexster.filter;

import org.apache.commons.lang.NullArgumentException;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class CharsetHolder implements Comparable<CharsetHolder> {
    private static final Logger logger = Logger.getLogger(CharsetHolder.class);

    private final String charset;
    private final float quality;
    private final int order;

    private static final int CACHE_MAX_SIZE = 1000;

    private static final Map<String, CharsetHolder> charsetCache = Collections.synchronizedMap(
            new LinkedHashMap(CACHE_MAX_SIZE) {

                private static final long serialVersionUID = 2546245625L;

                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > CACHE_MAX_SIZE;
                }
            });

    public CharsetHolder(final String charset, final float quality, final int order) {

        if (charset == null) {
            throw new NullArgumentException("charset");
        }

        this.charset = charset;
        this.quality = quality;
        this.order = order;
    }

    public String getCharset() {
        return this.charset;
    }

    public float getQuality() {
        return this.quality;
    }

    public int getOrder() {
        return this.order;
    }

    public boolean isSupported() {
        try {
            return Charset.isSupported(this.charset);
        } catch (IllegalCharsetNameException icne) {
            logger.debug("Illegal charset requested.", icne);
            return false;
        }
    }

    /**
     * Accepts a standard value from the Accept-Charset field and gets the first matching charset supported
     * by the server.
     * <p/>
     * Example: iso-8859-5, unicode-1-1;q=0.8
     *
     * @return the first matching charset that is supported by the server or null if one cannot be found.
     */
    public static CharsetHolder getFirstSupportedCharset(final String acceptCharsetHeaderValue) {
        CharsetHolder firstSupportedCharset = null;

        if (charsetCache.containsKey(acceptCharsetHeaderValue)) {
            firstSupportedCharset = charsetCache.get(acceptCharsetHeaderValue);
        } else {
            final List<CharsetHolder> charsetRanks = getAcceptableCharsets(acceptCharsetHeaderValue);

            for (CharsetHolder charsetRank : charsetRanks) {
                if (charsetRank.isSupported()) {
                    firstSupportedCharset = charsetRank;
                    break;
                }
            }

            charsetCache.put(acceptCharsetHeaderValue, firstSupportedCharset);
        }

        return firstSupportedCharset;
    }

    public static List<CharsetHolder> getAcceptableCharsets(final String acceptCharsetHeaderValue) {
        final ArrayList<CharsetHolder> charsetHolders = new ArrayList<CharsetHolder>();

        final String[] charsetStrings = acceptCharsetHeaderValue.split(",");
        int order = 0;

        CharsetHolder asteriskCharset = null;

        for (String charsetString : charsetStrings) {
            try {
                float qualityValue = 1f;

                final String[] charsetComponents = charsetString.split(";");

                final String charsetValue = charsetComponents[0];
                if (charsetComponents.length == 2) {
                    final String[] qualityPair = charsetComponents[1].trim().split("=");
                    if (qualityPair.length == 2) {
                        qualityValue = Float.parseFloat(qualityPair[1].trim());
                    }
                }

                if (charsetValue.equals("*")) {
                    asteriskCharset = new CharsetHolder("*", qualityValue, order);
                } else {
                    charsetHolders.add(new CharsetHolder(charsetValue, qualityValue, order));
                }

            } catch (Exception ex) {
                logger.warn("Charset from the request (or rexster.xml) is not valid: [" + charsetString + "].");
            }

            order++;
        }

        final SortedMap<String, Charset> availableCharsets = Charset.availableCharsets();
        if (asteriskCharset == null) {
            for (Map.Entry<String, Charset> availableCharset : availableCharsets.entrySet()) {

                CharsetHolder otherCharsetHolder = new CharsetHolder(availableCharset.getKey(), 0, Integer.MAX_VALUE);

                if (availableCharset.getKey().equals("ISO-8859-1")) {
                    otherCharsetHolder = new CharsetHolder(availableCharset.getKey(), 1, Integer.MAX_VALUE);
                }

                if (!charsetHolders.contains(otherCharsetHolder)) {
                    charsetHolders.add(otherCharsetHolder);
                }
            }
        } else {
            for (Map.Entry<String, Charset> availableCharset : availableCharsets.entrySet()) {
                final CharsetHolder otherCharsetHolder = new CharsetHolder(availableCharset.getKey(), asteriskCharset.getQuality(), 0);

                if (!charsetHolders.contains(otherCharsetHolder)) {
                    charsetHolders.add(otherCharsetHolder);
                }
            }
        }

        Collections.sort(charsetHolders);

        return charsetHolders;
    }

    public int compareTo(final CharsetHolder charsetHolder) {
        int compare = this.quality == charsetHolder.getQuality() ? 0 : (this.quality > charsetHolder.getQuality() ? -1 : 1);
        if (compare == 0) {
            compare = this.order == charsetHolder.getOrder() ? 0 : (this.order > charsetHolder.getOrder() ? 1 : -1);
        }

        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final CharsetHolder that = (CharsetHolder) o;

        if (!charset.equals(that.charset)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return charset.hashCode();
    }
}
