
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;

public class DeployCustomVM implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Deploys a Virtual Machine with Customization Specs.\n"
			+"\n"
			+"Requirement:\n"
			+"The customization template used should only require an ip address\n"
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-Name: name of the VM\n"
			+"-Template: VM template to use\n"
			+"-Cluster: cluster where to deploy the VM\n"
			+"-Datasstore: datastore to use\n"
			+"-Provisioning: provisionning type (thin/thick: default thick)\n"
			+"-ResourcePool: ressource pool where the VM will be placed (default: resource)\n"
			+"-Folder: folder where to place the VM (default: /)\n"
			+"-CustomizationTemplate: customization tempalte (sysprep) to use\n"
			+"-IPAddress: IP Address of the VM"
			+"\n"
			+"Responses:\n"
			+"-success: the VM was deployed\n"
			+"-failure: an error occured while deployin the VM\n"
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
		String template = ActionRequestUtils.resolveStringParam(request, "template");
		String cluster = ActionRequestUtils.resolveStringParam(request, "cluster");
		String datastore = ActionRequestUtils.resolveStringParam(request, "datastore");
		String provisionning = ActionRequestUtils.resolveStringParam(request, "provisionning");
		String resourcepool = ActionRequestUtils.resolveStringParam(request, "resourcepool");
		String folder = ActionRequestUtils.resolveStringParam(request, "folder");
		String customization = ActionRequestUtils.resolveStringParam(request, "customization");
		String ipaddress = ActionRequestUtils.resolveStringParam(request, "ipaddress");
		
		try {
			// Deploy the Virtual Machine
			Task task = deployCustomVM(
					server, username, password, name, template, cluster, datastore,
					provisionning, resourcepool, folder, customization, ipaddress
					);
			// Indicate the result (task & result)
			result.add("task",task.getMOR().val);
			result.add(DeployCustomVM.RETURNRESULT,"Deplyement started");
			// Set success return code
			result.setReturnCode(DeployCustomVM.SUCCESS);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(DeployCustomVM.FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(DeployCustomVM.RETURNRESULT,e.getMessage());
		}
		
		return result;
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(DeployCustomVM.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		// Set the name argument
		RASBinding namearg = RASBindingFactory.createPromptBinding("VM Name:", true);
		// Set the template argument
		RASBinding templatearg = RASBindingFactory.createPromptBinding("Template:", true);
		// Set the cluster argument
		RASBinding clusterarg = RASBindingFactory.createPromptBinding("Cluster:", true);
		// Set the datastore argument
		RASBinding datastorearg = RASBindingFactory.createPromptBinding("Datastore:",true);
		// Set the provisionning argument
		RASBinding provisionningarg = RASBindingFactory.createPromptBinding("Provisionning",false);
		// Set the resource pool argument
		RASBinding resourcepoolarg = RASBindingFactory.createPromptBinding("Resource Pool:", false);
		// Set the folder argument
		RASBinding folderarg = RASBindingFactory.createPromptBinding("Folder:", false);
		// Set the customization template argument
		RASBinding customizationtplarg = RASBindingFactory.createPromptBinding("Customization Template:", true);
		// Set the ipaddress
		RASBinding ipaddressarg = RASBindingFactory.createPromptBinding("IP Address", true);
		
		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		parameters.add("name",namearg);
		parameters.add("template",templatearg);
		parameters.add("cluster", clusterarg);
		parameters.add("datastore",datastorearg);
		parameters.add("provisionning", provisionningarg);
		parameters.add("resourcepool",resourcepoolarg);
		parameters.add("folder",folderarg);
		parameters.add("customization",customizationtplarg);
		parameters.add("ipaddress",ipaddressarg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add("task","");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(DeployCustomVM.SUCCESS));
		responses.add("failure",String.valueOf(DeployCustomVM.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}

	public static Task deployCustomVM(String server, String username, String password,
			String name, String template, String cluster, String datastore, String provisionning,
			String resourcepool, String folder, String customization, String ipaddress) throws Exception {
		
		// Set default parameters if provided empty
		// Provisionning type
		provisionning = provisionning.toLowerCase();
		if (provisionning.equals("")) {
			provisionning="thick";
		} else {
			if (!(provisionning.equals("thick") | provisionning.equals("thin"))) {
				throw new Exception("Provisionning nor thick or thin or empty.");
			}
		}
		// Resource Pool
		if (resourcepool.equals("")) {
			resourcepool = "resources";
		}
		// Folder
		if (folder.equals("")) {
			folder = "vm";
		}
		// Get the vCenter URL
		URL url = new URL("https://" + server + "/sdk");
		// Connect to vCenter
		ServiceInstance si = new ServiceInstance(url,username,password,true);
		// Get the root Folder
		Folder rootFolder = si.getRootFolder();
		// Get the inventory navigator
		InventoryNavigator navigator = new InventoryNavigator(rootFolder);
		// Get proper referances for variables on the deploy task.
		// Search for specified template
		VirtualMachine vm = (VirtualMachine) navigator.searchManagedEntity("VirtualMachine",template);
		if (vm==null) {
			throw new Exception("temlpate not found.");
		}
		if (!vm.getConfig().template) {
			throw new Exception("VM found by template name is not a template!");
		}
		// Search for specified datastore
		Datastore ds = (Datastore) navigator.searchManagedEntity("Datastore",datastore);
		if (ds==null) {
			throw new Exception("Provided datastore not found.");
		}
		// Search the resource pool
		ResourcePool rp = (ResourcePool) navigator.searchManagedEntity("ResourcePool",resourcepool);
		if (rp==null) {
			throw new Exception("Provided resourcepool not found.");
		}
		// Search for the right folder
		Folder fld = rootFolder;
		String[] folders = folder.split("/");
		InventoryNavigator currentnavigator = new InventoryNavigator(rootFolder);
		for (String subfolder : folders) {
			fld  = (Folder) currentnavigator.searchManagedEntity("Folder", subfolder);
			currentnavigator = new InventoryNavigator(fld);
		}
		if (fld==null) {
			throw new Exception("Provided folder not found.");
		}
		// Get the customization spec manager
		CustomizationSpecManager customSpecMgr = si.getCustomizationSpecManager();
		// Search for Specified Customization Sepcifications
		CustomizationSpecItem customSpec = customSpecMgr.getCustomizationSpec(customization);
		if (customSpec==null) {
			throw new Exception("Provided Customization specification not found.");
		}
		// Set the provisionning type
		Boolean thinProvisioned = provisionning.equals("thin");
		// Parameters Object Set
		// Build the clone specification
		VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
		// The target vm is not a template
		cloneSpec.template = false;
		// The target vm won't be powered on
		cloneSpec.powerOn = false;
		// Set the parameters for the VM destination
		cloneSpec.location =  new VirtualMachineRelocateSpec();
		// Set the datastore
		cloneSpec.location.datastore = ds.getMOR();
		// Set the resourcepool
		cloneSpec.location.pool = rp.getMOR();
		/*
		 * Does not work :-(
		 * Switching to deprecated transform property
		// Set the provisionning type (per device)
		// empty array of template disks
		ArrayList<VirtualDevice> hardDisks = new ArrayList<VirtualDevice>();
		// parse all devices of the template
		for (VirtualDevice device : vm.getConfig().hardware.device) {
			// if device is a disk
			if (VirtualDisk.class.isAssignableFrom(device.getClass())) {
				// add it to the disk array
				hardDisks.add(device);
			}
		}
		// Create the disk relocation array
		cloneSpec.location.disk = new VirtualMachineRelocateSpecDiskLocator[hardDisks.size()];
		for (int i = 0; i < hardDisks.size(); i++) {
			// Create the Disk locator object
			cloneSpec.location.disk[i] = new VirtualMachineRelocateSpecDiskLocator();
			// Set the Disk ID
			cloneSpec.location.disk[i].diskId = hardDisks.get(i).key;
			// Set the target Datastore
			cloneSpec.location.disk[i].datastore = ds.getMOR();
			// Create the diskBacking Object
			VirtualDiskFlatVer2BackingInfo diskBackingInfo = new VirtualDiskFlatVer2BackingInfo();
			// Set the backing type
			diskBackingInfo.thinProvisioned = thinProvisioned;
			// Set the disk mode
			diskBackingInfo.diskMode = "persistent";
			// Set the datastore
			diskBackingInfo.datastore = ds.getMOR();
			// Add the information to the spec
			cloneSpec.location.disk[i].diskBackingInfo = diskBackingInfo;
		} 
		*/
		// Set the transform parameter
		if (thinProvisioned) {
			cloneSpec.location.transform =  VirtualMachineRelocateTransformation.sparse;
		} else {
			cloneSpec.location.transform =  VirtualMachineRelocateTransformation.flat;
		}
		// Set the customization specification
		cloneSpec.customization = customSpec.getSpec();
		// Personalize the customisation spec (IP Address)
		cloneSpec.customization.nicSettingMap[0].adapter.ip = new CustomizationFixedIp();
		((CustomizationFixedIp)cloneSpec.customization.nicSettingMap[0].adapter.ip).ipAddress = ipaddress;
		// reset annotation
		cloneSpec.config =  new VirtualMachineConfigSpec();
		cloneSpec.config.annotation = "";
		//Start the clone
		Task task = vm.cloneVM_Task(fld, name, cloneSpec);
		si.getServerConnection().logout();
		//OO will fail if he must wait for task completion...
		//It can monitor the task status
		return task;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String server = null;
		String username = null; 
		String password = null;
		String name = null;
		String template = null;
		String cluster = null;
		String datastore = null;
		String provisionning = null;
		String resourcepool = null;
		String folder = null;
		String customization = null;
		String ipaddress = null;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print("vCenter Server:");
			server = br.readLine();
			System.out.print("Username:");
			username = br.readLine();
			System.out.print("Password:");
			password =br.readLine();
			System.out.print("Name:");
			name = br.readLine();
			System.out.print("Template:");
			template = br.readLine();
			System.out.print("Cluster:");
			cluster = br.readLine();
			System.out.print("Datastore:");
		    datastore = br.readLine();
			System.out.print("Provisioning:");
			provisionning = br.readLine();
			System.out.print("Resource Pool:");
			resourcepool = br.readLine();
			System.out.print("Folder:");
			folder = br.readLine();
			System.out.print("Customization:");
			customization = br.readLine();
			System.out.print("IP Address:");
			ipaddress = br.readLine();
			System.out.println("------------------------------");
			System.out.println("This will deploy a VM:");
			System.out.println("vCenter: " + username + "@" + server);
			System.out.println("VM name: " + name);
			System.out.println("Cluster: " + cluster);
			System.out.println("Datastore: " + datastore);
			System.out.println("Provisionning; " + provisionning);
			System.out.println("Resource Pool: " + resourcepool);
			System.out.println("Folder: " + folder);
			System.out.println("Customization: " + customization);
			System.out.println("IP Address: " + ipaddress);
			System.out.println("------------------------------");
			System.out.println("Confirm (y/n):");
			String confirm = br.readLine();
			if (confirm.equals("y")) {
				Task task = DeployCustomVM.deployCustomVM(server, username, password, name, template, cluster, datastore, provisionning, resourcepool, folder, customization, ipaddress);
				System.out.println("Task: " + task.getMOR().val);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
	
}
