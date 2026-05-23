package com.heben.clothingstore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * "我的"页面：整合店铺设置、可拖拽排序的功能菜单、固定功能、关于信息。
 * 恢复备份需要授权码 0831。
 */
public class MyActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private RecyclerView rvMenu;
    private MenuAdapter menuAdapter;
    private List<MenuItem> menuItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("clothing_store_prefs", Context.MODE_PRIVATE);
        setContentView(createRootLayout());

        rvMenu.setLayoutManager(new LinearLayoutManager(this));

        loadMenuItems();
        menuAdapter = new MenuAdapter(menuItems);
        rvMenu.setAdapter(menuAdapter);

        // 拖拽排序
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(menuItems, from, to);
                menuAdapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                saveMenuOrder();
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        touchHelper.attachToRecyclerView(rvMenu);
    }

    // ==================== 根布局 ====================

    private View createRootLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFFF5F5F5);

        // 标题栏
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setPadding(24, 24, 24, 16);
        titleBar.setBackgroundColor(0xFFF5F5F5);
        TextView title = new TextView(this);
        title.setText("👤 我的");
        title.setTextSize(24);
        title.setTextColor(0xFF333333);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBar.addView(title);
        layout.addView(titleBar);

        // 店铺名称设置（固定顶部）
        addShopNameSection(layout);

        // 提示
        TextView hint = new TextView(this);
        hint.setText("长按 ☰ 拖拽调整顺序");
        hint.setTextSize(12);
        hint.setTextColor(0xFF999999);
        hint.setPadding(24, 8, 24, 4);
        layout.addView(hint);

        // 可拖拽菜单列表
        rvMenu = new RecyclerView(this);
        rvMenu.setId(View.generateViewId());
        rvMenu.setPadding(8, 0, 8, 0);
        rvMenu.setClipToPadding(false);
        rvMenu.setNestedScrollingEnabled(false);
        // 根据内容自适应高度
        rvMenu.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false;  // 禁用RecyclerView滚动，让外层ScrollView接管
            }
        });
        layout.addView(rvMenu);

        // 固定功能按钮
        addFixedButton(layout, "📤 导出备份", v -> exportDatabase());
        addFixedButton(layout, "📥 恢复备份", v -> showAuthDialog());
        addFixedButton(layout, "📖 使用指南", v ->
                startActivity(new Intent(this, GuideActivity.class)));

        // 关于信息
        addAboutSection(layout);

        return layout;
    }

    // ==================== 店铺名称 ====================

    private void addShopNameSection(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 12, 16, 12);
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(8, 0, 8, 8);
        card.setLayoutParams(cardParams);

        EditText etShopName = new EditText(this);
        etShopName.setLayoutParams(new LinearLayout.LayoutParams(0, 80, 1));
        etShopName.setText(prefs.getString("shop_name", "我的小店"));
        etShopName.setTextSize(16);
        etShopName.setPadding(16, 0, 16, 0);
        etShopName.setBackgroundColor(0xFFF5F5F5);
        card.addView(etShopName);

        TextView btnSave = new TextView(this);
        btnSave.setText("  保存  ");
        btnSave.setTextSize(14);
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setBackgroundColor(0xFF2196F3);
        btnSave.setPadding(16, 12, 16, 12);
        btnSave.setOnClickListener(v -> {
            String name = etShopName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "店铺名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("shop_name", name).apply();
            Toast.makeText(this, "店铺名称已保存", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnSave);
        parent.addView(card);
    }

    // ==================== 固定按钮 ====================

    private void addFixedButton(LinearLayout parent, String text, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setPadding(16, 16, 16, 16);
        card.setBackgroundColor(0xFFFFFFFF);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(8, 0, 8, 4);
        card.setLayoutParams(cardParams);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(0xFF333333);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setPadding(8, 4, 8, 4);
        tv.setOnClickListener(listener);
        card.addView(tv);
        parent.addView(card);
    }

    // ==================== 授权码验证 ====================

    private void showAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 恢复备份需要授权");

        final EditText input = new EditText(this);
        input.setHint("请输入授权码");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);

        builder.setPositiveButton("确认", (dialog, which) -> {
            String code = input.getText().toString().trim();
            if ("0831".equals(code)) {
                restoreDatabase();
            } else {
                Toast.makeText(this, "❌ 授权码错误", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ==================== 关于信息 ====================

    private void addAboutSection(LinearLayout parent) {
        TextView tv = new TextView(this);
        tv.setText("版本 v1.0.0 | 毕业设计作品\n作者：HeBen_Cat | 为妈妈而做的专属记账工具");
        tv.setTextSize(12);
        tv.setTextColor(0xFF999999);
        tv.setPadding(16, 12, 16, 24);
        tv.setLineSpacing(4, 1);
        tv.setBackgroundColor(0xFFF5F5F5);
        parent.addView(tv);
    }

    // ==================== 菜单项管理 ====================

    private void loadMenuItems() {
        menuItems.clear();
        menuItems.add(new MenuItem("📋 商品管理", () ->
                startActivity(new Intent(this, ViewProductsActivity.class))));
        menuItems.add(new MenuItem("➕ 添加商品", () ->
                startActivity(new Intent(this, AddProductActivity.class))));
        menuItems.add(new MenuItem("🏷️ 属性管理", () ->
                startActivity(new Intent(this, AttributeGroupsActivity.class))));
        menuItems.add(new MenuItem("📦 库存查看", () ->
                startActivity(new Intent(this, InventoryActivity.class))));
        menuItems.add(new MenuItem("🏆 热销排行", () ->
                startActivity(new Intent(this, RankActivity.class))));
        menuItems.add(new MenuItem("📈 利润趋势", () ->
                startActivity(new Intent(this, ProfitActivity.class))));

        // 恢复保存的顺序
        String savedOrder = prefs.getString("menu_order", "");
        if (!savedOrder.isEmpty()) {
            String[] ids = savedOrder.split(",");
            List<MenuItem> sorted = new ArrayList<>();
            for (String id : ids) {
                for (MenuItem item : menuItems) {
                    if (item.id.equals(id)) {
                        sorted.add(item);
                        break;
                    }
                }
            }
            if (sorted.size() == menuItems.size()) {
                menuItems.clear();
                menuItems.addAll(sorted);
            }
        }
    }

    private void saveMenuOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < menuItems.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(menuItems.get(i).id);
        }
        prefs.edit().putString("menu_order", sb.toString()).apply();
    }

    // ==================== 菜单项数据类 ====================

    private static class MenuItem {
        String id;
        String text;
        Runnable action;

        MenuItem(String text, Runnable action) {
            this.id = text;
            this.text = text;
            this.action = action;
        }
    }

    // ==================== RecyclerView 适配器 ====================

    private class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {

        private List<MenuItem> list;

        MenuAdapter(List<MenuItem> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_menu, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuItem item = list.get(position);
            holder.tvText.setText(item.text);
            holder.itemView.setOnClickListener(v -> {
                if (item.action != null) item.action.run();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvText;

            ViewHolder(View itemView) {
                super(itemView);
                tvText = itemView.findViewById(R.id.tv_menu_text);
            }
        }
    }

    // ==================== 数据导出 ====================

    private void exportDatabase() {
        new Thread(() -> {
            try {
                File dbFile = getDatabasePath("clothing_store.db");
                if (!dbFile.exists()) {
                    runOnUiThread(() -> Toast.makeText(this, "数据库文件不存在", Toast.LENGTH_SHORT).show());
                    return;
                }
                String dateStr = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String exportFileName = "服装店数据_" + dateStr + ".db";
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File exportFile = new File(downloadDir, exportFileName);
                copyFile(dbFile, exportFile);
                runOnUiThread(() -> Toast.makeText(this,
                        "✅ 数据已导出！\n文件名：" + exportFileName + "\n位置：下载文件夹",
                        Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "❌ 导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ==================== 数据恢复 ====================

    private void restoreDatabase() {
        File backupDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "记账备份");
        if (!backupDir.exists() || backupDir.listFiles() == null || backupDir.listFiles().length == 0) {
            Toast.makeText(this, "没有找到备份文件", Toast.LENGTH_SHORT).show();
            return;
        }
        File[] files = backupDir.listFiles((dir, name) -> name.endsWith(".db"));
        String[] fileNames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }
        new AlertDialog.Builder(this)
                .setTitle("选择要恢复的备份")
                .setItems(fileNames, (dialog, which) -> {
                    new AlertDialog.Builder(this)
                            .setTitle("确认恢复")
                            .setMessage("将用「" + fileNames[which] + "」覆盖当前所有数据，确定吗？")
                            .setPositiveButton("确认恢复", (d, w) -> {
                                final File selectedFile = files[which];
                                new Thread(() -> {
                                    try {
                                        AppDatabase.getInstance(this).close();
                                        File dbFile = getDatabasePath("clothing_store.db");
                                        copyFile(selectedFile, dbFile);
                                        runOnUiThread(() -> Toast.makeText(this, "恢复成功！请重启App", Toast.LENGTH_LONG).show());
                                    } catch (IOException e) {
                                        runOnUiThread(() -> Toast.makeText(this, "恢复失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
                                    }
                                }).start();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyFile(File source, File dest) throws IOException {
        FileInputStream fis = new FileInputStream(source);
        FileOutputStream fos = new FileOutputStream(dest);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fis.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.flush();
        fos.close();
        fis.close();
    }
}