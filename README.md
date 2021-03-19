# EPOS-Fog
Epos-Fog-Cloud1001 program introduces a distributed multi-agent IoT service placement approach called EPOS Fog. 
This program experiments the placement of IoT services in EPOS Fog and evaluates the results in comparison
with Cloud and First Fit [1,2] approaches.
EPOS Fog placement system optimizes the distribution of IoT services over a network of edge to cloud nodes considering 
a global (workload balance) and a local (minimization of service execution cost and maximizing QoS) objective. 
Further details are available in our proposed paper at the following address: https://arxiv.org/abs/2005.00270

EPOS Fog employs the I-EPOS system (Available at: http://epos-net.org, https://github.com/epournaras/epos) as a
privacy-preserving learning mechanism for coordinating the planning of IoT requests.
As the input workload, the Google cluster trace (Available at: https://commondatastorage.googleapis.com/clusterdata-2011-2) is selected. 
Program IoTTasks extracts the required data from Google cluster trace and enhances it with more features. The prepared dataset is located in Epos-Fog-Cloud1001\indataset\tasks path.
As the network topology, three graph models are considered consist of scale-free (Barabasi-Albert), small-world (Watts–Strogatz), and random (Erdos-Renyi) networks.
Graph modeling and analysis are performed using a Java library, i.e., Graph-Stream (Available at: http://graphstream-project.org).

To run the project:
1. Command line: java -jar EposFogCloud1001.jar "path to the source directory" 
2. Gui (eclipse or netbeans): modify the path to the program directory and then run main class.
3. One bash script is provided in the subdirectory "ClusterRunScript" to guide how to run the program in a computer cluster. 
 
 Please note that, in order to provide a comprehensive evaluation, the program simulates 4950 (5*3*5*2*3*11) configurations for time-sensitive IoT tasks 
 (in which deadline ranges from 2ms to 120s) as follows:
 1. Networks with different sizes N, where N belongs to the set {200, 400, 600, 800, 1000} and various topologies as mentioned above. 
 2. Five profiles from Google cluster trace are selected as the input workload. Each profile is defined with four values that consist of: CPU load,
    memory load, storage load, and the number of requests per 5-min time period.
 3. IoT workload distribution employs two methods including Beta and Random distribution.
 4. Host proximity parameter investigates the impact of different distances between sources (i.e., service requesters)
	  and corresponding destinations (i.e., hosts) in terms of hop count include 1-hop (direct neighbors), 3-hop, and infinity-hop (unlimited). 
 4. Agent preference which examines the impact of different values in the interval [0,1] on the global and local cost reduction.
 The program can be modified to run a few of configurations by changing the value of above parameters.
 
 Considering various configuration parameters the overal procedure of EposFogCloud is as below:
  1. Generate infrastructure including several networks with heterogeneous edge-to-cloud nodes
  2. Read input workload (IoT services/tasks)
  3. Distribute input workload over edge nodes
  4. Run EPOS Fog approach:
       generate possible plans for each node 
       call I-EPOS
       apply selected plans 
       execute tasks
  5. Run First Fit approach
  6. Run Cloud approach
  7. Record results for evaluation 
 
An open dataset for the community containing service assignment plans of EPOS Fog agents is available at https://figshare.com/articles/Agentbased_Planning_Portfolio/7806548. 
By simulating, the program generate those plans and put them in \datasets\Utilization subdirectory. 
The output files of I-EPOS are included in output subdirectory.
The final output files which measure the performance of evaluated approaches in the form of excel files are available at https://github.com/epournaras/EPOS-Fog-Research in ExperimentResults directory.

[1] O. Skarlat, M. Nardelli, S. Schulte, M. Borkowski, and P. Leitner, “Optimized iot service placement in the fog,” Service Oriented Computing and
Applications, vol. 11, no. 4, pp. 427–443, 2017.

[2] R. P. Brent, “Efficient implementation of the first-fit strategy for dynamic storage allocation,” ACM Transactions on Programming Languages and
Systems (TOPLAS), vol. 11, no. 3, pp. 388–403, 1989.
