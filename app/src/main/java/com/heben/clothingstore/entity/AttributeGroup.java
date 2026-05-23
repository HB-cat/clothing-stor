package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attribute_groups")
public class AttributeGroup {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;              // 属性组名，如“风格”、“面料”、“季节”、“颜色”、“尺码”

    @ColumnInfo(name = "created_at")
    private String createdAt;

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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}