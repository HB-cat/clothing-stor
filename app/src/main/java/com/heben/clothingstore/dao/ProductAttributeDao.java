package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;

import com.heben.clothingstore.entity.ProductAttribute;

import java.util.List;

@Dao
public interface ProductAttributeDao {

    @Insert
    void insert(ProductAttribute productAttribute);

    @Delete
    void delete(ProductAttribute productAttribute);

    @Query("SELECT * FROM product_attributes WHERE product_id = :productId")
    List<ProductAttribute> getByProductId(long productId);

    @Query("SELECT * FROM product_attributes WHERE attribute_value_id = :attributeValueId")
    List<ProductAttribute> getByAttributeValueId(long attributeValueId);

    @Query("DELETE FROM product_attributes WHERE product_id = :productId")
    void deleteByProductId(long productId);
}