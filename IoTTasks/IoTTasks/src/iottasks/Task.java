/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Zeinab
 */
package iottasks;
public class Task {
        public Long taskId;
        public double taskArrivTime;
        public double taskExeTime;
        public double taskDProc;
        public double taskDMem;
        public double taskDStorage;
        public  double waitingTime;//in milisecond
        //public int taskType = 0;
        public int assignedNode = -1;//the edge node associated to the end-device/user to send tasks to.
        public int hostNode ;//target/host node to run task on it.
        public double taskDL; //milisecond
        public double diffDlWt;
        int reqPerSec = 1;//random (0-120)
        public  int runPeriod;//its excessive
        double serviceTime;//milisecond
        double service_delay;//milisecond
        int dl_viol=0;
        int finished = 0;
        //min,milli-second,milisecond,milisecond
        Task(double ArrivTime, double Proc, double Mem, double Storage, long id, double ExeTime, double waitTime, double DL, int assFogId, double difT){
            taskId = id;
            taskArrivTime = ArrivTime;//min
            taskExeTime = ExeTime;//milisecond
            taskDProc = Proc;
            taskDMem = Mem;
            taskDStorage = Storage;
            //taskType = Type;
            taskDL = DL;
            assignedNode = assFogId;
            waitingTime = waitTime;//milisecond
            if(difT<0.0)
                diffDlWt =Math.abs(difT)*100;
            else
                diffDlWt = difT;
        }
        
        Task(double ArrivTime, double Proc, double Mem, double Storage, long id, double ExeTime, double waitTime, double DL, double difT){
            taskId = id;
            taskArrivTime = ArrivTime;//min
            taskExeTime = ExeTime;//milisecond
            taskDProc = Proc;
            taskDMem = Mem;
            taskDStorage = Storage;
            //taskType = Type;
            taskDL = DL;
            //assignedNode = assFogId;
            waitingTime = waitTime;//milisecond
            if(difT<0.0)
                diffDlWt =Math.abs(difT)*100;
            else
                diffDlWt = difT;
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

