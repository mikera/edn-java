// (c) 2012 B Smith-Mannschott -- Distributed under the Eclipse Public License
package bpsm.edn.parser;

import static bpsm.edn.Symbol.newSymbol;
import static bpsm.edn.Tag.newTag;
import static bpsm.edn.parser.Parser.Config.BIG_DECIMAL_TAG;
import static bpsm.edn.parser.Parser.Config.BIG_INTEGER_TAG;
import static bpsm.edn.parser.Parser.Config.DOUBLE_TAG;
import static bpsm.edn.parser.Parser.Config.LONG_TAG;
import static bpsm.edn.util.CharClassify.isDigit;
import static bpsm.edn.util.CharClassify.isWhitespace;
import static bpsm.edn.util.CharClassify.separatesTokens;

import java.io.IOException;
import java.io.PushbackReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import bpsm.edn.EdnException;
import bpsm.edn.Keyword;
import bpsm.edn.Symbol;
import bpsm.edn.Tag;
import bpsm.edn.util.CharClassify;


class Scanner {

    static final Symbol NIL_SYMBOL = newSymbol(null, "nil");
    static final Symbol TRUE_SYMBOL = newSymbol(null, "true");
    static final Symbol FALSE_SYMBOL = newSymbol(null, "false");
    static final Symbol SLASH_SYMBOL = newSymbol(null, "/");

    static final int END = -1;

    private final TagHandler longHandler;
    private final TagHandler bigDecimalHandler;
    private final TagHandler bigIntegerHandler;
    private final TagHandler doubleHandler;

    /**
     * Scanner may throw an IOException during construction, in which case
     * an attempt will be made to close Reader cleanly.
     * @param reader
     * @throws IOException
     */
    Scanner(Parser.Config cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("cfg must not be null");
        }

        this.longHandler = cfg.getTagHandler(LONG_TAG);
        this.bigIntegerHandler = cfg.getTagHandler(BIG_INTEGER_TAG);
        this.doubleHandler = cfg.getTagHandler(DOUBLE_TAG);
        this.bigDecimalHandler = cfg.getTagHandler(BIG_DECIMAL_TAG);
    }

    public Object nextToken(PushbackReader pbr) throws IOException {
        skipWhitespaceAndComments(pbr);
        int curr = pbr.read();
        switch(curr) {
        case END:
            return Token.END_OF_INPUT;
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
            return readSymbol(unread(curr, pbr));
        case 'f':
        {
            Symbol sym = readSymbol(unread(curr, pbr));
            return FALSE_SYMBOL.equals(sym) ? false : sym;
        }
        case 'g':
        case 'h':
        case 'i':
        case 'j':
        case 'k':
        case 'l':
        case 'm':
            return readSymbol(unread(curr, pbr));
        case 'n':
        {
            Symbol sym = readSymbol(unread(curr, pbr));
            return NIL_SYMBOL.equals(sym) ? Token.NIL : sym;
        }
        case 'o':
        case 'p':
        case 'q':
        case 'r':
        case 's':
            return readSymbol(unread(curr, pbr));
        case 't':
        {
            Symbol sym = readSymbol(unread(curr, pbr));
            return TRUE_SYMBOL.equals(sym) ? true : sym;
        }
        case 'u':
        case 'v':
        case 'w':
        case 'x':
        case 'y':
        case 'z':
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
        case 'I':
        case 'J':
        case 'K':
        case 'L':
        case 'M':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'R':
        case 'S':
        case 'T':
        case 'U':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case '*':
        case '!':
        case '_':
        case '?':
        case '/':
        case '.':
            return readSymbol(unread(curr, pbr));
        case '+':
        case '-': {
            int peek = pbr.read();
            if (peek == END) {
                return readSymbol(unread(curr, pbr));
            } else {
                unread(curr, peek, pbr);
                if (isDigit((char)peek)) {
                    return readNumber(pbr);
                } else {
                    return readSymbol(pbr);
                }
            }}
        case ':':
            return readKeyword(pbr);
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return readNumber(unread(curr, pbr));
        case '{':
            return Token.BEGIN_MAP;
        case '}':
            return Token.END_MAP_OR_SET;
        case '[':
            return Token.BEGIN_VECTOR;
        case ']':
            return Token.END_VECTOR;
        case '(':
            return Token.BEGIN_LIST;
        case ')':
            return Token.END_LIST;
        case '#': {
            int peek = pbr.read();
            switch(peek) {
            case END:
                throw new EdnException("Unexpected end of input following '#'");
            case '{':
                return Token.BEGIN_SET;
            case '_':
                return Token.DISCARD;
            default:
                return readTag(unread(peek, pbr));
            }}
        case '"':
            return readStringLiteral(pbr);
        case '\\':
            return readCharacterLiteral(pbr);
        default:
            throw new EdnException(
                String.format("Unexpected character '%c', \\"+"u%04x",
                        (char)curr, curr));
        }
    }

    private static PushbackReader unread(int curr, PushbackReader pbr) throws IOException {
        if (curr != END) {
            pbr.unread((char)curr);
        }
        return pbr;
    }

    private static PushbackReader unread(int curr, int peek, PushbackReader pbr) throws IOException {
        if (peek != END) {
            pbr.unread((char)peek);
        }
        if (curr != END) {
            pbr.unread((char)curr);
        }
        return pbr;
    }

    private void skipWhitespaceAndComments(PushbackReader pbr) throws IOException {
        for (;;) {
            skipWhitespace(pbr);
            int curr = pbr.read();
            if (curr != ';') {
                unread(curr, pbr);
                break;
            }
            skipComment(pbr);
        }
    }

    private void skipWhitespace(PushbackReader pbr) throws IOException {
        int curr;
        do {
            curr = pbr.read();
        } while (curr != END && isWhitespace((char)curr));
        unread(curr, pbr);
    }

    private void skipComment(PushbackReader pbr) throws IOException {
        int curr;
        do {
            curr = pbr.read();
        } while (curr != END && curr != '\n' && curr != '\r');
        unread(curr, pbr);
    }

    private char readCharacterLiteral(PushbackReader pbr) throws IOException {
        int curr = pbr.read();
        if (curr == END) {
            throw new EdnException(
                    "Unexpected end of input following '\'");
        } else if (isWhitespace((char)curr)) {
            throw new EdnException(
                "A backslash introducing character literal must not be "+
                "immediately followed by whitespace.");
        }
        StringBuilder b = new StringBuilder();
        do {
            b.append((char)curr);
            curr = pbr.read();
        } while (curr != END && !separatesTokens((char)curr));
        unread(curr, pbr);
        if (b.length() == 1) {
            return b.charAt(0);
        } else {
            return charForName(b.toString());
        }
    }

    private static char charForName(String name) {
        switch (name.charAt(0)) {
        case 'n':
            if ("newline".equals(name)) {
                return '\n';
            }
            // fall through
        case 's':
            if ("space".equals(name)) {
                return ' ';
            }
            // fall through
        case 't':
            if ("tab".equals(name)) {
                return '\t';
            }
            // fall through
        case 'b':
            if ("backspace".equals(name)) {
                return '\b';
            }
            // fall through
        case 'f':
            if ("formfeed".equals(name)) {
                return '\f';
            }
            // fall through
        case 'r':
            if ("return".equals(name)) {
                return '\r';
            }
            // fall through
        default:
            throw new EdnException(
                "The character \\"+ name +" was not recognized.");
        }
    }

    private String readStringLiteral(PushbackReader pbr) throws IOException {
        StringBuffer b = new StringBuffer();
        for (;;) {
            int curr = pbr.read();
            switch (curr) {
            case END:
                throw new EdnException(
                        "Unexpected end of input in string literal");
            case '"':
                return b.toString();
            case '\\':
                curr = pbr.read();
                switch (curr) {
                case END:
                    throw new EdnException(
                            "Unexpected end of input in string literal");
                case 'b':
                    b.append('\b');
                    break;
                case 't':
                    b.append('\t');
                    break;
                case 'n':
                    b.append('\n');
                    break;
                case 'f':
                    b.append('\f');
                    break;
                case 'r':
                    b.append('\r');
                    break;
                case '"':
                    b.append('"');
                    break;
                case '\'':
                    b.append('\'');
                    break;
                case '\\':
                    b.append('\\');
                    break;
                default:
                    throw new EdnException("Unsupported '"+ ((char)curr)
                            +"' escape in string");
                }
                break;
            default:
                b.append((char)curr);
            }
        }
    }

    private Object readNumber(PushbackReader pbr) throws IOException {
        int curr = pbr.read();
        assert curr != END && CharClassify.startsNumber((char)curr);
        StringBuffer digits = new StringBuffer();

        if (curr != '+') {
            digits.append((char)curr);
        }
        curr = pbr.read();
        while (curr != END && isDigit((char)curr)) {
            digits.append((char)curr);
            curr = pbr.read();
        }
        unread(curr, pbr);
        if (curr == '.' || curr == 'e' || curr == 'E' || curr == 'M') {
            return parseFloat(pbr, digits);
        } else {
            return parseInteger(pbr, digits);
        }
    }

    private Object parseFloat(PushbackReader pbr, StringBuffer digits) throws IOException {
        int curr = pbr.read();
        assert (curr == '.' || curr == 'e' || curr == 'E' || curr == 'M');
        if (curr == '.') {
            do {
                digits.append((char)curr);
                curr = pbr.read();
            } while (curr != END && isDigit((char) curr));
        }

        if (curr == 'e' || curr == 'E') {
            digits.append((char)curr);
            curr = pbr.read();
            if (curr == END) {
                throw new EdnException("Unexpected end of input in numeric literal");
            }
            if (!(curr == '-' || curr == '+' || isDigit((char)curr))) {
                throw new EdnException("Not a number: '"+ digits + ((char)curr) +"'.");
            }
            do {
                digits.append((char)curr);
                curr = pbr.read();
            } while (curr != END && isDigit((char)curr));
        }

        final boolean decimal;
        if (curr == 'M') {
            decimal = true;
            curr = pbr.read();
        } else {
            decimal = false;
        }

        if (curr != END && !separatesTokens((char)curr)) {
            throw new EdnException("Not a number: '"+ digits + ((char)curr) +"'.");
        }
        unread(curr, pbr);

        if (decimal) {
            BigDecimal d = new BigDecimal(digits.toString());
            return bigDecimalHandler.transform(BIG_DECIMAL_TAG, d);
        } else {
            double d = Double.parseDouble(digits.toString());
            return doubleHandler.transform(DOUBLE_TAG, d);
        }
    }

    private Object parseInteger(PushbackReader pbr, CharSequence digits) throws IOException {
        int curr = pbr.read();
        final boolean bigint;
        if (curr == 'N') {
            bigint = true;
            curr = pbr.read();
        } else {
            bigint = false;
        }

        if (curr != END && !separatesTokens((char)curr)) {
            throw new EdnException("Not a number: '"+ digits + ((char)curr) +"'.");
        }
        unread(curr, pbr);

        final BigInteger n = new BigInteger(digits.toString());

        if (bigint || MIN_LONG.compareTo(n) > 0 || n.compareTo(MAX_LONG) > 0) {
            return bigIntegerHandler.transform(BIG_INTEGER_TAG, n);
        } else {
            return longHandler.transform(LONG_TAG, n.longValue());
        }
    }

    private Keyword readKeyword(PushbackReader pbr) throws IOException {
        Symbol sym = readSymbol(pbr);
        if (SLASH_SYMBOL.equals(sym)) {
            throw new EdnException("':/' is not a valid keyword.");
        }
        return Keyword.newKeyword(sym);
    }

    private Tag readTag(PushbackReader pbr) throws IOException {
        return newTag(readSymbol(pbr));
    }


    private Symbol readSymbol(PushbackReader pbr) throws IOException {
        int curr = pbr.read();
        if (curr == END) {
            throw new EdnException(
                    "Unexpected end of input while reading an identifier");
        }
        StringBuilder b = new StringBuilder();
        int n = 0;
        int p = Integer.MIN_VALUE;
        do {
            if (curr == '/') {
                n += 1;
                p = b.length();
            }
            b.append((char)curr);
            curr = pbr.read();
        } while (curr != END && !separatesTokens((char)curr));
        unread(curr, pbr);

        validateUseOfSlash(b, n, p);
        return makeSymbol(b, n, p);
    }

    private Symbol makeSymbol(StringBuilder b, int slashCount, int slashPos) {
        if (slashCount == 0) {
            return newSymbol(null, b.toString());
        } else {
            assert slashCount == 1;
            if (slashPos == 0) {
                assert b.length() == 1 && b.charAt(0) == '/';
                return newSymbol(null, b.toString());
            } else {
                return newSymbol(b.substring(0, slashPos), b.substring(slashPos+1));
            }
        }
    }

    private void validateUseOfSlash(CharSequence s, int slashCount, int lastSlashPos) {
        if (slashCount > 1) {
            throw new EdnException(
                "The name '"+ s +"' must not contain more than one '/'.");
        }
        if (lastSlashPos == 0 && s.length() > 1) {
            throw new EdnException(
                "The name '"+ s +"' must not start with '/'.");
        }
        if (s.length() > 1) {
            if (lastSlashPos == s.length() - 1) {
                throw new EdnException(
                    "The name '"+ s +"' must not end with '/'.");
            }
        }
    }

    private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);


}
