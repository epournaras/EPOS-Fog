/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import static org.graphstream.algorithm.Toolkit.*;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;


/**
 *
 * @author zeinab
 * This file produces an infrastructure consists of various networks with different topologies. 
 * Each network consists of several fog nodes and one cloud data center.
 * Every topology has its generator algorithm: 
 * BarabasiAlbertGenerator (scale-free graphs for Internet network and social network), 
 * WattsStrogatzGenerator (small-world graph), and Erdos-Renyi (random graph)
 * After the generation, resource capacities for each node is identified according to Google cluster trace.
**/
    /* Google cluster consists of 12,500 machines and 12MB tasks.
    *The profiles that is considered for this evaluation has 89354 tasks.
    *11 types of nodes in each network are: {10 types fog node plus one type cloud node}. 
    *Each input task from the dataset is assigned to one edge node.
    *The id of the node which a task is assigned to is set already according to Beta and random distributions.

    machine types, their probabilities, and capacities regarding to Google cluster data:
        class cloud: cloud                      CPU:10.0 MEM:10.0       
        class B1:    fog    color: pink         CPU:0.5  MEM:0.03      prob=0.0004
        class B2:    fog    color: pink         CPU:0.5  MEM:0.06      prob=0.00008
        class B4:    fog    color: violet       CPU:0.5  MEM:0.12      prob=0.00416
        class A:     fog    color: violet       CPU:0.25 MEM:0.25      prob=0.01008
        class B6:    fog    color: purple       CPU:0.5  MEM:0.25      prob=0.30904
        class B7:    fog    color: purple       CPU:0.5  MEM:0.5       prob=0.53856
        class B5:    fog    color: blue         CPU:0.5  MEM:0.75      prob=0.08008
        class B3:    fog    color: blue         CPU:0.5  MEM:0.97      prob=0.00040
        class C2:    fog    color: blue         CPU:1    MEM:0.5       prob=0.00024
        class C1:    fog    color: blue         CPU:1    MEM:1         prob=0.0636
       

        average of task demands (unit of resource) on 10 (5-min) periods from minute 10 to minute 60:
                  CPU    Mem     Storage
            avg:  100    124     2
            max:  248    347     3.3

            520 < number of tasks < 9527   avg: 3908
            
the total capacity (unit of resource) of each network is:
        total CPU capacity: 704.0
        total Memory capacity: 792.5
        total storage capacity: 313.5
        
    */


public class Infrastructure {
    //static Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
    private final int numOfNodes;
    private final int graphNumber;
    private final String graphType;
    private int cId;
        
    //coloring nodes in the topology based on their capacities
    protected  String  styleSheet =
                                "node {" +
                                "	fill-color: black;" +
                                "}" +
                                "node.black {" +
                                "	fill-color: black;" +
                                "}" +
                                "node.purple {" +
                                "	fill-color: purple;" +
                                "}" +
                                "node.blue {" +
                                "       fill-color: blue;" +
                                "}"  +
                                "node.violet {" +
                                "       fill-color: violet;" +
                                "}" +
                                "node.red {" +
                                "	fill-color: red;" +
                                "}" +
                                "node.yellow {" +
                                "	fill-color: yellow;" +
                                "}" +
                                "node.orange {" +
                                "	fill-color: orange;" +
                                "}" +
                                "node.green {" +
                                "	fill-color: green;" +
                                "}" +
                                "node.pink {" +
                                "	fill-color: pink;" +
                                "}"  ;

    Infrastructure(int numNod, int Graphnum, String graphTp)
    {
        numOfNodes = numNod;
        graphNumber = Graphnum;
        graphType = graphTp;
    }
    public Graph MakeGraph(int graphT){
        
        int l = 0;
        int k = 0;
        int i = 0;
        int sum = 0;
        double totStorage=0.0;
        double totMem=0.0;
        double totCPU=0.0;
        int maxDegree = 0;
        int minDegree = Integer.MAX_VALUE;
        int numOfNodeType[] = new int[10];
        double[] assWL = new double[]{0.0,0.0,0.0};
        double[] CloudCap = new double[]{400,500,200};//capacity of cloud node
        double capacityIF = 1.0 ;//an impact factor to set the capacities of network node
        //set the number of nodes of each type/capacity according to the Google cluster machines:
        double machineProbability[] = new double[] {0.0636, 0.00024, 0.0004, 0.08008, 0.53856, 0.01008, 0.31004, 0.00416, 0.00008, 0.0004};//10
        String[] nodeClass = new String []{"C1","C2","B1","B2","B7","A","B3","B4","B5","B6"};//10 values
        //for graph coloring purpose:
        String[] uiClass = new String []{"blue","blue","purple","purple","blue","pink","violet","violet","pink","pink"};//10
        double[][] nodeCap = new double [][]{ //{Cpu, Mem, Storage};
                                                {2.00,2.00,1.0},
                                                {2.00,1.50,1.0},
                                                {1.50,1.97,1.0},
                                                {1.50,1.75,1.0},
                                                {1.50,1.50,0.50},
                                                {1.25,1.25,0.50},
                                                {1.50,1.25,0.50},
                                                {1.50,1.12,0.25},
                                                {1.50,1.06,0.25},
                                                {1.50,1.03,0.25},
                                                };//10 arrays
        
        
        Graph graph = new SingleGraph("This is a small-world/Random/BarabasiAlbertGenerator");
        System.out.println("generating nodes.....");
        
        //BarabasiAlbert graph:
        if(graphT == 0){
            //Graph graph = new SingleGraph(graphType);
            Generator gen = new BarabasiAlbertGenerator(1); // Between 1 and 3 new links per node added.
            gen.addSink(graph); 
            gen.begin();
            for(i=0; i<numOfNodes-2; i++) {//the actual number of generated nodes is numOfNodes; Since Barabasi by default produce 2 nodes at initializing graph this modification is necassary.
                gen.nextEvents();// each event produce one node (node id: 0 to numOfNodes-1)
            }
            gen.end();
        }
        /*Small-world graph :
        WattsStrogatzGenerator(n, k , beta).
        You must provide values for n, k and beta at construction time. 
        You must ensure that k is even, that n Â» k Â» log(n) Â» 1. 
        Furthermore, beta being a probability it must be between 0 and 1.
        */
        //generate suitable k value for each network size:
        if (graphT == 1){
            switch (numOfNodes) { 
                case 200: k = 6; break;
                case 400:
                case 600: k = 8; break;
                case 800: 
                case 1000: k = 10; break;
            }
            Generator gen = new WattsStrogatzGenerator(numOfNodes, k , 0.5);
            gen.addSink(graph);
            gen.begin();
            while(gen.nextEvents()){}
            gen.end();
            //return graph;
        }
        /*Erdos-Renyi­ random graph:
            After n - k steps we obtain a Erdos-Renyi­ random graph G(n, p) with p = k / (n - 1). 
            In other words the result is the same as if we started with n isolated nodes and 
            connected each pair of them with probability p.
        */
        else if(graphT == 2){
        // Since usually the generated graph is disconnected we should take care of its input parameters to generate enough links between nodes.
            int lnp = 3*(int)Math.ceil(Math.log(numOfNodes));
            System.out.println("lnp "+lnp);
            Generator gen = new RandomGenerator(lnp, false);
            gen.addSink(graph);
            gen.begin();
            while (graph.getNodeCount() < numOfNodes && gen.nextEvents());
            gen.end();
        }
        
        //determine required number of nodes of each type using :machineprobability*numOfNodes (note that cloud is not included in the output array)
        if (numOfNodes == 200){
            for (i=0;i<10;i++){
                numOfNodeType[i] = (int) Math.floor(machineProbability[i]*numOfNodes);//numOfNodes = fog+cloud(1)
                sum+=numOfNodeType[i];
            }
            capacityIF = 1.0; 
        }
        else if(numOfNodes == 400){
            for (i=0;i<10;i++){
            numOfNodeType[i] = (int) Math.floor(machineProbability[i]*(numOfNodes-0.5));
            sum+=numOfNodeType[i];
            }
            capacityIF = 0.6;
        }
        else if(numOfNodes == 600){
            for (i=0;i<10;i++){
            numOfNodeType[i] = (int) Math.floor(machineProbability[i]*(numOfNodes-2.4));
            sum+=numOfNodeType[i];
            }
            capacityIF = 0.5;
        }
        else if(numOfNodes == 800){
            for (i=0;i<10;i++){ 
            numOfNodeType[i] = (int) Math.floor(machineProbability[i]*(numOfNodes-3.4));
            sum+=numOfNodeType[i];
            }
            capacityIF = 0.4;
        }
        else if(numOfNodes == 1000){
            for (i=0;i<10;i++){
            numOfNodeType[i] = (int) Math.floor(machineProbability[i]*(numOfNodes-5));
            sum+=numOfNodeType[i];
            }
            capacityIF = 0.3;
        }
        System.out.println("sum of network nodes:"+sum);

        //to return a sorted list of nodes in graph regarding to their degree in descendant order (we assume that the higher the number of edges the node is more powerful):
        ArrayList<Node> nodeList = degreeMap(graph);
        //set attributes for Cloud node; the first node at nodeList with max degree.
        cId=Integer.parseInt(nodeList.get(0).getId());//return cloud's id
        graph.getNode(cId).addAttribute("class");                     
        graph.getNode(cId).addAttribute("ui.class"); 
        graph.getNode(cId).addAttribute("ui.label");//id of the node for visualization
        graph.getNode(cId).addAttribute("capacity");//whole capacity(Cpu,Mem,Storage)            
        graph.getNode(cId).addAttribute("assignedWorkLoad");//initial allocated workload
        graph.getNode(cId).setAttribute("class", "Cloud");                     
        graph.getNode(cId).setAttribute("ui.class", "black"); 
        graph.getNode(cId).setAttribute("ui.label", cId);
        graph.getNode(cId).setAttribute("capacity", CloudCap);                
        graph.getNode(cId).setAttribute("assignedWorkLoad", assWL);
        
        
        totCPU += CloudCap[0];
        totMem += CloudCap[1];
        totStorage += CloudCap[2];
        double[] nodeap=new double [3];
        l=0;
        //set the attributes of fog nodes regarding to machineprobability and nodes' degree
        for(int j=0 ; j<numOfNodeType.length ; j++){
            double[] newNodCap = new double[]{0.0,0.0,0.0};
            int nodeCount = numOfNodeType[j];
            for(i=0;i<nodeCount;i++){
                newNodCap[0] = capacityIF*nodeCap[j][0];
                newNodCap[1] = capacityIF*nodeCap[j][1];
                newNodCap[2] = capacityIF*nodeCap[j][2];
                l++;
                int nodei = nodeList.get(l).getIndex();
                //graph.getNode(nodeList.get(l).getId()).setAttribute(styleSheet, nodeCap);
                graph.getNode(nodei).addAttribute("class");
                graph.getNode(nodei).addAttribute("ui.label");//id of the node for visualization
                graph.getNode(nodei).addAttribute("ui.class");
                graph.getNode(nodei).addAttribute("capacity");//whole capacity(Cpu,Mem,Storage)
                graph.getNode(nodei).addAttribute("assignedWorkLoad");//initial assigned workload 
                graph.getNode(nodei).setAttribute("class", nodeClass[j]);
                graph.getNode(nodei).setAttribute("ui.label", nodeList.get(l).getId());
                graph.getNode(nodei).setAttribute("ui.class", uiClass[j]);
                graph.getNode(nodei).setAttribute("capacity", newNodCap);
                graph.getNode(nodei).setAttribute("assignedWorkLoad", assWL);
                nodeap =graph.getNode(nodei).getAttribute("capacity");

                totCPU+=newNodCap[0];
                totMem+=newNodCap[1];
                totStorage+=newNodCap[2];
                         
            }
        }

            System.out.println("number of nodes over the network: "+(l+1));
            System.out.println("total CPU capacity: "+totCPU);
            System.out.println("total Memory capacity: "+totMem);
            System.out.println("total storage capacity: "+totStorage);
           
            graph.addAttribute("ui.stylesheet", styleSheet);
            graph.display();
            
        return graph;
    }
    
  //color graph in terms of the utilization (allocated load to capacity ration) of its nodes and write the utilization
    public void colorGraph(Graph g, String np, String ep){
        
        double[] asswl, cap;
        double ratio;
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        String DL = np+"load"+ep+".csv";
        String FILE_HEADER2 = "node,utilization";
                 
        try 
        {
            fileWriter = new FileWriter(DL);
            fileWriter.append(FILE_HEADER2.toString());
            fileWriter.append(NEW_LINE_SEPARATOR);
            
            for(int i=0;i<numOfNodes;i++){//calculate cpu utilization (cpu-load/capacity)
                asswl = new double[3];
                cap  = new double[3];  
                asswl = g.getNode(i).getAttribute("assignedWorkLoad");
                cap = g.getNode(i).getAttribute("capacity");
                ratio = asswl[0]/cap[0];
                if (ratio<0.2){
                    //g.getNode(i).addAttribute("ui.label", g.getNode(i).getId());
                    g.getNode(i).setAttribute("ui.class", "green");
                }
                else if(ratio<0.4){
                    //g.getNode(i).addAttribute("ui.label", g.getNode(i).getId());
                    g.getNode(i).setAttribute("ui.class", "blue");
                }
                else if(ratio<0.6){
                    //g.getNode(i).addAttribute("ui.label", g.getNode(i).getId());
                    g.getNode(i).setAttribute("ui.class", "pink");
                }
                else if(ratio<0.8){
                    //g.getNode(i).addAttribute("ui.label", g.getNode(i).getId());
                    g.getNode(i).setAttribute("ui.class", "orange");
                }
                else {
                    //g.getNode(i).addAttribute("ui.label", g.getNode(i).getId());
                    g.getNode(i).setAttribute("ui.class", "red");
                }

                fileWriter.append(String.valueOf(i));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(ratio));
                fileWriter.append(NEW_LINE_SEPARATOR);
            }
                  
            for(int i=0;i<numOfNodes;i++){//calculate memory ratio (load/capacity)
                 asswl = new double[3];
                 cap  = new double[3]; 
                 asswl = g.getNode(i).getAttribute("assignedWorkLoad");
                 cap = g.getNode(i).getAttribute("capacity");
                 ratio = asswl[1]/cap[1];

                fileWriter.append(String.valueOf(i));
                fileWriter.append(COMMA_DELIMITER);
                fileWriter.append(String.valueOf(ratio));  
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
     
    public int getCloudId(){
        return cId;
    }
    public int getnumofnodes(){
        return numOfNodes;
    }
    public int getgraphNumber(){
        return graphNumber;
    }
    public String getgraphType(){
        return graphType;
    }
    
}

//        nodeList.get(0).setAttribute("class", "Cloud");                     
//        nodeList.get(0).setAttribute("ui.class", "black"); 
//        nodeList.get(0).setAttribute("ui.label", cId);
//        nodeList.get(0).setAttribute("capacity", CloudCap);                
//        nodeList.get(0).setAttribute("assignedWorkLoad", assWL);
//        System.out.println("cloud  "+"id "+nodeList.get(0).getId());
//        //int n = graph.getNodeCount();
//for(Node n2 : graph.getEachNode() ) {
//            int deg = n2.getDegree();
//            maxDegree = Math.max(maxDegree, deg);
//            minDegree = Math.min(minDegree, deg);
//            }   
 

//Based on different deadlines we can have 22 service type. each node has an attribute named deployedServices which shows the services on it. Cloud has all the services.
        //nodeList.get(0).setAttribute("deployedServices", deployedServicesOnCloud);  //service types which are already deployed on each node. So, for each node 1 means the service type is already exist and
   

/*to assign other parameters or attributes to the nodes or services; not used
    private int[] serviceSelection(int numOfSS){
        int i;
        int minVal = 0;
        int maxVal = 1;
        int[] a = new int[numOfSS];
        for (i= 0 ;i<numOfSS ; i++){
            Random r = new Random();
            a[i] = r.nextInt((maxVal - minVal) + 1) + minVal;
        }
        return a;
    }
    */