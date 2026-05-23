package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heben.clothingstore.entity.AttributeValue;

import java.util.List;

@Dao
public interface AttributeValueDao {

    @Insert
    long insert(AttributeValue value);

    @Update
    void update(AttributeValue value);

    @Delete
    void delete(AttributeValue value);

    @Query("SELECT * FROM attribute_values WHERE group_id = :groupId ORDER BY created_at ASC")
    List<AttributeValue> getByGroupId(long groupId);

    @Query("SELECT * FROM attribute_values ORDER BY created_at DESC")
    List<AttributeValue> getAllValues();

    // 新增：根据ID查询单个属性值
    @Query("SELECT * FROM attribute_values WHERE id = :id")
    AttributeValue getById(long id);
}