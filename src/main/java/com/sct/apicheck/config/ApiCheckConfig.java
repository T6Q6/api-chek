package com.sct.apicheck.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检清单根配置，对应 apis.yaml。
 */
public class ApiCheckConfig {

    private int version = 1;
    private DefaultsConfig defaults;
    private List<ApiDefinition> apis = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public DefaultsConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(DefaultsConfig defaults) {
        this.defaults = defaults;
    }

    public List<ApiDefinition> getApis() {
        return apis;
    }

    public void setApis(List<ApiDefinition> apis) {
        this.apis = apis;
    }
}