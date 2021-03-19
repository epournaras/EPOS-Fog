/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package epos.fog.cloud;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import java.util.Iterator;
import org.graphstream.algorithm.AStar;

/**
 *
 * @author zeinab
 * Simulation the First Fit approach:
 * Each node, regarding the latency of communication link between itself and other nodes in network, 
 * makes a sorted list of its direct neighbor nodes. Then, upon the receipt of each request, 
 * the list is checked, and if any node in the list meets the request
 * requirements, the request is sent to the node. Otherwise, the request is propagated to the cloud.
 */
public class FirstFit {
    static int numOfNodes;
    static int cloudId;
    static double base;
    static double periodDu;
    static AStar aStarFF;
    String wPat;
    FirstFit(int nN, int cId, double pD, double baseT, AStar aS, String pat){
        numOfNodes = nN;
        cloudId = cId;  
        base = baseT;
        periodDu = pD;
        aStarFF = aS;
        wPat = pat;
    }
    public EPOSplan runFirstFit(Graph ffGraph, Map<Integer, Map<Long,Task>> inTaskList, ArrayList<Task> hostedTasks, ArrayList<Task> unHostTasks, int pe){
        
        System.out.println("running first fit..........");
        int i;int id;
        Task t = new Task(); 
        Task t1;
        int host;
        boolean assigned;
        //int UnassTasks = 0;
        double total_delay= 0.0;
        int assTasks =0;
        int[] deployedTasksPerPeriod = new int[numOfNodes];
        double[] nodcap = new double[3];      
        double[] preAssWL = new double[3];
        EPOSplan ffResults = new EPOSplan(0,0,numOfNodes);//store the results for First Fit approach
        ArrayList<Task> newHostedTasks = new ArrayList<Task>();
        double[] exeTimeForNodes = new double[numOfNodes];

        for(int fkey : inTaskList.keySet()) {  //for each node in the network which may receive IoT requests
            Map<Long,Task> tasksPerFogNode = inTaskList.get(fkey); // all of the requested tasks 
            for(long tkey : tasksPerFogNode.keySet()){            //for each requested task 
                try{
                    t = (Task)tasksPerFogNode.get(tkey).clone();
                }
                catch(CloneNotSupportedException c)
                {
                } 
                assigned = false;
                //determine directly neighboring nodes:
                Iterator<Node> it = ffGraph.getNode(fkey).getNeighborNodeIterator();
                //check if any neighbor has sufficient capacity:
                while (it.hasNext()&& (assigned != true)){
                    Node nex = it.next();
                    nodcap  = nex.getAttribute("capacity");
                    preAssWL = nex.getAttribute("assignedWorkLoad");
                    preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                    id=nex.getIndex();
                    if ((t.getDProc() + preAssWL[0] < nodcap[0]) && (t.getDMem() + preAssWL[1] < nodcap[1]) && (t.getDStorage() + preAssWL[2] < nodcap[2])){
                        //CPU
                        ffResults.wlPlan[id] += (t.getDProc());
                        ffResults.utilPlan[id] = Math.round(((t.getDProc()+preAssWL[0])/nodcap[0])* 10000.0) / 10000.0;//update cpu capacity for the neighbor node
                        preAssWL[0] += t.getDProc();
                        //Memory
                        ffResults.utilPlan[id+numOfNodes] = Math.round(((t.getDMem()+preAssWL[1])/nodcap[1])* 10000.0) / 10000.0;//update memory for the neighbor node
                        ffResults.wlPlan[id+numOfNodes] += (t.getDMem());
                        preAssWL[1] += t.getDMem();
                        //Storage
                        preAssWL[2] += t.getDStorage();

                        preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                        ffGraph.getNode(id).setAttribute("assignedWorkLoad", preAssWL);
                        deployedTasksPerPeriod[id] += 1;
                        t.sethostNode(id);
                        BinaryPlan biPlan = new BinaryPlan(t, id, 1);
                        ffResults.biPlans.add(biPlan);
                        assTasks++;
                        newHostedTasks.add(t);

                        if (id == cloudId){
                            ffResults.assToCloud++;
                        }
                    assigned = true;
                    }
                }
                if (assigned != true){//if there is not sufficient capacity at the neighbor nodes, the task is forwarded to the cloud node              
                    preAssWL = ffGraph.getNode(cloudId).getAttribute("assignedWorkLoad");
                    nodcap  = ffGraph.getNode(cloudId).getAttribute("capacity");

                    if ((t.getDProc() + preAssWL[0] < nodcap[0]) && (t.getDMem() + preAssWL[1] < nodcap[1]) && (t.getDStorage() + preAssWL[2] < nodcap[2])){

                        ffResults.utilPlan[cloudId] = Math.round(((t.getDProc()+preAssWL[0])/nodcap[0])* 10000.0) / 10000.0;//update cpu capacity for cloud node
                        preAssWL[0] += t.getDProc();
                        ffResults.wlPlan[cloudId] += (t.getDProc());
                        //Memory
                        ffResults.utilPlan[cloudId+numOfNodes] = Math.round(((t.getDMem()+preAssWL[1])/nodcap[1])* 10000.0) / 10000.0;//update memory capacity for cloud node
                        preAssWL[1] += t.getDMem();
                        ffResults.wlPlan[cloudId+numOfNodes] += (t.getDMem());
                        //storage
                        preAssWL[2] += t.getDStorage();
                        preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                        ffGraph.getNode(cloudId).setAttribute("assignedWorkLoad", preAssWL);
                        deployedTasksPerPeriod[cloudId] += 1;
                        assTasks++;     
                        ffResults.assToCloud++;
                        t.sethostNode(cloudId);
                        BinaryPlan biPlan = new BinaryPlan(t, cloudId, 1);
                        ffResults.biPlans.add(biPlan);
                        //hostedTasks.add(t);
                        newHostedTasks.add(t);

                    }
                    else{
                         ffResults.unassTasks++;   
                         t.sethostNode(-1);
                         BinaryPlan biPlan = new BinaryPlan(t, -1, 0);
                         ffResults.biPlans.add(biPlan);
                         unHostTasks.add(t);
                    } 
                assigned = true;
                }
            }
        }
        //compute execution time for each node regarding to the queuing theory:
        for(i = 0; i<numOfNodes ; i++){                   
            nodcap = ffGraph.getNode(i).getAttribute("capacity");  
            preAssWL = ffGraph.getNode(i).getAttribute("assignedWorkLoad");
       
            //considers total capacity, the allocated load form previous periods (currently running tasks), and the assigned load in current period:
            exeTimeForNodes[i] =  1.0/(nodcap[0]-preAssWL[0]);
            }
       
        //calculation of service execution delay and deadline violation rate:
        for (i=0 ; i<newHostedTasks.size() ; i++){
            t1 = newHostedTasks.get(i);
            host = t1.gethostNode();
            aStarFF.compute(String.valueOf(t1.getassignedNode()),String.valueOf(host));
            double le = aStarFF.getShortestPath().getEdgeCount();     
            double dl = t1.gettaskDL();
            double st =exeTimeForNodes[host] + t1.waitingTime + 2*0.3*(le/1000000);
            t1.serviceTime = st;
            t1.service_delay = Math.abs(dl - st);
            if (st > dl) {
                ffResults.dlViol += 1;
                t1.dl_viol = 1;
                total_delay += (st-dl);
            }
            hostedTasks.add(t1);
        }
        total_delay /= newHostedTasks.size();//avg of service delay
        ffResults.SD = total_delay;
        ffResults.assTasks = assTasks;
        ffResults.valid = true;
        taskDeployedDist(deployedTasksPerPeriod, numOfNodes, pe);
    return ffResults;  
    
    }

    //releasing resources based on completing tasks execution time:
    public void releaseFirstFit(int period, Graph Graphff, ArrayList<Task> HostedTasksff, ArrayList<Task> FiTasksff){
        
        System.out.println("-------------------release first fit-----------------");
        int i,j=0;
        double[] preAssWL = new double[3];
        double[] nodcap = new double[3];     

        for(i=0; i<HostedTasksff.size() ; i++) {
            Task t = HostedTasksff.get(i);
            int host = t.gethostNode();
            preAssWL = Graphff.getNode(host).getAttribute("assignedWorkLoad");
            if((t.getexetime() + t.waitingTime <= (period*periodDu + base - t.getArrivTime())*60000)){//this task is finished
                //update node capacities and services:
                preAssWL[0] -= t.getDProc();
                preAssWL[1] -= t.getDMem();
                preAssWL[2] -= t.getDStorage();
                preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                //update resources at the First Fit graph:
                Graphff.getNode(host).setAttribute("assignedWorkLoad", preAssWL);
                  
                FiTasksff.add(t);
                
            }
        }
        HostedTasksff.removeAll(FiTasksff);
   
    /*
    double totCPU=0.0;
    double totMem=0.0;
    double totStorage=0.0;
    for(j=0 ; j<numOfNodes ; j++){
            preAssWL = Graphff.getNode(j).getAttribute("assignedWorkLoad");
            nodcap = Graphff.getNode(j).getAttribute("capacity");
            totCPU+=(nodcap[0]-preAssWL[0]);
            totMem+=(nodcap[1]-preAssWL[1]);
            totStorage+=(nodcap[2]-preAssWL[2]);
    }
    */ 
    
    }
    //Write the distribution of tasks over the network nodes after running First Fit approach:
    public void taskDeployedDist(int[] deployedTasks, int n, int p){
        String taskDep = wPat+"/tasklistffGraphnumnode"+n+"period"+p+".csv";
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        try 
        {    
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

}