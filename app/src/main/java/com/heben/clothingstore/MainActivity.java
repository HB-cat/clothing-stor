package com.heben.clothingstore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.CategoryDao;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvLastSync;
    private TextView tvShopTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化预置数据
        DatabaseInitializer.initAll(this);
        // 自动备份（每天首次启动时执行）
        AutoBackupHelper.backupIfNeeded(this);

        // 绑定视图
        tvStatus = findViewById(R.id.tv_status);
        tvLastSync = findViewById(R.id.tv_last_sync);
        tvShopTitle = findViewById(R.id.tv_shop_title);

        // 加载店铺名称
        loadShopName();

        // ========== 快捷指令 ==========
        EditText etCommand = findViewById(R.id.et_command);
        Button btnSendCommand = findViewById(R.id.btn_send_command);

        // 输入框获得焦点时显示提示
        etCommand.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                Toast.makeText(MainActivity.this,
                        "📢 可用格式：\n• 卖了[商品] [数量] [价格]\n• 进了[商品] [数量] [价格]\n• 退了[商品] [数量]",
                        Toast.LENGTH_LONG).show();
            }
        });

        btnSendCommand.setOnClickListener(view -> {
            String cmd = etCommand.getText().toString().trim();
            if (cmd.isEmpty()) {
                Toast.makeText(MainActivity.this, "请输入指令，例如：卖了连衣裙一件90块", Toast.LENGTH_SHORT).show();
                return;
            }

            // 在后台线程解析并执行
            new Thread(() -> {
                CommandParser.ParseResult result = CommandParser.parse(MainActivity.this, cmd);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, result.message, Toast.LENGTH_LONG).show();
                    if (result.type.equals("sale") || result.type.equals("purchase") || result.type.equals("refund")) {
                        etCommand.setText(""); // 清空输入框
                    }
                });
            }).start();
        });

        // ========== 分类网格按钮（动态加载） ==========
        LinearLayout llRow1 = findViewById(R.id.ll_cat_row1);
        LinearLayout llRow2 = findViewById(R.id.ll_cat_row2);
        loadCategoryButtons(llRow1, llRow2);

        // ========== 底部导航 ==========
        Button btnNavHome = findViewById(R.id.btn_nav_home);
        Button btnNavBill = findViewById(R.id.btn_nav_bill);
        Button btnNavStock = findViewById(R.id.btn_nav_stock);
        Button btnNavMy = findViewById(R.id.btn_nav_my);

        btnNavHome.setOnClickListener(view ->
                Toast.makeText(this, "已经是首页", Toast.LENGTH_SHORT).show()
        );

        btnNavBill.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, BillActivity.class))
        );

        btnNavStock.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, PurchaseActivity.class))
        );

        btnNavMy.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, MyActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到首页时刷新店铺名称和分类网格
        loadShopName();
        LinearLayout llRow1 = findViewById(R.id.ll_cat_row1);
        LinearLayout llRow2 = findViewById(R.id.ll_cat_row2);
        loadCategoryButtons(llRow1, llRow2);
    }

    private void loadCategoryButtons(LinearLayout row1, LinearLayout row2) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(MainActivity.this);
            CategoryDao categoryDao = db.categoryDao();
            List<com.heben.clothingstore.entity.Category> categories = categoryDao.getAllCategories();

            runOnUiThread(() -> {
                row1.removeAllViews();
                row2.removeAllViews();

                int maxShow = Math.min(categories.size(), 5);

                for (int i = 0; i < maxShow; i++) {
                    com.heben.clothingstore.entity.Category cat = categories.get(i);
                    Button btn = new Button(MainActivity.this);
                    btn.setText(getCategoryEmoji(cat.getName()) + "\n" + cat.getName());
                    btn.setTextSize(16);
                    btn.setLayoutParams(new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                    ));
                    ((LinearLayout.LayoutParams) btn.getLayoutParams()).setMargins(4, 4, 4, 4);
                    btn.setAllCaps(false);

                    btn.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, ViewProductsActivity.class);
                        intent.putExtra("category_id", cat.getId());
                        startActivity(intent);
                    });

                    if (i < 3) {
                        row1.addView(btn);
                    } else {
                        row2.addView(btn);
                    }
                }

                // "更多..."按钮
                Button btnMore = new Button(MainActivity.this);
                btnMore.setText("📂\n更多...");
                btnMore.setTextSize(16);
                btnMore.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1
                ));
                ((LinearLayout.LayoutParams) btnMore.getLayoutParams()).setMargins(4, 4, 4, 4);
                btnMore.setAllCaps(false);
                btnMore.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE0E0E0));
                btnMore.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                    startActivity(intent);
                });

                if (maxShow < 3) {
                    row1.addView(btnMore);
                } else {
                    row2.addView(btnMore);
                }
            });
        }).start();
    }

    private String getCategoryEmoji(String name) {
        if (name.contains("连衣裙") || name.contains("裙")) return "👗";
        if (name.contains("T恤") || name.contains("恤")) return "🎽";
        if (name.contains("裤")) return "👖";
        if (name.contains("衬衫")) return "👔";
        if (name.contains("防晒")) return "🧴";
        if (name.contains("套装")) return "👘";
        return "📁";
    }

    /** 从设置中读取店铺名称并更新首页标题 */
    private void loadShopName() {
        android.content.SharedPreferences prefs = getSharedPreferences("clothing_store_prefs", MODE_PRIVATE);
        String shopName = prefs.getString("shop_name", "我的小店");
        tvShopTitle.setText(shopName + " — 服装店记账系统");
    }
}