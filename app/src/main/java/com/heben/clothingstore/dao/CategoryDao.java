package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heben.clothingstore.entity.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert
    long insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories ORDER BY created_at DESC")
    List<Category> getAllCategories();

    @Query("SELECT * FROM categories WHERE parent_id = :parentId")
    List<Category> getByParentId(long parentId);

    @Query("SELECT * FROM categories WHERE parent_id = 0")
    List<Category> getTopLevelCategories();
}