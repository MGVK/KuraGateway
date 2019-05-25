package ru.mgvk.kura.gateway;

import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.metatype.OCD;

import java.util.Map;

public class ComponentConfigurationI implements ComponentConfiguration {

    private String              pid;
    private OCD                 ocd;
    private Map<String, Object> map;

    public ComponentConfigurationI(String pid, OCD ocd, Map<String, Object> map) {
        super();
        this.pid = pid;
        this.ocd = ocd;
        this.map = map;
    }

    public ComponentConfigurationI() {

    }

    @Override
    public String getPid() {
        return pid;
    }

    @Override
    public OCD getDefinition() {
        return ocd;
    }

    @Override
    public Map<String, Object> getConfigurationProperties() {
        return map;
    }

}
