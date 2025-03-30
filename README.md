# CloudSim Assignment

This repository stores the Java files for a given CloudSim assignment. CLOUDs LABS, the University of Melbourne provide the assignment.

### Steps to follow

*You should install the latest Java and Maven to run the code. Recommended IDE - `IntelliJ IDEA`*

1. Clone the [CloudSim v7 project](https://github.com/Cloudslab/cloudsim) to your local directory
2. Explore the folder structure and create a new package named `project` at `cloudsim/modules/cloudsim-examples/src/main/java/org/cloudbus/cloudsim`
3. Copy and Paste the files in this [repo](https://github.com/AvishkaSandeepa/CloudSimProject/tree/master/cloudsim/java-code) inside the created package.
4. Run the Application files with the main method. Java files are divided as question numbers (InitialDatacenterCreationApplication.java, DynamicVMProvisionApplication.java and RoundRobinApplication.java)
5. The sample project structure can be found :

<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/java-code/project-structure.png" alt="Sample project structure" style="width:400px;"/>



### +++++++++++++++++++++ Result Comparison +++++++++++++++++++++

To understand VM usage, I present graphs of cost and execution time per VM instance. (*consider all running VMs for these graphs*). I used google colab to run my [Python notebook file](https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/cloudsim_histogram.ipynb) to get the following results.



**SPECIAL NOTICE !** -----> *Log scale is used for the y-axis of the first two graphs given below because the variation of the y-axis cannot be shown on a single graph using a normal scale*

`Graphs`

### Deadline Aware Provisioning (DAP)

Execution Time vs VM Instance - Deadline Aware Provisioning (DAP)


<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/Images/Dynamic-Time-vs-VMs.png" alt="Execution Time vs VM Instance" style="width:2000px;"/>


Cost vs VM Instance - Deadline Aware Provisioning (DAP)


<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/Images/Dynamic-Cost-vs-VMs.png" alt="Cost vs VM Instance" style="width:2000px;"/>


*---------------------------------------------------------------------------------------------------------------------------------------------------------*

Usage of each commercially available VM Instances (Time usage and Cost) - Deadline Aware Provisioning (DAP)

<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/Images/Dynamic-TotalTimeAndCost-vs-UsedInstance.png" alt="Usage of each commercially available VM Instances" style="width:2000px;"/>




### Fixed Round Robin (FRR)

Execution Time vs VM Instance - Fixed Round Robin (FRR)


<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/Images/Fixed-Time-vs-VMs.png" alt="Execution Time vs VM Instance" style="width:2000px;"/>


Cost vs VM Instance - Fixed Round Robin (FRR)


<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/Images/Fixed-Cost-vs-VMs.png" alt="Cost vs VM Instance" style="width:2000px;"/>


*---------------------------------------------------------------------------------------------------------------------------------------------------------*

Usage of each commercially available VM Instances (Time usage and Cost) - Fixed Round Robin (FRR)

<img src="https://github.com/AvishkaSandeepa/CloudSimProject/blob/master/cloudsim/python-code/Images/Fixed-TotalTimeAndCost-vs-UsedInstance.png" alt="Usage of each commercially available VM Instances" style="width:2000px;"/>






























# Job scheduling Assignment

This repository stores the Java files for a given job scheduling assignment. CLOUDs LABS, the University of Melbourne provide the assignment.

### Steps to follow

*You should install the latest Java and Maven to run the code. Recommended IDE - `IntelliJ IDEA`*

1. Clone the project files and navigate to [this directory](https://github.com/AvishkaSandeepa/CloudSimProject/tree/master/job-management/job-management-system/src/main/java/com/jobmanagement)
2. There are three serverside applications can be found (MasterServer.java, MasterCLI.java, and WorkerServer.java)
3. Firstly run the MasterServer.java using `java MasterServer` command. The master server will be started at port 8080
4. You can start as many workers who communicate with the master server on different ports.
5. You should pass the command line arguments to start the workers `java WorkerServer <port> <master password> <initial budget>`
6. If the worker password matches the master password, worker will successfully register under master and can be executed assign jobs through MasterCLI
7. Master CLI has several commands


  _submit-job <command> <deadline> <budget>  - Submit a new job. Deadline should be in HH:mm:ss format_
  
 _submit-job-file <address of a job file>_
  
  _done - Start scheduling and execution of initially submitted jobs_
  
  _job-status <jobId>    - Check job status_
  
 _cancel-job <jobId>    - Cancel a running job_
  
  _list-workers      - List all registered workers_
  
  _exit              - Exit the CLI_


  ### To start the jobs, you should always enter `done` command after submitting jobs (by command line or by text file)


  

