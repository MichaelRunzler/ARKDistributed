package Iris.util;

import java.util.ArrayList;

/**
 * Stores multiple CPEStats in a compact object form.
 * Convenience class to avoid multiple-arraylist encapsulation.
 */
public class APStats
{
    private ArrayList<CPEStats> CPEs;

    public APStats(ArrayList<CPEStats> CPEList) {
        CPEs = new ArrayList<>(CPEList);
    }

    public APStats() {
        CPEs = new ArrayList<>();
    }

    public ArrayList<CPEStats> getCPEs(){
        return new ArrayList<>(CPEs);
    }

    public int getCPECount() {
        return CPEs.size();
    }

    public CPEStats getCPE(int index){
        return index >= 0 && index < CPEs.size() ? CPEs.get(index) : null;
    }

    public void addCPE(CPEStats CPE){
        if(CPE != null) {
            CPEs.add(CPE);
        }else{
            throw new NullPointerException("CPE cannot be null!");
        }
    }

    public void addMultipleCPEs(CPEStats... CPE){
        for(CPEStats c : CPE){
            if(c != null) {
                CPEs.add(c);
            }
        }
    }

    public void addMultipleCPEs(ArrayList<CPEStats> CPE){
        if(CPE != null && CPE.size() > 0) {
            CPEs.addAll(CPE);
        }else{
            throw new IllegalArgumentException("List cannot be empty or null!");
        }
    }
}
