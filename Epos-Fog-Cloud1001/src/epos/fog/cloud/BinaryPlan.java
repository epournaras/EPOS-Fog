/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;

/**
 *
 * @author zeinab
 * BinaryPlan identifies which task is hosted on which host
 */
public class BinaryPlan {
    Task T;
    int hostNodeId;
    int value;//if (value = 0) hostNodeId is invalid, if (value = 1) hostNodeId is valid 
    
    BinaryPlan(Task task, int hostId, int v){
        T = task;
        hostNodeId = hostId;
        value = v;
    }

	public void setT(Task t) {
		T = t;
	}

	public void setHostNodeId(int hostNodeId) {
		this.hostNodeId = hostNodeId;
	}

	public void setValue(int value) {
		this.value = value;
	}
    
}
