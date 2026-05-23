package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heben.clothingstore.entity.AttributeGroup;

import java.util.List;

@Dao
public interface AttributeGroupDao {

    @Insert
    long insert(AttributeGroup group);

    @Update
    void update(AttributeGroup group);

    @Delete
    void delete(AttributeGroup group);

    @Query("SELECT * FROM attribute_groups ORDER BY created_at DESC")
    List<AttributeGroup> getAllGroups();

    // 新增：根据ID查询单个属性组
    @Query("SELECT * FROM attribute_groups WHERE id = :id")
    AttributeGroup getById(long id);
}