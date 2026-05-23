package com.heben.clothingstore;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.ProductDao;
import com.heben.clothingstore.dao.SaleDao;
import com.heben.clothingstore.entity.Product;
import com.heben.clothingstore.entity.Sale;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewProductsActivity extends AppCompatActivity {

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private long categoryId = -1; // -1 表示显示全部

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_products);

        // 获取可能传来的分类ID
        categoryId = getIntent().getLongExtra("category_id", -1);

        rvProducts = findViewById(R.id.rv_products);
        rvProducts.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProductAdapter(productList);
        rvProducts.setAdapter(adapter);

        loadProducts();
    }

    private void loadProducts() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(ViewProductsActivity.this);
            ProductDao productDao = db.productDao();
            List<Product> products;
            if (categoryId > 0) {
                products = productDao.getProductsByCategory(categoryId);
            } else {
                products = productDao.getAllProducts();
            }

            runOnUiThread(() -> {
                productList.clear();
                productList.addAll(products);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    // 弹出销售确认对话框
    private void showSaleDialog(Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("记一笔 - " + product.getName());

        // 自定义布局
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_sale, null);
        EditText etQuantity = view.findViewById(R.id.et_quantity);
        EditText etPrice = view.findViewById(R.id.et_price);
        TextView tvDefaultPrice = view.findViewById(R.id.tv_default_price);

        etQuantity.setText("1");
        etPrice.setText(String.valueOf(product.getSellingPrice()));
        tvDefaultPrice.setText("默认售价: ¥" + product.getSellingPrice());

        builder.setView(view);

        builder.setPositiveButton("确认卖出", (dialog, which) -> {
            String qtyStr = etQuantity.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (qtyStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "请填写数量和价格", Toast.LENGTH_SHORT).show();
                return;
            }

            int quantity = Integer.parseInt(qtyStr);
            double price = Double.parseDouble(priceStr);

            if (quantity <= 0) {
                Toast.makeText(this, "数量必须大于0", Toast.LENGTH_SHORT).show();
                return;
            }

            // 执行销售
            sellProduct(product, quantity, price);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void sellProduct(Product product, int quantity, double price) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(ViewProductsActivity.this);
            SaleDao saleDao = db.saleDao();

            Sale sale = new Sale();
            sale.setProductId(product.getId());
            sale.setQuantity(quantity);
            sale.setSellingPrice(price);
            sale.setCostPrice(product.getCostPrice()); // 记录当时进价
            sale.setSaleDate(getCurrentDate());
            sale.setSaleTime(getCurrentTime());
            sale.setRefunded(false);
            sale.setCreatedAt(getCurrentDateTime());

            long id = saleDao.insert(sale);

            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(ViewProductsActivity.this,
                            "记账成功！卖出 " + quantity + " 件，¥" + (price * quantity),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ViewProductsActivity.this, "记账失败，请重试", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ========== 辅助方法 ==========
    private String getCurrentDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    // ========== 适配器 ==========
    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

        private List<Product> list;

        public ProductAdapter(List<Product> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = list.get(position);
            holder.tvName.setText(product.getName());
            holder.tvCost.setText("进价: ¥" + product.getCostPrice());
            holder.tvSelling.setText("售价: ¥" + product.getSellingPrice());

            // 点击整个条目弹出销售对话框
            holder.itemView.setOnClickListener(view -> showSaleDialog(product));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCost, tvSelling;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_product_name);
                tvCost = itemView.findViewById(R.id.tv_cost_price);
                tvSelling = itemView.findViewById(R.id.tv_selling_price);
            }
        }
    }
}