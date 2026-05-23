package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;

/**
 * 库存查询结果临时类（不存数据库，仅用于接收SQL聚合查询结果）
 */
public class ProductStock {

    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "total_purchase")
    private int totalPurchase;

    @ColumnInfo(name = "total_sale")
    private int totalSale;

    @ColumnInfo(name = "current_stock")
    private int currentStock;

    // ========== Getter 和 Setter ==========
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getTotalPurchase() { return totalPurchase; }
    public void setTotalPurchase(int totalPurchase) { this.totalPurchase = totalPurchase; }

    public int getTotalSale() { return totalSale; }
    public void setTotalSale(int totalSale) { this.totalSale = totalSale; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
}