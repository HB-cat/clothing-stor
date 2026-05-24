package com.heben.clothingstore;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddProductActivity extends BaseActivity {

    private EditText etProductName;
    private Spinner spCategory;
    private EditText etCostPrice;
    private EditText etSellingPrice;
    private Button btnSaveProduct;
    private LinearLayout llAttributes;
    private ImageView ivPhoto;
    private Button btnTakePhoto;

    private List<Category> categoryList = new ArrayList<>();
    private List<AttributeGroup> groupList = new ArrayList<>();
    private Map<Long, Spinner> spinnerMap = new HashMap<>();
    private Map<Long, List<AttributeValue>> valueMap = new HashMap<>();

    private String photoPath = null;          // 保存的照片路径
    private static final int REQUEST_TAKE_PHOTO = 100;

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
        ivPhoto = findViewById(R.id.iv_product_photo);
        btnTakePhoto = findViewById(R.id.btn_take_photo);

        // 拍照按钮
        btnTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                        "product_" + System.currentTimeMillis() + ".jpg");
                photoPath = photoFile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(this,
                        "com.heben.clothingstore.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            } else {
                Toast.makeText(this, "设备不支持拍照", Toast.LENGTH_SHORT).show();
            }
        });

        loadCategories();
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
                for (Category c : list) names.add(c.getName());
                spCategory.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, names));
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

                for (AttributeGroup group : groups) {
                    android.widget.TextView label = new android.widget.TextView(AddProductActivity.this);
                    label.setText(group.getName());
                    label.setTextSize(16);
                    label.setTextColor(0xFF666666);
                    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    labelParams.setMargins(0, 12, 0, 8);
                    label.setLayoutParams(labelParams);
                    llAttributes.addView(label);

                    Spinner spinner = new Spinner(AddProductActivity.this);
                    spinner.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 120));
                    spinner.setBackground(getDrawable(R.drawable.edit_bg));
                    spinner.setPadding(12, 12, 12, 12);
                    llAttributes.addView(spinner);
                    spinnerMap.put(group.getId(), spinner);
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
                names.add("不选");
                for (AttributeValue v : values) names.add(v.getValue());
                spinner.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, names));
            });
        }).start();
    }

    private void saveProduct() {
        String name = etProductName.getText().toString().trim();
        String costStr = etCostPrice.getText().toString().trim();
        String sellingStr = etSellingPrice.getText().toString().trim();

        if (name.isEmpty()) {
            MediaSoundHelper.getInstance().playError(AddProductActivity.this);
            Toast.makeText(this, "请输入商品名称", Toast.LENGTH_SHORT).show();
            return;
        }
        if (costStr.isEmpty()) {
            MediaSoundHelper.getInstance().playError(AddProductActivity.this);
            Toast.makeText(this, "请输入进价", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sellingStr.isEmpty()) {
            MediaSoundHelper.getInstance().playError(AddProductActivity.this);
            Toast.makeText(this, "请输入售价", Toast.LENGTH_SHORT).show();
            return;
        }

        double costPrice = Double.parseDouble(costStr);
        double sellingPrice = Double.parseDouble(sellingStr);

        int catPosition = spCategory.getSelectedItemPosition();
        long categoryId = -1;
        if (catPosition >= 0 && catPosition < categoryList.size()) {
            categoryId = categoryList.get(catPosition).getId();
        }

        List<Long> selectedValueIds = new ArrayList<>();
        for (AttributeGroup group : groupList) {
            Spinner spinner = spinnerMap.get(group.getId());
            if (spinner == null) continue;
            int pos = spinner.getSelectedItemPosition();
            if (pos <= 0) continue;
            List<AttributeValue> values = valueMap.get(group.getId());
            if (values != null && pos - 1 < values.size()) {
                selectedValueIds.add(values.get(pos - 1).getId());
            }
        }

        final long finalCategoryId = categoryId;
        final String finalPhotoPath = photoPath;  // 拍照路径
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AddProductActivity.this);
            ProductDao productDao = db.productDao();

            Product product = new Product();
            product.setName(name);
            product.setCategoryId(finalCategoryId);
            product.setCostPrice(costPrice);
            product.setSellingPrice(sellingPrice);
            product.setImagePath(finalPhotoPath);  // 保存照片路径
            product.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long productId = productDao.insert(product);

            if (productId > 0) {
                ProductAttributeDao paDao = db.productAttributeDao();
                for (long valueId : selectedValueIds) {
                    paDao.insert(new ProductAttribute(productId, valueId));
                }
            }

            runOnUiThread(() -> {
                if (productId > 0) {
                    Toast.makeText(AddProductActivity.this, "商品添加成功！", Toast.LENGTH_SHORT).show();
                    MediaSoundHelper.getInstance().playSuccess(AddProductActivity.this);
                    finish();
                } else {
                    Toast.makeText(AddProductActivity.this, "添加失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (photoPath != null) {
                ivPhoto.setImageURI(Uri.fromFile(new File(photoPath)));
            }
        }
    }
}