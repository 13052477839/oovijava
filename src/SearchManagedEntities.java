
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;

import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.util.*;

public class SearchManagedEntities implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Get Management Objects.\n"
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-columnSeparator: column separator\n"
			+"-rowSeparator: raw separator\n" 
			+"-type: Management Object Type\n"
			+"-properties: properties to retrieve\n"
			+"-rootType: Root Management Object Type\n"
			+"-rootMOR: Reference to the root Object\n"
			+"-recurse: recurse in inventory\n"
			+"-nameMatch: case insensitive contains on name\n"
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
		String columnSeparator = ActionRequestUtils.resolveStringParam(request, "columnSeparator"); 
		String rowSeparator = ActionRequestUtils.resolveStringParam(request, "rowSeparator");
		String type = ActionRequestUtils.resolveStringParam(request, "type");
		String properties = ActionRequestUtils.resolveStringParam(request, "properties");
		String rootType = ActionRequestUtils.resolveStringParam(request, "rootType");
		String rootMOR = ActionRequestUtils.resolveStringParam(request, "rootMOR");
		String recurse = ActionRequestUtils.resolveStringParam(request, "recurse");
		String nameMatch = ActionRequestUtils.resolveStringParam(request, "nameMatch");
		
		try {
			// Deploy the Virtual Machine
			String objects = searchManagedEntities(server, username, password, columnSeparator, rowSeparator, type, properties, rootType, rootMOR, recurse, nameMatch);
			// Indicate the result (task & result)
			result.add("objects",objects);
			result.add(SearchManagedEntities.RETURNRESULT,"Retruned Objects.");
			// Set success return code
			result.setReturnCode(SearchManagedEntities.SUCCESS);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(SearchManagedEntities.FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(SearchManagedEntities.RETURNRESULT,e.getMessage());
		}
		
		return result;
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(SearchManagedEntities.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the column separator argument
		RASBinding columnSeparatorarg = RASBindingFactory.createPromptBinding("Column separator:", true);
		// Set the row separator argument
		RASBinding rowSeparatorarg = RASBindingFactory.createPromptBinding("Row separator:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		// Set the type argument
		RASBinding typearg = RASBindingFactory.createPromptBinding("Managed Object Type:", true);
		// Set the properties argument
		RASBinding propertiesarg = RASBindingFactory.createPromptBinding("Properties", false);
		// Set the rootType argument
		RASBinding rootTypearg = RASBindingFactory.createPromptBinding("Root Managed Object Type:", false);
		// Set the rootMOR argument
		RASBinding rootMORarg = RASBindingFactory.createPromptBinding("Root Managed Object Refrence:", false);
		// Set the rootMOR argument
		RASBinding recursearg = RASBindingFactory.createPromptBinding("Recurse:", false);
		// Set the nameMatch argument
		RASBinding nameMatcharg = RASBindingFactory.createPromptBinding("Name Match:",false);
		
		
		
		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		parameters.add("columnSeparator",columnSeparatorarg);
		parameters.add("rowSeparator",rowSeparatorarg);
		parameters.add("type",typearg);
		parameters.add("properties",propertiesarg);
		parameters.add("rootType",rootTypearg);
		parameters.add("rootMOR",rootMORarg);
		parameters.add("recurse",recursearg);
		parameters.add("nameMatch",nameMatcharg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add("objects", "");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(SearchManagedEntities.SUCCESS));
		responses.add("failure",String.valueOf(SearchManagedEntities.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}

	public static String searchManagedEntities(String server, String username,
			String password, String columnSeparator, String rowSeparator, String type, String properties, String rootType, String rootMOR, String recurse, String nameMatch) throws Exception {
		//Parse parameters
		ManagedObjectReference mor = null;
		if (!((("".equals(rootType)) | ("".equals(rootMOR))))) {
			mor = new ManagedObjectReference();
			mor.setType(rootType);
			mor.setVal(rootMOR);
		}
		if (!(properties.contains("name"))) {
			properties = "name," + properties;
		}
		if (type==null) { type = "ManagedEntity"; }
		String[] props = properties.split(",");
		String[][] typeinfo = new String[1][props.length+1];
		typeinfo[0][0] = type;
		int index = 1;
		for (String prop : props) {
			typeinfo[0][index++] = prop;
		}
		Boolean rec = !("false".equals(recurse.toLowerCase()));
		// Get the vCenter URL
		URL url = new URL("https://" + server + "/sdk");
		// Connect to vCenter
		ServiceInstance si = new ServiceInstance(url,username,password,true);
		ManagedEntity root = null;
		if (mor!=null) {
			root =  MorUtil.createExactManagedEntity(si.getServerConnection(), mor);
		} else {
			// Get the root Folder
			root = si.getRootFolder();
		}	
		if (root==null) {
			throw new Exception("Root Managed Object not found.");
		}
		// Get the inventory navigator
		// InventoryNavigator navigator = new InventoryNavigator(root);
		// Get proper references for variables on the deploy task.
		// Search for specified template
		// Replace with proper search including filters.
		// ManagedEntity[] objects = navigator.searchManagedEntities(typeinfo,rec);
		// Get object contents according to requested infos
		ObjectContent[] ocs = retrieveObjectContents(typeinfo, rec, si, root);
		// if nothing returned... exception
		if (ocs==null || ocs.length == 0) {
			throw new Exception("Null array returned.");
		}
		// build the retuned array
		ArrayList<String> results = new ArrayList<String>(); 
		for ( ObjectContent oc: ocs) {
			DynamicProperty[] propSet = oc.getPropSet();
			Hashtable<String,String> strProps = new Hashtable<String,String>();
			if (propSet.length > 0) {
				for (DynamicProperty prop : propSet) {
					strProps.put(prop.getName().toLowerCase(),prop.getVal().toString());
				}
			} else {
				// no properties returned !
				throw new Exception("no properties returned");
			}
			if (strProps.containsKey("name")) {
				if (strProps.get("name").toLowerCase().contains(nameMatch.toLowerCase())) {
					String value = "";
					value += "type:" + oc.getObj().getType();
					value += columnSeparator + "id:" + oc.getObj().getVal();
					for (String prop: props) {
						if (strProps.containsKey(prop.toLowerCase())) {
							value += columnSeparator + prop + ":" + strProps.get(prop.toLowerCase());
						}
					}
					results.add(value);
				}
			} else {
				throw new Exception("name not returned");
			}
		}
		/* done directly with object content parser
		for (ManagedEntity object : objects) {
			Boolean match = true;
			if (nameMatch != null) {
				if (!("".equals(nameMatch))) {
					match = object.getName().contains(nameMatch);
				}
			}
			if (match) {
				String value = "type:" + object.getMOR().type;
				value += ",id:" + object.getMOR().val;			
				for (String property : props) {
					value += columnSeparator  + property + ":" + object.getPropertyByPath(property);
				}
				results.add(value);
			}
		} */
		String finalResult = "";
		boolean first = true;
		for (String result : results) {
			if (!(first)) {
				finalResult += rowSeparator;
			}
			finalResult += result;
			first = false;
		}
		return finalResult;
	}	
	
	@SuppressWarnings("deprecation")
	private static ObjectContent[] retrieveObjectContents(String[][] typeinfo, boolean recurse, ServiceInstance si, ManagedEntity rootEntity) throws InvalidProperty, RuntimeFault, RemoteException
	{
		if (typeinfo == null || typeinfo.length == 0) 
		{
			return null;
		}
		PropertyCollector pc = si.getPropertyCollector();
		SelectionSpec[] selectionSpecs = null;
		if (recurse) {
			selectionSpecs = PropertyCollectorUtil.buildFullTraversalV4();
		}
	
		PropertySpec[] propspecary = PropertyCollectorUtil.buildPropertySpecArray(typeinfo);
		
		ObjectSpec os = new ObjectSpec();
		os.setObj(rootEntity.getMOR());
		os.setSkip(Boolean.FALSE);
		os.setSelectSet(selectionSpecs);
		
		PropertyFilterSpec spec = new PropertyFilterSpec();
		spec.setObjectSet(new ObjectSpec[] { os });
		spec.setPropSet(propspecary);
		
		return pc.retrieveProperties(new PropertyFilterSpec[] { spec });
	}
	
	
	/*private static ManagedEntity[] createManagedEntities(ObjectContent[] ocs, ServiceInstance si) {
		if(ocs==null) {
			return new ManagedEntity[] {};
		}
		ServerConnection sc = si.getServerConnection();
		ManagedEntity[] mes = new ManagedEntity[ocs.length];
		for(int i=0; i<mes.length; i++) {
			ManagedObjectReference mor = ocs[i].getObj();
			mes[i] = MorUtil.createExactManagedEntity(sc, mor);
		}
		return mes;
	}*/
	
	 
	public static void main(String[] args) {
		String server = null;
		String username = null; 
		String password = null;
		String type = null;
		String properties = null;
		String rootType = null;
		String rootMOR = null;
		String recurse = null;
		String nameMatch = null;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print("vCenter Server:");
			server = br.readLine();
			System.out.print("Username:");
			username = br.readLine();
			System.out.print("Password:");
			password =br.readLine();
			System.out.print("type:");
			type = br.readLine();
			System.out.print("properties:");
			properties = br.readLine();
			System.out.print("root type:");
			rootType = br.readLine();
			System.out.print("root identifier:");
		    rootMOR = br.readLine();
			System.out.print("recurse:");
			recurse = br.readLine();
			System.out.print("name match:");
			nameMatch = br.readLine();
			System.out.println("------------------------------");
			System.out.println("This will deploy a VM:");
			System.out.println("vCenter: " + username + "@" + server);
			System.out.println("Type: " + type);
			System.out.println("Properties: " + properties);
			System.out.println("root: " + rootType + "@" + rootMOR );
			System.out.println("recurse: " + recurse);
			System.out.println("name match: " + nameMatch);
			System.out.println("------------------------------");
			System.out.println("Confirm (y/n):");
			String confirm = br.readLine();
			if (confirm.equals("y")) {
				String objects = searchManagedEntities(server, username, password,";","\n", type, properties, rootType, rootMOR, recurse, nameMatch);
         	   System.out.println("Objects:");
               System.out.println(objects);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
}

