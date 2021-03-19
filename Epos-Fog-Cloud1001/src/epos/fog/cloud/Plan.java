/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.algorithm.AStar;



/**
 *
 * @author zeinab
 * 1. plan generation:
 *      input : list of tasks at a specified period
 *      output: possible plans
 * 2. apply and execute selected plans:
 *      resource allocation
 *      task execution
 *      resource release
 * 
 */
public class Plan {
    
	static int maxid;
    final double alpha = 0.95;//maximum percentage of resource capacity that can be allocated to iot request in each node.
    int numOfNodes;
    int numOfPlans; 
    static int periodDu;
    static double base;
    static int cloudId;
    static AStar astar1;
    static String ppath ;
        
    Plan (int numNodes, int cloudIndex, int numPlans, int periodL, double baseT, AStar aS, String path){
               
        astar1 = aS;//to measure the distance between resource nodes (IoT end-devices) and destination nodes (hosts)
        numOfNodes = numNodes;
        maxid = numNodes-1;
        numOfPlans = numPlans;
        periodDu = periodL;
        base = baseT;
        cloudId = cloudIndex;
        ppath = path;
        
    }
    
    //local plan generation:
    public EPOSplan[][] generatePlans(int pn, int hopLevel, Graph NetInfrustructure, Map<Integer, Map<Long,Task>> TaskList, double confnum){
        //Random r = new Random(System.nanoTime());
        System.out.println("generating plans............");
        Map<Integer, Map<Long,Task>> taskList = new HashMap<Integer,Map<Long,Task>>();
        taskList.putAll(TaskList);  
        int numOfTask;  
        double pathLength; 
        int host,i;
        double[] exeTimeForNodes = new double[numOfNodes];
        double[] nodcap = new double[3];                  //(CPU,Mem,Storage)
        double[] preAssWL = new double[3];
        double[] pre_nodcap = new double[3];
        Task t,t1,t2;
        double[] newWL = new double[3];
        int[] selectedHosts;
        
        //since the possible plans generated in this step are just as candidate plans they must not affect network graph (resource allocation)
        //hence, we create a temp graph to do the generation and evaluate local-cost and other metrics based on it.
        Graph tempGraph = Graphs.clone(NetInfrustructure);
        for(int k=0 ; k<numOfNodes ; k++){
            nodcap = new double[3]; double[] tem = new double[3];
            nodcap = tempGraph.getNode(k).getAttribute("capacity");
            preAssWL = tempGraph.getNode(k).getAttribute("assignedWorkLoad");
            //measure the remaining capacity for each node
            tem[0]= nodcap[0]-  preAssWL[0];
            tem[1]= nodcap[1]-  preAssWL[1];
            tem[2]= nodcap[2]-  preAssWL[2];
            preAssWL[0]=0.0;preAssWL[1]=0.0;preAssWL[2]=0.0;
            tempGraph.getNode(k).setAttribute("capacity", tem);
            tempGraph.getNode(k).setAttribute("assignedWorkLoad",preAssWL);
            
        }
        //each node is supposed to have numOfPlans number of possible plans:
        EPOSplan [][] NodePlans = new EPOSplan [numOfNodes][numOfPlans]; //all generated plans for all nodes
        //some nodes might not receive any requests. as a result they don't generate any possible plans. 
        //Therefore, at the end of plan generation we assume they have plans with zero values for all variable.
        int [] nodeWithPlan = new int [numOfNodes];

        for(int fkey : taskList.keySet()){ //generate plans for each node
            nodeWithPlan[fkey] = 1;
            //since each node generates more than one possible plans, task list must remain un-modified for the generation of each of them.
            Map<Long,Task> unsortedTasks = new HashMap<Long,Task>();
            ArrayList<Task> sortedTasks = new ArrayList<Task>();
            for (long ikey : taskList.get(fkey).keySet()){
                try
                {
                    t1 = (Task)taskList.get(fkey).get(ikey).clone();
                    unsortedTasks.put(ikey,t1);
                }
                catch(CloneNotSupportedException c){}  
            }
            sortedTasks = orderTasks(unsortedTasks);    //sort all of the received tasks for each node
            //generate one plan:
            for(int pNum = 0 ; pNum<numOfPlans ; pNum++){                 
                    int dViol = 0;
                    int unassTasks = 0;
                    numOfTask = taskList.get(fkey).size();
                    Graph genGraph = Graphs.clone(tempGraph);
                    EPOSplan p = new EPOSplan(fkey,pNum,numOfNodes); //each plan consists of: local cost,binary plan,utilization plan
                    p.localCost = 0.0;
                    ArrayList<Task> sortedTasks1 = new ArrayList<Task>();
                    for (int mn=0;mn<sortedTasks.size();mn++){
                        try
                        {
                            t2 = (Task)sortedTasks.get(mn).clone();
                            sortedTasks1.add(t2);
                        }
                        catch(CloneNotSupportedException c){}  
                    }
                    //select randomly one (candidate) host (from network nodes) for each of the received tasks:
                    selectedHosts = hostSelection(numOfTask, fkey, hopLevel); 
                    //check if the selected hosts satisfy capacity constraints:
                    for (i = 0 ; i<sortedTasks1.size() ; i++){
                        
                        t = sortedTasks1.get(i);
                        host = selectedHosts[i];           
                        nodcap = genGraph.getNode(host).getAttribute("capacity");
                        preAssWL = genGraph.getNode(host).getAttribute("assignedWorkLoad");
                        
                        preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                            if ((t.getDProc() + preAssWL[0] < alpha * nodcap[0]) && (t.getDMem() + preAssWL[1] < alpha * nodcap[1])&&(t.getDStorage() + preAssWL[2] < alpha * nodcap[2])){
                                        astar1.compute(String.valueOf(fkey),String.valueOf(host));//calculate the distance of the host from this node
                                        pathLength = astar1.getShortestPath().getEdgeCount();  //getDistanceAstar(fkey, host);
                                        //if (Double.compare(pathLength,0.0) != 0) //this node itself can be host node
                                        p.localCost += pathLength;//length is considered as a parameter of local-cost
                                        //deployedService[(t.getType())] = 1;//if type of task does matter this parameter should be checked;+1 or 1
                                        //genGraph.getNode(host).setAttribute("deployedServices", deployedService);
                                        //CPU update:
                                        p.utilPlan[host] = Math.round(((t.getDProc()+preAssWL[0])/nodcap[0])* 10000.0) / 10000.0;//update cpu utilization for the selected host
                                        preAssWL[0] += Math.round(t.getDProc()* 10000.0) / 10000.0;//update cpu assigned workload to the host
                                        p.wlPlan[host] += (t.getDProc());//update cpu workload assigned to the host
                                        //Memory update:
                                        p.utilPlan[host+numOfNodes] = Math.round(((t.getDMem()+preAssWL[1])/nodcap[1])* 10000.0) / 10000.0;//update mem capacity for selected node
                                        preAssWL[1] += Math.round(t.getDMem()* 10000.0) / 10000.0;
                                        p.wlPlan[host+numOfNodes] += (t.getDMem());
                                        //Storage update: since as two parameter of workload only cpu and memory are considered, storage utilization does not measure.
                                        preAssWL[2] += Math.round(t.getDStorage()* 10000.0) / 10000.0;
                                      
                                        genGraph.getNode(host).setAttribute("assignedWorkLoad", preAssWL);
                                        t.sethostNode(host);//set the selcted host as host node for the task
                                        BinaryPlan biPlan = new BinaryPlan(t, host, 1);//update binary plan
                                        p.biPlans.add(biPlan);
                                        p.assTasks++;//add 1 to number of hosted tasks
                                        
                                        if (host == cloudId)//if the host node is cloud node update number of assigned tasks to the cloud node
                                            p.assToCloud++;
                                        }//end capacity satisfied
                                        else{
                                            
                                            if (host != cloudId){//if cloud node is not examined at the above (candidate host != cloud)
                                                preAssWL = genGraph.getNode(cloudId).getAttribute("assignedWorkLoad");
                                                preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                                                nodcap  = genGraph.getNode(cloudId).getAttribute("capacity");
                                                host = cloudId;
                                                //if capacity constraint are satisfied with cloud:
                                                if ((t.getDProc() + preAssWL[0] < nodcap[0]) && (t.getDMem() + preAssWL[1] < nodcap[1]) && (t.getDStorage() + preAssWL[2] < nodcap[2])){
                                                    //assign the task to the cloud:
                                                    astar1.compute(String.valueOf(fkey),String.valueOf(host));
                                                    pathLength = astar1.getShortestPath().getEdgeCount(); 
                                                    p.localCost += pathLength;//* (tasksPerFog.get(tkey).getDStorage());//must be checked: does this the service storage really?
                                                    //CPU update:
                                                    p.utilPlan[host] = Math.round(((t.getDProc()+preAssWL[0])/nodcap[0])* 10000.0) / 10000.0;//update cpu capacity for selected node
                                                    preAssWL[0] += Math.round(t.getDProc()* 10000.0) / 10000.0;
                                                    p.wlPlan[host] += (t.getDProc());
                                                    //Memory update:
                                                    p.utilPlan[host+numOfNodes] = Math.round(((t.getDMem()+preAssWL[1])/nodcap[1])* 10000.0) / 10000.0;//update mem capacity for selected node
                                                    preAssWL[1] += Math.round(t.getDMem()* 10000.0) / 10000.0;
                                                    p.wlPlan[host+numOfNodes] += (t.getDMem());
                                                    //Storage update:
                                                    preAssWL[2] += Math.round(t.getDStorage()* 10000.0) / 10000.0;
              
                                                    genGraph.getNode(host).setAttribute("assignedWorkLoad", preAssWL);
                                        
                                                    t.sethostNode(host);
                                                    BinaryPlan biPlan = new BinaryPlan(t, host, 1);
                                                    p.biPlans.add(biPlan);
                                                    p.assTasks++;
                                                    p.assToCloud++;
                                                    
                                                }
                                                else{//if capacity constraint on the candidate host and cloud node did not satisfied:
                                                    unassTasks++;   //add 1 to unassigned tasks for local-cost  
                                                    t.sethostNode(-1);//set host node to -1
                                                    BinaryPlan biPlan = new BinaryPlan(t, -1, 0);
                                                    p.biPlans.add(biPlan);
                                                    
                                                }
                                                    
                                            }
                                            else
                                            {//if host node is examined in the previous if clause and not satisfied the capacity constraints:
                                                unassTasks++;   //set zero for unassigned tasks  
                                                t.sethostNode(-1);
                                                BinaryPlan biPlan = new BinaryPlan(t, -1, 0);
                                                p.biPlans.add(biPlan);

                                            }
                            }
                        
                    }//examine the assignment of next task
                    
                //compute deadline-violation cost:
                    //compute exe time for each node regarding to queue theory
                    for(i = 0 ; i<p.wlPlan.length/2 ; i++){
                        pre_nodcap = NetInfrustructure.getNode(i).getAttribute("capacity");
                        preAssWL = NetInfrustructure.getNode(i).getAttribute("assignedWorkLoad");
                        newWL= genGraph.getNode(i).getAttribute("assignedWorkLoad");
                        exeTimeForNodes[i] = 1.0/(pre_nodcap[0]-(preAssWL[0]+newWL[0]));//considers main capacity and its assigned load form previous and now
                        
                    }
                    //compute response time and deadline violation:
                    for (i = 0; i<p.biPlans.size(); i++){
                        t = p.biPlans.get(i).T;
                        host = p.biPlans.get(i).hostNodeId;
                        if (host != -1){    //if the task is going to be hosted on any machine
                            astar1.compute(String.valueOf(fkey),String.valueOf(host));
                            double le = astar1.getShortestPath().getEdgeCount(); //distance to time; propagation delay for each link = 0.3s
                            double dl = t.gettaskDL();
                            if (exeTimeForNodes[host]*1000 + (t.waitingTime) + 2*300*(le) > dl)//0.3s == 300ms 
                            {
                                dViol += 1;
                            }
                        }
                        else{}
                    }
                    //assign parameters for the generated possible plan:
                    p.node = fkey;
                    p.planIndex = pNum;
                    p.unassTasks = unassTasks;
                    p.dlViol = dViol;
                    p.localCost += dViol;
                    p.localCost += unassTasks;
                    p.valid = true;
                    //check if there is any node without any assigned task and then consider previous utilization for it:
                    for(int k=0 ; k<numOfNodes ; k++){
                        if ((p.utilPlan[k]==0.0)&&(p.utilPlan[numOfNodes+k]==0.0)){  
                            nodcap = new double[3]; 
                            preAssWL = new double[3]; 
                            nodcap = genGraph.getNode(k).getAttribute("capacity");
                            preAssWL = genGraph.getNode(k).getAttribute("assignedWorkLoad");
                            preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                            p.utilPlan[k] = Math.round((preAssWL[0]/nodcap[0])* 10000.0) / 10000.0;
                            p.utilPlan[numOfNodes+k] = Math.round((preAssWL[1]/nodcap[1])* 10000.0) / 10000.0;
                        }
                    }
                    
                    NodePlans[fkey][pNum]=p;
                   
                }//end of plan generation for one node
           
        }//end of plan generation for all nodes
            //check if there is any node without any possible plan and then produce plans with zero values for it:
                for (i=0 ; i<numOfNodes ; i++)
                if (nodeWithPlan[i] != 1){
                    for(int j=0 ; j<numOfPlans ; j++){    
                        EPOSplan ep = new EPOSplan(i,j,numOfNodes);
                        ep.unassTasks = 0;
                        ep.dlViol = 0;
                        ep.localCost = 0.0;
                        ep.assToCloud = 0;
                        ep.assTasks = 0;
                        for(int k=0 ; k<numOfNodes ; k++){
                            nodcap = new double[3]; 
                            preAssWL = new double[3]; 
                            nodcap = tempGraph.getNode(k).getAttribute("capacity");
                            preAssWL = tempGraph.getNode(k).getAttribute("assignedWorkLoad");
                            preAssWL[0] = Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                            ep.utilPlan[k] = Math.round((preAssWL[0]/nodcap[0])* 10000.0) / 10000.0;
                            ep.utilPlan[numOfNodes+k] = Math.round((preAssWL[1]/nodcap[1])* 10000.0) / 10000.0;
                            
                        }
                    NodePlans[i][j]=ep;
                    }
                }
           
        desSort(NodePlans);//descending sort of generated plans
        boolean append_value = false;
        writeToFileUtilization(append_value, numOfNodes, numOfPlans, NodePlans, confnum);//writing generated plans (utilization values) to feed I-EPOS
        System.out.println("End of plan generation...........");
        return NodePlans;
    }
 
    //applying selected plans, allocating resources and executing the tasks:
    //I-EPOS plan indexes coming form input; one plan (index) per node. Based on that we can identify the selected plan and apply it to the network nodes
    public EPOSplan applyEpos(Graph g1, int gType, int co, int pe, int cLevel, ArrayList<Integer> selectedPIndex, EPOSplan[][] plans, ArrayList<Task> hostedTasks, ArrayList<Task> unhostedTasks){//gets binary plans and workload plans and then updates the nodes' capacity and deployed services 
        
        System.out.println("applying generated plans: assigning tasks to resources....");
        int[] deployedTasksPerPeriod = new int[numOfNodes];
        Task t= new Task();
        int i, j, k, host; k= 0;
        double alpha = 0.95;
        double[] assWL1 = new double[]{0,0,0};
        double[] exeTimeForNodes = new double[numOfNodes];
        int nodeIndex;
        double[] nodcap = new double[3];
        double[] nodcap1 = new double[3];
        EPOSplan totalResult = new EPOSplan(0,0,numOfNodes);
        EPOSplan selectedPlan = new EPOSplan(0,0,numOfNodes);
        ArrayList<Task> newHostedTasks = new ArrayList<Task>();
        double[] preAssWL = new double[]{0,0,0};
        //all nodes have to apply their selected plans:
        for (nodeIndex = 0; nodeIndex<numOfNodes; nodeIndex++){
            ArrayList<BinaryPlan> biplan = new ArrayList<BinaryPlan>(); 
            selectedPlan = plans[nodeIndex][selectedPIndex.get(nodeIndex)];
            biplan.addAll(selectedPlan.biPlans);
            //one node applies its selected plan;
            for (i = 0 ; i<biplan.size() ; i++){  
                try
                {
                    t = (Task)biplan.get(i).T.clone();
                }
                catch(CloneNotSupportedException c){}  

               
                host = biplan.get(i).hostNodeId;
                if(biplan.get(i).value == 1){ //if task will be running on some nodes then update its host capacity
                    nodcap  = g1.getNode(host).getAttribute("capacity");
                    preAssWL = g1.getNode(host).getAttribute("assignedWorkLoad");
                    preAssWL[0] = Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                    //since network nodes generate possible plans independently maybe capacity constraints are violated by applying the plans.
                    if ((t.getDProc() + preAssWL[0] < nodcap[0]) && (t.getDMem() + preAssWL[1] < nodcap[1]) && (t.getDStorage() + preAssWL[2] < nodcap[2])){
                        //CPU
                        totalResult.utilPlan[host] = (t.getDProc()+preAssWL[0])/(nodcap[0]);//update cpu capacity for selected node
                        preAssWL[0] += t.getDProc();
                        totalResult.wlPlan[host] += (t.getDProc());
                        //Memory
                        totalResult.utilPlan[host+numOfNodes] = (t.getDMem()+preAssWL[1])/(nodcap[1]);//update memory capacity for selected node
                        preAssWL[1] += t.getDMem();
                        totalResult.wlPlan[host+numOfNodes] += (t.getDMem());
                        //storage
                        preAssWL[2] += t.getDStorage();//update storage capacity for selected node
                        //to count number of running tasks on each host: runningTaskPerNode = g1.getNode(host).getAttribute("runningTask");
                        //runningTaskPerNode[serviceType] += 1;  //g1.getNode(host).setAttribute("runningTask", runningTaskPerNode);
                        g1.getNode(host).setAttribute("assignedWorkLoad", preAssWL);
                        deployedTasksPerPeriod[host] += 1;
                        t.sethostNode(host);
                        BinaryPlan biPlan = new BinaryPlan(t, host, 1);
                        
                        totalResult.biPlans.add(biPlan);
                        totalResult.assTasks++;
                        newHostedTasks.add(t);
                         k++;
                        if (host == cloudId){
                            totalResult.assToCloud++;
                            }
                        } 
                        else{//if the task is assigned to a host without enogh capacity
                            if (host != cloudId){//try to host this task to cloud node
                                preAssWL = g1.getNode(cloudId).getAttribute("assignedWorkLoad");
                                nodcap  = g1.getNode(cloudId).getAttribute("capacity");
                                preAssWL[0] = Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                        
                                if ((t.getDProc() + preAssWL[0] < nodcap[0]) && (t.getDMem() + preAssWL[1] < nodcap[1]) && (t.getDStorage() + preAssWL[2] < nodcap[2])){
                                    //CPU
                                    totalResult.utilPlan[cloudId] = (t.getDProc()+preAssWL[0])/(nodcap[0]);//update cpu capacity for selected node
                                    preAssWL[0] += t.getDProc();
                                    totalResult.wlPlan[cloudId] += (t.getDProc());
                                    //Memory
                                    totalResult.utilPlan[cloudId+numOfNodes] = (t.getDMem()+preAssWL[1])/(nodcap[1]);//update memory capacity for selected node
                                    preAssWL[1] += t.getDMem();
                                    totalResult.wlPlan[cloudId+numOfNodes] += (t.getDMem());
                                    //storage
                                    preAssWL[2] += t.getDStorage();
                                    //runningTaskPerNode = g1.getNode(host).getAttribute("runningTask");
                                    //runningTaskPerNode[serviceType] += 1;
                                    g1.getNode(cloudId).setAttribute("assignedWorkLoad", preAssWL);
                                    deployedTasksPerPeriod[host] += 1;
                                    totalResult.assTasks++;    
                                    totalResult.assToCloud++;
                                    t.sethostNode(cloudId);
                                    BinaryPlan biPlan = new BinaryPlan(t, cloudId, 1);
                                    totalResult.biPlans.add(biPlan);
                                    newHostedTasks.add(t);
                                }
                                else{
                                    totalResult.unassTasks++;   
                                    t.sethostNode(-1);
                                    BinaryPlan biPlan = new BinaryPlan(t, -1, 0);
                                    totalResult.biPlans.add(biPlan);
                                    unhostedTasks.add(t);
                              }
                            }
                            else{
                                totalResult.unassTasks++;   
                                t.sethostNode(-1);
                                BinaryPlan biPlan = new BinaryPlan(t, -1, 0);
                                totalResult.biPlans.add(biPlan);
                                unhostedTasks.add(t);
                              }
                        }

                }
                else{//task is unhosted yet because of value != 1; try to host on the cloud node
                    preAssWL = g1.getNode(cloudId).getAttribute("assignedWorkLoad");
                    nodcap  = g1.getNode(cloudId).getAttribute("capacity");
                    preAssWL[0] = Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                    
                    if ((t.getDProc() + preAssWL[0] < nodcap[0]) && (t.getDMem() + preAssWL[1] < nodcap[1]) && (t.getDStorage() + preAssWL[2] < nodcap[2])){
                        //cpu
                        totalResult.utilPlan[cloudId] = (t.getDProc()+preAssWL[0])/(nodcap[0]);
                        totalResult.wlPlan[cloudId] += (t.getDProc());
                        preAssWL[0] += t.getDProc();
                        //Memory
                        totalResult.utilPlan[cloudId+numOfNodes] = (t.getDMem()+preAssWL[1])/(nodcap[1]);//update mem capacity for selected node
                        totalResult.wlPlan[cloudId+numOfNodes] += (t.getDMem());
                        preAssWL[1] += t.getDMem();
                        //storage
                        preAssWL[2] += t.getDStorage();
                        g1.getNode(cloudId).setAttribute("assignedWorkLoad", preAssWL);
                        deployedTasksPerPeriod[cloudId] += 1;
                        totalResult.assTasks++;    
                        totalResult.assToCloud++;
                        t.sethostNode(cloudId);
                        BinaryPlan biPlan = new BinaryPlan(t, cloudId, 1);
                        totalResult.biPlans.add(biPlan);
                        newHostedTasks.add(t);
                    }
                    else{
                        totalResult.unassTasks++;    
                        t.sethostNode(-1);
                        BinaryPlan biPlan = new BinaryPlan(t, -1, 0);
                        totalResult.biPlans.add(biPlan);
                        unhostedTasks.add(t);
                    }
                }
              
            }//end of binary plan application for one node
        }//end of applying all plans to the network nodes
        
            //compute deadline-violation cost:
            for(i = 0; i<numOfNodes ; i++){ //compute exe time for each node regarding to queue theory
                nodcap1 = g1.getNode(i).getAttribute("capacity");  
                assWL1 = g1.getNode(i).getAttribute("assignedWorkLoad");
                assWL1[0] = Math.round(assWL1[0]* 10000.0) / 10000.0;assWL1[1]=Math.round(assWL1[1]* 10000.0) / 10000.0;assWL1[2]= Math.round(assWL1[2]* 10000.0) / 10000.0;
                totalResult.utilPlan[i] = Math.round((assWL1[0]/nodcap1[0])* 10000.0) / 10000.0;
                totalResult.utilPlan[numOfNodes+i] = Math.round((assWL1[1]/nodcap1[1])* 10000.0) / 10000.0;
                exeTimeForNodes[i] =  1.0/(nodcap1[0]-assWL1[0]);//consider total capacity, assigned load form previous periods, and current assigned load
                if ((totalResult.utilPlan[i]==0.0)&&(totalResult.utilPlan[numOfNodes+i]==0.0)){    
                }
            }
            
            double total_delay= 0.0;
            for (i=0 ; i<newHostedTasks.size() ; i++){
                t = newHostedTasks.get(i);
                host = t.gethostNode();
                astar1.compute(String.valueOf(t.getassignedNode()),String.valueOf(host));
                double le = astar1.getShortestPath().getEdgeCount();   //getDistanceAstar(fkey, host);
                double dl = t.gettaskDL();
                double st =exeTimeForNodes[host] + t.waitingTime + 2*0.3*(le/1000000);
                t.serviceTime = st;
                
                if (st > dl) {
                    totalResult.dlViol += 1;
                    t.dl_viol = 1;
                    total_delay += (st-dl);
                }
                t.service_delay = Math.abs(dl - st);
                hostedTasks.add(t);
            }
            
            total_delay/=newHostedTasks.size();//avg of service delay in milisecond
            totalResult.SD = total_delay;
            taskDeployedDist(deployedTasksPerPeriod, gType, numOfNodes, co, pe, cLevel);
        
        return totalResult;
    } 
    
    //check periodically if there is any task which complete its execution time. Release resources and update nodes' capacity.
    public void releaseResource(double nextPeriod, Graph graphRel, ArrayList<Task> hostedTasks, ArrayList<Task> fiTasks){//do we need to graph here??????
        int i,j;j=0;
        int host;
        double[] assWL = new double[3];
        double[] cap = new double[3];
        ArrayList<Task> remHostTasks = new ArrayList<Task>();
        System.out.println("releasing resources..... ");
        
        for(i=0; i<hostedTasks.size() ; i++) {//check all of the hosted and running tasks 
            Task t = hostedTasks.get(i);
            host = t.gethostNode();
            assWL = graphRel.getNode(host).getAttribute("assignedWorkLoad");
            cap = graphRel.getNode(host).getAttribute("capacity");
            assWL[0] = Math.round(assWL[0]* 10000.0) / 10000.0;assWL[1]=Math.round(assWL[1]* 10000.0) / 10000.0;assWL[2]= Math.round(assWL[2]* 10000.0) / 10000.0;
            if((t.getexetime() + t.waitingTime <= (nextPeriod*periodDu + base - t.getArrivTime())*60000)){//this task is finished
                    //update node capacities and services:
                    assWL[0] -= t.getDProc();
                    assWL[1] -= t.getDMem();
                    assWL[2] -= t.getDStorage();
                    assWL[0] = Math.round(assWL[0]* 10000.0) / 10000.0;assWL[1]=Math.round(assWL[1]* 10000.0) / 10000.0;assWL[2]= Math.round(assWL[2]* 10000.0) / 10000.0;
                    graphRel.getNode(host).setAttribute("assignedWorkLoad", assWL);
                   
                    fiTasks.add(t);     //hostedTasks.get(i).finished = 1;
                    
            }
            else{
                remHostTasks.add(t);
            }
        }
        hostedTasks.removeAll(fiTasks);
//      for(i=0; i<hostedTasks.size() ; i++)
//          if (hostedTasks.get(i).finished == 1)
//                hostedTasks.remove(i);
//                    
    
    }
    
//sort hosts based on their distance to source (associated) nodes in ascendent order
    private int[] hostSelection(int numOfTasks, int node, int level){
        Random r = new Random();
        int i = 0, temp; int m,n;
        int[] a = new int[numOfTasks];
        while (i < numOfTasks){
            temp = r.nextInt(maxid + 1);
            astar1.compute(String.valueOf(node),String.valueOf(temp));
            if (astar1.getShortestPath().getEdgeCount() <= level){
                a[i] = temp;
                i++;
            }
        }
         
        for (i= 0 ;i<a.length-1 ; i++){
            for (int k=i+1 ; k<a.length ; k++){
                astar1.compute(String.valueOf(node),String.valueOf(a[i]));
                m = astar1.getShortestPath().getEdgeCount();
                astar1.compute(String.valueOf(node),String.valueOf(a[k]));
                n = astar1.getShortestPath().getEdgeCount();
                if(m>n){
                    temp = a[k];
                    a[k] = a[i];
                    a[i] = temp;
                }
            }
        }
        return a;
    }
    
    public void writeToFileUtilization(boolean append_value, int numNodes, int numPlans, EPOSplan[][] plans, double co) {
        try { 
            for(int row = 0; row < numNodes; row++){
                String file_path_EPOS =ppath+"/datasets/Utilization/agent_"+row+".plans";
                FileWriter writer_EPOS = new FileWriter (file_path_EPOS, append_value);
                for(int col =0; col < numPlans; col++){
                    writer_EPOS.append(String.valueOf(plans[row][col].localCost));
                    writer_EPOS.append(":");
                    int size = plans[row][col].utilPlan.length;
                    for (int i =0; i<size ; i++){//both of CPU and Mem will be printed
                         writer_EPOS.write(String.format("%.4f", plans[row][col].utilPlan[i]));//preventing wrong written of values as negative values
                         if (i != size-1){
                             writer_EPOS.append(",");
                         }
                     }
                    // writer.append("\r\n");
                     writer_EPOS.append("\r\n");
                } 
            writer_EPOS.close();
            }
                     
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
      
    }
    
    //write the distribution of requested tasks after applying selected plans from I-EPOS (hosting on network nodes):
    public void taskDeployedDist(int[] deployedTasks, int g, int n, int c, int p, int level){
        String taskDep = ppath+"/plots/tasklists/tasklistGraph"+g+"numnode"+n+"conf"+c+"level"+level+"period"+p+".csv";
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        try {    
            fileWriter = new FileWriter(taskDep);
            for (int j = 0; j<n ; j++){
                fileWriter.append(String.valueOf(j));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(deployedTasks[j]));
                fileWriter.append(NEW_LINE_SEPARATOR);
            }
            System.out.println("CSV file was created successfully !!!");

        } 
        catch (Exception e) 
        {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        }
        finally
        {
            try 
            {
                fileWriter.flush();
                fileWriter.close();
            }
            catch (IOException e) 
            {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }

        }   
        
    }
//sort tasks based on their dl-wt in ascendent order
    private  ArrayList<Task> orderTasks(Map<Long,Task> tList){

        ArrayList<Task> Tasks = new ArrayList<Task>();
        for (long ikey : tList.keySet())
                Tasks.add(tList.get(ikey));

        for (int i=0; i<Tasks.size()-1; i++){ 
             for (int j=i+1; j<Tasks.size(); j++){ 
                if(Tasks.get(i).diffDlWt>Tasks.get(j).diffDlWt){
                    Task temp = Tasks.get(i);
                    Tasks.set(i, Tasks.get(j));
                    Tasks.set(j, temp);
                }
            }
        }
        return Tasks;
    }

     
    private void desSort(EPOSplan [][] genPlans){

        EPOSplan temp = new EPOSplan(0,0,numOfNodes);
        for (int i=0 ; i<numOfNodes ; i++){
            for (int j=0 ; j<numOfPlans-1 ; j++){
                for (int k=j+1 ; k<numOfPlans ; k++){
                    EPOSplanWrapper cw1 = new EPOSplanWrapper(genPlans[i][j]);
                    EPOSplanWrapper cw2 = new EPOSplanWrapper(genPlans[i][k]); 
                    if (genPlans[i][j].localCost < genPlans[i][k].localCost){
                        temp = cw1.p;
                        genPlans[i][j] = cw2.p;
                        genPlans[i][k] = temp;
                    }    
                }
            }

        }
    }
/*
    private void ascSort(EPOSplan [][] genPlans){
        EPOSplan temp = new EPOSplan(0,0,numOfNodes);
        for (int i=0 ; i<numOfNodes ; i++){
            for (int j=0 ; j<numOfPlans-1 ; j++){
                for (int k=j+1 ; k<j ; k++){
                    if (genPlans[i][j].localCost > genPlans[i][k].localCost)
                        temp = genPlans[i][j];
                        genPlans[i][j] = genPlans[i][k];
                        genPlans[i][j] = temp;
                }
            }

        }
    }
*/
}

               