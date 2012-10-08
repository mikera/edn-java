// (c) 2012 B Smith-Mannschott -- Distributed under the Eclipse Public License
package bpsm.edn.parser;

import static bpsm.edn.TaggedValue.newTaggedValue;
import static bpsm.edn.parser.Token.END_LIST;
import static bpsm.edn.parser.Token.END_MAP_OR_SET;
import static bpsm.edn.parser.Token.END_VECTOR;

import java.io.IOException;
import java.io.PushbackReader;

import bpsm.edn.EdnException;
import bpsm.edn.EdnIOException;
import bpsm.edn.Tag;

class ParserImpl implements Parser {

    private static final Object DISCARDED_VALUE = new Object() {
        @Override
        public String toString() { return "##discarded value##"; }
    };

    private Config cfg;
  
    ParserImpl(Config cfg) {
         this.cfg = cfg;
    }

    public Object nextValue(Scanner scanner) {
        return nextValue(scanner.nextToken(),scanner,false);
    }

    private Object nextToken(Scanner scanner) {
        return scanner.nextToken();
    }

    private Object nextValue(Object curr, Scanner scanner, boolean discard) {
        if (curr instanceof Token) {
            switch ((Token) curr) {
            case BEGIN_LIST:
                return parseIntoCollection(cfg.getListFactory(), END_LIST, nextToken(scanner), scanner, discard);
            case BEGIN_VECTOR:
                return parseIntoCollection(cfg.getVectorFactory(), END_VECTOR, nextToken(scanner), scanner, discard);
            case BEGIN_SET:
                return parseIntoCollection(cfg.getSetFactory(), END_MAP_OR_SET, nextToken(scanner), scanner, discard);
            case BEGIN_MAP:
                return parseIntoMap(cfg.getMapFactory(), nextToken(scanner), scanner, discard);
            case DISCARD:
                nextValue(nextToken(scanner), scanner, true);
                return nextValue(nextToken(scanner), scanner, discard);
            case NIL:
                return null;
            case END_OF_INPUT:
                return END_OF_INPUT;
            case END_LIST:
            case END_MAP_OR_SET:
            case END_VECTOR:
                throw new EdnException("Unexpected Token: " + curr);
            default:
                throw new EdnException("Unrecognized Token: " + curr);
            }
        } else if (curr instanceof Tag) {
            return nextValue((Tag)curr, nextToken(scanner), scanner, discard);
        } else {
            return curr;
        }
    }

    private Object nextValue(Tag t, Object curr, Scanner scanner, boolean discard) {
        Object v = nextValue(curr, scanner, discard);
        if (discard) {
            // It doesn't matter what we return here, as it will be discarded.
            return DISCARDED_VALUE;
        }
        TagHandler x = cfg.getTagHandler(t);
        return x != null ? x.transform(t, v) : newTaggedValue(t, v);
    }

    private Object parseIntoMap(CollectionBuilder.Factory f, Object curr, Scanner scanner, boolean discard) {
        CollectionBuilder b = !discard ? f.builder() : null;
        while (curr != END_MAP_OR_SET) {
            Object o = nextValue(curr, scanner, discard);
            if (o == END_OF_INPUT) {
                throw new EdnException("Expected '}', but found end of input.\n" +
                        String.valueOf(b.build()));
            }
            if (!discard) {
                b.add(o);
            }
            curr = nextToken(scanner);
        }
        return (!discard) ? b.build() : null;
    }

    private Object parseIntoCollection(CollectionBuilder.Factory f, Token end, Object curr, Scanner scanner, boolean discard) {
        CollectionBuilder b = !discard ? f.builder() : null;
        while (curr != end) {
            Object value = nextValue(curr, scanner, discard);
            if (value == END_OF_INPUT) {
                throw new EdnException();
            }
            if (!discard) {
                b.add(value);
            }
            curr = scanner.nextToken();
        }
        return !discard ? b.build() : null;
    }

}
