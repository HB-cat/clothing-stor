package com.heben.clothingstore.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.heben.clothingstore.dao.ProductDao;
import com.heben.clothingstore.dao.CategoryDao;
import com.heben.clothingstore.dao.AttributeGroupDao;
import com.heben.clothingstore.dao.AttributeValueDao;
import com.heben.clothingstore.dao.ProductAttributeDao;
import com.heben.clothingstore.dao.SaleDao;
import com.heben.clothingstore.dao.PurchaseDao;

import com.heben.clothingstore.entity.Product;
import com.heben.clothingstore.entity.Category;
import com.heben.clothingstore.entity.AttributeGroup;
import com.heben.clothingstore.entity.AttributeValue;
import com.heben.clothingstore.entity.ProductAttribute;
import com.heben.clothingstore.entity.Sale;
import com.heben.clothingstore.entity.Purchase;

@Database(
        entities = {
                Product.class,
                Category.class,
                AttributeGroup.class,
                AttributeValue.class,
                ProductAttribute.class,
                Sale.class,
                Purchase.class
        },
        version = 2,   // 已升级为 2，因为 Purchase 表增加了 attribute_desc 字段
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ProductDao productDao();
    public abstract CategoryDao categoryDao();
    public abstract AttributeGroupDao attributeGroupDao();
    public abstract AttributeValueDao attributeValueDao();
    public abstract ProductAttributeDao productAttributeDao();
    public abstract SaleDao saleDao();
    public abstract PurchaseDao purchaseDao();

    private static AppDatabase instance;

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "clothing_store.db"
                    ).fallbackToDestructiveMigration()   // 数据库改变时自动重建
                    .build();
        }
        return instance;
    }
}