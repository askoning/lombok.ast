/**
 * Main javadoc {@code description}.
 * 
 * @param T a type parameter
 * @author author tag
 * @since since tag
 * @see http://www.projectlombok.org/
 */
public class ExtensiveJavadoc<T> {
    /**
     * Javadoc containing a value tag that's not in a legal location.
     * 
     * @value Description of this constant's value.
     */
    private int test;
    
    /**
     * Some javadoc containing links and some light html.
     * to {@link #test} a local field. <br/>
     * to {@link #foo(int)} a local method. <br/>
     * to {@link java.lang.String#CASE_INSENSITIVE_ORDER} a remote field. <br/>
     * to {@link java.lang.String} a remote class. <br/>
     * to {@link java.lang.String#toUpperCase()} a remote method. <br/>
     */
    public void bar() {
    }
    
    /**
     * @param in A method level param tag.
     * @throws NumberFormatException A throws tag.
     * @return A return tag.
     */
    public int foo(int in) throws NumberFormatException {
        return 0;
    }
    
    /**
     * A deprecated method.
     * 
     * @deprecated Deprecated description.
     */
    public void deprecatedMethod() {
    }
}