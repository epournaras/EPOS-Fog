/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud.old;
import org.apache.commons.math3.distribution.BetaDistribution;

import epos.fog.cloud.Task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


/**
 *  
 * @author zeinab
 * This is old class for reading input workload and is currently replaced with InTask.
 * In contrast to the new class, this class itself generates some parameters such as deadline of each task.
 */
public class iotTask {
    
    static Map<Integer,Double> serviceTypesToDL = new HashMap<Integer,Double>(); 
    static double sTime;
    static double eTime;
    int period;
    static int fNode;
    static int lNode;
    static boolean beta;
    iotTask (double start, double end, int period, int fNodeId, int lNodeId, boolean be){
        beta = be;
        setTimes(start, end, period );
        setNodeId(fNodeId, lNodeId);
        //determine service types and deadlines in milisecond; provides deadlines in the range of (2.0ms,460000.0ms). should be edited
        serviceTypesToDL.put(0, 2.0);
        serviceTypesToDL.put(1, 2.0);
        serviceTypesToDL.put(2, 2.0);
        serviceTypesToDL.put(3, 2.0);
        serviceTypesToDL.put(4, 5.0);
        serviceTypesToDL.put(5, 10.0);
        serviceTypesToDL.put(6, 10.0);
        serviceTypesToDL.put(7, 10.0);
        serviceTypesToDL.put(8, 10.0);
        serviceTypesToDL.put(9, 10.0);
        serviceTypesToDL.put(10, 30.0);//kind = 1
        serviceTypesToDL.put(11, 50.0);
        serviceTypesToDL.put(12, 100.0);
        serviceTypesToDL.put(13, 100.0);
        serviceTypesToDL.put(14, 100.0);
        serviceTypesToDL.put(15, 1000.0);
        serviceTypesToDL.put(16, 1000.0);
        serviceTypesToDL.put(17, 10000.0);
        serviceTypesToDL.put(18, 10000.0);
        serviceTypesToDL.put(19, 10000.0);//kind = 1
        serviceTypesToDL.put(20, 100000.0);
        serviceTypesToDL.put(21, 100000.0);
        serviceTypesToDL.put(22, 100000.0);//more than 100 second
        serviceTypesToDL.put(23, 120000.0);
        serviceTypesToDL.put(24, 140000.0);
        serviceTypesToDL.put(25, 160000.0);
        serviceTypesToDL.put(26, 180000.0);
        serviceTypesToDL.put(27, 200000.0);
        serviceTypesToDL.put(28, 220000.0);
        serviceTypesToDL.put(29, 240000.0);
        serviceTypesToDL.put(30, 260000.0);
        serviceTypesToDL.put(31, 280000.0);
        serviceTypesToDL.put(32, 300000.0);
        serviceTypesToDL.put(33, 320000.0);
        serviceTypesToDL.put(34, 340000.0);
        serviceTypesToDL.put(35, 360000.0);
        serviceTypesToDL.put(36, 380000.0);
        serviceTypesToDL.put(37, 400000.0);
        serviceTypesToDL.put(38, 420000.0);
        serviceTypesToDL.put(39, 460000.0);//40 elements in terms of milisecond
               
    }
    
    public void setTimes(double start, double end, int pdu){
        sTime = start;
        eTime = end;
        period = pdu;
    }
    public void setNodeId(int fNodeId, int lNodeId){
        fNode= fNodeId; //first node id = 0
        lNode= lNodeId; //last node id=number of nodes - 1
    }
    public Map<Integer, Map<Long,Task>> makeNewTasks(int startDL, int endDL, int pnum ){
        
        System.out.println("assigning tasks to nodes........");
        Map<Integer, Map<Long,Task>> taskList = new HashMap<Integer , Map<Long,Task>>();
        String csvFile ="C:/Epos-Fog-Cloud/indataset/taskEvent012WithoutZeroExetime.csv";//Google Cluster trace as input file which contains 4.5 hours and 89354 tasks
        String line = "";
        String cvsSplitBy = ","; //use comma as separator
        //some not used parameters in the input file: int event;  int schC;   int prio;
        //task distribution method over network nodes:
        BetaDistribution betaDis = new BetaDistribution(2.0, 5.0);
        //service types is between 0 to 39:   int minST = 0; int maxST = 21;         
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
        //read task lines one by one from start timestamp to end timestamp and assign them to receiver network nodes
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                while((line = br.readLine()) != null){
                    String[] input = line.split(cvsSplitBy);
                    double TS = Double.parseDouble(input[0]);//first param: timestamp of task arrival(in microsecond)
                    double scaleToSecTS = (TS*1)/60000000;   // convert microsecond to min
                    if (((Double.compare(scaleToSecTS,sTime) > 0) && (Double.compare(eTime,scaleToSecTS) > 0))||(Double.compare(scaleToSecTS,sTime) == 0)){
                        //read characteristics of the task:
                        tCpu = Double.parseDouble(input[2]);
                        totCPU+=tCpu;
                        tMem = Double.parseDouble(input[3]);
                        totMem+=tMem;
                        tStorage = Double.parseDouble(input[4]);
                        totStorage+=tStorage;
                        tId = Long.parseLong(input[5]);
                        tExeTime = Double.parseDouble(input[6])*1/1000;//microsecond to milisecond
                        //generate task type in terms of service deadline and associated node randomly from {cloud+fog}
                        //tType = getvalue(minST, maxST);
                        
                        tDL = serviceTypesToDL.get(getvalue(startDL,endDL));//in terms of milisecond-should be modified
                        if(beta)
                            assFogId = (int)(lNode*betaDis.sample());
                        else 
                            assFogId= getvalue(fNode, lNode);  
                        tWaitTime = randWT(0.00001 , scaleToSecTS);//in terms of milisecond
                        if (tExeTime>maxexe)
                            maxexe = tExeTime;
                        Task t= new Task(scaleToSecTS, tCpu, tMem, tStorage, tId, tExeTime, tWaitTime, tDL, assFogId, (tDL-tWaitTime));
                        //assign the task to its associated node
                        
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
    
    private int getvalue(int min, int max){ 
            if (min >= max) {
                throw new IllegalArgumentException("max must be greater than min");
            }
            Random r = new Random();
            return r.nextInt((max - min) + 1) + min;
    }
    private double randWT(double rangeMin, double arrivTime){
                   double rangeMax = (Math.abs(arrivTime%10-period))/2;
                   Random r = new Random();
                   double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble()*1000;//in terms of milisecond (range=(0-period)s)
                   return randomValue;
    }

 }
