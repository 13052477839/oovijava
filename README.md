# HP OpenView Orchestrator - ViJava Actions 

Hp Openview Actions based on the ViJava library

Please place the HP OO SDK libraries inside the `./lib` folder:
  - JRAS-sdk.jar
  - ContentCommons.jar

# Using

## Build

to build

> gradle build

to cleanup

> gradle clean

to run a test with one of the classes

> gradle -Pmain_class=UpdateVMHardware exec -q

## Generated files

Library

> ./build/libs/oovijava-1.1.jar

Zip containing libraries and dependencies

> ./build/distributions/oovijava-1.1.zip

# Actions

## CreateResourcePool

Creates a ressource pool.

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - Cluster: cluster to contain the resourcepool
  - name: resourcepool name

Responses:
  - success: the ressource pool is created or exists
  - failure: the ressource pool could not be created

## DeployCustomVM

Deploys a Virtual Machine with Customization Specs.

Requirement:
The customization template used should only require an ip address

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - Name: name of the VM
  - Template: VM template to use
  - Cluster: cluster where to deploy the VM
  - Datasstore: datastore to use
  - Provisioning: provisionning type (thin/thick: default thick)
  - ResourcePool: ressource pool where the VM will be placed (default: resource)
  - Folder: folder where to place the VM (default: /)
  - CustomizationTemplate: customization tempalte (sysprep) to use
  - IPAddress: IP Address of the VM"

Responses:
  - success: the VM was deployed
  - failure: an error occured while deployin the VM

## GetCustomizationSpecs

Get Customization Specs

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password

Responses:
  - success: the VM has been adapted
  - failure: the VM failed to be adapted

## GetGuestTools

Get Guest Tools informations (non nested info)

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - name: virtual machine name

Responses:
  - success: the VM has been adapted
  - failure: the VM failed to be adapted

## GetVirtualDisks

Get the virtual machine disks.

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - name: virtual machine name

Responses:
  - success: disks returned
  - failure: an error occured

## SearchManagedEntities

Get Management Objects.

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - columnSeparator: column separator
  - rowSeparator: raw separator 
  - type: Management Object Type
  - properties: properties to retrieve
  - rootType: Root Management Object Type
  - rootMOR: Reference to the root Object
  - recurse: recurse in inventory
  - nameMatch: case insensitive contains on name

Responses:
  - success: disks returned
  - failure: an error occured

## SetPowerState

Set the powerState of a virtual machine.

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - name: virtual machine name
  - power operation: poweron, poweroff, suspend

Responses:
  - success: the VM has been adapted
  - failure: the VM failed to be adapted

## ShutdownVMGuest

Shut down VM Guest

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - name: virtual machine name

Responses:
  - success: the VM has been adapted
  - failure: the VM failed to be adapted

## UpdateVMHardware

Udate a VM with specified hardware specifications.
If disks are not present they will be ignored.
If new disk sizes are inferior to current size they will be ignored.

Requirements
 - For CPU & Memory upgrade, if hotadd is not activated, the VM needs to be shutted down.

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - CPU: number of CPU
  - Memory: quantity of Memory (GB)
  - DiskSizes:  a comma separated list of disk sizes.
    * 0: Will skip a disk
    * Sizes inferior to current size will be ignored
  - portgroup

Responses:
  - success: the VM has been adapted
  - failure: the VM failed to be adapted

## UpgradeVMHardware

Upgrade the hardware of a virtual machine

Inputs:
  - vCenter: vCenter server
  - Username: vCenter username
  - Password: vCenter password
  - name: virtual machine name

Responses:
  - success: the VM has been adapted
  - failure: the VM failed to be adapted

# License

See [License](./LICENSE.txt)