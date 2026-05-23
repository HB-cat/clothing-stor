package com.heben.clothingstore.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.heben.clothingstore.entity.Sale;
import com.heben.clothingstore.entity.ProductRank;
import com.heben.clothingstore.entity.MonthProfit;

import java.util.List;

@Dao
public interface SaleDao {

    @Insert
    long insert(Sale sale);

    @Update
    void update(Sale sale);

    @Query("SELECT * FROM sales ORDER BY sale_date DESC, sale_time DESC")
    List<Sale> getAllSales();

    @Query("SELECT * FROM sales WHERE sale_date = :date ORDER BY sale_time DESC")
    List<Sale> getSalesByDate(String date);

    @Query("SELECT * FROM sales WHERE product_id = :productId")
    List<Sale> getSalesByProduct(long productId);

    @Query("SELECT * FROM sales WHERE sale_date BETWEEN :startDate AND :endDate")
    List<Sale> getSalesByDateRange(String startDate, String endDate);

    @Query("SELECT * FROM sales WHERE is_refunded = 0")
    List<Sale> getActiveSales();

    @Query("SELECT SUM(selling_price * quantity) FROM sales WHERE sale_date = :date AND is_refunded = 0")
    Double getDailyTotal(String date);

    @Query("SELECT SUM((selling_price - cost_price) * quantity) FROM sales WHERE sale_date = :date AND is_refunded = 0")
    Double getDailyProfit(String date);

    // 热销排行：按销量降序
    @Query("SELECT s.product_id AS id, p.name, " +
            "SUM(s.quantity) AS totalQty, " +
            "SUM(s.selling_price * s.quantity) AS totalAmount, " +
            "SUM((s.selling_price - s.cost_price) * s.quantity) AS totalProfit " +
            "FROM sales s INNER JOIN products p ON s.product_id = p.id " +
            "WHERE s.is_refunded = 0 " +
            "GROUP BY s.product_id ORDER BY totalQty DESC")
    List<ProductRank> getProductRank();

    // 利润趋势：按月汇总
    @Query("SELECT SUBSTR(sale_date, 1, 7) AS month, " +
            "SUM(selling_price * quantity) AS totalSales, " +
            "SUM((selling_price - cost_price) * quantity) AS totalProfit, " +
            "COUNT(*) AS saleCount " +
            "FROM sales WHERE is_refunded = 0 " +
            "GROUP BY month ORDER BY month ASC")
    List<MonthProfit> getMonthProfit();
}