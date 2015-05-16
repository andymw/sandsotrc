/**
 * These files under common.codec have been take from the open-source
 * Apache libraries and slightly modified (removal of interfaces and
 * consolidation of interfaces) for use in SAND and SOTRC.
 * These files are under the apache package
 *    org.apache.commons.codec and org.apache.commons.codec.binary
 * View these raw files here:
 *   http://grepcode.com/file/repo1.maven.org/maven2/commons-codec/commons-codec/1.5/org/apache/commons/codec/binary/Base32.java
 * This exception is a consolidation of DecoderException and EncoderException.
 */

package common.codec;

public class CodecException extends Exception {
    private static final long serialVersionUID = 1L;

    public CodecException() {
        super();
    }

    public CodecException(String message) {
        super(message);
    }

    public CodecException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodecException(Throwable cause) {
        super(cause);
    }
}
