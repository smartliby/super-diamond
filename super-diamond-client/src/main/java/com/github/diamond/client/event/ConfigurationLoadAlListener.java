package com.github.diamond.client.event;

import java.util.Map;

/**
 * Created by dongjiejie on 2017/2/15.
 */
public interface ConfigurationLoadAlListener {

    void loadAll(Map<String, String> newMap);
}
