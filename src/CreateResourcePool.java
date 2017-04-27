import java.net.URL;

import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

public class CreateResourcePool implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Set the powerState of a virtual machine."
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-Cluster: cluster to contain the resourcepool"
			+"-name: resourcepool name\n"
			+"\n"
			+"Responses:\n"
			+"-success: the ressource pool is created or exists"
			+"-failure: the ressource pool could not be created"
			+"\n</pre>";
	
	// Success return code
	public static final int SUCCESS = 0;
	
	// Failure return code
	public static final int FAILURE = 1;
	
	// result String identifier.
	private static final String RETURNRESULT = "returnResult";

	@Override
	public ActionResult execute(ISessionContext session, ActionRequest request,
			IActionRegistry registry) throws Exception {
		
		// Create the result
		ActionResult result = new ActionResult();
		
		String server = ActionRequestUtils.resolveStringParam(request, "server");
		String username = ActionRequestUtils.resolveStringParam(request, "username"); 
		String password = ActionRequestUtils.resolveStringParam(request, "password");
		String cluster = ActionRequestUtils.resolveStringParam(request, "cluster");
		String name = ActionRequestUtils.resolveStringParam(request, "name");
		
		try {
			// Get the Guest informations
			createResourcePool(server, username, password, cluster, name);
			result.add(CreateResourcePool.RETURNRESULT,"resource pool created.");
			// Set success return code
			result.setReturnCode(SUCCESS);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(RETURNRESULT,e.getMessage());
		}
		
		return result;
	}

	public static void createResourcePool(String server, String username, String password,
			String cluster, String name) throws Exception {
		// Get the vCenter URL
		URL url = new URL("https://" + server + "/sdk");
		// Connect to vCenter
		ServiceInstance si = new ServiceInstance(url,username,password,true);
		// Get the root Folder
		Folder rootFolder = si.getRootFolder();
		// Get the inventory navigator
		InventoryNavigator navigator = new InventoryNavigator(rootFolder);
		// get the cluster
		ClusterComputeResource clusterEntity = (ClusterComputeResource) navigator.searchManagedEntity("ClusterComputeResource", cluster);
		if (clusterEntity == null) {
			throw new Exception("Cluster not found.");
		}
		// Change the search root object to the found cluster
		navigator = new InventoryNavigator(clusterEntity);
		// Search for the root resourcepool
		ResourcePool curRpEntity = (ResourcePool) navigator.searchManagedEntity("ResourcePool", "Resources");
	    if (curRpEntity == null) {
	    	throw new Exception("Root resource pool not found (DRS enabled cluster?).");
	    }
	    // Change the search root to the found resourcepool
	    navigator = new InventoryNavigator(curRpEntity);
	    // parse the given new resourcepool
	    ResourceConfigSpec resConfSpec = new ResourceConfigSpec();
	    ResourceAllocationInfo resAllocInfo = new ResourceAllocationInfo();
	    resConfSpec.cpuAllocation = resAllocInfo;
	    resConfSpec.memoryAllocation = resAllocInfo;
		for (String folder : name.split("/")) {
			ResourcePool rpEntity = (ResourcePool) navigator.searchManagedEntity("ResourcePool",folder);
			if (rpEntity == null) {
				rpEntity = curRpEntity.createResourcePool(folder, resConfSpec);
			}
			curRpEntity = rpEntity;
		    // Change the search root to the found or created resourcepool
			navigator = new InventoryNavigator(curRpEntity);
		}
		//logout
		si.getServerConnection().logout();
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(CreateResourcePool.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		// Set the cluster 
		RASBinding clusterarg = RASBindingFactory.createPromptBinding("Cluster:", true);
		// Set the name argument
		RASBinding namearg = RASBindingFactory.createPromptBinding("Resource Pool Path:", true);

		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		parameters.add("cluster",clusterarg);
		parameters.add("name",namearg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(CreateResourcePool.SUCCESS));
		responses.add("failure",String.valueOf(CreateResourcePool.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}
	
}

