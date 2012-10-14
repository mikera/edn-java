package us.bpsm.edn.unmarshall;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import us.bpsm.edn.Keyword;
import us.bpsm.edn.Symbol;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;
import us.bpsm.edn.unmarshal.FieldHandler;
import us.bpsm.edn.unmarshal.Unmarshaller;
import us.bpsm.edn.unmarshal.Unmarshallers;

public class UnmarshallersTest {

    static class Point {
        final int x, y;
        Point(int x, int y) {
            this.x = x; this.y = y;
        }
    }

    static class Rect {
        final Point upperLeft, lowerRight;
        Rect(Point ul, Point lr) {
            upperLeft = ul; lowerRight = lr;
        }
    }


    static final Keyword RECT = Keyword.newKeyword(Symbol.newSymbol(null, "rect"));
    static final Keyword UPPER_LEFT = Keyword.newKeyword(Symbol.newSymbol(null, "upperLeft"));
    static final Keyword LOWER_RIGHT = Keyword.newKeyword(Symbol.newSymbol(null, "lowerRight"));
    static final Keyword X = Keyword.newKeyword(Symbol.newSymbol(null, "x"));
    static final Keyword Y = Keyword.newKeyword(Symbol.newSymbol(null, "y"));

    private static final FieldHandler MAKE_POINT = new FieldHandler() {
        public Object transform(Keyword field, Object value) {
            Map<?,?> m = (Map<?,?>)value;
            return new Point(
                ((Number) m.get(X)).intValue(),
                ((Number) m.get(Y)).intValue());
        }
    };

    private static final FieldHandler MAKE_RECT = new FieldHandler() {
        public Object transform(Keyword field, Object value) {
            Map<?,?> m = (Map<?,?>)value;
            return new Rect(
                (Point) m.get(UPPER_LEFT),
                (Point) m.get(LOWER_RIGHT));
        }
    };

    @Test
    public void test() {
        Parser p = Parsers.newParser(Parsers.defaultConfiguration());
        Parseable r = Parsers.newParseable(
            "{:upperLeft {:x 1 :y 2} :lowerRight {:x 3 :y 4}}");
        Object ednValue = p.nextValue(r);

        Unmarshaller u = Unmarshallers.newUnmarshaller(
            Unmarshallers.newUnmarshallerConfigBuilder()
            .putFieldHandler(UPPER_LEFT, MAKE_POINT)
            .putFieldHandler(LOWER_RIGHT, MAKE_POINT)
            .putFieldHandler(RECT, MAKE_RECT)
            .build());

        Rect rect = (Rect) u.unmarshal(RECT, ednValue);
        assertEquals(1, rect.upperLeft.x);
        assertEquals(2, rect.upperLeft.y);
        assertEquals(3, rect.lowerRight.x);
        assertEquals(4, rect.lowerRight.y);
    }

}
