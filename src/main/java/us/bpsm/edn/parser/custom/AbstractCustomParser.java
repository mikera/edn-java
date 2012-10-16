package us.bpsm.edn.parser.custom;

import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;

/**
 * Abstract base class for custom parsers.
 * 
 * Can be extended to create a new type of custom parser.
 * 
 * @author Mike
 *
 * @param <T>
 */
public abstract class AbstractCustomParser<T> implements Parser {

	public abstract T nextValue(Parseable pbr);

}
