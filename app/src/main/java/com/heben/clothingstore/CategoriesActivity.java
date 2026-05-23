package com.heben.clothingstore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.app.AlertDialog;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.CategoryDao;
import com.heben.clothingstore.entity.Category;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private EditText etCategoryName;
    private Button btnAddCategory;
    private CategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        etCategoryName = findViewById(R.id.et_category_name);
        btnAddCategory = findViewById(R.id.btn_add_category);
        rvCategories = findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        // 先设置空 Adapter
        adapter = new CategoryAdapter(categoryList);
        rvCategories.setAdapter(adapter);

        // 加载分类列表
        loadCategories();

        // 点击添加
        btnAddCategory.setOnClickListener(view -> addCategory());
    }

    private void loadCategories() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(CategoriesActivity.this);
            CategoryDao categoryDao = db.categoryDao();
            List<Category> categories = categoryDao.getAllCategories();

            runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(categories);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void addCategory() {
        String name = etCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入分类名", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(CategoriesActivity.this);
            CategoryDao categoryDao = db.categoryDao();

            Category category = new Category();
            category.setName(name);
            category.setParentId(0);    // 新增的分类都是一级分类
            category.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long id = categoryDao.insert(category);

            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(CategoriesActivity.this, "分类添加成功！", Toast.LENGTH_SHORT).show();
                    etCategoryName.setText("");    // 清空输入框
                    loadCategories();              // 刷新列表
                } else {
                    Toast.makeText(CategoriesActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void deleteCategory(Category category) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(CategoriesActivity.this);
            CategoryDao categoryDao = db.categoryDao();
            categoryDao.delete(category);

            runOnUiThread(() -> {
                Toast.makeText(CategoriesActivity.this, "已删除: " + category.getName(), Toast.LENGTH_SHORT).show();
                loadCategories();  // 刷新列表
            });
        }).start();
    }

    // ========== 适配器 ==========
    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

        private List<Category> list;

        public CategoryAdapter(List<Category> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category category = list.get(position);
            holder.tvName.setText(category.getName());
            holder.btnDelete.setOnClickListener(view -> {
                new AlertDialog.Builder(CategoriesActivity.this)
                        .setTitle("删除分类")
                        .setMessage("确定要删除「" + category.getName() + "」吗？")
                        .setPositiveButton("删除", (dialog, which) -> deleteCategory(category))
                        .setNegativeButton("取消", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            Button btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_category_name);
                btnDelete = itemView.findViewById(R.id.btn_delete_category);
            }
        }
    }
}