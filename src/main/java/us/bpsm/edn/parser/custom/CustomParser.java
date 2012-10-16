package us.bpsm.edn.parser.custom;

import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;

public interface CustomParser<T> extends Parser {
	
	public T nextValue(Object firstToken, Parseable pbr);

}
