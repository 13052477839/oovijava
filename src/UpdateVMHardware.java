import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import java.util.Collections;

//import com.google.gson.Gson;
import com.iconclude.webservices.extensions.java.interfaces.*;
import com.iconclude.webservices.extensions.java.types.*;
import com.iconclude.webservices.extensions.java.util.*;
import com.opsware.pas.content.commons.util.StringUtils;
import com.vmware.vim25.*;
import com.vmware.vim25.mo.*;
import com.vmware.vim25.mo.util.MorUtil;

public class UpdateVMHardware implements IAction {
	
	// Description of the action
	private static String DESCRIPTION =  ""
			+"<pre>"
			+"Udate a VM with specified hardware specifications.\n"
			+"If disks are not present they will be ignored.\n"
			+"If new disk sizes are inferior to current size they will be ignored.\n"
			+"\n"
			+"Requirements"
			+"-For CPU & Memory upgrade, if hotadd is not activated, the VM needs to be shutted down."
			+"\n"
			+"Inputs:\n"
			+"-vCenter: vCenter server\n"
			+"-Username: vCenter username\n"
			+"-Password: vCenter password\n"
			+"-CPU: number of CPU\n"
			+"-Memory: quantity of Memory (GB)\n"
			+"-DiskSizes:  a comma separated list of disk sizes.\n"
			+"   0: Will skip a disk\n"
			+"   Sizes inferior to current size will be ignored\n"
			+"-portgroup\n"
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
		String cpu = ActionRequestUtils.resolveStringParam(request, "cpu");
		String mem = ActionRequestUtils.resolveStringParam(request, "memory");
		String disks = ActionRequestUtils.resolveStringParam(request, "disks");
		String portgroup = ActionRequestUtils.resolveStringParam(request, "portgroup"); 
		Task task = null;
		try {
			//Update virtual machine hardware
			task = updateVMHardware(server, username, password, name, cpu, mem, disks, portgroup);
		} catch (Exception e) {
			// Set failed return code
			result.setReturnCode(UpdateVMHardware.FAILURE);
			// Set Exception Stack trace
			result.setException(StringUtils.toString(e));
			// Set the error message
			result.add(UpdateVMHardware.RETURNRESULT,e.getMessage());
		}
		if (task!=null) {
			//Indicate the result (task & result)
			result.add("task",task.getMOR().val);
			result.add(UpdateVMHardware.RETURNRESULT,"Update Virtula Machine Hardware.");
			//Set success return code
			result.setReturnCode(UpdateVMHardware.SUCCESS);
		}
		return result;
	}

	@Override
	public ActionTemplate getActionTemplate() {
		
		// Create the action template
		ActionTemplate actionTemplate = new ActionTemplate();
		
		// Set the description
		actionTemplate.setDescription(UpdateVMHardware.DESCRIPTION);
		
		// Set the vCenter argument
		RASBinding vcenterarg = RASBindingFactory.createPromptBinding("vCenter Server:", true);
		// Set the username argument
		RASBinding usernamearg = RASBindingFactory.createPromptBinding("vCenter User:", true);
		// Set the password argument
		RASBinding passwordarg = RASBindingFactory.createPromptBinding("vCenter Password:", true, true);
		// Set the name argument
		RASBinding namearg = RASBindingFactory.createPromptBinding("VM Name:", true);
		// Set the cpu argument
		RASBinding cpuarg = RASBindingFactory.createPromptBinding("CPU:", false);
		// Set the memory argument
		RASBinding memarg = RASBindingFactory.createPromptBinding("Memory (GB):", false);
		// Set the disk1index argument
		RASBinding disksarg = RASBindingFactory.createPromptBinding("disks:",false);
		// Set the portgroup argument
		RASBinding portgrouparg = RASBindingFactory.createPromptBinding("NIC1 protgroup:",false);
		
		// Create the parameter map
		Map parameters = new Map();
		parameters.add("server",vcenterarg);
		parameters.add("username",usernamearg);
		parameters.add("password",passwordarg);
		parameters.add("name",namearg);
		parameters.add("cpu",cpuarg);
		parameters.add("memory", memarg);
		parameters.add("disks",disksarg);
		parameters.add("portgroup",portgrouparg);
		
		// Set the parameter map
		actionTemplate.setParameters(parameters);
		
		// Create the result fields map
		Map resultFields = new Map();
		resultFields.add("task","");
		resultFields.add(RETURNRESULT, "");
		actionTemplate.setResultFields(resultFields);
		
		// Create the response map
		Map responses = new Map();
		responses.add("success",String.valueOf(UpdateVMHardware.SUCCESS));
		responses.add("failure",String.valueOf(UpdateVMHardware.FAILURE));
		
		// Set the response map
		actionTemplate.setResponses(responses);
		
		return actionTemplate;
	}

	public static Task updateVMHardware(String server, String username,
			String password, String name, String cpu, String mem, String disks, String portgroup) throws Exception {
		// Set the parameters
		int numcpu = -1;
		long memorymb = -1;
		if ((cpu!=null) & (!"".equals(cpu))) { numcpu = Integer.parseInt(cpu); }
		if ((mem!=null) & (!"".equals(mem))) { memorymb = (long) (Integer.parseInt(mem) * 1024); }
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
		// Get VM Config
		VirtualMachineConfigInfo config = vm.getConfig();
		// Get the hardware description
		VirtualHardware hardware = config.hardware;
		//list disk & nic devices
		ArrayList<VirtualDisk> virtualDisks = new ArrayList<VirtualDisk>();
		VirtualEthernetCard virtualEthernetCard = null;
		VirtualSCSIController virtualSCSIController = null;
		for (VirtualDevice device :  hardware.device) {
			if (device instanceof VirtualDisk) {
				// add it to the disk array
				virtualDisks.add((VirtualDisk)device);
			} else if (device instanceof VirtualEthernetCard & virtualEthernetCard==null) {
				// add it to the disk array
				virtualEthernetCard = (VirtualEthernetCard)device;
			} else if (device instanceof VirtualSCSIController) {
				// add it to the disk controler array
				virtualSCSIController = (VirtualSCSIController)device;
			}
		}
		if (virtualSCSIController == null) {
			throw new Exception("No existing virtual disk found.");
		}
		if (virtualDisks.size() == 0) {
			throw new Exception("No existing virtual disk found.");
		}
		if (virtualEthernetCard == null) {
			throw new Exception("No existing NIC found.");
		}
		//Change witness
		Boolean changed = false;
		//Create new Config Spec
		VirtualMachineConfigSpec vmConfSpec = new VirtualMachineConfigSpec();
		//Check CPU count
		if ((hardware.numCPU != numcpu) & (numcpu != -1)) {
			vmConfSpec.numCPUs = numcpu;
			changed = true;
		}
		//Check Memory Size
		if ((hardware.memoryMB != memorymb) & (memorymb != -1)) {
			vmConfSpec.memoryMB = memorymb;
			changed = true;
		}
		ArrayList<VirtualDeviceConfigSpec> devConfSpecs = new ArrayList<VirtualDeviceConfigSpec>();
		// Update the portgroup if needed
		if (portgroup!=null) {
			Network pg = (Network) navigator.searchManagedEntity("Network", portgroup);
			if (pg==null) { throw new Exception("Provided port group not found"); }
			VirtualDeviceConfigSpec devConfSpec = new VirtualDeviceConfigSpec();
			devConfSpec.operation  = VirtualDeviceConfigSpecOperation.edit;
			virtualEthernetCard.getConnectable().setStartConnected(true);
			if (pg instanceof DistributedVirtualPortgroup) {
				//distributed network
				DistributedVirtualPortgroup dpg = (DistributedVirtualPortgroup) pg;
				VirtualEthernetCardDistributedVirtualPortBackingInfo netbacking = new VirtualEthernetCardDistributedVirtualPortBackingInfo();
				netbacking.port = new DistributedVirtualSwitchPortConnection();
				netbacking.port.portgroupKey = dpg.getKey();
				DistributedVirtualSwitch dvs = (DistributedVirtualSwitch) MorUtil.createExactManagedObject(si.getServerConnection(),dpg.getConfig().getDistributedVirtualSwitch());
				if (dvs == null) {
					throw new Exception("Distributed virtual switch not found.");
				}
				if ("".equals(dvs.getUuid())) {
					throw new Exception("Distributed virtual Uuid empty.");
				}
				netbacking.port.switchUuid = dvs.getUuid();
				virtualEthernetCard.backing = netbacking;
				devConfSpec.device = virtualEthernetCard;
				devConfSpecs.add(devConfSpec);
			} else {
				//generic network
				VirtualEthernetCardNetworkBackingInfo netbacking = new VirtualEthernetCardNetworkBackingInfo();
				netbacking.network = pg.getMOR();
				netbacking.deviceName = portgroup;
				virtualEthernetCard.backing = netbacking;
				devConfSpec.device = virtualEthernetCard;
				devConfSpecs.add(devConfSpec);
			}
			changed = true;
		} 
		//get the last disk file index and default datastore
		Datastore ds = vm.getDatastores()[0];
		String vmdkFileNameTemplate = config.files.vmPathName.replaceAll("\\.vmx","_");
		VirtualMachineFileLayoutExFileInfo[] allFiles = vm.getLayoutEx().getFile();
		ArrayList<String> vmdkFiles = new ArrayList<String>();
		for (VirtualMachineFileLayoutExFileInfo info: allFiles) {
			//get disk descriptors
			if (info.getType().equals("diskDescriptor")) {
				//on default datastore
				if (info.name.startsWith(vmdkFileNameTemplate)) {
					//System.out.println("adding: " + info.name);
					vmdkFiles.add(info.name);
				}
			}
		}
		Collections.sort(vmdkFiles);
		/*for(String tmp: vmdkFiles) {
			System.out.println("sorted: " + tmp);
		}*/
		int vmdkIndex = 0;
		if (vmdkFiles.size() > 0) {
			String lastFile = vmdkFiles.get(vmdkFiles.size()-1);
			vmdkIndex = Integer.valueOf(lastFile.replace(vmdkFileNameTemplate, "").replace(".vmdk", ""));
		}
		//System.out.println("Last disk index:" + vmdkIndex);
		// Parse disk changes
		String[] diskSizes = disks.split(",");
		for (int i = 0; i < diskSizes.length; i++) {
			if ("0".equals(diskSizes[i])) {
				//skipping 0 sized changes
				continue;
			}
			//check size of existing disks
			if (i < virtualDisks.size()) {
				//existing disk
				//get the disk
				VirtualDisk disk = virtualDisks.get(i);
				int newSizeKb = Integer.parseInt(diskSizes[i]) * 1024 * 1024;
				if (newSizeKb <= disk.capacityInKB) {
					//requested size inferior to current size
					continue;
				}
				VirtualDeviceConfigSpec devConfSpec = new VirtualDeviceConfigSpec();
				devConfSpec.operation = VirtualDeviceConfigSpecOperation.edit;
				disk.capacityInKB = newSizeKb;
				devConfSpec.device = disk;
				devConfSpecs.add(devConfSpec);
				changed = true;
			} else {
				//new disk
				int newSizeKb = Integer.parseInt(diskSizes[i]) * 1024 * 1024;
				//Set the config specs
				VirtualDeviceConfigSpec devConfSpec = new VirtualDeviceConfigSpec();
				devConfSpec.operation = VirtualDeviceConfigSpecOperation.add;
				devConfSpec.fileOperation = VirtualDeviceConfigSpecFileOperation.create;
				//base the new disk on the first disk
				VirtualDisk firstDisk = virtualDisks.get(0);
				VirtualDisk newDisk = new VirtualDisk();
				//set new size
				newDisk.capacityInKB = newSizeKb;
				VirtualDiskFlatVer2BackingInfo newDiskBacking = new VirtualDiskFlatVer2BackingInfo();
				if (firstDisk.backing instanceof VirtualDiskFlatVer2BackingInfo) {
					//set the file backing info
					VirtualDiskFlatVer2BackingInfo firstDiskBacking = (VirtualDiskFlatVer2BackingInfo)firstDisk.backing;
					newDiskBacking.thinProvisioned = firstDiskBacking.thinProvisioned;
				}
				//set the disk mode
				newDiskBacking.diskMode = "persistent";
				//set the datastore
				newDiskBacking.datastore = ds.getMOR();
				//set the filename
				newDiskBacking.fileName = vmdkFileNameTemplate + ++vmdkIndex + ".vmdk";
				newDisk.backing = newDiskBacking;
				//set the controller info
				newDisk.controllerKey = virtualSCSIController.key;
				//set the unit number
				newDisk.unitNumber = virtualDisks.size();
				devConfSpec.device = newDisk;
				devConfSpecs.add(devConfSpec);
				changed = true;
			}
		}
	    vmConfSpec.deviceChange = devConfSpecs.toArray(new VirtualDeviceConfigSpec[0]);
	    Task task = null;
		if (changed) {
			task = vm.reconfigVM_Task(vmConfSpec);
		}
		si.getServerConnection().logout();
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
		String cpu = null;
		String memory = null;
		String disks = null;
		String portgroup = null;
		
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
			System.out.print("cpu:");
			cpu = br.readLine();
			System.out.print("memory:");
			memory = br.readLine();
			System.out.print("disks:");
			disks = br.readLine();
			System.out.print("port group:");
			portgroup = br.readLine();
			System.out.println("------------------------------");
			System.out.println("This will modify a VM:");
			System.out.println("vCenter: " + username + "@" + server);
			System.out.println("VM name: " + name);
			System.out.println("CPU: " + cpu);
			System.out.println("Memory: " + memory);
			System.out.println("Disks: "  + disks);
			System.out.println("Port Group: " + portgroup);
			System.out.println("------------------------------");
			System.out.println("Confirm (y/n):");
			String confirm = br.readLine();
			if (confirm.equals("y")) {
				Task task = UpdateVMHardware.updateVMHardware(server, username, password, name, cpu, memory, disks, portgroup);
				System.out.println("Task: " + task.getMOR().val);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}
}