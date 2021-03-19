/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;
//import static epos.fog.cloud.EposFogCloud.numNod;
import java.util.ArrayList;

/**
 *
 * @author zeinab
 * each service placement plan consists of one binary plan (biPlans), one utilization plan (utilPlan), 
 * and corresponding local-cost.
 * binary plan determines which task is assigned to which host for execution in future time slot.
 * utilization plan represents the utilization value (as the ratio of assigned workload to the capacity) of network nodes.
 * some other parameters are used for evaluation and simplicity such as wlPlan that is used for calculating cumulative
 * workload of newly assigned workload plus already assigned one in previous time slots.
 */

public class EPOSplan {
    boolean valid = false;
    int node;//associated node
    int planIndex;//id of this plan 
    int unassTasks;//number of tasks without any host in this plan
    int assTasks;//number of hosted tasks in this plan
    int assToCloud;//number of assigned tasks to cloud node in this plan
    int dlViol;//number of tasks  whose deadline is violated in this plan
    int numOfNodes;
    public double localCost = 0.0; 
    public ArrayList<BinaryPlan> biPlans = new ArrayList<BinaryPlan>();//identify which task is (going to be) hosted on which host.
    double[] utilPlan;// = new double[numOfNodes*2];//first half for CPU utilization and second half for Memory utilization.
    double[] wlPlan;// = new double[numOfNodes*2];////first half for CPU workload and second half for Memory workload.
    double SD;//service execution delay
            
    EPOSplan(int i, int j, int numNodes){
        node = i ;
        planIndex = j ;
        unassTasks = 0;
        assTasks= 0;
        assToCloud= 0;
        dlViol= 0;
        localCost = 0.0; 
        numOfNodes = numNodes;
        utilPlan = new double[numOfNodes*2];//CPU and Mem
        wlPlan = new double[numOfNodes*2];//CPU and Mem
        SD=0.0;
    } 
    public int getNumAssTasksToFog(){
       return assTasks - assToCloud;
    }

    public int getNumAssTasksToFog(ArrayList<BinaryPlan> plan){
        int numToFog = 0;
        for (int i = 0; i<plan.size(); i++){
            if (plan.get(i).value == 1 && plan.get(i).hostNodeId != 0)
                numToFog++;
        }
        return numToFog;
    }

    public int getNumAssTasksToCloud(ArrayList<BinaryPlan> plan){
        int numToCloud = 0;
        for (int i = 0; i<plan.size(); i++){
            if (plan.get(i).value == 1 && plan.get(i).hostNodeId == 0)
                numToCloud++;
        }
    return numToCloud;
    }

    public int getNumAssTasksToAllFog(ArrayList<BinaryPlan> plan){
        int numToFog = 0;
        for (int i = 0; i<plan.size(); i++){
            if (plan.get(i).value == 1 && plan.get(i).hostNodeId != 0)
                numToFog++;
        }
    return numToFog;
    }

}
