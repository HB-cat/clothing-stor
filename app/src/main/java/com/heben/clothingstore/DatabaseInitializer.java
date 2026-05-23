package com.heben.clothingstore;

import android.content.Context;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.*;
import com.heben.clothingstore.entity.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseInitializer {

    private static final String[] DEFAULT_CATEGORIES = {
            "连衣裙类", "T恤类", "衬衫类", "防晒类", "半身裙", "裤子"
    };

    public static void initAll(Context context) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            initCategories(db);
            initAttributeGroups(db);
            initSampleProducts(db);   // 预置示例商品
        }).start();
    }

    private static void initCategories(AppDatabase db) {
        CategoryDao dao = db.categoryDao();
        List<Category> existing = dao.getAllCategories();
        if (existing != null && !existing.isEmpty()) return;

        String now = getNow();
        for (String name : DEFAULT_CATEGORIES) {
            Category category = new Category();
            category.setName(name);
            category.setParentId(0);
            category.setCreatedAt(now);
            dao.insert(category);
        }
    }

    private static void initAttributeGroups(AppDatabase db) {
        AttributeGroupDao groupDao = db.attributeGroupDao();
        List<AttributeGroup> existingGroups = groupDao.getAllGroups();
        if (existingGroups != null && !existingGroups.isEmpty()) return;

        AttributeValueDao valueDao = db.attributeValueDao();
        String now = getNow();

        // 预置风格
        long styleGroupId = createGroup(groupDao, "风格", now);
        createValue(valueDao, styleGroupId, "复古风", now);
        createValue(valueDao, styleGroupId, "休闲风", now);
        createValue(valueDao, styleGroupId, "淑女风", now);

        // 预置面料
        long fabricGroupId = createGroup(groupDao, "面料", now);
        createValue(valueDao, fabricGroupId, "纯棉", now);
        createValue(valueDao, fabricGroupId, "冰丝", now);
        createValue(valueDao, fabricGroupId, "针织", now);
        createValue(valueDao, fabricGroupId, "雪纺", now);

        // 预置颜色
        long colorGroupId = createGroup(groupDao, "颜色", now);
        createValue(valueDao, colorGroupId, "红色", now);
        createValue(valueDao, colorGroupId, "白色", now);
        createValue(valueDao, colorGroupId, "黑色", now);
        createValue(valueDao, colorGroupId, "蓝色", now);

        // 预置尺码
        long sizeGroupId = createGroup(groupDao, "尺码", now);
        createValue(valueDao, sizeGroupId, "M", now);
        createValue(valueDao, sizeGroupId, "L", now);
        createValue(valueDao, sizeGroupId, "XL", now);
    }

    // ========== 预置示例商品 ==========
    private static void initSampleProducts(AppDatabase db) {
        ProductDao productDao = db.productDao();
        // 防止重复初始化
        if (productDao.getAllProducts() != null && !productDao.getAllProducts().isEmpty()) return;

        ProductAttributeDao paDao = db.productAttributeDao();
        String now = getNow();

        // 获取分类ID
        List<Category> categories = db.categoryDao().getAllCategories();
        long dressCatId = findCategoryId(categories, "连衣裙类");
        long tshirtCatId = findCategoryId(categories, "T恤类");
        long pantsCatId = findCategoryId(categories, "裤子");

        // 获取属性值ID (注意顺序: M, L, XL, 复古风, 休闲风, 淑女风, 纯棉, 冰丝, 针织, 雪纺, 红色, 白色, 黑色, 蓝色)
        List<AttributeValue> allValues = db.attributeValueDao().getAllValues();
        long mId = findValueId(allValues, "M");
        long lId = findValueId(allValues, "L");
        long xlId = findValueId(allValues, "XL");
        long vintageId = findValueId(allValues, "复古风");
        long casualId = findValueId(allValues, "休闲风");
        long ladyId = findValueId(allValues, "淑女风");
        long cottonId = findValueId(allValues, "纯棉");
        long iceSilkId = findValueId(allValues, "冰丝");
        long redId = findValueId(allValues, "红色");
        long whiteId = findValueId(allValues, "白色");
        long blueId = findValueId(allValues, "蓝色");

        // 商品1：复古风连衣裙，红色，M，纯棉，进60卖120
        Product p1 = createProduct(productDao, "复古风连衣裙", dressCatId, 60, 120, now);
        if (p1 != null) {
            linkAttribute(paDao, p1.getId(), vintageId);
            linkAttribute(paDao, p1.getId(), redId);
            linkAttribute(paDao, p1.getId(), mId);
            linkAttribute(paDao, p1.getId(), cottonId);
        }

        // 商品2：白色冰丝T恤，休闲风，L，进30卖60
        Product p2 = createProduct(productDao, "白色冰丝T恤", tshirtCatId, 30, 60, now);
        if (p2 != null) {
            linkAttribute(paDao, p2.getId(), casualId);
            linkAttribute(paDao, p2.getId(), whiteId);
            linkAttribute(paDao, p2.getId(), lId);
            linkAttribute(paDao, p2.getId(), iceSilkId);
        }

        // 商品3：蓝色阔腿裤，休闲风，XL，进50卖100
        Product p3 = createProduct(productDao, "蓝色阔腿裤", pantsCatId, 50, 100, now);
        if (p3 != null) {
            linkAttribute(paDao, p3.getId(), casualId);
            linkAttribute(paDao, p3.getId(), blueId);
            linkAttribute(paDao, p3.getId(), xlId);
        }
    }

    // ========== 辅助方法 ==========
    private static Product createProduct(ProductDao dao, String name, long catId,
                                         double cost, double price, String now) {
        Product p = new Product();
        p.setName(name);
        p.setCategoryId(catId);
        p.setCostPrice(cost);
        p.setSellingPrice(price);
        p.setCreatedAt(now);
        long id = dao.insert(p);
        if (id > 0) {
            p.setId(id);
            return p;
        }
        return null;
    }

    private static void linkAttribute(ProductAttributeDao dao, long productId, long valueId) {
        if (valueId <= 0) return;
        ProductAttribute pa = new ProductAttribute(productId, valueId);
        dao.insert(pa);
    }

    private static long findCategoryId(List<Category> list, String name) {
        for (Category c : list) {
            if (c.getName().contains(name) || name.contains(c.getName())) return c.getId();
        }
        return -1;
    }

    private static long findValueId(List<AttributeValue> list, String value) {
        for (AttributeValue v : list) {
            if (v.getValue().equals(value)) return v.getId();
        }
        return -1;
    }

    private static long createGroup(AttributeGroupDao dao, String name, String now) {
        AttributeGroup group = new AttributeGroup();
        group.setName(name);
        group.setCreatedAt(now);
        return dao.insert(group);
    }

    private static void createValue(AttributeValueDao dao, long groupId, String value, String now) {
        AttributeValue attrValue = new AttributeValue();
        attrValue.setGroupId(groupId);
        attrValue.setValue(value);
        attrValue.setCreatedAt(now);
        dao.insert(attrValue);
    }

    private static String getNow() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}