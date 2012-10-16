package us.bpsm.edn.parser.custom;

import java.io.IOException;

import us.bpsm.edn.EdnIOException;
import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parsers;
import us.bpsm.edn.parser.Scanner;

/**
 * Abstract base class for custom parsers.
 * 
 * Can be extended to create a new type of custom parser.
 * 
 * @author Mike
 *
 * @param <T>
 */
public abstract class AbstractCustomParser<T> implements CustomParser<T> {

	private Scanner scanner;

	protected AbstractCustomParser(Scanner scanner) {
		this.scanner=scanner;
	}
	
	public AbstractCustomParser() {
		this(Parsers.defaultConfiguration());
	}
	
	public AbstractCustomParser(Config config) {
		this(new Scanner(config));
	}

	/**
	 * Returns the text token using the parser's embedded scanner.
	 * 
	 * @param pbr
	 * @return
	 */
    protected Object nextToken(Parseable pbr) {
        try {
            return scanner.nextToken(pbr);
        } catch (IOException e) {
            throw new EdnIOException(e);
        }
	}
	   
	public T nextValue(Parseable pbr) {
		Object peek=nextToken(pbr);
		return nextValue(peek, pbr);
	}
	
	public abstract T nextValue(Object firstToken, Parseable pbr);


}
