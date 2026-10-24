package com.floral.opalina.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "opalina")
public class PropiedadesOpalina {
    private final DemoData demoData = new DemoData();

    public DemoData getDemoData() {
        return demoData;
    }

    public static class DemoData {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
