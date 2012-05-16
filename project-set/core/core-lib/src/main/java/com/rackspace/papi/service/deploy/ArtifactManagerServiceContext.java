package com.rackspace.papi.service.deploy;

import com.rackspace.papi.commons.util.thread.DestroyableThreadWrapper;
import com.rackspace.papi.container.config.ContainerConfiguration;
import com.rackspace.papi.service.context.ContextAdapter;
import com.rackspace.papi.service.context.ServiceContext;
import com.rackspace.papi.service.context.ServletContextHelper;
import com.rackspace.papi.service.event.PowerFilterEvent;
import com.rackspace.papi.service.event.common.Event;
import com.rackspace.papi.service.event.common.EventService;
import com.rackspace.papi.service.event.listener.SingleFireEventListener;
import java.io.File;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.slf4j.Logger;

public class ArtifactManagerServiceContext implements ServiceContext<ArtifactManager> {

   private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ArtifactManagerServiceContext.class);
   public static final String SERVICE_NAME = "powerapi:/kernel/artifact-deployment";
   private DestroyableThreadWrapper watcherThread;
   private ArtifactManager artifactManager;
   private EventService eventManagerReference;
   private ContainerConfigurationListener containerCfgListener = null;

   public ArtifactManagerServiceContext(ArtifactManager artifactManager, EventService eventManagerReference, ContainerConfigurationListener containerCfgListener) {
      this.artifactManager = artifactManager;
      this.eventManagerReference = eventManagerReference;
      this.containerCfgListener = containerCfgListener;
   }

   @Override
   public String getServiceName() {
      return SERVICE_NAME;
   }

   @Override
   public ArtifactManager getService() {
      return artifactManager;
   }

   @Override
   public void contextInitialized(ServletContextEvent sce) {
      final ServletContext ctx = sce.getServletContext();
      final ContextAdapter contextAdapter = ServletContextHelper.getInstance().getPowerApiContext(ctx);

      watcherThread = new DestroyableThreadWrapper(contextAdapter.threadingService().newThread(containerCfgListener.getDirWatcher(), "Artifact Watcher Thread"), containerCfgListener.getDirWatcher());

      contextAdapter.configurationService().subscribeTo("container.cfg.xml", containerCfgListener, ContainerConfiguration.class);

      eventManagerReference.listen(artifactManager, ApplicationArtifactEvent.class);
      eventManagerReference.listen(new SingleFireEventListener<PowerFilterEvent, Long>(PowerFilterEvent.class) {

         @Override
         public void onlyOnce(Event<PowerFilterEvent, Long> e) {
            watcherThread.start();
         }
      }, PowerFilterEvent.POWER_FILTER_CONFIGURED);
   }

   @Override
   public void contextDestroyed(ServletContextEvent sce) {
      try {
         final EventService eventManagerReference = ServletContextHelper.getInstance().getPowerApiContext(sce.getServletContext()).eventService();
         eventManagerReference.squelch(artifactManager, ApplicationArtifactEvent.class);

         if (containerCfgListener.isAutoClean()) {
            delete(containerCfgListener.getUnpacker().getDeploymentDirectory());
         }
      } finally {
         watcherThread.destroy();
      }
   }

   private void delete(File file) {
      if (file.isDirectory()) {
         for (File c : file.listFiles()) {
            delete(c);
         }
      }

      if (!file.delete()) {
         LOG.warn("Failure to delete file " + file.getName() + " on repose shutdown.");
      }
   }
}