package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;

@Entity(
        tableName = "product_attributes",
        primaryKeys = {"product_id", "attribute_value_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = Product.class,
                        parentColumns = "id",
                        childColumns = "product_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = AttributeValue.class,
                        parentColumns = "id",
                        childColumns = "attribute_value_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("product_id"),
                @Index("attribute_value_id")
        }
)
public class ProductAttribute {

    @ColumnInfo(name = "product_id")
    private long productId;

    @ColumnInfo(name = "attribute_value_id")
    private long attributeValueId;

    // 无参构造（Room 需要）
    public ProductAttribute() {}

    // 带参构造，加 @Ignore 让 Room 忽略
    @Ignore
    public ProductAttribute(long productId, long attributeValueId) {
        this.productId = productId;
        this.attributeValueId = attributeValueId;
    }

    // ========== Getter 和 Setter ==========
    public long getProductId() { return productId; }
    public void setProductId(long productId) { this.productId = productId; }

    public long getAttributeValueId() { return attributeValueId; }
    public void setAttributeValueId(long attributeValueId) { this.attributeValueId = attributeValueId; }
}