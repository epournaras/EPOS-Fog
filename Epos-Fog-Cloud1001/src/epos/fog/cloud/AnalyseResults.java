/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package epos.fog.cloud;

//import static epos.fog.cloud.EposFogCloud.periodNum;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import experiment.IEPOSExperiment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.Graphs;
import java.io.IOException;
import java.io.FileWriter;

/**
 *
 * @author zeinab
 * This class writes the resulted metrics in the excel files for further evaluation.
 */
public class AnalyseResults {
    static String prePath;
    static int[] gt;
    static int[] nn;
    static double[] bb;
    static int[] ll;
    int pp;
    boolean[] tDis;
    AnalyseResults(String path, int[] gT, int[] nN, boolean[] Dis, int[] le, double[] con, int p){
        prePath = path;//path to write files
        gt = gT;//graph topology
        nn = nN;//number of nodes
        bb = con;//lambda value
        ll = le;//host-proximity parameter
        pp = p;//time slot/period
        tDis = Dis;//task distribution method
    }
    
    AnalyseResults(){
        //prePath = path;
    }
    
  //calculate and write performance results for evaluated approaches: EPOF Fog, Cloud, and First Fit
    public void analyse(EPOSplan[][][][][][] Fog1, EPOSplan[][][][] Cloud1 , EPOSplan[][][][] FF1){
        
        analyseSDDL(Fog1, Cloud1, FF1);
        analyseSDDLperiodic(Fog1, Cloud1, FF1);
        analyseVarUtil(Fog1, Cloud1, FF1);
        analysefog(Fog1);
        
    }
    //write service execution delay and deadline violation rate for three evaluated approaches:
    public void analyseSDDL(EPOSplan[][][][][][] Fog, EPOSplan[][][][] Cloud, EPOSplan[][][][] FF){
    //EPOS Fog: average on all of the requested tasks for different time periods
    //First Fit, cloud:: average on all of the requested tasks and all time periods
    //header format:
    //nodenum	AVG(dlV)CloudDisfalse	AVG(dlV)FFDisfalse	FDisfalselevel1000per0	FDisfalselevel1000per1	FDisfalselevel1000per2	FDisfalselevel1000per3	FDisfalselevel1000per4

        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        //deadline violation rate:
        for (int g=0; g<gt.length; g++){
            String DL = prePath+"/sddl/gt"+g+"Dl.csv";
            String FILE_HEADER2 = "nodenum,";
            try 
            {
                fileWriter = new FileWriter(DL);
                fileWriter.append(FILE_HEADER2.toString());
                for(int d = 0; d<tDis.length; d++){
                    fileWriter.append("AVG(dlV)CloudDis"+tDis[d]);
                    fileWriter.append(COMMA_DELIMITER); 
                    fileWriter.append("AVG(dlV)FFDis"+tDis[d]);
                    fileWriter.append(COMMA_DELIMITER); 
                }
                for(int d = 0; d<tDis.length; d++){
                    for(int l = 0; l<ll.length; l++){
                        for(int p = 0; p<pp; p++){
                            fileWriter.append("FDis"+tDis[d]+"level"+ll[l]+"per"+p);
                            fileWriter.append(COMMA_DELIMITER);
                        }
                    }
                }
                fileWriter.append(NEW_LINE_SEPARATOR);

                for (int n =0; n<nn.length; n++) {
                    fileWriter.append(String.valueOf(nn[n]));
                    fileWriter.append(COMMA_DELIMITER);
                    for(int d = 0; d<tDis.length; d++){
                        double avgC = 0.0;double avgFF = 0.0;
                        double totalC = 0.0;double totalFF = 0.0;
                        for(int p = 0; p<pp; p++){
                            avgC = avgC+Cloud[g][n][d][p].dlViol;
                            totalC = totalC+Cloud[g][n][d][p].assToCloud;
                            avgFF = avgFF+FF[g][n][d][p].dlViol;
                            totalFF = totalFF+FF[g][n][d][p].assTasks;
                        }
                        if (totalC !=0.0) avgC = (avgC/pp)/totalC;
                        if (totalFF !=0.0) avgFF = (avgFF/pp)/totalFF;
                        fileWriter.append(String.valueOf(avgC));
                        fileWriter.append(COMMA_DELIMITER);
                        fileWriter.append(String.valueOf(avgFF));
                        fileWriter.append(COMMA_DELIMITER);
                    }
                    for(int d = 0; d<tDis.length; d++){    
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                double totalF = 0.0;double avgF = 0.0;
                                    for(int b = 0; b<bb.length; b++){
                                        avgF = avgF+Fog[g][n][d][l][p][b].dlViol;
                                        totalF = totalF+Fog[g][n][d][l][p][b].assTasks;
                                    }
                                    avgF = (avgF/bb.length)/totalF;
                                    fileWriter.append(String.valueOf(avgF));
                                    fileWriter.append(COMMA_DELIMITER);
                            }
                        }
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
                catch (IOException e) 
                {
                    System.out.println("Error while flushing/closing fileWriter !!!");
                    e.printStackTrace();
                }
             
            }
        
        }
    
        //service delay/response time:
        for (int g=0; g<gt.length; g++){
            String SD = prePath+"/sddl/gt"+g+"SD.csv";
            String FILE_HEADER2 = "nodenum,";
            try 
            {
                fileWriter = new FileWriter(SD);
                fileWriter.append(FILE_HEADER2.toString());
                for(int d = 0; d<tDis.length; d++){
                    fileWriter.append("AVG(SD)CloudDis"+tDis[d]);
                    fileWriter.append(COMMA_DELIMITER); 
                    fileWriter.append("AVG(SD)FFDis"+tDis[d]);
                    fileWriter.append(COMMA_DELIMITER); 
                }
                for(int d = 0; d<tDis.length; d++){
                    for(int l = 0; l<ll.length; l++){
                        for(int p = 0; p<pp; p++){
                            fileWriter.append("FDis"+tDis[d]+"level"+ll[l]+"per"+p);
                            fileWriter.append(COMMA_DELIMITER);
                        }
                    }
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
                for (int n =0; n<nn.length; n++) {
                    fileWriter.append(String.valueOf(nn[n]));
                    fileWriter.append(COMMA_DELIMITER);
                    for(int d = 0; d<tDis.length; d++){
                        double avgC = 0.0;double avgFF = 0.0;
                        double totalC = 0.0; double totalFF=0.0;
                        for(int p = 0; p<pp; p++){
                            avgC = avgC+Cloud[g][n][d][p].SD;
                            totalC = totalC+Cloud[g][n][d][p].assToCloud;
                            avgFF = avgFF+FF[g][n][d][p].SD;
                            totalFF = totalFF+FF[g][n][d][p].assTasks;
                        }
                        if (totalC !=0.0) avgC = (avgC/pp)/totalC;
                        if (totalFF !=0.0) avgFF = (avgFF/pp)/totalFF;
                        fileWriter.append(String.valueOf(avgC));
                        fileWriter.append(COMMA_DELIMITER);
                        fileWriter.append(String.valueOf(avgFF));
                        fileWriter.append(COMMA_DELIMITER);
                    }
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                double avgF = 0.0;double totalF =0.0;
                                for(int b = 0; b<bb.length; b++){
                                    avgF = avgF+Fog[g][n][d][l][p][b].SD;
                                    totalF = totalF+Fog[g][n][d][l][p][b].assTasks;
                                }
                                avgF = (avgF/bb.length)/totalF;
                                fileWriter.append(String.valueOf(avgF));
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
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
                catch (IOException e) 
                {
                    System.out.println("Error while flushing/closing fileWriter !!!");
                    e.printStackTrace();
                }
            }
        }
    }
    //
    public void analyseSDDLperiodic(EPOSplan[][][][][][] Fog, EPOSplan[][][][] Cloud, EPOSplan[][][][] FF){
    //EPOS Fog: average on all of the requested tasks for different periods
    //First Fit, cloud:: average on all of the requested tasks for different periods
    //header format:
    //nodenum	AVG(dlV)CloudDisfalseper0	AVG(dlV)FFDisfalseper0	AVG(dlV)CloudDisfalseper1	AVG(dlV)FFDisfalseper1	AVG(dlV)CloudDisfalseper2	AVG(dlV)FFDisfalseper2	AVG(dlV)CloudDisfalseper3	AVG(dlV)FFDisfalseper3	AVG(dlV)CloudDisfalseper4	AVG(dlV)FFDisfalseper4	FDisfalselevel1000per0beta0.0	FDisfalselevel1000per0beta0.1	FDisfalselevel1000per0beta0.2	FDisfalselevel1000per0beta0.3	FDisfalselevel1000per0beta0.4	FDisfalselevel1000per0beta0.5	FDisfalselevel1000per0beta0.6	FDisfalselevel1000per0beta0.7	FDisfalselevel1000per0beta0.8	FDisfalselevel1000per0beta0.9	FDisfalselevel1000per0beta1.0	FDisfalselevel1000per1beta0.0	FDisfalselevel1000per1beta0.1	FDisfalselevel1000per1beta0.2	FDisfalselevel1000per1beta0.3	FDisfalselevel1000per1beta0.4	FDisfalselevel1000per1beta0.5	FDisfalselevel1000per1beta0.6	FDisfalselevel1000per1beta0.7	FDisfalselevel1000per1beta0.8	FDisfalselevel1000per1beta0.9	FDisfalselevel1000per1beta1.0	FDisfalselevel1000per2beta0.0	FDisfalselevel1000per2beta0.1	FDisfalselevel1000per2beta0.2	FDisfalselevel1000per2beta0.3	FDisfalselevel1000per2beta0.4	FDisfalselevel1000per2beta0.5	FDisfalselevel1000per2beta0.6	FDisfalselevel1000per2beta0.7	FDisfalselevel1000per2beta0.8	FDisfalselevel1000per2beta0.9	FDisfalselevel1000per2beta1.0	FDisfalselevel1000per3beta0.0	FDisfalselevel1000per3beta0.1	FDisfalselevel1000per3beta0.2	FDisfalselevel1000per3beta0.3	FDisfalselevel1000per3beta0.4	FDisfalselevel1000per3beta0.5	FDisfalselevel1000per3beta0.6	FDisfalselevel1000per3beta0.7	FDisfalselevel1000per3beta0.8	FDisfalselevel1000per3beta0.9	FDisfalselevel1000per3beta1.0	FDisfalselevel1000per4beta0.0	FDisfalselevel1000per4beta0.1	FDisfalselevel1000per4beta0.2	FDisfalselevel1000per4beta0.3	FDisfalselevel1000per4beta0.4	FDisfalselevel1000per4beta0.5	FDisfalselevel1000per4beta0.6	FDisfalselevel1000per4beta0.7	FDisfalselevel1000per4beta0.8	FDisfalselevel1000per4beta0.9	FDisfalselevel1000per4beta1.0
        //deadline violation rate:
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        for (int g=0; g<gt.length; g++){
            String DL = prePath+"/sddl/gt"+g+"Dlperiodic.csv";
            String FILE_HEADER2 = "nodenum,";
            try {
                fileWriter = new FileWriter(DL);
                fileWriter.append(FILE_HEADER2.toString());
                for(int d = 0; d<tDis.length; d++){
                    for(int p = 0; p<pp; p++){
                        fileWriter.append("AVG(dlV)CloudDis"+tDis[d]+"per"+p);
                        fileWriter.append(COMMA_DELIMITER); 
                        fileWriter.append("AVG(dlV)FFDis"+tDis[d]+"per"+p);
                        fileWriter.append(COMMA_DELIMITER); 
                    }
                }
                for(int d = 0; d<tDis.length; d++){
                    for(int l = 0; l<ll.length; l++){
                        for(int p = 0; p<pp; p++){
                            for(int b = 0; b<bb.length; b++){
                                fileWriter.append("FDis"+tDis[d]+"level"+ll[l]+"per"+p+"beta"+bb[b]);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
                for (int n =0; n<nn.length; n++) {
                    fileWriter.append(String.valueOf(nn[n]));
                    fileWriter.append(COMMA_DELIMITER);
                    for(int d = 0; d<tDis.length; d++){
                        for(int p = 0; p<pp; p++){
                            double avgC = 0.0;double avgFF = 0.0;
                            avgC = (double)Cloud[g][n][d][p].dlViol/Cloud[g][n][d][p].assToCloud;
                            avgFF = (double)FF[g][n][d][p].dlViol/FF[g][n][d][p].assTasks;
                            fileWriter.append(String.valueOf(avgC));
                            fileWriter.append(COMMA_DELIMITER);
                            fileWriter.append(String.valueOf(avgFF));
                            fileWriter.append(COMMA_DELIMITER);
                        }
                    }
                    for(int d = 0; d<tDis.length; d++){    
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                for(int b = 0; b<bb.length; b++){
                                    double avgF = 0.0;
                                    avgF = (double)Fog[g][n][d][l][p][b].dlViol/Fog[g][n][d][l][p][b].assTasks;
                                    fileWriter.append(String.valueOf(avgF));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }
                        }
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
                catch (IOException e) 
                {
                    System.out.println("Error while flushing/closing fileWriter !!!");
                    e.printStackTrace();
                }
             
            }
        
        }
        //service delay/response time:
        for (int g=0; g<gt.length; g++){
            String SD = prePath+"/sddl/gt"+g+"SDperiodic.csv";
            String FILE_HEADER2 = "nodenum,";
            try 
            {
                fileWriter = new FileWriter(SD);
                fileWriter.append(FILE_HEADER2.toString());
                for(int d = 0; d<tDis.length; d++){
                    for(int p = 0; p<pp; p++){
                        fileWriter.append("AVG(SD)CloudDis"+tDis[d]+"per"+p);
                        fileWriter.append(COMMA_DELIMITER); 
                        fileWriter.append("AVG(SD)FFDis"+tDis[d]+"per"+p);
                        fileWriter.append(COMMA_DELIMITER); 
                    }
                }
                for(int d = 0; d<tDis.length; d++){
                    for(int l = 0; l<ll.length; l++){
                        for(int p = 0; p<pp; p++){
                            for(int b = 0; b<bb.length; b++){
                                fileWriter.append("FDis"+tDis[d]+"level"+ll[l]+"per"+p+"beta"+bb[b]);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                }
                fileWriter.append(NEW_LINE_SEPARATOR);
                for (int n =0; n<nn.length; n++) {
                    fileWriter.append(String.valueOf(nn[n]));
                    fileWriter.append(COMMA_DELIMITER);
                    for(int d = 0; d<tDis.length; d++){
                        for(int p = 0; p<pp; p++){
                            double avgC = 0.0;double avgFF = 0.0;
                            avgC = (double)Cloud[g][n][d][p].SD/Cloud[g][n][d][p].assToCloud;
                            avgFF = (double)FF[g][n][d][p].SD/FF[g][n][d][p].assTasks;
                            fileWriter.append(String.valueOf(avgC));
                            fileWriter.append(COMMA_DELIMITER);
                            fileWriter.append(String.valueOf(avgFF));
                            fileWriter.append(COMMA_DELIMITER);
                        } 
                    }
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                for(int b = 0; b<bb.length; b++){
                                    double avgF = 0.0;
                                    avgF = (double)Fog[g][n][d][l][p][b].SD/Fog[g][n][d][l][p][b].assTasks;
                                    fileWriter.append(String.valueOf(avgF));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }
                        }
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
                catch (IOException e) 
                {
                    System.out.println("Error while flushing/closing fileWriter !!!");
                    e.printStackTrace();
                }
             
            }
        
        }
    }
    
    public void analyseVarUtil(EPOSplan[][][][][][] Fog, EPOSplan[][][][] Cloud , EPOSplan[][][][] FF){
    //utilization variance for three evaluated approaches regarding different time periods:
    //header format:
    //periodnumber	Var(util)CloudDisfalse	Var(util)FFDisfalse	FVar(Util)Disfalselev1000beta0.0	FVar(Util)Disfalselev1000beta0.1	FVar(Util)Disfalselev1000beta0.2	FVar(Util)Disfalselev1000beta0.3	FVar(Util)Disfalselev1000beta0.4	FVar(Util)Disfalselev1000beta0.5	FVar(Util)Disfalselev1000beta0.6	FVar(Util)Disfalselev1000beta0.7	FVar(Util)Disfalselev1000beta0.8	FVar(Util)Disfalselev1000beta0.9	FVar(Util)Disfalselev1000beta1.0

    //cpu utilization variance:
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                String Util = prePath+"/util/gt"+g+"nn"+nn[n]+"CPUUtil.csv";
                String FILE_HEADER2 = "period,";
                try {
                    fileWriter = new FileWriter(Util);
                    fileWriter.append(FILE_HEADER2.toString());
                    for(int d = 0; d<tDis.length; d++){
                        fileWriter.append("Var(util)CloudDis"+tDis[d]);
                        fileWriter.append(COMMA_DELIMITER); 
                        fileWriter.append("Var(util)FFDis"+tDis[d]);
                        fileWriter.append(COMMA_DELIMITER); 
                    }
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int b = 0; b<bb.length; b++){
                                fileWriter.append("FVar(Util)Dis"+tDis[d]+"lev"+ll[l]+"beta"+bb[b]);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                    fileWriter.append(NEW_LINE_SEPARATOR);
                    for(int p = 0; p<pp; p++){
                        fileWriter.append(String.valueOf(p));
                        fileWriter.append(COMMA_DELIMITER);
                        for(int d = 0; d<tDis.length; d++){
                            double avgC = 0.0;double avgFF = 0.0;double SumC = 0.0;double SumFF = 0.0;double VarFF = 0.0;double VarC = 0.0;
                            for (int i=0; i<nn[n];i++){
                                SumC = SumC+Cloud[g][n][d][p].utilPlan[i];
                                SumFF = SumFF+FF[g][n][d][p].utilPlan[i];
                            }
                            avgC = SumC/nn[n];
                            avgFF = SumFF/nn[n];
                            for (int i=0; i<nn[n];i++){
                                VarC = VarC + Math.pow((Cloud[g][n][d][p].utilPlan[i] - avgC), (double)2);
                                VarFF = VarFF + Math.pow((FF[g][n][d][p].utilPlan[i] - avgFF), (double)2);
                            }
                            fileWriter.append(String.valueOf(VarC/nn[n]));
                            fileWriter.append(COMMA_DELIMITER);
                            fileWriter.append(String.valueOf(VarFF/nn[n]));
                            fileWriter.append(COMMA_DELIMITER);
                        }
                        for(int d = 0; d<tDis.length; d++){
                            for(int l = 0; l<ll.length; l++){
                                for(int b = 0; b<bb.length; b++){
                                    double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                    for (int i=0; i<nn[n];i++){
                                        SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                    }
                                    avgF = SumF/nn[n];
                                    for (int i=0; i<nn[n];i++){
                                        VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                    }
                                    fileWriter.append(String.valueOf(VarF/nn[n]));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }
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
                    catch (IOException e) 
                    {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        e.printStackTrace();
                    }

                }
            }
        }
    
        //memory utilization variance:
        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                String Util = prePath+"/util/gt"+g+"nn"+nn[n]+"MemUtil.csv";
                String FILE_HEADER2 = "period,";
                try {
                    fileWriter = new FileWriter(Util);
                    fileWriter.append(FILE_HEADER2.toString());
                    for(int d = 0; d<tDis.length; d++){
                        fileWriter.append("Var(util)CloudDis"+tDis[d]);
                        fileWriter.append(COMMA_DELIMITER); 
                        fileWriter.append("Var(util)FFDis"+tDis[d]);
                        fileWriter.append(COMMA_DELIMITER); 
                    }
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int b = 0; b<bb.length; b++){
                                fileWriter.append("FVar(Util)Dis"+tDis[d]+"lev"+ll[l]+"beta"+bb[b]);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                    fileWriter.append(NEW_LINE_SEPARATOR);
                    for(int p = 0; p<pp; p++){
                        fileWriter.append(String.valueOf(p));
                        fileWriter.append(COMMA_DELIMITER);
                        for(int d = 0; d<tDis.length; d++){
                            double avgC = 0.0;double avgFF = 0.0;double SumC = 0.0;double SumFF = 0.0;double VarFF = 0.0;double VarC = 0.0;
                            for (int i=nn[n]; i<2*nn[n];i++){
                                SumC = SumC+Cloud[g][n][d][p].utilPlan[i];
                                SumFF = SumFF+FF[g][n][d][p].utilPlan[i];
                            }
                            avgC = SumC/nn[n];
                            avgFF = SumFF/nn[n];
                            for (int i=nn[n]; i<2*nn[n];i++){
                                VarC = VarC + Math.pow((Cloud[g][n][d][p].utilPlan[i] - avgC), (double)2);
                                VarFF = VarFF + Math.pow((FF[g][n][d][p].utilPlan[i] - avgFF), (double)2);
                            }
                            fileWriter.append(String.valueOf(VarC/nn[n]));
                            fileWriter.append(COMMA_DELIMITER);
                            fileWriter.append(String.valueOf(VarFF/nn[n]));
                            fileWriter.append(COMMA_DELIMITER);
                        }
                        for(int d = 0; d<tDis.length; d++){
                            for(int l = 0; l<ll.length; l++){
                                for(int b = 0; b<bb.length; b++){
                                    double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                    for (int i=nn[n]; i<2*nn[n];i++){
                                        SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                    }
                                    avgF = SumF/nn[n];
                                    for (int i=nn[n]; i<2*nn[n];i++){
                                        VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                    }
                                    fileWriter.append(String.valueOf(VarF/nn[n]));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }
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
                    catch (IOException e) 
                    {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        e.printStackTrace();
                    }

                }
            }
        }
        
    
    //overall utilization variance (cpu|memory) for First Fit and cloud approach:
    //header format:
    //period	Var(util)CloudDisfalse	Var(util)FFDisfalse

        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                String Util = prePath+"/util/gt"+g+"nn"+nn[n]+"allUtil.csv";
                String FILE_HEADER2 = "period,";
                try {
                    fileWriter = new FileWriter(Util);
                    fileWriter.append(FILE_HEADER2.toString());
                    for(int d = 0; d<tDis.length; d++){
                        fileWriter.append("Var(util)CloudDis"+tDis[d]);
                        fileWriter.append(COMMA_DELIMITER); 
                        fileWriter.append("Var(util)FFDis"+tDis[d]);
                        fileWriter.append(COMMA_DELIMITER); 
                    }
                    fileWriter.append(NEW_LINE_SEPARATOR);
                    for(int p = 0; p<pp; p++){
                        fileWriter.append(String.valueOf(p));
                        fileWriter.append(COMMA_DELIMITER);
                        for(int d = 0; d<tDis.length; d++){
                            double avgC = 0.0;double avgFF = 0.0;double SumC = 0.0;double SumFF = 0.0;double VarFF = 0.0;double VarC = 0.0;
                            for (int i=0; i<2*nn[n];i++){
                                SumC = SumC+Cloud[g][n][d][p].utilPlan[i];
                                SumFF = SumFF+FF[g][n][d][p].utilPlan[i];
                            }
                            avgC = SumC/(2*nn[n]);
                            avgFF = SumFF/(2*nn[n]);
                            for (int i=0; i<2*nn[n];i++){
                                VarC = VarC + Math.pow((Cloud[g][n][d][p].utilPlan[i] - avgC), (double)2);
                                VarFF = VarFF + Math.pow((FF[g][n][d][p].utilPlan[i] - avgFF), (double)2);
                            }
                            fileWriter.append(String.valueOf(VarC/(2*nn[n])));
                            fileWriter.append(COMMA_DELIMITER);
                            fileWriter.append(String.valueOf(VarFF/(2*nn[n])));
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
                    catch (IOException e) 
                    {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        e.printStackTrace();
                    }
             
                }
            }
        }
           
    }
    //
    public void analysefog(EPOSplan[][][][][][] Fog){
        
        FileWriter fileWriter = null;
        String COMMA_DELIMITER = ",";
        String NEW_LINE_SEPARATOR = "\n";
       
       //overall (cpu|memory) utilization variance for EPOS Fog:
       //header format:
       //beta	FVar(Util)Disfalselev1000period0	FVar(Util)Disfalselev1000period1	FVar(Util)Disfalselev1000period2	FVar(Util)Disfalselev1000period3	FVar(Util)Disfalselev1000period4
        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                String Util = prePath+"/util/fog/gt"+g+"nn"+nn[n]+".csv";
                String FILE_HEADER2 = "beta,";
                try {
                    fileWriter = new FileWriter(Util);
                    fileWriter.append(FILE_HEADER2.toString());
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                fileWriter.append("FVar(Util)Dis"+tDis[d]+"lev"+ll[l]+"period"+p);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                    fileWriter.append(NEW_LINE_SEPARATOR);
                    for(int b = 0; b<bb.length; b++){    
                        fileWriter.append(String.valueOf(bb[b]));
                        fileWriter.append(COMMA_DELIMITER);
                        for(int d = 0; d<tDis.length; d++){ 
                            for(int l = 0; l<ll.length; l++){  
                                for(int p = 0; p<pp; p++){
                                    double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                    for (int i=0; i<(2*nn[n]);i++){
                                        SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                    }
                                    avgF = SumF/(2*nn[n]);
                                    for (int i=0; i<(2*nn[n]);i++){
                                        VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                    }
                                    fileWriter.append(String.valueOf(VarF/(2*nn[n])));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }
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
                    catch (IOException e) 
                    {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        e.printStackTrace();
                    }
                }
            }
        }
        
        //cpu utilization variance for EPOS Fog:
        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                String Util = prePath+"/util/fog/gt"+g+"nn"+nn[n]+"CPUUtil.csv";
                String FILE_HEADER2 = "beta,";
                try {
                    fileWriter = new FileWriter(Util);
                    fileWriter.append(FILE_HEADER2.toString());
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                fileWriter.append("FVar(Util)Dis"+tDis[d]+"lev"+ll[l]+"period"+p);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                    fileWriter.append(NEW_LINE_SEPARATOR);
                    for(int b = 0; b<bb.length; b++){    
                        fileWriter.append(String.valueOf(bb[b]));
                        fileWriter.append(COMMA_DELIMITER);
                        for(int d = 0; d<tDis.length; d++){ 
                            for(int l = 0; l<ll.length; l++){  
                                for(int p = 0; p<pp; p++){
                                    double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                    for (int i=0; i<nn[n];i++){
                                        SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                    }
                                    avgF = SumF/nn[n];
                                    for (int i=0; i<nn[n];i++){
                                        VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                    }
                                    fileWriter.append(String.valueOf(VarF/nn[n]));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }
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
                    catch (IOException e)
                    {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        e.printStackTrace();
                    }
             
                }
        
            }
        }

    // cpu utilization variance for EPOS Fog regarding different host-proximity values:
    for (int g=0; g<gt.length; g++){
        for (int n =0; n<nn.length; n++) {
            for(int d = 0; d<tDis.length; d++){ 
                for(int l = 0; l<ll.length; l++){
                    String Utillev = prePath+"/util/fog/gt"+g+"nn"+nn[n]+"Dis"+tDis[d]+"level"+ll[l]+"CPUUtil.csv";
                    String FILE_HEADER2 = "beta,";
                    try {
                        fileWriter = new FileWriter(Utillev);
                        fileWriter.append(FILE_HEADER2.toString());
                        for(int p = 0; p<pp; p++){
                            fileWriter.append("FVar(Util)period"+p);
                            fileWriter.append(COMMA_DELIMITER);
                        }
                        fileWriter.append(NEW_LINE_SEPARATOR);
                        for(int b = 0; b<bb.length; b++){    
                            fileWriter.append(String.valueOf(bb[b]));
                            fileWriter.append(COMMA_DELIMITER);
                            for(int p = 0; p<pp; p++){
                                double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                for (int i=0; i<nn[n];i++){
                                    SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                }
                                avgF = SumF/nn[n];
                                for (int i=0; i<nn[n];i++){
                                    VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                }
                                fileWriter.append(String.valueOf(VarF/nn[n]));
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
                        catch (IOException e) 
                        {
                            System.out.println("Error while flushing/closing fileWriter !!!");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }
        
    //memory utilization variance for EPOS Fog:  
        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                String Util = prePath+"/util/fog/gt"+g+"nn"+nn[n]+"MemUtil.csv";
                String FILE_HEADER2 = "beta,";
                try {
                    fileWriter = new FileWriter(Util);
                    fileWriter.append(FILE_HEADER2.toString());
                    for(int d = 0; d<tDis.length; d++){
                        for(int l = 0; l<ll.length; l++){
                            for(int p = 0; p<pp; p++){
                                fileWriter.append("FVar(Util)Dis"+tDis[d]+"lev"+ll[l]+"period"+p);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                        }
                    }
                    fileWriter.append(NEW_LINE_SEPARATOR);
                    for(int b = 0; b<bb.length; b++){    
                        fileWriter.append(String.valueOf(bb[b]));
                        fileWriter.append(COMMA_DELIMITER);
                        for(int d = 0; d<tDis.length; d++){ 
                            for(int l = 0; l<ll.length; l++){  
                                for(int p = 0; p<pp; p++){
                                   double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                    for (int i=nn[n]; i<2*nn[n];i++){
                                        SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                    }
                                    avgF = SumF/nn[n];
                                    for (int i=nn[n]; i<2*nn[n];i++){
                                        VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                    }
                                    fileWriter.append(String.valueOf(VarF/nn[n]));
                                    fileWriter.append(COMMA_DELIMITER);
                                }
                            }   
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
                    catch (IOException e) 
                    {
                        System.out.println("Error while flushing/closing fileWriter !!!");
                        e.printStackTrace();
                    }
             
                }
            }
        }

        // memory utilization variance for EPOS Fog regarding different host-proximity values:
        for (int g=0; g<gt.length; g++){
            for (int n =0; n<nn.length; n++) {
                for(int d = 0; d<tDis.length; d++){ 
                    for(int l = 0; l<ll.length; l++){
                        String Utillev = prePath+"/util/fog/gt"+g+"nn"+nn[n]+"Dis"+tDis[d]+"level"+ll[l]+"MemUtil.csv";
                        String FILE_HEADER2 = "beta,";
                        try {
                            fileWriter = new FileWriter(Utillev);
                            fileWriter.append(FILE_HEADER2.toString());
                            for(int p = 0; p<pp; p++){
                                fileWriter.append("FVar(Util)period"+p);
                                fileWriter.append(COMMA_DELIMITER);
                            }
                            fileWriter.append(NEW_LINE_SEPARATOR);
                            for(int b = 0; b<bb.length; b++){    
                                fileWriter.append(String.valueOf(bb[b]));
                                fileWriter.append(COMMA_DELIMITER);
                                for(int p = 0; p<pp; p++){
                                    double avgF = 0.0;double SumF = 0.0; double VarF = 0.0;
                                    for (int i=nn[n]; i<2*nn[n];i++){
                                        SumF = SumF+Fog[g][n][d][l][p][b].utilPlan[i];
                                    }
                                    avgF = SumF/nn[n];
                                    for (int i=nn[n]; i<2*nn[n];i++){
                                        VarF = VarF + Math.pow((Fog[g][n][d][l][p][b].utilPlan[i] - avgF), (double)2);
                                    }
                                    fileWriter.append(String.valueOf(VarF/nn[n]));
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
            }
        }
    }
                               
}    
        