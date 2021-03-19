/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package epos.fog.cloud;
//import cern.jet.random.Poisson;
import experiment.IEPOSExperiment;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.Graphs;
import java.util.ArrayList;
import org.graphstream.algorithm.AStar;
import org.graphstream.ui.view.Viewer;
//import org.graphstream.ui.swingViewer.View;
//import org.graphstream.ui.swingViewer.Viewer;


/**
 *
 * @author zeinab
 * this project runs and evaluates three service placement approaches including EPOS Fog (the proposed one), First Fit, and Cloud.
 * considering various configuration parameters (such as network size and topology, etc.) the following algorithm is executed:
 * 1. generate infrastructure including several networks with heterogeneous edge-to-cloud nodes
 * 2. read input workload (IoT services/tasks)
 * 3. distribute input workload over edge nodes
 * 4. run EPOS Fog approach:
 *      generate possible plans for each node 
 *      call I-EPOS
 *      apply selected plans (output of I-EPOS)
 *      execute tasks
 * 5. run First Fit approach
 * 6. run Cloud approach
 * 7. record results for evaluation (for the evaluation several varied parameters are considered that consist of: size of the network, type of topology, workload distribution method, lambda values, and host proximity)
 * */

/*
* for further information refer to our paper in the following address: https://arxiv.org/abs/2005.00270
*/

/**
* To run the project:
*1. command line: java -jar EposFogCloud1001.jar "path to this directory" 
*2. gui (eclipse or netbeans): modify the path to this directory in the line number 84 and then run this main class
*/


public class EposFogCloud {
	
 //set simulation parameter:
	
  static String bpath ;//input parameter to set the base path of files= "C:/Epos-Fog-Cloud1001";
       
  static final int PERIODDU = 5;//time period duration in minute for reading the input workload
  static final int periodNum =5;//number of input profiles
  static double [] config = {0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0};//Lambda values as input for I-EPOS in the range [0,1] 
  static boolean[] bDis = new boolean[]{false,true};//task distribution over network nodes: random = false, beta = true 
  static int[] hopConstLevel = {1000,3,1};//host-proximity parameter: default value is infinity as 1000
  static int[] gType = {2,1,0};//topology type: {0=BarabasiAlbertGenerator-scale_free graph, 1=WattsStrogatzGenerator-Small_world graph, 2=Erdos_Renyi≠ random graph};
  static int[] numNod = new int[]{1000,800,600,400,200};//network size or number of nodes
  
  public static void main(String[] args) {
    	
    	bpath = "C:/Epos-Fog-Cloud1001";//input directory address, change it according to yours or get it as an input param as follows: bpath = args[0];
        
        //config parameters for I-EPOS: further details are available in the associated papers indicated in our published one      
        int num_Run=10; //number of run
        int numOfPlans = 20;//number of possible plans for each agent   
        int num_Iteration=40;//number of iterations
        
        //three different network topologies are evaluated as follows:
        String[] graphType = new String[]{"Barab√†si-Albert","small world","Random"};
        double baseTime = 20.0; //base value as start timestamp for reading input workload
        int Graphnum = 1;       //number of generated graphs for evaluations
        int i = 0;  
        int cloud;//cloud node id
        Graph graph;//
        Graph graphForCloud;//graph for evaluation cloud approach
        Graph graphForFF;//graph for evaluation First Fit approach
        
        //arrays for storing produced results for evaluation:
        EPOSplan[][][][][][] FogResults = new EPOSplan[gType.length][numNod.length][bDis.length][hopConstLevel.length][periodNum][config.length];//selected plans in all periods+one for total result
        EPOSplan[][][][] CloudResults = new EPOSplan[gType.length][numNod.length][bDis.length][periodNum];
        EPOSplan[][][][] FFResults = new EPOSplan[gType.length][numNod.length][bDis.length][periodNum];
        
        String path = bpath+"/plots";//path to generated results as excel files for following plots
        String npath,epath,cnpath,cepath,ffnpath,ffepath;
        cnpath = path+"/loadcolor/cloud/";//path to measure the utilization of network nodes in cloud approach
        ffnpath = path+"/loadcolor/ff/";//path to measure the utilization of network nodes in First Fit approach
        //create an object for evaluation and writing produced results:
        AnalyseResults AR = new AnalyseResults(path, gType, numNod, bDis, hopConstLevel, config, periodNum);
        
        
        
for (int gt=0 ; gt<gType.length ; gt++){//for different types of graphs simulation repeats
    System.out.println("-------graph type: "+gt+"----------");
    for (int nn=0 ; nn<numNod.length ; nn++){//for each graph type, according to the size of the network, different infrastructures and graphs are generated. 
        System.out.println("----------numNod: "+numNod[nn]+"-----------");
        Graphnum=gt*10+nn;
        //create an object of infrastructure class to generate intended graphs:
        Infrastructure Inf1 = new Infrastructure(numNod[nn], Graphnum, graphType[gt]);
        ArrayList<Graph> graphForFog= new ArrayList<Graph>();
        ArrayList<Graph> graphForPlan= new ArrayList<Graph>();
        graph = Inf1.MakeGraph(gType[gt]);
        cloud = Inf1.getCloudId();
        
        for(int ge=0; ge<config.length ; ge++)//based on the number of lambda values different graphs for EPOS Fog are created
            graphForFog.add(Graphs.clone(graph));
    
    //task distribution on the generated graphs:       
        for (int bd = 0 ; bd<bDis.length ; bd++){//based on task distribution methods input workload is distributed on the generated graphs
            graphForCloud = Graphs.clone(graph);
            graphForFF = Graphs.clone(graph);
            AStar aStar = new AStar(graph);   
            
            //Viewer v1,v2,v3 for taking snapshot of graphs- currently this utility is disabled for evaluation on cluster machines without gui
            InTask iot1 = new InTask (PERIODDU, numNod[nn], bDis[bd]);//read input file for getting tasks 
            ArrayList<Map<Integer, Map<Long,Task>>> periodTaskList = new ArrayList<Map<Integer , Map<Long,Task>>>();//newcomer tasks per period which will be associated with network nodes
            ArrayList<Map<Integer, Map<Long,Task>>> CloudTaskList = new ArrayList<Map<Integer , Map<Long,Task>>>();//newcomer tasks per period which will be associated with network nodes
            ArrayList<Map<Integer, Map<Long,Task>>> FFTaskList = new ArrayList<Map<Integer , Map<Long,Task>>>();//newcomer tasks per period which will be associated with network nodes
            Map<Integer,ArrayList<Map<Integer, Map<Long,Task>>>>TList = new HashMap<Integer,ArrayList<Map<Integer, Map<Long,Task>>>>();

            //reading required periods from input file:
            for (i=0 ; i<periodNum ; i++){
                double startT = i*PERIODDU+baseTime;//start time of the period
                double endT = (i+1)*PERIODDU+baseTime;//end time of the period
                periodTaskList.add(i,iot1.makeNewTasks(startT, endT, i, bpath));//a: start timestamp index, b: end timestamp index
                CloudTaskList.add(i,iot1.makeNewTasks(startT, endT, i, bpath));
                FFTaskList.add(i,iot1.makeNewTasks(startT, endT, i, bpath));
            }
            //for each lambda value one particular task list is considered:
            for(int ge=0; ge<config.length ; ge++){
                TList.put(ge, new ArrayList<Map<Integer, Map<Long,Task>>>());
                ArrayList<Map<Integer, Map<Long,Task>>> newTaskList = new ArrayList<Map<Integer , Map<Long,Task>>>();//newcomer tasks per period which will be associated with fog nodes
                Map<Long,Task> tempTaskList = new HashMap<Long,Task>();

            //all periods are run considering each lambda value:hence we use cloning to create independent identical input tasks for each of them
            for (i=0 ; i<periodNum ; i++){
                     Map<Integer, Map<Long,Task>> tTaskList = new HashMap<Integer, Map<Long,Task>>();
                        for(int fkey : periodTaskList.get(i).keySet()){ 
                        tempTaskList=periodTaskList.get(i).get(fkey);
                        Task t1;
                        Map<Long,Task> unsortedTasks = new HashMap<Long,Task>();

                        for (long ikey : tempTaskList.keySet()){
                            try{
                                t1 = (Task)tempTaskList.get(ikey).clone();
                                unsortedTasks.put(ikey,t1);
                            }
                            catch(CloneNotSupportedException c){}  
                        }
                         tTaskList.put(fkey, unsortedTasks);
                        }
                newTaskList.add(i, tTaskList);
            }
            TList.put(ge,newTaskList);
            }
            
            //distribute tasks over the network:
            taskAssignmentDist(gType[gt], numNod[nn], bDis[bd], periodTaskList);
            npath= path+"/loadcolor/fog/";epath = "0";
            Inf1.colorGraph(graphForFog.get(0),npath,epath);//coloring first graph of EPOS Fog with initial loads and capacity of network nodes
            //v1= graphForFog.get(0).display();
            //creating plan object:
            Plan p1= new Plan(numNod[nn], cloud, numOfPlans, PERIODDU, baseTime, aStar, bpath);//note that the inf and its state must not change during all periods. hence, we created graphs as many as needed.

//start the simulation process of EPOS Fog:
            System.out.println("EPOS Fog approach....");
            //three task lists consist of running ,unrunning (tasks without hosts), finished tasks is created for each approach
            for (int l = 0; l<hopConstLevel.length; l++){//host proximity
                Map<Integer,ArrayList<Task>> runningTasks = new HashMap<Integer,ArrayList<Task>>();
                Map<Integer,ArrayList<Task>> unRunningTasks = new HashMap<Integer,ArrayList<Task>>();
                Map<Integer,ArrayList<Task>> finishedTasks = new HashMap<Integer,ArrayList<Task>>();
                for (int gr=0 ; gr<config.length ; gr++){//initializing task lists for each lambda value
                    runningTasks.put(gr, new ArrayList<Task>());
                    unRunningTasks.put(gr, new ArrayList<Task>());
                    finishedTasks.put(gr, new ArrayList<Task>());
                }
                //start running EPOS Fog with respect to different input profiles and lambda values
                for (i=0 ; i<periodNum ; i++){
                    System.out.println("-----------------graph type: "+gt);
                    System.out.println("-----------------numofnode: "+numNod[nn]);
                    System.out.println("-----------------Beta distribution: "+bDis[bd]);
                    System.out.println("-----------------hop-level constraint: "+ hopConstLevel[l]);
                    System.out.println("-----------------period num: "+i);

                    EPOSplan[][][] genPlans = new EPOSplan[config.length][numNod[nn]][numOfPlans];//to save generated plans which are the input to I-EPOS

                    graphForPlan.clear();//in each simulation one particular graph is used for plan generation which is a clone of the graph resulted from applying the plans selected in previous profile
                    for(int ge=0; ge<config.length ; ge++)//to clone the previous graph for new profile
                        graphForPlan.add(ge,Graphs.clone(graphForFog.get(ge)));

       //step one: plan generation:
                    //for the first profile, generated plans (as input for I-EPOS) are the same for all of the lambda values 
                    if (i==0){ 
                        genPlans[0] = p1.generatePlans(i, hopConstLevel[l], graphForPlan.get(0), TList.get(0).get(i), 0.0);
                        for(int f =1; f<config.length;f++)
                            genPlans[f] = genPlans[0];

                    }
                    //for profiles other than the first profile, generated plans are different for each lambda value: because the output of I-EPOS is different according to each lambda in the previous profiles
                    for (int p = 0 ; p<config.length ; p++){
                        System.out.println("-----------------beta value: "+config[p]);
                        if (i!=0){
                            genPlans[p] = p1.generatePlans(i, hopConstLevel[l], graphForPlan.get(p), TList.get(p).get(i), config[p]);
                        }
        //end of first step, the generated plans are written into the intended files    

                        //graphForFog.get(p).addAttribute("ui.screenshot", path+"/screenshots/fog/SSgt"+gType[gt]+"nn"+numNod[nn]+"level"+hopConstLevel[l]+"beta"+config[p]+"period"+i+".jpg");
                        //v1.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
                        //v1.close();

        //step two: plan selection, calling I-EPOS after plan generation to select one of generated plans
                        //the address of input file which contains generated plans has been already set in the I-EPOS config files
                        IEPOSExperiment iepos = new IEPOSExperiment();
                        iepos.main(args);
        //end of second step   
                        
                        ArrayList<Integer> selectedPlansIndex = new ArrayList<Integer>();// to save the output of I-EPOS; index of selected plans
                        selectedPlansIndex.addAll(getInputFromEpos(numNod[nn], num_Iteration, num_Run, gt, nn, bd, l, p, i));
                        //apply the selected plans: running tasks and allocating resources accordingly:
                        FogResults[gt][nn][bd][l][i][p] = p1.applyEpos(graphForFog.get(p), gt, p, i, hopConstLevel[l], selectedPlansIndex, genPlans[p], runningTasks.get(p), unRunningTasks.get(p));//based on outplans from this period we can easily drive assignments...
                        //path to measure the utilization of network nodes in EPOS Fog approach:
                        //epath is set as follows: graphtopology-numberofnodes-taskdistribution(0/1)-hostproximity(1000/3/1)-profilenumber-lambdavalue
                        epath = String.valueOf(gt)+String.valueOf(nn)+String.valueOf(bd)+String.valueOf(l)+String.valueOf(i)+String.valueOf(p);
                        Inf1.colorGraph(graphForFog.get(p), npath, epath);
                        //v1= graphForFog.get(p).display();
                        //after applying the selected plans and running tasks maybe some tasks are finished and need to release their occupied resources at the end of the period:
                        p1.releaseResource(i+1, graphForFog.get(p), runningTasks.get(p), finishedTasks.get(p));
                        //set config parameters with respect to the next profile:
                        copyConfig(gType, numNod, gt, nn, bd, l, p);   

                        //clearTaskList(periodTaskList); 
                    }//end config
                }//end period

                graphForPlan.clear();
                graphForFog.clear();
                for(int ge=0; ge<config.length ; ge++){
                    graphForPlan.add(ge, Graphs.clone(graph));
                    graphForFog.add(ge, Graphs.clone(graph));
                    //clearTaskList(TList.get(ge)); 
                }
                //clearTaskList(periodTaskList); 
            }//end hopconstlevel
            
//start simulating cloud approach for different profiles
            System.out.println("Cloud approach....");
            Cloud C1 = new Cloud(cloud, numNod[nn], PERIODDU, baseTime, aStar,path+"/tasklists/cloud");
            ArrayList<Task> runningTasksOnCloud = new ArrayList<Task>();//all of running tasks in the network
            ArrayList<Task> unHostedTasksOnCloud = new ArrayList<Task>();
            ArrayList<Task> finishedTasksOnCloud = new ArrayList<Task>();

            for (i=0 ; i<periodNum ; i++){
                CloudResults[gt][nn][bd][i] = C1.runCloud(graphForCloud, CloudTaskList.get(i), unHostedTasksOnCloud, runningTasksOnCloud,i);
                cepath =String.valueOf(gt)+String.valueOf(nn)+String.valueOf(bd)+String.valueOf(i);
                Inf1.colorGraph(graphForCloud,cnpath,cepath);
                //v2= graphForCloud.display();
                C1.releaseCloud(i, graphForCloud, runningTasksOnCloud, finishedTasksOnCloud);
                //graphForCloud.addAttribute("ui.screenshot", path+"/screenshots/cloud/gt"+gType[gt]+"nn"+numNod[nn]+"period"+i+".jpg");
                //v2.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
            }
//end of simulating cloud approach
            
//start simulating First Fit approach for different profiles
            System.out.println("First Fit approach....");
            clearTaskList(CloudTaskList);  
            FirstFit FF1 = new FirstFit(numNod[nn], cloud, PERIODDU, baseTime, aStar, path+"/tasklists/ff");
            ArrayList<Task> FFunHostedTasks = new ArrayList<Task>();
            ArrayList<Task> FFfiTasks = new ArrayList<Task>();
            ArrayList<Task> FFrunningTasks = new ArrayList<Task>();

            for (i=0 ; i<periodNum ; i++){
                FFResults[gt][nn][bd][i] = FF1.runFirstFit(graphForFF, FFTaskList.get(i), FFrunningTasks, FFunHostedTasks,i);
                ffepath =String.valueOf(gt)+String.valueOf(nn)+String.valueOf(bd)+String.valueOf(i);
                Inf1.colorGraph(graphForFF, ffnpath, ffepath);
                //v3= graphForFF.display();
                FF1.releaseFirstFit(i+1, graphForFF, FFrunningTasks, FFfiTasks);
                //graphForFF.addAttribute("ui.screenshot", path+"/screenshots/firstfit/gt"+gType[gt]+"nn"+numNod[nn]+"period"+i+".jpg");
                //v3.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
            }
//end of simulating First Fit approach for different profiles
            
            //to check if unwanted modification is occurred for task lists:
            clearTaskList(FFTaskList);
        }//end bd
    }//end nn
}//end gt

//end of simulation of three approaches
//start writing obtained results into excel files:
    AR.analyse(FogResults, CloudResults, FFResults);
    System.out.println("finish");  
    }  
    
    //to set config parameters with respect to the next profile for I-EPOS:
    public static void copyConfig(int[]gTyp, int[] numNo, int g, int numnod, int b,  int ll, int cnum)  {
    
        try{
            
            if (cnum !=(config.length-1)){
                Files.copy(Paths.get(bpath+"/conf/oldFiles"+numNo[numnod]+"/epos"+config[cnum+1]+".properties"), Paths.get(bpath+"/conf/epos.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
            else if(ll!=hopConstLevel.length-1){
                Files.copy(Paths.get(bpath+"/conf/oldFiles"+numNo[numnod]+"/epos"+config[0]+".properties"), Paths.get(bpath+"/conf/epos.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
            else if(b!=bDis.length-1){
                Files.copy(Paths.get(bpath+"/conf/oldFiles"+numNo[numnod]+"/epos"+config[0]+".properties"), Paths.get(bpath+"/conf/epos.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
            else if(numnod!=numNo.length-1){
                Files.copy(Paths.get(bpath+"/conf/oldFiles"+numNo[numnod+1]+"/epos"+config[0]+".properties"), Paths.get(bpath+"/conf/epos.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
            else if(g!= gTyp.length){
                Files.copy(Paths.get(bpath+"/conf/oldFiles"+numNo[0]+"/epos"+config[0]+".properties"), Paths.get(bpath+"/conf/epos.properties"), StandardCopyOption.REPLACE_EXISTING);
            }
 
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
    
    //to measure the assignment of input tasks to each network nodes regarding task distribution method:
    public static void taskAssignmentDist(int graphT, int numNode, boolean betaDist, ArrayList<Map<Integer, Map<Long,Task>>> Tasks){
                            
        String taskDis = bpath+"/plots/taskDist/taskDistgraph"+graphT+"node"+numNode+"beta"+betaDist+".csv";
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        try 
        {    
            fileWriter = new FileWriter(taskDis);
                for (int j = 0; j<numNode ; j++){
                    fileWriter.append(String.valueOf(j));
                    fileWriter.append(COMMA_DELIMITER);
                    for (int p = 0; p<periodNum ; p++){
                        if (Tasks.get(p).containsKey(j)){
                            fileWriter.append(String.valueOf(Tasks.get(p).get(j).size()));
                        }
                        else{
                            fileWriter.append(String.valueOf(0));
                        }
                        fileWriter.append(COMMA_DELIMITER);
                    }
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
            catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }

        }
    }
    //to check if any unwanted modification happened to the input tasklist
    public static void clearTaskList(ArrayList<Map<Integer, Map<Long,Task>>> peTaskList){
        for (int i=0 ; i<periodNum ; i++){
            Map<Integer, Map<Long,Task>> taskL;
            taskL = peTaskList.get(i);
            for(int fkey : taskL.keySet()){   
                Map<Long,Task> Tasks;
                Tasks  = taskL.get(fkey);
                for (long l : Tasks.keySet()){
                    Task t = Tasks.get(l);    
                    t.diffDlWt = 0.0;
                    t.dl_viol = 0;
                    t.finished = 0;
                    t.hostNode = 0;
                    t.serviceTime =0.0;
                    t.service_delay =0.0 ;
                    Tasks.replace(l, t);
                }
                taskL.replace(fkey, Tasks);
            }
        }
        
    }
    //get the index of selected plans from I-EPOS:
    public static ArrayList<Integer> getInputFromEpos(int numNod, int iteration, int run, int g, int n, int d, int le, int c, int p){
    
        System.out.println("reading input from EPOS......\n");
        ArrayList<Integer> selectedPlans = new ArrayList<Integer>();// output of I-EPOS; index of selected plans
        File dir = new File(bpath+"/output");
        File[] files = dir.listFiles();
        //to simply check the last output folder of I-EPOS which contains the output of last recent run:
        File lastModified = Arrays.stream(files).filter(File::isDirectory).max(Comparator.comparing(File::lastModified)).orElse(null);
        System.out.println(lastModified);
        /*
        * In addition to the selected plans, the global-cost (utilization variance) of the selected plans and corresponding local-cost 
        *is stored for further comparison with EPOS Fog results: 
        *the realized variance and the predicted one
        **/
        String global_cost_File =lastModified+"/global-cost.csv";
        String local_cost_File =lastModified+"/local-cost.csv";
        String selected_plans_File =lastModified+"/selected-plans.csv";
        String line = "";
        String cvsSplitBy = ",";
        int i = 1;
        int minRun=0;
        int index=0;
        double loc_costs=0.0;
        double[] costs= new double[run];
        //extract the iteration and run number with the minimum global-cost
        try (BufferedReader br = new BufferedReader(new FileReader(global_cost_File))) 
        {
            // br.readHeaders();
            for (i = 0; i < iteration; i++)
                br.readLine();
            line = br.readLine();
            String[] input = line.split(cvsSplitBy);
            for (i = 0; i < run; i++)
                costs[i] = Double.parseDouble(input[3+i]);

            minRun = findMinRun(costs, run);
            index = iteration*minRun + iteration;
            //System.out.println(" minRun0: "+minRun);
                
        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }  
        //read the corresponding local-cost with the glocal-cost
        try (BufferedReader br = new BufferedReader(new FileReader(local_cost_File))) 
        {
            //System.out.println(" minRun1: "+minRun);
            for (i = 0; i < iteration; i++)
                br.readLine();
            line = br.readLine();
            String[] input = line.split(cvsSplitBy);
            loc_costs = Double.parseDouble(input[3+minRun]);

        }
        catch (IOException e) {
            e.printStackTrace();
        }
        //find the selected plan index for each agent/node:
        try (BufferedReader br = new BufferedReader(new FileReader(selected_plans_File))) 
        {
            // br.readHeaders();
            for (i = 0; i < index; i++)
                br.readLine();
            line = br.readLine();//index+1
            String[] input = line.split(cvsSplitBy);
            for (i = 0; i < numNod; i++){
                selectedPlans.add(Integer.parseInt(input[2+i]));
            }

        }
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    
    writeGC(costs[minRun],loc_costs, g, n, d, le, c, p); 
    return selectedPlans;
    
}

    public static int findMinRun(double[] globalCosts, int index){
        int mini = 0;
        double min = globalCosts[0];
        for (int j = 0; j<index; j++)
            if (globalCosts[j] < min){
                min = globalCosts[j];
                mini = j;
            }
        return mini;
    }
    
    public static void writeGC(double gc, double lc, int gw, int nw, int dw, int lw, int cw, int pw){
        boolean append_value = true;
        String file_path = bpath+"/plots/gc/gc.csv";
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        try 
        {    
            fileWriter = new FileWriter(file_path, append_value);
            fileWriter.append(String.valueOf(gType[gw]));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(numNod[nw]));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(bDis[dw]));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(hopConstLevel[lw]));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(pw));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(config[cw]));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(gc));
            fileWriter.append(COMMA_DELIMITER);
            fileWriter.append(String.valueOf(lc));
            fileWriter.append(NEW_LINE_SEPARATOR);
            
            System.out.println("CSV file was created successfully !!!");
             
        } catch (Exception e) 
        {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        } 
        finally 
        {
            try {
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
