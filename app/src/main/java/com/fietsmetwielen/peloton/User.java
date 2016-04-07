package com.fietsmetwielen.peloton;

public class User {
    public String userName;
    public double mean;
    public double standaardafwijking;
    public User(String userName, double mean, double standaardafwijking){
        this.userName = userName;
        this.mean = mean;
        this.standaardafwijking = standaardafwijking;
    }
}
