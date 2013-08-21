package com.rackspace.papi.components.uritranslation;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.components.uristripper.config.UriStripperConfig;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;

import java.util.HashMap;
import java.util.Map;

public class UriStripperHandlerFactory extends AbstractConfiguredFilterHandlerFactory<UriStripperHandler> {


    int stripId;

    protected UriStripperHandlerFactory() {
    }

    @Override
    protected UriStripperHandler buildHandler() {
        if (!this.isInitialized()) {
            return null;
        }
        return new UriStripperHandler(stripId);
    }

    @Override
    protected Map<Class, UpdateListener<?>> getListeners() {
        return new HashMap<Class, UpdateListener<?>>() {
            {
                put(UriStripperConfig.class, new UriTranslationConfigurationListener());
            }
        };
    }

    private class UriTranslationConfigurationListener implements UpdateListener<UriStripperConfig> {

        private boolean isInitialized = false;

        @Override
        public void configurationUpdated(UriStripperConfig config) {

            stripId = config.getPosition();
            isInitialized = true;

        }
        @Override
        public boolean isInitialized() {
            return isInitialized;
        }
    }
}
