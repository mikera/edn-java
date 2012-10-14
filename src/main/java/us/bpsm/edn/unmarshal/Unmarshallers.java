package us.bpsm.edn.unmarshal;

import java.util.HashMap;
import java.util.Map;

import us.bpsm.edn.Keyword;
import us.bpsm.edn.unmarshal.Unmarshaller.Config;
import us.bpsm.edn.unmarshal.Unmarshaller.Config.Builder;

public class Unmarshallers {

    public static Unmarshaller newUnmarshaller(final Unmarshaller.Config cfg) {
        return new Unmarshaller() {
            public Object unmarshal(Keyword field, Object ednValue) {
                FieldHandler f = cfg.getFieldHandler(field);
                if (ednValue instanceof Map) {
                    Map<Object,Object> m = new HashMap<Object,Object>((Map<?,?>)ednValue);
                    for (Object k: m.keySet()) {
                        if (k instanceof Keyword) {
                            m.put(k, unmarshal((Keyword)k, m.get(k)));
                        }
                    }
                    if (f != null) {
                        return f.transform(field, m);
                    } else {
                        return m;
                    }
                } else {
                    if (f != null) {
                        return f.transform(field, ednValue);
                    } else {
                        return ednValue;
                    }
                }
            }
        };
    }

    public static Unmarshaller.Config.Builder newUnmarshallerConfigBuilder() {
        return new Unmarshaller.Config.Builder() {
            Map<Keyword,FieldHandler> m = new HashMap<Keyword,FieldHandler>();
            public Builder putFieldHandler(Keyword field, FieldHandler h) {
                m.put(field, h);
                return this;
            }
            public Config build() {
                return new Config() {
                    public FieldHandler getFieldHandler(Keyword field) {
                        return m.get(field);
                    }
                };
            }
        };
    }

}
