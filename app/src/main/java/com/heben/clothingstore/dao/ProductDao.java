package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.heben.clothingstore.entity.ProductStock;

import com.heben.clothingstore.entity.Product;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert
    long insert(Product product);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products ORDER BY created_at DESC")
    List<Product> getAllProducts();

    @Query("SELECT * FROM products WHERE category_id = :categoryId")
    List<Product> getProductsByCategory(long categoryId);

    @Query("SELECT * FROM products WHERE name LIKE '%' || :keyword || '%'")
    List<Product> searchProducts(String keyword);

    @Query("SELECT * FROM products WHERE id = :id")
    Product getById(long id);

    @Query("SELECT p.id, p.name, p.image_path, " +
            "COALESCE((SELECT SUM(pur.quantity) FROM purchases pur WHERE pur.product_id = p.id), 0) AS total_purchase, " +
            "COALESCE((SELECT SUM(s.quantity) FROM sales s WHERE s.product_id = p.id AND s.is_refunded = 0), 0) AS total_sale, " +
            "COALESCE((SELECT SUM(pur.quantity) FROM purchases pur WHERE pur.product_id = p.id), 0) - " +
            "COALESCE((SELECT SUM(s.quantity) FROM sales s WHERE s.product_id = p.id AND s.is_refunded = 0), 0) AS current_stock " +
            "FROM products p ORDER BY current_stock ASC")
    List<ProductStock> getProductsWithStock();

    
}