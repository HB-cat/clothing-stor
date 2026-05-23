package com.heben.clothingstore;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.*;
import com.heben.clothingstore.entity.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PurchaseActivity extends AppCompatActivity {

    private Spinner spProduct;
    private LinearLayout llRows;
    private TextView tvTotalPrice;
    private Button btnAddRow, btnSave;
    private List<Product> productList = new ArrayList<>();
    private List<AttributeGroup> allGroups = new ArrayList<>();
    private Map<Long, List<AttributeValue>> valuesMap = new HashMap<>();
    // 每一行 → (属性组ID → 该组对应的 Spinner)
    private Map<View, Map<Long, Spinner>> rowAttrSpinners = new HashMap<>();
    private Map<View, EditText> rowQtyEdits = new HashMap<>();
    private Map<View, EditText> rowPriceEdits = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        spProduct = findViewById(R.id.sp_product);
        llRows = findViewById(R.id.ll_rows);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        btnAddRow = findViewById(R.id.btn_add_row);
        btnSave = findViewById(R.id.btn_save_purchase);

        loadProducts();
        loadAllAttributeGroups();

        btnAddRow.setOnClickListener(v -> addRow());
        btnSave.setOnClickListener(v -> savePurchase());
    }

    private void loadProducts() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Product> list = db.productDao().getAllProducts();
            runOnUiThread(() -> {
                productList.clear();
                productList.addAll(list);
                List<String> names = new ArrayList<>();
                for (Product p : list) names.add(p.getName());
                spProduct.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names));
            });
        }).start();
    }

    private void loadAllAttributeGroups() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            allGroups = db.attributeGroupDao().getAllGroups();
            valuesMap.clear();
            for (AttributeGroup g : allGroups) {
                List<AttributeValue> vals = db.attributeValueDao().getByGroupId(g.getId());
                valuesMap.put(g.getId(), vals);
            }
            runOnUiThread(() -> {
                // 添加默认第一行
                addRow();
            });
        }).start();
    }

    private void addRow() {
        View row = LayoutInflater.from(this).inflate(R.layout.item_purchase_row, llRows, false);
        LinearLayout llAttrSpinners = row.findViewById(R.id.ll_attr_spinners);
        EditText etQty = row.findViewById(R.id.et_quantity);
        EditText etPrice = row.findViewById(R.id.et_price);
        Button btnRemove = row.findViewById(R.id.btn_remove_row);

        // 为每个属性组创建下拉框
        Map<Long, Spinner> spinnerMap = new HashMap<>();
        for (AttributeGroup group : allGroups) {
            // 创建一个小容器包含标签和下拉框
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            colParams.setMargins(0, 0, 6, 0);
            col.setLayoutParams(colParams);

            TextView label = new TextView(this);
            label.setText(group.getName());
            label.setTextSize(11);
            label.setTextColor(0xFF888888);

            Spinner spinner = new Spinner(this);
            spinner.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 80));
            spinner.setBackground(getDrawable(R.drawable.edit_bg));
            spinner.setPadding(4, 4, 4, 4);

            List<AttributeValue> values = valuesMap.get(group.getId());
            List<String> names = new ArrayList<>();
            names.add("不选");
            if (values != null) {
                for (AttributeValue v : values) names.add(v.getValue());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            col.addView(label);
            col.addView(spinner);
            llAttrSpinners.addView(col);
            spinnerMap.put(group.getId(), spinner);
        }

        // 监听数量、单价变化，更新总价
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateTotal(); }
        };
        etQty.addTextChangedListener(watcher);
        etPrice.addTextChangedListener(watcher);

        // 删除行
        btnRemove.setOnClickListener(v -> {
            llRows.removeView(row);
            rowAttrSpinners.remove(row);
            rowQtyEdits.remove(row);
            rowPriceEdits.remove(row);
            updateTotal();
        });

        // 保存引用
        rowAttrSpinners.put(row, spinnerMap);
        rowQtyEdits.put(row, etQty);
        rowPriceEdits.put(row, etPrice);

        llRows.addView(row);
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (View row : new HashSet<>(rowQtyEdits.keySet())) {
            EditText etQty = rowQtyEdits.get(row);
            EditText etPrice = rowPriceEdits.get(row);
            if (etQty == null || etPrice == null) continue;
            try {
                int qty = Integer.parseInt(etQty.getText().toString().trim());
                double price = Double.parseDouble(etPrice.getText().toString().trim());
                total += qty * price;
            } catch (NumberFormatException ignored) {}
        }
        tvTotalPrice.setText("总进货价：¥" + total);
    }

    private void savePurchase() {
        int productPos = spProduct.getSelectedItemPosition();
        if (productPos < 0 || productPos >= productList.size()) {
            Toast.makeText(this, "请选择商品", Toast.LENGTH_SHORT).show();
            return;
        }
        Product product = productList.get(productPos);
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        List<Purchase> purchases = new ArrayList<>();
        for (View row : new HashSet<>(rowQtyEdits.keySet())) {
            EditText etQty = rowQtyEdits.get(row);
            EditText etPrice = rowPriceEdits.get(row);
            Map<Long, Spinner> spinnerMap = rowAttrSpinners.get(row);
            if (etQty == null || etPrice == null || spinnerMap == null) continue;

            // 构建属性描述：风格:复古风 / 面料:纯棉 / 颜色:红色 / 尺码:M
            StringBuilder descBuilder = new StringBuilder();
            for (AttributeGroup group : allGroups) {
                Spinner spinner = spinnerMap.get(group.getId());
                if (spinner == null) continue;
                int pos = spinner.getSelectedItemPosition();
                if (pos <= 0) continue; // 不选
                List<AttributeValue> values = valuesMap.get(group.getId());
                if (values != null && pos - 1 < values.size()) {
                    if (descBuilder.length() > 0) descBuilder.append(" / ");
                    descBuilder.append(values.get(pos - 1).getValue());
                }
            }
            String desc = descBuilder.toString();
            if (desc.isEmpty()) desc = "默认";

            String qtyStr = etQty.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            if (qtyStr.isEmpty() || priceStr.isEmpty()) continue;
            int qty = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);
            if (qty <= 0) continue;

            Purchase p = new Purchase();
            p.setProductId(product.getId());
            p.setQuantity(qty);
            p.setCostPrice(price);
            p.setPurchaseDate(today);
            p.setAttributeDesc(desc);
            p.setCreatedAt(now);
            purchases.add(p);
        }

        if (purchases.isEmpty()) {
            Toast.makeText(this, "请至少填写一行有效的进货信息", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            PurchaseDao dao = db.purchaseDao();
            for (Purchase p : purchases) dao.insert(p);
            runOnUiThread(() -> {
                Toast.makeText(this, "入库成功！共" + purchases.size() + "条记录", Toast.LENGTH_SHORT).show();
                // 清空所有行，保留一行空白
                llRows.removeAllViews();
                rowAttrSpinners.clear();
                rowQtyEdits.clear();
                rowPriceEdits.clear();
                addRow();
            });
        }).start();
    }
}