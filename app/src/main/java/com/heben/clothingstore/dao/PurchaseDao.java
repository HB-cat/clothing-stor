package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.heben.clothingstore.entity.Purchase;

import java.util.List;

@Dao
public interface PurchaseDao {

    @Insert
    long insert(Purchase purchase);

    @Query("SELECT * FROM purchases ORDER BY purchase_date DESC, created_at DESC")
    List<Purchase> getAllPurchases();

    @Query("SELECT * FROM purchases WHERE product_id = :productId")
    List<Purchase> getPurchasesByProduct(long productId);

    @Query("SELECT * FROM purchases WHERE purchase_date = :date")
    List<Purchase> getPurchasesByDate(String date);
}