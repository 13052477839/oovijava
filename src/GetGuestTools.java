import java.net.URL;

import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

public class GetGuestTools implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Get Guest Tools informations (non nested info)"
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-name: virtual machine name\n"
			+"\n"
			+"Responses:\n"
			+"-success: the VM has been adapted\n"
			+"-failure: the VM failed to be adapted\n"
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
			// Get the Guest informations
			GuestInfo infos = getGuestTools(server, username, password, name);
			result.add("appHeartbeatStatus",infos.appHeartbeatStatus);
			result.add("guestFamily", infos.guestFamily);
			result.add("guestFullName",infos.guestFullName);
			result.add("guestState",infos.guestState);
			result.add("hostName",infos.hostName);
			result.add("ipAddress",infos.ipAddress);
			result.add("toolsRunningStatus",infos.toolsRunningStatus);
			result.add("toolsVersion",infos.toolsVersion);
			result.add("toolsVersionStatus",infos.toolsVersionStatus);
			result.add("toolsVersionStatus2",infos.toolsVersionStatus2);
			// Indicate the result (task & result)
			result.add(GetGuestTools.RETURNRESULT,"Guest Informations retrieved");
			// Set success return code
			result.setReturnCode(GetGuestTools.SUCCESS);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(GetGuestTools.FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(GetGuestTools.RETURNRESULT,e.getMessage());
		}
		
		return result;
	}

	public static GuestInfo getGuestTools(String server, String username, String password,
			String name) throws Exception {
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
		GuestInfo guest = vm.getGuest();
		//logout
		si.getServerConnection().logout();
		return guest;
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(GetGuestTools.DESCRIPTION);
		
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
		resultFields.add("appHeartbeatStatus","");
		resultFields.add("guestFamily", "");
		resultFields.add("guestFullName","");
		resultFields.add("guestState","");
		resultFields.add("hostName","");
		resultFields.add("ipAddress","");
		resultFields.add("toolsRunningStatus","");
		resultFields.add("toolsVersion","");
		resultFields.add("toolsVersionStatus","");
		resultFields.add("toolsVersionStatus2","");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(GetGuestTools.SUCCESS));
		responses.add("failure",String.valueOf(GetGuestTools.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}
	
}

