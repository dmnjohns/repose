
package com.rackspace.cloud.valve.controller.service;

import com.rackspace.papi.model.Node;
import java.util.List;
import java.util.Map;
import java.util.Set;


public interface ControllerService {
   
   
   Set<String> getManagedInstances();
   
   void updateManagedInstances(Map<String, Node> nodesToStart, Set<String> nodesToStop);
   
   Boolean reposeInstancesInitialized();
   
   void setConfigDirectory(String directory);
   
   void setConnectionFramework(String framework);
   
   String getConfigDirectory();
   
   String getConnectionFramework();
}