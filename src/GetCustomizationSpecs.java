import java.net.URL;

import com.google.gson.Gson;

import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

public class GetCustomizationSpecs implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Get Customization Specs"
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
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
		
		try {
			// Get the Guest informations
			CustomizationSpecInfo[] infos = getCustomizationSpecs(server, username, password);
			// Indicate the result (task & result)
			result.add("customizations", new Gson().toJson(infos));
			result.add(GetCustomizationSpecs.RETURNRESULT,"Customization Spec Informations retrieved");
			// Set success return code
			result.setReturnCode(GetCustomizationSpecs.SUCCESS);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(GetCustomizationSpecs.FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(GetCustomizationSpecs.RETURNRESULT,e.getMessage());
		}
		
		return result;
	}

	public static CustomizationSpecInfo[] getCustomizationSpecs(String server, String username, String password) throws Exception {
		// Get the vCenter URL
		URL url = new URL("https://" + server + "/sdk");
		// Connect to vCenter
		ServiceInstance si = new ServiceInstance(url,username,password,true);
		CustomizationSpecInfo[] infos = si.getCustomizationSpecManager().getInfo();
		si.getServerConnection().logout();
		return infos;
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(GetCustomizationSpecs.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		
		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add("customizations","");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(GetCustomizationSpecs.SUCCESS));
		responses.add("failure",String.valueOf(GetCustomizationSpecs.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}
	
}

