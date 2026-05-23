package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchases")
public class Purchase {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "product_id")
    private long productId;

    @ColumnInfo(name = "quantity")
    private int quantity;

    @ColumnInfo(name = "cost_price")
    private double costPrice;

    @ColumnInfo(name = "purchase_date")
    private String purchaseDate;

    @ColumnInfo(name = "attribute_desc")
    private String attributeDesc;     // 属性描述，如“红色 / M码”

    @ColumnInfo(name = "created_at")
    private String createdAt;

    // ========== Getter 和 Setter ==========
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public String getAttributeDesc() { return attributeDesc; }
    public void setAttributeDesc(String attributeDesc) { this.attributeDesc = attributeDesc; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}