package com.ub.pocketcares.home;

class HealthSummaryItem {
    private String date;
    private String response;
    private String recommendation;

    HealthSummaryItem(String d,String res,String recommend){
        this.date=d;
        this.response=res;
        this.recommendation=recommend;
    }

    public String getDate() {
        return this.date;
    }

    public String getResponse(){
        return this.response;
    }

    public String getRecommendation(){
        return this.recommendation;
    }
}
