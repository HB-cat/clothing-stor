package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales")
public class Sale {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "product_id")
    private long productId;           // 卖出的商品ID

    @ColumnInfo(name = "quantity")
    private int quantity;             // 数量

    @ColumnInfo(name = "selling_price")
    private double sellingPrice;      // 实际售价（可能议价）

    @ColumnInfo(name = "cost_price")
    private double costPrice;         // 进价（记录当时的进价，方便算毛利）

    @ColumnInfo(name = "sale_date")
    private String saleDate;          // 销售日期 yyyy-MM-dd

    @ColumnInfo(name = "sale_time")
    private String saleTime;          // 销售时间 HH:mm:ss

    @ColumnInfo(name = "is_refunded")
    private boolean isRefunded;       // 是否已退款

    @ColumnInfo(name = "created_at")
    private String createdAt;

    // ========== Getter 和 Setter ==========

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getProductId() {
        return productId;
    }

    public void setProductId(long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public String getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(String saleDate) {
        this.saleDate = saleDate;
    }

    public String getSaleTime() {
        return saleTime;
    }

    public void setSaleTime(String saleTime) {
        this.saleTime = saleTime;
    }

    public boolean isRefunded() {
        return isRefunded;
    }

    public void setRefunded(boolean refunded) {
        isRefunded = refunded;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}