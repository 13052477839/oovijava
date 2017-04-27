
import java.net.URL;
import java.util.ArrayList;

import com.google.gson.Gson;

import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

public class GetVirtualDisks implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Get the virtual machine disks.\n"
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-name: virtual machine name\n"
			+"\n"
			+"Responses:\n"
			+"-success: disks returned\n"
			+"-failure: an error occured\n"
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
		String name = ActionRequestUtils.resolveStringParam(request, "name");
		
		try {
			// Deploy the Virtual Machine
			VirtualDisk[] disks = getVirtualDisks(server, username, password, name);
			// Indicate the result (task & result)
			result.add("disks",new Gson().toJson(disks));
			result.add(GetVirtualDisks.RETURNRESULT,"Deplyement started");
			// Set success return code
			result.setReturnCode(GetVirtualDisks.SUCCESS);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(GetVirtualDisks.FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(GetVirtualDisks.RETURNRESULT,e.getMessage());
		}
		
		return result;
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(GetVirtualDisks.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		// Set the name argument
		RASBinding namearg = RASBindingFactory.createPromptBinding("VM Name:", true);
		
		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		parameters.add("name",namearg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add("disks", "");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(GetVirtualDisks.SUCCESS));
		responses.add("failure",String.valueOf(GetVirtualDisks.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}

	public static VirtualDisk[] getVirtualDisks(String server, String username,
			String password, String name) throws Exception {
		// Get the vCenter URL
		URL url = new URL("https://" + server + "/sdk");
		// Connect to vCenter
		ServiceInstance si = new ServiceInstance(url,username,password,true);
		// Get the root Folder
		Folder rootFolder = si.getRootFolder();
		// Get the inventory navigator
		InventoryNavigator navigator = new InventoryNavigator(rootFolder);
		// Get proper references for variables on the deploy task.
		// Search for specified template
		VirtualMachine vm = (VirtualMachine) navigator.searchManagedEntity("VirtualMachine",name);
		if (vm==null) {
			throw new Exception("Virtual Machine not found.");
		}
		// Get the hardware description
		VirtualHardware hardware = vm.getConfig().hardware;
		//list disk & nic devices
		ArrayList<VirtualDisk> virtualDisks = new ArrayList<VirtualDisk>();
		for (VirtualDevice device :  hardware.device) {
			Class<? extends VirtualDevice> devClass = device.getClass();
			if (VirtualDisk.class.isAssignableFrom(devClass)) {
				// add it to the disk array
				virtualDisks.add((VirtualDisk)device);
			}
		}
		si.getServerConnection().logout();
		VirtualDisk[] disks = new VirtualDisk[virtualDisks.size()];
		for (int i = 0; i < virtualDisks.size();i++) {
			disks[i] = virtualDisks.get(i);
		}
		return disks;
	}	
}

