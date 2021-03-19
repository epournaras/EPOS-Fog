/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;


/**
 *
 * @author zeinab
 * task objects as the processing element of input workload
 */
public class Task implements Cloneable {
    
	public Long taskId;// unique id for each task
    public double taskArrivTime;// time of arrival of each task to the system
    public double taskExeTime;// execution time for each task
    //resource demands in terms of resource unit:
    public double taskDProc;// processing power demand of each task
    public double taskDMem;// memory demand of each task
    public double taskDStorage;// storage demand of each task
    public  double waitingTime;// waiting time of each task in terms of milisecond
    public int assignedNode = -1;//the receiver node for a task (node associated)
    public int hostNode ;// destination (host) node to run the task
    public double taskDL; // deadline of each task in terms of milisecond
    public double diffDlWt;// the difference between task deadline and its waiting time
    int reqPerSec = 1;// number of requests for each task per second (can be selected randomly). not used
    public  int runPeriod;// the period that the task is run. not used
    double serviceTime;// time of service execution in milisecond
    double service_delay;// the difference between the time a task is requested and the time of starting it in milisecond
    int dl_viol=0;// whether a task deadline is violated
    int finished = 0;//whether a task is finished
        
    //the following input time parameters are in terms of minute,milisecond,milisecond,milisecond respectively:
    public Task(double ArrivTime, double Proc, double Mem, double Storage, long id, double ExeTime, double waitTime, double DL, int assFogId, double difT){
        taskId = id;
        taskArrivTime = ArrivTime;//minute
        taskExeTime = ExeTime;//milisecond
        taskDProc = Proc;
        taskDMem = Mem;
        taskDStorage = Storage;
        taskDL = DL;
        assignedNode = assFogId;
        waitingTime = waitTime;//milisecond
        if(difT<0.0)
            diffDlWt =Math.abs(difT)*100;
        else
            diffDlWt = difT;
    }
        
    Task(){}
        
    public Object clone() throws CloneNotSupportedException {
        try
            {
                Task clonedMyClass = (Task)super.clone();
                return super.clone();
            } 
        catch (CloneNotSupportedException e) 
            {
                e.printStackTrace();
                return e;
            }
    }
    public double getexetime(){
        return taskExeTime;
    }
    public double getDProc(){
        return taskDProc;
    }
    public double getDMem(){
        return taskDMem;
    }
    public double getDStorage(){
        return taskDStorage;
    }
    public long gettaskId(){
        return taskId;
    }
    public double getArrivTime(){
        return taskArrivTime;
    }
    public int getassignedNode(){
        return assignedNode;
    }
    public int gethostNode(){
        return hostNode;
    }        
    public double gettaskDL(){
            return taskDL;
    }
    public void setassNode(int aN){
        assignedNode = aN;
    }
    public void sethostNode(int hN){
        hostNode = hN;
    }
    public void settaskDL(double dl){
        taskDL = dl;
    }
}
