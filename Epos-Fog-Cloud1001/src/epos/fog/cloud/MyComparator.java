
package epos.fog.cloud;
import java.util.Comparator;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author zeinab
 */
public class MyComparator implements Comparator<Task> {
        @Override
        public int compare(Task t1, Task t2) {
            return Double.valueOf(t1.getexetime()).compareTo(Double.valueOf(t2.getexetime()));//or just compare the primitives - must be sorted on dl-wt
        }
                
}
