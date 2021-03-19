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
import org.graphstream.algorithm.AStar;
import org.graphstream.graph.Graph;

/**
 *
 * @author zeinab
 * simulation of cloud approach
 * This approach assumes that the fog infrastructure is not available, and all services are sent directly to the cloud
 */
public class Cloud {
    double alpha = 0.95;//the percentage of the resources which are available for hosting services
    static int cloudId;// cloud node as the host node for all of the requested tasks
    static double periodDu  ;
    static double base ;
    static int numNodes ;
    static AStar aStarC;
    String wPat;
    
    Cloud(int cloud, int nodes, double pDuration, double baseT, AStar aS, String pat){
        numNodes = nodes;
        cloudId = cloud;
        periodDu = pDuration;
        base = baseT;
        aStarC = aS;
        wPat = pat;
    }
    //running cloud approach for received tasks requests:
    public EPOSplan runCloud(Graph cloudGraph, Map<Integer, Map<Long,Task>> inTaskList, ArrayList<Task> unHostTasks, ArrayList<Task> runningTasks, int p){
    
        System.out.println("running tasks on cloud..........");
    
        EPOSplan cloudResults = new EPOSplan(0,0,numNodes);//store the results for cloud approach 
        Task t = new Task();
        Task t1;
        int UnassTasks = 0;
        int cloudDlViol = 0;
        int numOfRunTasks = 0;
        double total_delay= 0.0;
        double exeTimeForCloud = 0.0;
        double[] nodcap = new double[3];      
        double[] preAssWL = new double[3];
        ArrayList<Task> HostTasks = new ArrayList<Task>();
        for(int j=0 ; j<numNodes*2 ; j++){
            cloudResults.utilPlan[j] = 0.0;//initial utilization of all nodes
            cloudResults.wlPlan[j] = 0.0;//initial workload allocated to all nodes
        }

        for(int fkey : inTaskList.keySet()) {   //for each node
                Map<Long,Task> tasksPerFogNode = inTaskList.get(fkey);//all received requests tasks
                for(long tkey : tasksPerFogNode.keySet()){            //for each of those task
                    try{
                        t = (Task)tasksPerFogNode.get(tkey).clone();
                    }
                    catch(CloneNotSupportedException c)
                    {
                    }  
                    nodcap = cloudGraph.getNode(cloudId).getAttribute("capacity");
                    preAssWL = cloudGraph.getNode(cloudId).getAttribute("assignedWorkLoad");
                    preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                        
                    //sending tasks to cloud is based on their arrival time and the availability of resource capacity:
                    if ((t.getDStorage() + preAssWL[2] < alpha * nodcap[2])&&(t.getDProc() + preAssWL[0] < alpha * nodcap[0]) & (t.getDMem() + preAssWL[1] < alpha * nodcap[1])){
                        //CPU
                        cloudResults.utilPlan[cloudId] = Math.round(((t.getDProc()+preAssWL[0])/nodcap[0])* 10000.0) / 10000.0;//update cpu capacity for selected node
                        //Memory
                        cloudResults.utilPlan[cloudId+numNodes] = Math.round(((t.getDMem()+preAssWL[1])/nodcap[1])* 10000.0) / 10000.0;//update mem capacity for selected node
                        //CPU
                        preAssWL[0] += t.getDProc();
                        //Memory
                        preAssWL[1] += t.getDMem();
                        //storage
                        preAssWL[2] += t.getDStorage();
                    
                        cloudResults.wlPlan[cloudId] += (t.getDProc());
                        cloudResults.wlPlan[cloudId+numNodes] += (t.getDMem());

                        preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                        cloudGraph.getNode(cloudId).setAttribute("assignedWorkLoad", preAssWL);
                        t.sethostNode(cloudId);
                        
                        numOfRunTasks += 1;                                        
                        HostTasks.add(t);
                    }
                    else{//in the case of insufficient capacity at the cloud node:
                        t.sethostNode(-1);
                        unHostTasks.add(t);
                        UnassTasks += 1;
                    }
                
                }
                    //inTaskList.replace(fkey, tasksPerFogNode);
        }
        //calculate tasks' execution time according to queuing theory:
        nodcap = cloudGraph.getNode(cloudId).getAttribute("capacity");  
        preAssWL = cloudGraph.getNode(cloudId).getAttribute("assignedWorkLoad");
        exeTimeForCloud =  1.0/(nodcap[0]- preAssWL[0]);//considers total capacity, the allocated load form previous periods, and the assigned load in current period
       
        //calculate deadline violation and service execution delay:
        for (int i=0; i<HostTasks.size(); i++){
            t1 = HostTasks.get(i);
            aStarC.compute(String.valueOf(t1.getassignedNode()),String.valueOf(cloudId));
            double le = aStarC.getShortestPath().getEdgeCount();     
            double dl = t1.gettaskDL();
            double st =exeTimeForCloud + 2*0.3*(le/1000000);   //converting distance to time; it is assumed that each link introduces delay-time = 0.3s
            t1.serviceTime = st;

            if (st > dl) {
                cloudDlViol += 1;
                t1.dl_viol = 1;
                total_delay += (st-dl);
            }
            t1.service_delay = Math.abs(dl - st);
            runningTasks.add(t1);
        }
        //set evaluation parameter for cloud approach in current time period:
        total_delay/=HostTasks.size();//average of service execution delay
        cloudResults.SD = total_delay;
        cloudResults.assToCloud = numOfRunTasks;
        cloudResults.dlViol = cloudDlViol;
        cloudResults.unassTasks =  UnassTasks;         
    
        taskDeployedDist(numOfRunTasks, numNodes, p);
        return cloudResults;
  
    }  
    //Write the distribution of tasks over the network nodes after running cloud approach:
    public void taskDeployedDist(int tasknum, int n, int pn){
        String taskDep = wPat+"/tasklistCloudGraphnumnode"+n+"period"+pn+".csv";
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        try {    
            fileWriter = new FileWriter(taskDep);
            for (int j = 0; j<n ; j++){
                fileWriter.append(String.valueOf(j));
                fileWriter.append(COMMA_DELIMITER);
                if(j != cloudId)
                    fileWriter.append(String.valueOf(0));
                else
                    fileWriter.append(String.valueOf(tasknum));
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
    //releasing resources based on completing tasks execution time:
    public void releaseCloud(int period, Graph cloudGraph, ArrayList<Task> runningTasks, ArrayList<Task> finishedTasks){
    
        System.out.println("release resources on cloud..........");
        int j;
        double[] preAssWL = new double[3];
        //check if any hosted task on the cloud node is finished and release its allocated resources:
        for(j=0; j<runningTasks.size() ; j++) { 
            Task t = runningTasks.get(j);
            if((t.getexetime() <= ((period+1)*periodDu + base - t.getArrivTime())*60000)){//Since tasks are forwarded to the cloud node immediately, waiting time is supposed to be 0. 
                //update node capacities and services: 
                preAssWL = cloudGraph.getNode(cloudId).getAttribute("assignedWorkLoad");
                preAssWL[0] -= t.getDProc();
                preAssWL[1] -= t.getDMem();
                preAssWL[2] -= t.getDStorage();
                preAssWL[0]=Math.round(preAssWL[0]* 10000.0) / 10000.0;preAssWL[1]=Math.round(preAssWL[1]* 10000.0) / 10000.0;preAssWL[2]= Math.round(preAssWL[2]* 10000.0) / 10000.0;
                //update resources at the cloud graph 
                cloudGraph.getNode(cloudId).setAttribute("assignedWorkLoad", preAssWL);

                finishedTasks.add(t);
                //runningTasks.get(j).finished = 1;
            }
        }

        runningTasks.removeAll(finishedTasks);
     
    }

//public double distanceEdges(int node1, int node2, Graph ng){
//        AStar aStar = new AStar(ng);
//        aStar.compute(String.valueOf(node1),String.valueOf(node2));
//        return aStar.getShortestPath().getEdgeCount();        //distance to time; each edge delay = 0.3s
//    }

}
