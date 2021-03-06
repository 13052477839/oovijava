import java.net.URL;

import java.util.HashSet;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.DatastoreSummary;
import com.vmware.vim25.StoragePodSummary;

public class GetDatastoreCluster implements IAction {

    	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Get Datastore Cluster."
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-name: Datastore Cluster Name\n"
            +"-folder: Folder in wich to search for the datastore cluster\n"
			+"-columnSeparator: column separator\n"
			+"-rowSeparator: raw separator\n" 
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
		String folder = ActionRequestUtils.resolveStringParam(request, "folder");
		String columnSeparator = ActionRequestUtils.resolveStringParam(request, "columnSeparator"); 
		String rowSeparator = ActionRequestUtils.resolveStringParam(request, "rowSeparator");
		
		try {
			// Get the Guest informations
			String pods = getDatastoreCluster(server, username, password, name, folder, columnSeparator, rowSeparator);
			// Indicate the result (task & result)
			result.add("storagepods",pods);
			result.add(GetDatastoreCluster.RETURNRESULT,"storage cluster found.");
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

    public static String getDatastoreCluster(String server, String username, String password,
			String name, String folder, String columnSeparator, String rowSeparator) throws Exception {
		// set default values
		if (name.isEmpty()) { name = ".*"; }
		//prepare folder - remove starting and trailing '/'
		folder = folder.replaceAll("/$|^/", "");
		String[] folders = folder.split("/");
		// Get the vCenter URL
		URL url = new URL("https://" + server + "/sdk");
		// Connect to vCenter
		ServiceInstance si = new ServiceInstance(url,username,password,true);
		// Get the root Folder
		ManagedEntity rootFolder = si.getRootFolder();
		// move the rootFolder to desired location
		if (folders.length > 0) {
			if (!folders[0].isEmpty()) {
				String matched = "";
				for (String tfolder: folders) {
					//search childs of current folder for next matching folders
					Boolean found = false;
					if (rootFolder instanceof Folder) {
						ManagedEntity[] childs = ((Folder)rootFolder).getChildEntity();
						for(ManagedEntity child: childs) {
							if (child instanceof Folder && tfolder.equals(child.getName())) {
								// match found
								// update matched, level and set rootFolder
								matched += "/" + tfolder; 
								rootFolder = (Folder)child;
								found = true;
								break;
							} else if (child instanceof Datacenter && tfolder.equals(child.getName())) {
								matched += "/" + tfolder;
								rootFolder = child;
								found = true;
								break;
							} 
						}
					} else if (rootFolder instanceof Datacenter && tfolder.equals("datastore"))  {
						matched += "/datastore";
						rootFolder = ((Datacenter)rootFolder).getDatastoreFolder();
						continue;
					} 
					if (found) { continue; }
					matched = matched.replaceAll("/$|^/", "");
					throw new Exception("Could not find folder '" + folder + "' matched '" + matched + "'" );
				}
			}
		}
		// Get the inventory navigator
		InventoryNavigator navigator = new InventoryNavigator(rootFolder);
		// Get proper references for variables on the deploy task.
		// Search for storage pods in folder
		//ManagedEntity[] pods = navigator.searchManagedEntities("StoragePod");
		//More precise search
		String[][] typeinfo = new String[1][5];
		typeinfo[0][0] = "StoragePod";
		typeinfo[0][1] = "name";
		typeinfo[0][2] = "childEntity";
		typeinfo[0][3] = "summary.freeSpace";
		typeinfo[0][4] = "summary.capacity";
		ManagedEntity[] pods = navigator.searchManagedEntities(typeinfo,true);
		if (pods.length == 0) {
			throw new Exception("No data store clusters found.");
		}
		// collect matching storage pods
		HashSet<StoragePod> matchedpods = new HashSet<StoragePod>();
		for(ManagedEntity pod: pods) {
			if (pod instanceof StoragePod) {
				if (pod.getName().matches(name)) {
					matchedpods.add((StoragePod)pod);
				}
			}
		}
		// prepare the resulting table
		String strresult  = "";
		for(StoragePod pod: matchedpods) {
			// get summary
		    StoragePodSummary summary = pod.getSummary();
			// get the datastore with the largest free space percentage
			Datastore largestFree = null;
			double largestFreePc = 0.0;
			long largestFreeSpace = 0;
			//ManagedEntity[] childs = pod.getChildEntity();
			//Gather informations about child Datastores
			String[][] dstypeinfo = new String[1][6];
			dstypeinfo[0][0] = "Datastore";
			dstypeinfo[0][1] = "name";
			dstypeinfo[0][2] = "summary.freeSpace";
			dstypeinfo[0][3] = "summary.capacity";
			dstypeinfo[0][4] = "summary.maintenanceMode";
			dstypeinfo[0][5] = "summary.accessible";
			// Get the inventory navigator
			InventoryNavigator dsnavigator = new InventoryNavigator(pod);
			ManagedEntity[] ds = dsnavigator.searchManagedEntities(dstypeinfo,true);
			for(ManagedEntity child: ds) {
				// find child object of type datastore
				if (child instanceof Datastore) {
					Datastore toCheck = (Datastore)child;
					// get the summary
					DatastoreSummary toCheckSummary = toCheck.getSummary();
					// check if accessible otherwise ignore
					if (!toCheckSummary.accessible) { 
						System.out.println("Datastore not accessible");
						continue;	
					}
					// check if in maintenance otherwise ignore
					if (!"normal".equals(toCheckSummary.getMaintenanceMode())) {
						System.out.println("Datastore state: " + toCheckSummary.getMaintenanceMode());
						continue;
					}
					float toCheckFreePc = toCheckSummary.getFreeSpace() / toCheckSummary.getCapacity();
					if (largestFree == null) {
						largestFree = toCheck;
						largestFreePc = toCheckFreePc;
						largestFreeSpace = toCheckSummary.getFreeSpace();
						continue;
					}
					if (largestFreePc < toCheckFreePc) {
						largestFree = toCheck;
						largestFreePc = toCheckFreePc;
						largestFreeSpace = toCheckSummary.getFreeSpace();
					}
				}
			}
			if (largestFree == null) {
				continue;
			}
			// generate the path to simplify further searches
			String path = "";
			ManagedEntity mo = pod;
			while (mo!=null) {
				path =  mo.getName() + "/" + path;
				if (mo instanceof Datacenter) {
					break;
				}
				mo = mo.getParent();
			}
			path = path.replaceAll("/$|^/","");
			if (largestFree == null) {
				continue;
			}
			strresult += rowSeparator 
			    + "moref:" + pod.getMOR().toString() + columnSeparator 
				+ "name:" + pod.getName() + columnSeparator 
				+ "size:" + summary.getCapacity() + columnSeparator 
				+ "free:" + summary.getFreeSpace() + columnSeparator
				+ "largestfree:" + largestFreeSpace + columnSeparator
				+ "datastore:" + largestFree.getMOR().toString() + columnSeparator
				+ "datastorename:" + largestFree.getName() + columnSeparator
				+ "path:" + path;
		}
		// strip extra rowseparators
		strresult = strresult.replaceAll(rowSeparator + "$|^" + rowSeparator, "");
		//logout
		si.getServerConnection().logout();
		return strresult;
    }

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(GetDatastoreCluster.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		// Set the name argument
		RASBinding namearg = RASBindingFactory.createPromptBinding("Datastore Clusetr Name:", true);
		// Set the name argument
		RASBinding folderarg = RASBindingFactory.createPromptBinding("Folder Name:", true);
		// Set the column separator argument
		RASBinding columnSeparatorarg = RASBindingFactory.createPromptBinding("Column separator:", true);
		// Set the row separator argument
		RASBinding rowSeparatorarg = RASBindingFactory.createPromptBinding("Row separator:", true);
		
		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		parameters.add("name",namearg);
		parameters.add("folder",folderarg);
		parameters.add("columnSeparator",columnSeparatorarg);
		parameters.add("rowSeparator",rowSeparatorarg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add("datastore","");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(SetPowerState.SUCCESS));
		responses.add("failure",String.valueOf(SetPowerState.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}

	public static void main(String[] args) {

		String server = null;
		String username = null; 
		String password = null;
		String name = null;
		String folder= null;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print("vCenter Server:");
			server = br.readLine();
			System.out.print("Username:");
			username = br.readLine();
			System.out.print("Password:");
			password =br.readLine();
			System.out.print("Datastore Cluster Name:");
			name = br.readLine();
			System.out.print("Folder:");
			folder = br.readLine();
			System.out.println("------------------------------");
			System.out.println("This will search for Datastore Clusters:");
			System.out.println("vCenter: " + username + "@" + server);
			System.out.println("Name: " + name);
			System.out.println("Folder: " + folder);
			System.out.println("------------------------------");
			System.out.print("Confirm (y/n):");
			String confirm = br.readLine();
			if (confirm.equals("y")) {
				String objects = getDatastoreCluster(server,username,password,name,folder,";","\n");
         	    System.out.println("Datastore clusters:");
                System.out.println(objects);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}