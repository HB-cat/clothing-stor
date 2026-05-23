package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "name")
    private String name;              // 分类名，如“上衣”、“连衣裙类”

    @ColumnInfo(name = "parent_id")
    private long parentId;            // 父分类ID。0=一级分类，非0=二级分类的父级ID

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

    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}