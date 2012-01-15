package org.openrepose.components.routing.servlet;

import com.rackspace.papi.commons.config.manager.UpdateListener;
import com.rackspace.papi.filter.logic.AbstractConfiguredFilterHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import org.openrepose.components.routing.servlet.config.ServletContextRouterConfiguration;

public class RoutingHandlerFactory extends AbstractConfiguredFilterHandlerFactory<RoutingTagger> {

   private ServletContextRouterConfiguration contextRouterConfiguration;

   private class RoutingConfigurationListener implements UpdateListener<ServletContextRouterConfiguration> {

      @Override
      public void configurationUpdated(ServletContextRouterConfiguration configurationObject) {
         contextRouterConfiguration = configurationObject;
      }
   }

   @Override
   protected RoutingTagger buildHandler() {
      return new RoutingTagger(contextRouterConfiguration.getContextPath());
   }

   @Override
   protected Map<Class, UpdateListener<?>> getListeners() {
      final Map<Class, UpdateListener<?>> updateListeners = new HashMap<Class, UpdateListener<?>>();
      updateListeners.put(ServletContextRouterConfiguration.class, new RoutingConfigurationListener());

      return updateListeners;
   }
}
