/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zeinab
 * This class read the input workload and submit them to their receiver/edge nodes.
 * All of the parameters (such as receiver node, execution time, and resource demands) 
 * for these tasks are identified beforehand considering various parameters such as task distribution method
 * in another IIoTTasks program included in this repository.
 * Hence this class only input them as required.
 */
public class InTask {
    static double periodDU;//duration of period to identify the arrival timestamp range of tasks. 
    static int numN;//number of network nodes
    static boolean beta;//task distribution method
   
    InTask(double pDu, int numNod, boolean Dis){
        beta = Dis;
        periodDU = pDu;
        numN = numNod;
    }
    
    //read input workload/tasks using the parameters as follows. 
    //sTime: start timestamp, eTime: end timestamp, pnum: time period, path:input file path.
    public Map<Integer, Map<Long,Task>> makeNewTasks(double sTime, double eTime, int pnum, String path){
        
        System.out.println("assigning tasks to nodes........");
    
        Map<Integer, Map<Long,Task>> taskList = new HashMap<Integer , Map<Long,Task>>();
      /*
       * address to the preprocessed Google Cluster trace file which contains 4.5 hours and 89354 tasks
       * this files are preprocessed using IIoTTasks project
       */
        String csvFile =path+"/indataset/tasks/tasknn"+numN+"beta"+beta+".csv";
        String line = "";
        String cvsSplitBy = ","; // use comma as separator
        long tId=0;
        double totCPU=0.0;
        double totMem=0.0;
        double totStorage=0.0;
        Double tExeTime;
        Double tCpu;
        Double tMem;
        Double tStorage;
        double tDL;
        int assFogId;
        double tWaitTime;
        double maxexe = 0.0;
        int i = 0;
        int j = 0;
        //read task lines one by one from start timestamp to end timestamp and assign to associated/receiver network nodes
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                while((line = br.readLine()) != null){
                    String[] input = line.split(cvsSplitBy);
                    double scaleToSecTS = Double.parseDouble(input[0]); //input[0] is the first parameter in the input file: timestamp of task arrival(in microsecond)
                    //double scaleToSecTS = (TS*1)/60000000;  // convert microsecond to min, not used
                    //compare if the task arrival timestamp is in the range or not:
                    if (((Double.compare(scaleToSecTS,sTime) > 0) && (Double.compare(eTime,scaleToSecTS) > 0))||(Double.compare(scaleToSecTS,sTime) == 0)){
                        //read characteristics of input task:
                        tCpu = Math.round(Double.parseDouble(input[1])* 10000.0) / 10000.0;
                        totCPU+=tCpu;
                        tMem = Math.round(Double.parseDouble(input[2])* 10000.0) / 10000.0;
                        totMem+=tMem;
                        tStorage = Math.round(Double.parseDouble(input[3])* 10000.0) / 10000.0;
                        totStorage+=tStorage;
                        tId = Long.parseLong(input[4]);
                        tExeTime = Math.round(Double.parseDouble(input[5])* 10000.0) / 10000.0;//milisecond
                        tWaitTime =Math.round(Double.parseDouble(input[6])* 10000.0) / 10000.0;//milisecond
                        tDL = Double.parseDouble(input[7]);//in terms of milisecond
                        assFogId = Integer.parseInt(input[8]);//the id of receiver node 
                        
                        Task t= new Task(scaleToSecTS, tCpu, tMem, tStorage, tId, tExeTime, tWaitTime, tDL, assFogId, (tDL-tWaitTime));
                        //assign the task to its associated node:
                        if(!taskList.containsKey(assFogId)) {
                            Map<Long,Task> taskToFog1 = new HashMap<Long,Task>();
                            taskToFog1.put(tId, t);
                            taskList.put(assFogId,taskToFog1);
                            i++; 
                        }
                         else
                        {
                            Map<Long,Task> taskToFog1= taskList.get(assFogId);
                            taskToFog1.put(tId, t);
                            taskList.put(assFogId,taskToFog1);
                                
                        }
                        j++;
                    }
                    //timestamp is not in the range: 
                    else if (Double.compare(scaleToSecTS,eTime) >0){
                        break;
                    }
                }
        
                System.out.println("period: "+pnum+" Number of tasks: "+j+" Number of associated nodes: "+i);
                System.out.println("CPU demand: "+totCPU);
                System.out.println("Memory demand: "+totMem);
                System.out.println("Storage demand: "+totStorage);

        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }  
      
    return taskList;
    }
}
