package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "products")
public class Product {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "category_id")
    private long categoryId;

    @ColumnInfo(name = "cost_price")
    private double costPrice;

    @ColumnInfo(name = "selling_price")
    private double sellingPrice;

    @ColumnInfo(name = "image_path")
    private String imagePath;

    @ColumnInfo(name = "alias")
    private String alias;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @Ignore
    private int totalPurchase;   // 累计进货（非数据库字段，仅用于查询结果）

    @Ignore
    private int totalSale;       // 累计销售（非数据库字段，仅用于查询结果）

    @Ignore
    private int currentStock;    // 当前库存（非数据库字段，仅用于查询结果）

    // Getter 和 Setter
    public int getTotalPurchase() { return totalPurchase; }
    public void setTotalPurchase(int totalPurchase) { this.totalPurchase = totalPurchase; }

    public int getTotalSale() { return totalSale; }
    public void setTotalSale(int totalSale) { this.totalSale = totalSale; }

    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }

    // ========== Getter 和 Setter ==========

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}