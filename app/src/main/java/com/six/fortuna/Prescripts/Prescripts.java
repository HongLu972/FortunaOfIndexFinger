package com.six.fortuna.Prescripts;
import java.util.*;

public class Prescripts {
    Random r = new Random();
    ArrayList<String> prescripts = new ArrayList<String>();
    ArrayList<Integer> prescripts_time = new ArrayList<Integer>();
    String name;

    public void add_prescripts(String prescript, int time_taken){
        prescripts.add(prescript);
        prescripts_time.add(time_taken);
    }
    public Prescripts(){
        //在这添加指令们
        add_prescripts("Sleep for a total of eight hundred hours per day", 24*60*60);
        add_prescripts("Eat or write or pull the trigger with your right hand only", 0);
        add_prescripts("Warm up before your play", 0);
        add_prescripts("Leave Hermes' index, using the Fortuna one", 0);
        add_prescripts("Leave a drink and a paper slit which has the sentence \"I'm sorry that I scratch your car, so I buy you a drink\" on one unknown's car.", 0);
    }

    public Prescripts(int i){

    }

    public Prescripts_finished getPrescripts(){
        int temp = r.nextInt(prescripts.size());
        Prescripts_finished i = new Prescripts_finished(prescripts_time.get(temp), prescripts.get(temp));
        return i;
    }
}
