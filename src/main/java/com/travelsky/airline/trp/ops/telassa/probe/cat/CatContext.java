package com.travelsky.airline.trp.ops.telassa.probe.cat;


import com.dianping.cat.Cat;

import java.util.HashMap;
import java.util.Map;

/**
 * Cat 上下文, 这里重新实现，而不使用 com.dianping.cat.Cat.Context 是为了避免 java.lang.NoClassDefFoundError: com/travelsky/airline/trp/ops/telassa/probe/cat/CatContext 的问题
 *
 * @author zengfan
 */
public class CatContext implements Cat.Context {

    private final Map<String, String> map = new HashMap<String, String>();

    @Override
    public String getProperty(String key) {
        return map.get(key);
    }

    @Override
    public void addProperty(String key, String value) {
        map.put(key, value);
    }

}
