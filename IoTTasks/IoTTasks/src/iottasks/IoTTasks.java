/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iottasks;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.distribution.BetaDistribution;

/**
 *read task lines one by one from start timestamp to end timestamp and assign them to edge nodes
 * @author Zeinab
 */
public class IoTTasks {

    /**
     * @param args the command line arguments
     */
    
    static int[] numNod = new int[]{200, 400, 600, 800, 1000};//various network sizes for different experiments
    static int period = 5;// number of time slots
    static String prePath="C:/Epos-Fog-Cloud";
    static double baseTime = 20.0;
    static int PERIODDU = 5;// time duration for one slot
    
    public static void main(String[] args) {
        //determine service types and deadlines; millisecond; 
        double[] serviceTypesToDL = new double[]{2.0, 2.0, 2.0, 2.0, 5.0, 10.0, 10.0, 10.0, 10.0, 10.0, 30.0, 50.0, 100.0, 100.0, 100.0, 1000.0, 1000.0, 10000.0, 10000.0, 10000.0, 100000.0, 100000.0, 100000.0, 120000.0, 140000.0, 160000.0, 180000.0, 200000.0, 220000.0, 240000.0, 260000.0, 280000.0 , 300000.0, 320000.0, 340000.0, 360000.0, 380000.0, 400000.0, 420000.0, 460000.0};//40 in terms of milisecond
        ArrayList<ArrayList<Task>> taskList = new ArrayList<ArrayList<Task>>();//list of random distributed tasks
        ArrayList<ArrayList<Task>> taskListBeta = new ArrayList<ArrayList<Task>>();// list of tasks distributed base on Beta distribution
    
                
    String csvFile =prePath+"/indataset/taskEvent012WithoutZeroExetime.csv";//input workload file for 4 hours and 30 minutes and 89354 tasks
        //int event;int schC;int prio;int tType;
        //int minST = 0;int maxST = 21;service types is between 0 to 39
        String line = "";
        String cvsSplitBy = ","; 
        long tId=0;        
        double tDL;double tWaitTime;
        int assFogId, assFogIdBeta;
        int i = 0;  int j = 0;
        double[][] periodLoad= new double[period][3];
        int fNode, lNode;
        
        int a = 0;//dlKind*10;
        int b = 23;//(dlKind+1)*10-1;   23: 120  32: 300
        
    for (i =0;i<period; i++){
        Double tExeTime=0.0;
        Double tCpu=0.0;
        Double tMem=0.0;
        Double tStorage;
        double totCPU=0.0;
        double totMem=0.0;
        double totStorage=0.0;
        
        double startT = i*PERIODDU+baseTime;
        double endT = (i+1)*PERIODDU+baseTime;
       
        ArrayList<Task> task1 = new ArrayList<Task>();
        ArrayList<Task> task2 = new ArrayList<Task>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
                while((line = br.readLine()) != null){
                    String[] input = line.split(cvsSplitBy);
                    double TS = Double.parseDouble(input[0]);           //first parameter: timestamp of task arrival(in microsecond)
                    double scaleToSecTS = (TS*1)/60000000;              //convert microsecond to minute
                    
                    if (((Double.compare(scaleToSecTS,startT) > 0) && (Double.compare(endT,scaleToSecTS) > 0))||(Double.compare(scaleToSecTS,startT) == 0)){
                        tCpu = Double.parseDouble(input[2]);//second param: processing power
                        totCPU+=tCpu;
                        tMem = Double.parseDouble(input[3]);//third param: memory demand
                        totMem+=tMem;
                        tStorage = Double.parseDouble(input[4]);//forth param: storage demand
                        totStorage+=tStorage;
                        tId = Long.parseLong(input[5]);//fifth param:task Id
                        tExeTime = Double.parseDouble(input[6])*1/1000;//expected execution time, converts microsecond to milisecond
                        
                        tDL = serviceTypesToDL[getvalue(a,b)];//in terms of milisecond
                        tWaitTime = randWT(0.00001 , scaleToSecTS);//in terms of milisecond
                        
                        //create task
                        Task t= new Task(scaleToSecTS, tCpu, tMem, tStorage, tId, tExeTime, tWaitTime, tDL, (tDL-tWaitTime));
                        
                        //assign read task to its associated edge node
                        task1.add(t);
                        task2.add(t);
                        
                        
                    }
                    else if (Double.compare(scaleToSecTS,endT) >0){
                                break;
                    }
           }
        
                        
        } 
        catch (IOException e) {
                               e.printStackTrace();
        }
        System.out.println("period: "+i+" Number of tasks: "+task1.size());
        System.out.println("CPU demand: "+totCPU);
        System.out.println("Memory demand: "+totMem);
        System.out.println("Storage demand: "+totStorage);
        periodLoad[i][0]=totCPU; periodLoad[i][1]=totMem;periodLoad[i][2]=totStorage;
        
        taskList.add(task1);
        taskListBeta.add(task2);
        }
    //assign tasks to the edge nodes for different size of network considering two types of distribution
    for (i =0; i<numNod.length;i++){
        lNode=numNod[i]; //largest node id in the network
        fNode=0;//first node id in the network
        for (j =0;j<period;j++){
            BetaDistribution betaDis = new BetaDistribution(2.0, 5.0);
            for (int k =0;k<taskList.get(j).size();k++){
                assFogId= getvalue(fNode, lNode);  
                taskList.get(j).get(k).setassNode(assFogId);
            }
            for (int k =0;k<taskListBeta.get(j).size();k++){
                assFogIdBeta = (int)(lNode*betaDis.sample());
                taskListBeta.get(j).get(k).setassNode(assFogIdBeta);
            }
       
       }
        WriteFile(numNod[i], false, taskList);
        WriteFile(numNod[i], true, taskListBeta);
        Writeload(periodLoad);
       }
    }
    
   
    public static void WriteFile(int nn, boolean beta, ArrayList<ArrayList<Task>> tasks){
        
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        
        //String task = prePath+"/indataset/tasks/tasknn"+nn+"beta"+beta+".csv";
          String task = "C:/tasks/tasknn"+nn+"beta"+beta+".csv";
                 
        try {
            fileWriter = new FileWriter(task);
            for (int j =0;j<period;j++){
                for (int k =0;k<tasks.get(j).size();k++){
                    Task t = tasks.get(j).get(k);
                    fileWriter.append(String.valueOf(t.taskArrivTime));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.taskDProc));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.taskDMem));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.taskDStorage));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.taskId));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.taskExeTime));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.waitingTime));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.taskDL));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(t.assignedNode));
                    fileWriter.append(COMMA_DELIMITER);

                        fileWriter.append(NEW_LINE_SEPARATOR);
                }
            }
 
             
             
            System.out.println("CSV file was created successfully !!!");
             
        } catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } finally {
             
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
             
        }
         
    }
    private static void Writeload(double[][]periodLoad){
        
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        
        String load = "C:/load/load"+PERIODDU+".csv";
                 
        try {
            fileWriter = new FileWriter(load);
            for (int j =0;j<period;j++){
                
                    fileWriter.append(String.valueOf(j));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(periodLoad[j][0]));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(periodLoad[j][1]));
                    fileWriter.append(COMMA_DELIMITER);
                    fileWriter.append(String.valueOf(periodLoad[j][2]));
                    fileWriter.append(NEW_LINE_SEPARATOR);
                }
             
            System.out.println("CSV file was created successfully !!!");
             
        } catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } finally {
             
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
             
        }
         
    }
    
    
    private static int getvalue(int min, int max){ 
            if (min >= max) {
                throw new IllegalArgumentException("max must be greater than min");
            }
            Random r = new Random();
            return r.nextInt((max - min) + 1) + min;
    }
    
    private static double  randWT(double rangeMin, double arrivTime){
                   double rangeMax = (Math.abs(arrivTime%10-period))/2;
                   Random r = new Random();
                   double randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble()*1000;//in terms of milisecond (range=(0-period)s)
                   return randomValue;
    }
    
}
