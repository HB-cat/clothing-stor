package com.heben.clothingstore.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attribute_values")
public class AttributeValue {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "group_id")
    private long groupId;             // 属于哪个属性组

    @ColumnInfo(name = "value")
    private String value;             // 属性值，如“淑女风”、“纯棉”、“春季”

    @ColumnInfo(name = "created_at")
    private String createdAt;

    // ========== Getter 和 Setter ==========

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}