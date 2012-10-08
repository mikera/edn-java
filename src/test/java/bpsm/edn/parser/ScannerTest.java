// (c) 2012 B Smith-Mannschott -- Distributed under the Eclipse Public License
package bpsm.edn.parser;

import static bpsm.edn.Keyword.newKeyword;
import static bpsm.edn.Symbol.newSymbol;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;

import org.junit.Test;

import bpsm.edn.EdnException;
import bpsm.edn.Keyword;
import bpsm.edn.Symbol;

public class ScannerTest {

    @Test
    public void testEmpty() {
        assertEquals(Token.END_OF_INPUT, scan(""));
    }

    @Test
    public void testNil() {
        assertEquals(Token.NIL, scan("nil"));
    }

    @Test
    public void testTrue() {
        assertEquals(true, scan("true"));
    }

    @Test
    public void testFalse() {
        assertEquals(false, scan("false"));
    }

    @Test
    public void symbolWithoutPrefix() {
        assertEquals(sym("foo"), scan("foo"));
    }

    @Test
    public void symbolSlash() {
        assertEquals(sym("/"), scan("/"));
    }

    @Test
    public void symbolWithPrefix() {
        assertEquals(sym("a", "b"), scan("a/b"));
    }

    @Test(expected=EdnException.class)
    public void symbolHasTooManySlashes() {
        scan("a/b/c");
    }

    @Test(expected=EdnException.class)
    public void symbolEndsInSlash() {
        scan("a/");
    }

    @Test(expected=EdnException.class)
    public void symbolStartsWithSlash() {
        scan("/a");
    }

    @Test(expected=EdnException.class)
    public void symbolStartsWithDotDigit() {
        scan(".4symbol");
    }

    @Test(expected=EdnException.class)
    public void symbolStartsWithDashDigit() {
        scan("-4symbol");
    }

    @Test(expected=EdnException.class)
    public void symbolStartsWithPlusDigit() {
        scan("+4symbol");
    }

    @Test
    public void keywordWithoutPrefix() {
        assertEquals(key("+"), scan(":+"));
    }

    /** issue 5 */
    @Test(expected=EdnException.class)
    public void colonSlashIsNotAValidKeyword() {
        scan(":/");
    }

    @Test
    public void keywordWithPrefix() {
        assertEquals(key("foo:bar", ".baz"), scan(":foo:bar/.baz"));
    }

    @Test
    public void beginList() {
        assertEquals(Token.BEGIN_LIST, scan("("));
    }

    @Test
    public void endList() {
        assertEquals(Token.END_LIST, scan(")"));
    }

    @Test
    public void beginVector() {
        assertEquals(Token.BEGIN_VECTOR, scan("["));
    }

    @Test
    public void endVector() {
        assertEquals(Token.END_VECTOR, scan("]"));
    }

    @Test
    public void beginMap() {
        assertEquals(Token.BEGIN_MAP, scan("{"));
    }

    @Test
    public void beginSet() {
        assertEquals(Token.BEGIN_SET, scan("#{"));
    }

    @Test
    public void endMapOrSet() {
        assertEquals(Token.END_MAP_OR_SET, scan("}"));
    }

    @Test
    public void discard() {
        assertEquals(Token.DISCARD, scan("#_"));
    }

    @Test
    public void comment() {
        assertEquals(Token.NIL, scan("; 1\n ; 2\r\nnil"));
    }

    @Test
    public void zero() {
        assertEquals(0L, scan("0"));
    }

    @Test
    public void maxLong() {
        assertEquals(9223372036854775807L, scan("9223372036854775807"));
    }

    @Test
    public void minLong() {
        assertEquals(-9223372036854775808L, scan("-9223372036854775808"));
    }

    @Test
    public void maxInteger() {
        assertEquals(2147483647L, scan("2147483647"));
        assertEquals(2147483647L, scan("+2147483647"));
    }

    @Test
    public void minInteger() {
        assertEquals(-2147483648L, scan("-2147483648"));
    }

    @Test
    public void bigIntegerAutopromote() {
        assertEquals(new BigInteger("9223372036854775808"), scan("9223372036854775808"));
        assertEquals(new BigInteger("-9223372036854775809"), scan("-9223372036854775809"));
    }

    @Test
    public void bigIntegerRequested() {
        assertEquals(BigInteger.valueOf(1), scan("1N"));
    }

    @Test
    public void floatWithFraction() {
        assertEquals(1.23456d, scan("1.23456"));
    }

    @Test
    public void floatWithExponent() {
        assertEquals(123456e-10d, scan("123456e-10"));
    }

    @Test
    public void floatWithFractionAndExponent() {
        assertEquals(-1.23456e3d, scan("-1.23456E3"));
        assertEquals(1.23456e3d, scan("+1.23456E3"));
        assertEquals(1.23456e3d, scan("1.23456E3"));
    }

    @Test
    public void decimalWithFraction() {
        assertEquals(new BigDecimal("1.23456"), scan("1.23456M"));
    }

    @Test
    public void decimalWithExponent() {
        assertEquals(new BigDecimal("123456e-10"), scan("123456e-10M"));
    }

    @Test
    public void decimalWithFractionAndExponent() {
        assertEquals(new BigDecimal("-1.23456e3"), scan("-1.23456E3M"));
    }

    @Test
    public void emptyString() {
        assertEquals("", scan("\"\""));
    }

    @Test
    public void simpleStringEscapes() {
        assertEquals("\t\n\r\f\"\'\b\\",
            scan("\"\\t\\n\\r\\f\\\"\\\'\\b\\\\\""));
    }

    @Test
    public void namedCharacters() {
        assertEquals('\n', scan("\\newline"));
        assertEquals('\t', scan("\\tab"));
        assertEquals('\f', scan("\\formfeed"));
        assertEquals('\r', scan("\\return"));
        assertEquals(' ', scan("\\space"));
        assertEquals('\b', scan("\\backspace"));
    }

    @Test
    public void keywordsAreInternedGlobally() {
        assertSame(scan(":foo/bar"), scan(":foo/bar"));
    }

    @Test
    public void keywordsAreInternedGloballyWithoutPrefix() {
        assertSame(scan(":foo"), scan(":foo"));
    }

    @Test
    public void keywordWithDifferentPrefixNotIdentical() {
        assertTrue(scan(":a/foo")!=scan(":b/foo"));
    }

    @Test
    public void sequenceOfTokens() throws IOException {
        String txt = "; comment\n" +
            "\t\n" +
            "true false nil \\#{:keyword  [1 2N 3.0 4.0M]}symbol\n" +
            "\\newline \"some text\"\\x ; another comment\n" +
            "() #{-42}";
        Scanner s = new Scanner(txt);
        Object[] expected = {
            true, false, Token.NIL, '#',
            Token.BEGIN_MAP, key("keyword"),
            Token.BEGIN_VECTOR, 1L, BigInteger.valueOf(2),
            3.0d, new BigDecimal("4.0"), Token.END_VECTOR,
            Token.END_MAP_OR_SET, sym("symbol"), '\n',
            "some text", 'x', Token.BEGIN_LIST, Token.END_LIST,
            Token.BEGIN_SET, -42L, Token.END_MAP_OR_SET,
            Token.END_OF_INPUT
        };
        for (Object o: expected) {
            assertEquals(o, s.nextToken());
        }
    }

    @Test
    public void simpleStringWithLinebreak() {
        assertEquals("\n", scan("\"\n\""));
    }

    static Object scan(String input) {
        Scanner s = new Scanner(input);
        return s.nextToken();
    }

    static Symbol sym(String name) {
        return newSymbol(null, name);
    }

    static Symbol sym(String prefix, String name) {
        return newSymbol(prefix, name);
    }

    static Keyword key(String name) {
        return newKeyword(sym(null, name));
    }

    static Keyword key(String prefix, String name) {
        return newKeyword(sym(prefix, name));
    }


}
