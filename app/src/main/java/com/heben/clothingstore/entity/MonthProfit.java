package com.heben.clothingstore.entity;

public class MonthProfit {
    private String month;
    private double totalSales;
    private double totalProfit;
    private int saleCount;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getTotalSales() { return totalSales; }
    public void setTotalSales(double totalSales) { this.totalSales = totalSales; }

    public double getTotalProfit() { return totalProfit; }
    public void setTotalProfit(double totalProfit) { this.totalProfit = totalProfit; }

    public int getSaleCount() { return saleCount; }
    public void setSaleCount(int saleCount) { this.saleCount = saleCount; }
}