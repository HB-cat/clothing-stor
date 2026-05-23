package com.heben.clothingstore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.ProductDao;
import com.heben.clothingstore.entity.ProductStock;

import java.util.ArrayList;
import java.util.List;

public class InventoryActivity extends AppCompatActivity {

    private RecyclerView rvInventory;
    private InventoryAdapter adapter;
    private List<ProductStock> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        rvInventory = findViewById(R.id.rv_inventory);
        rvInventory.setLayoutManager(new LinearLayoutManager(this));

        adapter = new InventoryAdapter(productList);
        rvInventory.setAdapter(adapter);

        loadInventory();
    }

    private void loadInventory() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(InventoryActivity.this);
            ProductDao productDao = db.productDao();
            List<ProductStock> products = productDao.getProductsWithStock();

            runOnUiThread(() -> {
                productList.clear();
                productList.addAll(products);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {

        private List<ProductStock> list;

        public InventoryAdapter(List<ProductStock> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_inventory, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductStock product = list.get(position);
            holder.tvName.setText(product.getName());

            int stock = product.getCurrentStock();
            holder.tvStock.setText(String.valueOf(stock));

            if (stock <= 5) {
                holder.tvStock.setTextColor(0xFFFF5252);
            } else {
                holder.tvStock.setTextColor(0xFF4CAF50);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvStock;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_inventory_product_name);
                tvStock = itemView.findViewById(R.id.tv_inventory_stock);
            }
        }
    }
}