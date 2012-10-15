package us.bpsm.edn.unmarshal;

import us.bpsm.edn.Keyword;

public interface Unmarshaller {
    Object unmarshal(Keyword field, Object ednValue);

    interface Config {
        FieldHandler getFieldHandler(Keyword field);

        interface Builder {
            Builder putFieldHandler(Keyword field, FieldHandler h);
            Config build();
        }
    }
}
