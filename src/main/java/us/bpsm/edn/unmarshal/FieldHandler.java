package us.bpsm.edn.unmarshal;

import us.bpsm.edn.Keyword;
import us.bpsm.edn.Tag;

public interface FieldHandler {
    Object transform(Keyword field, Object value);
}
