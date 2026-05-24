package com.heben.clothingstore;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.CategoryDao;

import java.util.List;

public class MainActivity extends BaseActivity {

    private TextView tvShopTitle;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化预置数据
        DatabaseInitializer.initAll(this);

        // 绑定视图
        tvShopTitle = findViewById(R.id.tv_shop_title);
        tvStatus = findViewById(R.id.tv_status);
        loadShopName();

        // 自动备份
        AutoBackupHelper.backupIfNeeded(this);

        // ========== 快捷指令 ==========
        EditText etCommand = findViewById(R.id.et_command);
        Button btnSendCommand = findViewById(R.id.btn_send_command);

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
                MediaSoundHelper.getInstance().playError(MainActivity.this);
                Toast.makeText(this, "请输入指令，例如：卖了连衣裙一件90块", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                CommandParser.ParseResult result = CommandParser.parse(MainActivity.this, cmd);
                runOnUiThread(() -> {
                    if (!result.type.equals("sale") && !result.type.equals("purchase") && !result.type.equals("refund")) {
                        MediaSoundHelper.getInstance().playError(MainActivity.this);
                        Toast.makeText(MainActivity.this, result.message, Toast.LENGTH_LONG).show();
                        return;
                    }
                    showCommandConfirmDialog(result, etCommand);
                });
            }).start();
        });

        // ========== 分类垂直列表 ==========
        LinearLayout llCategoryList = findViewById(R.id.ll_category_list);
        loadCategoryList(llCategoryList);

        // 延迟刷新，确保预置数据写入
        new Handler().postDelayed(() -> {
            runOnUiThread(() -> {
                LinearLayout list = findViewById(R.id.ll_category_list);
                loadCategoryList(list);
            });
        }, 500);

        // ========== 底部导航 ==========
        Button btnNavHome = findViewById(R.id.btn_nav_home);
        Button btnNavBill = findViewById(R.id.btn_nav_bill);
        Button btnNavStock = findViewById(R.id.btn_nav_stock);
        Button btnNavMy = findViewById(R.id.btn_nav_my);

        btnNavHome.setOnClickListener(view ->
                Toast.makeText(this, "已经是首页", Toast.LENGTH_SHORT).show());
        btnNavBill.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, BillActivity.class)));
        btnNavStock.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, PurchaseActivity.class)));
        btnNavMy.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, MyActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadShopName();
        LinearLayout llCategoryList = findViewById(R.id.ll_category_list);
        loadCategoryList(llCategoryList);
    }

    private void loadCategoryList(LinearLayout container) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(MainActivity.this);
            CategoryDao categoryDao = db.categoryDao();
            List<com.heben.clothingstore.entity.Category> categories = categoryDao.getAllCategories();

            runOnUiThread(() -> {
                container.removeAllViews();

                for (com.heben.clothingstore.entity.Category cat : categories) {
                    Button btn = new Button(MainActivity.this);
                    btn.setText(cat.getName());
                    btn.setTextSize(17);
                    btn.setAllCaps(false);
                    btn.setBackground(getDrawable(R.drawable.btn_rounded_primary));
                    btn.setTextColor(getColor(android.R.color.white));
                    btn.setGravity(android.view.Gravity.CENTER);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (56 * getResources().getDisplayMetrics().density)
                    );
                    params.setMargins(0, 0, 0, 12);
                    btn.setLayoutParams(params);

                    btn.setOnClickListener(v -> {
                        Intent intent = new Intent(MainActivity.this, ViewProductsActivity.class);
                        intent.putExtra("category_id", cat.getId());
                        startActivity(intent);
                    });

                    container.addView(btn);
                }

                // 添加“更多分类”按钮（与分类按钮完全同色）
                Button btnMore = new Button(MainActivity.this);
                btnMore.setText("更多分类");
                btnMore.setTextSize(17);
                btnMore.setAllCaps(false);
                btnMore.setBackground(getDrawable(R.drawable.btn_rounded_primary));
                btnMore.setTextColor(getColor(android.R.color.white));
                btnMore.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (int) (56 * getResources().getDisplayMetrics().density)
                );
                moreParams.setMargins(0, 0, 0, 12);
                btnMore.setLayoutParams(moreParams);

                btnMore.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, CategoriesActivity.class);
                    startActivity(intent);
                });

                container.addView(btnMore);
            });
        }).start();
    }

    private void loadShopName() {
        android.content.SharedPreferences prefs = getSharedPreferences("clothing_store_prefs", MODE_PRIVATE);
        String shopName = prefs.getString("shop_name", "我的小店");
        tvShopTitle.setText(shopName);
    }

    private void showCommandConfirmDialog(CommandParser.ParseResult result, EditText etCommand) {
        String typeName = result.type.equals("sale") ? "卖出" :
                result.type.equals("purchase") ? "进货" : "退货";

        double totalAmount = result.price * result.quantity;
        String detail = "类型：" + typeName + "\n" +
                "商品：" + result.productName + "\n" +
                "数量：" + result.quantity + " 件\n" +
                "单价：¥" + result.price + "\n" +
                "总价：¥" + totalAmount;

        new AlertDialog.Builder(this)
                .setTitle("📋 请确认" + typeName + "信息")
                .setMessage(detail)
                .setPositiveButton("确认" + typeName, (dialog, which) -> {
                    new Thread(() -> {
                        CommandParser.executeCommand(MainActivity.this, result);
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "✅ " + typeName + "成功！", Toast.LENGTH_SHORT).show();
                            if (result.type.equals("sale")) {
                                MediaSoundHelper.getInstance().playSale(MainActivity.this);
                            } else if (result.type.equals("purchase")) {
                                MediaSoundHelper.getInstance().playPurchase(MainActivity.this);
                            }
                            etCommand.setText("");
                        });
                    }).start();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    MediaSoundHelper.getInstance().playCancel(MainActivity.this);
                })
                .show();
    }
}