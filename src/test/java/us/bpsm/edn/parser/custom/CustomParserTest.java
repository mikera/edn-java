package us.bpsm.edn.parser.custom;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parsers;

public class CustomParserTest {

	@Test 
	public void testVectorParser() {
        CustomParser<List<Long>> p = new CustomParsers.VectorParser<Long>(new CustomParsers.LongParser());
        Parseable r = Parsers.newParseable("[1 2 3 4 5]");
        
        List<Long> v=(List<Long>) p.nextValue(r);
        assertEquals(5,v.size());
        assertEquals(1,(long)v.get(0));
        assertEquals(5,(long)v.get(4));
        
    }

}
