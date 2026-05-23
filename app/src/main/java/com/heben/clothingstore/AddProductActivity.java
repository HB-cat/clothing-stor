package com.heben.clothingstore;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.ProductDao;
import com.heben.clothingstore.dao.ProductAttributeDao;
import com.heben.clothingstore.dao.CategoryDao;
import com.heben.clothingstore.dao.AttributeGroupDao;
import com.heben.clothingstore.dao.AttributeValueDao;
import com.heben.clothingstore.entity.Product;
import com.heben.clothingstore.entity.Category;
import com.heben.clothingstore.entity.AttributeGroup;
import com.heben.clothingstore.entity.AttributeValue;
import com.heben.clothingstore.entity.ProductAttribute;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddProductActivity extends AppCompatActivity {

    private EditText etProductName;
    private Spinner spCategory;
    private EditText etCostPrice;
    private EditText etSellingPrice;
    private Button btnSaveProduct;
    private LinearLayout llAttributes;

    private List<Category> categoryList = new ArrayList<>();
    private List<AttributeGroup> groupList = new ArrayList<>();
    // 存储每个属性组对应的Spinner，key=groupId, value=Spinner
    private Map<Long, Spinner> spinnerMap = new HashMap<>();
    // 存储每个属性组对应的属性值列表，key=groupId, value=List<AttributeValue>
    private Map<Long, List<AttributeValue>> valueMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        etProductName = findViewById(R.id.et_product_name);
        spCategory = findViewById(R.id.sp_category);
        etCostPrice = findViewById(R.id.et_cost_price);
        etSellingPrice = findViewById(R.id.et_selling_price);
        btnSaveProduct = findViewById(R.id.btn_save_product);
        llAttributes = findViewById(R.id.ll_attributes);

        // 加载分类
        loadCategories();

        // 加载属性组和属性值
        loadAttributes();

        btnSaveProduct.setOnClickListener(view -> saveProduct());
    }

    private void loadCategories() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AddProductActivity.this);
            CategoryDao dao = db.categoryDao();
            List<Category> list = dao.getAllCategories();

            runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(list);
                List<String> names = new ArrayList<>();
                for (Category c : list) {
                    names.add(c.getName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddProductActivity.this,
                        android.R.layout.simple_spinner_item,
                        names
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCategory.setAdapter(adapter);
            });
        }).start();
    }

    private void loadAttributes() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AddProductActivity.this);
            AttributeGroupDao groupDao = db.attributeGroupDao();
            AttributeValueDao valueDao = db.attributeValueDao();

            List<AttributeGroup> groups = groupDao.getAllGroups();

            runOnUiThread(() -> {
                groupList.clear();
                groupList.addAll(groups);
                llAttributes.removeAllViews();
                spinnerMap.clear();
                valueMap.clear();

                // 为每个属性组创建一个标签 + 下拉框
                for (AttributeGroup group : groups) {
                    // 标签
                    android.widget.TextView label = new android.widget.TextView(AddProductActivity.this);
                    label.setText(group.getName());
                    label.setTextSize(16);
                    label.setTextColor(0xFF666666);
                    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    labelParams.setMargins(0, 12, 0, 8);
                    label.setLayoutParams(labelParams);
                    llAttributes.addView(label);

                    // 下拉框
                    Spinner spinner = new Spinner(AddProductActivity.this);
                    spinner.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            120
                    ));
                    spinner.setBackground(getDrawable(R.drawable.edit_bg));
                    spinner.setPadding(12, 12, 12, 12);
                    llAttributes.addView(spinner);

                    spinnerMap.put(group.getId(), spinner);

                    // 加载该组的属性值
                    loadValuesForGroup(group.getId(), spinner);
                }
            });
        }).start();
    }

    private void loadValuesForGroup(long groupId, Spinner spinner) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AddProductActivity.this);
            AttributeValueDao valueDao = db.attributeValueDao();
            List<AttributeValue> values = valueDao.getByGroupId(groupId);

            runOnUiThread(() -> {
                valueMap.put(groupId, values);
                List<String> names = new ArrayList<>();
                names.add("不选");  // 第一个选项为空
                for (AttributeValue v : values) {
                    names.add(v.getValue());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AddProductActivity.this,
                        android.R.layout.simple_spinner_item,
                        names
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
            });
        }).start();
    }

    private void saveProduct() {
        String name = etProductName.getText().toString().trim();
        String costStr = etCostPrice.getText().toString().trim();
        String sellingStr = etSellingPrice.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (costStr.isEmpty()) {
            Toast.makeText(this, "请输入进价", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sellingStr.isEmpty()) {
            Toast.makeText(this, "请输入售价", Toast.LENGTH_SHORT).show();
            return;
        }

        double costPrice = Double.parseDouble(costStr);
        double sellingPrice = Double.parseDouble(sellingStr);

        // 获取选中的分类ID
        int catPosition = spCategory.getSelectedItemPosition();
        long categoryId = -1;
        if (catPosition >= 0 && catPosition < categoryList.size()) {
            categoryId = categoryList.get(catPosition).getId();
        }

        // 收集选中的属性值ID
        List<Long> selectedValueIds = new ArrayList<>();
        for (AttributeGroup group : groupList) {
            Spinner spinner = spinnerMap.get(group.getId());
            if (spinner == null) continue;
            int pos = spinner.getSelectedItemPosition();
            if (pos <= 0) continue;  // "不选"
            List<AttributeValue> values = valueMap.get(group.getId());
            if (values != null && pos - 1 < values.size()) {
                selectedValueIds.add(values.get(pos - 1).getId());
            }
        }

        // 保存到数据库
        final long finalCategoryId = categoryId;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AddProductActivity.this);
            ProductDao productDao = db.productDao();

            Product product = new Product();
            product.setName(name);
            product.setCategoryId(finalCategoryId);
            product.setCostPrice(costPrice);
            product.setSellingPrice(sellingPrice);
            product.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long productId = productDao.insert(product);

            if (productId > 0) {
                // 保存属性关联
                ProductAttributeDao paDao = db.productAttributeDao();
                for (long valueId : selectedValueIds) {
                    paDao.insert(new ProductAttribute(productId, valueId));
                }
            }

            runOnUiThread(() -> {
                if (productId > 0) {
                    Toast.makeText(AddProductActivity.this, "商品添加成功！", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddProductActivity.this, "添加失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}