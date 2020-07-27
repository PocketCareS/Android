package com.ub.pocketcares.home;

import java.util.ArrayList;

public class HomeItem {
    private ArrayList<Integer> assets;
    private ArrayList<String> descriptions;

    HomeItem(ArrayList<Integer> a, ArrayList<String> d){
        this.assets=a;
        this.descriptions=d;
    }

    public ArrayList<Integer> getAssets(){
        return this.assets;
    }

    public ArrayList<String> getDescriptions(){
        return this.descriptions;
    }
}
