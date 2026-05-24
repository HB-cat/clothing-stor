package com.heben.clothingstore;

import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.*;
import com.heben.clothingstore.entity.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class PurchaseActivity extends BaseActivity {

    private Spinner spProduct, spSize, spColor;
    private EditText etCostPrice, etQuantity;
    private LinearLayout llOtherAttrs;
    private TextView tvTotalPrice;
    private Button btnSave;

    private List<Product> productList = new ArrayList<>();
    private List<AttributeGroup> allGroups = new ArrayList<>();       // 所有属性组
    private List<AttributeGroup> otherGroups = new ArrayList<>();     // 其他属性组（排除尺码、颜色）
    private Map<Long, List<AttributeValue>> valuesMap = new HashMap<>();
    private Map<Long, Spinner> otherAttrSpinners = new HashMap<>();

    // 尺码、颜色属性组ID
    private long sizeGroupId = -1;
    private long colorGroupId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        spProduct = findViewById(R.id.sp_product);
        spSize = findViewById(R.id.sp_size);
        spColor = findViewById(R.id.sp_color);
        etCostPrice = findViewById(R.id.et_cost_price);
        etQuantity = findViewById(R.id.et_quantity);
        llOtherAttrs = findViewById(R.id.ll_other_attrs);
        tvTotalPrice = findViewById(R.id.tv_total_price);
        btnSave = findViewById(R.id.btn_save_purchase);

        loadProducts();
        loadAllAttributeGroups();

        spProduct.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < productList.size()) {
                    Product selected = productList.get(position);
                    // 自动填充默认进价
                    etCostPrice.setText(String.valueOf(selected.getCostPrice()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 监听进货价和数量变化，实时更新总金额
        TextWatcher totalWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateTotal(); }
        };
        etCostPrice.addTextChangedListener(totalWatcher);
        etQuantity.addTextChangedListener(totalWatcher);

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

            // 找到尺码和颜色组ID
            for (AttributeGroup g : allGroups) {
                if ("尺码".equals(g.getName())) {
                    sizeGroupId = g.getId();
                } else if ("颜色".equals(g.getName())) {
                    colorGroupId = g.getId();
                }
            }

            // 筛选“其他属性”（排除尺码、颜色）
            otherGroups.clear();
            for (AttributeGroup g : allGroups) {
                if (g.getId() != sizeGroupId && g.getId() != colorGroupId) {
                    otherGroups.add(g);
                }
            }

            // 加载所有属性值
            valuesMap.clear();
            for (AttributeGroup g : allGroups) {
                List<AttributeValue> vals = db.attributeValueDao().getByGroupId(g.getId());
                valuesMap.put(g.getId(), vals);
            }

            runOnUiThread(() -> buildAttrViews());
        }).start();
    }

    private void buildAttrViews() {
        // 加载尺码下拉框
        if (sizeGroupId > 0) {
            loadSpinnerValues(spSize, sizeGroupId);
        }
        // 加载颜色下拉框
        if (colorGroupId > 0) {
            loadSpinnerValues(spColor, colorGroupId);
        }

        // 清空并重新生成“其他属性”下拉框
        llOtherAttrs.removeAllViews();
        otherAttrSpinners.clear();

        for (AttributeGroup group : otherGroups) {
            // 外层水平容器：让标签和下拉框在同一行
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 8, 0, 8);
            row.setLayoutParams(rowParams);

            // 标签：属性名：
            TextView label = new TextView(this);
            label.setText(group.getName() + "：");
            label.setTextSize(16);
            label.setTextColor(0xFF666666);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            row.addView(label);

            // 下拉框：具体属性值
            Spinner spinner = new Spinner(this);
            spinner.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    96,
                    1
            ));
            spinner.setBackground(getDrawable(R.drawable.edit_bg));
            spinner.setPadding(12, 12, 12, 12);
            row.addView(spinner);

            llOtherAttrs.addView(row);

            loadSpinnerValues(spinner, group.getId());
            otherAttrSpinners.put(group.getId(), spinner);
        }
    }

    private void loadSpinnerValues(Spinner spinner, long groupId) {
        List<AttributeValue> values = valuesMap.get(groupId);
        List<String> names = new ArrayList<>();
        names.add("不选");
        if (values != null) {
            for (AttributeValue v : values) names.add(v.getValue());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void updateTotal() {
        try {
            double price = Double.parseDouble(etCostPrice.getText().toString().trim());
            int qty = Integer.parseInt(etQuantity.getText().toString().trim());
            tvTotalPrice.setText("总金额：¥" + (price * qty));
        } catch (NumberFormatException e) {
            tvTotalPrice.setText("总金额：¥0");
        }
    }

    private void savePurchase() {
        int productPos = spProduct.getSelectedItemPosition();
        if (productPos < 0 || productPos >= productList.size()) {
            MediaSoundHelper.getInstance().playError(PurchaseActivity.this);
            Toast.makeText(this, "请选择商品", Toast.LENGTH_SHORT).show();
            return;
        }
        String priceStr = etCostPrice.getText().toString().trim();
        String qtyStr = etQuantity.getText().toString().trim();
        if (priceStr.isEmpty() || qtyStr.isEmpty()) {
            MediaSoundHelper.getInstance().playError(PurchaseActivity.this);
            Toast.makeText(this, "请填写进货价和数量", Toast.LENGTH_SHORT).show();
            return;
        }
        double price = Double.parseDouble(priceStr);
        int qty = Integer.parseInt(qtyStr);
        if (qty <= 0) {
            MediaSoundHelper.getInstance().playError(PurchaseActivity.this);
            Toast.makeText(this, "数量必须大于0", Toast.LENGTH_SHORT).show();
            return;
        }

        Product product = productList.get(productPos);
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 构建属性描述
        StringBuilder desc = new StringBuilder();

        // 尺码
        int sizePos = spSize.getSelectedItemPosition();
        if (sizePos > 0 && sizeGroupId > 0) {
            List<AttributeValue> sizeVals = valuesMap.get(sizeGroupId);
            if (sizeVals != null && sizePos - 1 < sizeVals.size()) {
                desc.append(sizeVals.get(sizePos - 1).getValue());
            }
        }

        // 颜色
        int colorPos = spColor.getSelectedItemPosition();
        if (colorPos > 0 && colorGroupId > 0) {
            List<AttributeValue> colorVals = valuesMap.get(colorGroupId);
            if (colorVals != null && colorPos - 1 < colorVals.size()) {
                if (desc.length() > 0) desc.append(" / ");
                desc.append(colorVals.get(colorPos - 1).getValue());
            }
        }

        // 其他属性
        for (AttributeGroup group : otherGroups) {
            Spinner spinner = otherAttrSpinners.get(group.getId());
            if (spinner == null) continue;
            int pos = spinner.getSelectedItemPosition();
            if (pos <= 0) continue;
            List<AttributeValue> vals = valuesMap.get(group.getId());
            if (vals != null && pos - 1 < vals.size()) {
                if (desc.length() > 0) desc.append(" / ");
                desc.append(vals.get(pos - 1).getValue());
            }
        }

        String attrDesc = desc.length() > 0 ? desc.toString() : "默认";

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            PurchaseDao dao = db.purchaseDao();

            Purchase purchase = new Purchase();
            purchase.setProductId(product.getId());
            purchase.setQuantity(qty);
            purchase.setCostPrice(price);
            purchase.setPurchaseDate(today);
            purchase.setAttributeDesc(attrDesc);
            purchase.setCreatedAt(now);

            dao.insert(purchase);

            runOnUiThread(() -> {
                Toast.makeText(this, "入库成功！", Toast.LENGTH_SHORT).show();
                MediaSoundHelper.getInstance().playPurchase(PurchaseActivity.this);
                etQuantity.setText("");
                updateTotal();
            });
        }).start();
    }
}