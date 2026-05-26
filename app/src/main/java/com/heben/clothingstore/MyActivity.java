package com.heben.clothingstore;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

public class MyActivity extends BaseActivity {

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
        // 根布局：垂直排列，内容区 + 导航栏
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(getColor(android.R.color.transparent));

        // 滚动内容区域
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(getColor(android.R.color.transparent));

        // 标题栏（与账单、进货页面统一：绿色背景、白色文字、56dp高）
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setBackgroundColor(getColor(R.color.primary));
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(24, 0, 24, 0);
        titleBar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (56 * getResources().getDisplayMetrics().density)));

        TextView title = new TextView(this);
        title.setText("👤 我的");
        title.setTextSize(20);
        title.setTextColor(getColor(android.R.color.white));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBar.addView(title);

        layout.addView(titleBar);

        title.setOnLongClickListener(v -> {
            TestRunner.runAll(MyActivity.this);
            return true;
        });

        addShopNameSection(layout);

        TextView hint = new TextView(this);
        hint.setText("长按 ☰ 拖拽调整顺序");
        hint.setTextSize(12);
        hint.setTextColor(getColor(R.color.text_hint));
        hint.setPadding(24, 8, 24, 4);
        layout.addView(hint);

        rvMenu = new RecyclerView(this);
        rvMenu.setId(View.generateViewId());
        rvMenu.setPadding(8, 0, 8, 0);
        rvMenu.setClipToPadding(false);
        rvMenu.setNestedScrollingEnabled(true);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setMinimumHeight(200);
        layout.addView(rvMenu);

        addSoundSwitchButton(layout);
        addFixedButton(layout, "📤 导出备份", v -> exportDatabase());
        addFixedButton(layout, "📥 恢复备份", v -> showAuthDialog());
        addFixedButton(layout, "🧪 运行测试", v -> showTestAuthDialog());
        addFixedButton(layout, "🔄 初始化系统", v -> showInitAuthDialog());
        addFixedButton(layout, "📖 使用指南", v -> startActivity(new Intent(this, GuideActivity.class)));

        addAboutSection(layout);

        scrollView.addView(layout);

        // 底部导航（固定）
        LinearLayout bottomNav = createBottomNav();

        // 组装
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        rootLayout.addView(scrollView, scrollParams);

        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rootLayout.addView(bottomNav, navParams);

        return rootLayout;
    }

    // 创建底部导航栏，样式与 activity_main.xml 等完全一致
    private LinearLayout createBottomNav() {
        // 外层容器：负责左右内边距、上下内边距，透明背景
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setBackgroundColor(getColor(android.R.color.transparent));
        wrapper.setGravity(Gravity.CENTER);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        // 左右 12dp，上 6dp，下 10dp，与 XML 一致
        int px12 = (int) (12 * getResources().getDisplayMetrics().density);
        int px6 = (int) (6 * getResources().getDisplayMetrics().density);
        int px10 = (int) (10 * getResources().getDisplayMetrics().density);
        wrapper.setPadding(px12, px6, px12, px10);

        // 内层胶囊容器：固定高度 56dp，白色半透明背景，圆角卡片
        LinearLayout nav = new LinearLayout(this);
        nav.setBackground(getDrawable(R.drawable.bg_card_white));
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        // 内边距 4dp，与 XML 的 padding="4dp" 一致
        int px4 = (int) (4 * getResources().getDisplayMetrics().density);
        nav.setPadding(px4, px4, px4, px4);
        // 固定高度 56dp
        int height = (int) (56 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        nav.setLayoutParams(navParams);

        // 创建四个导航按钮（与 XML 中的按钮属性完全一致）
        Button btnHome = createNavButton("🏠\n首页");
        btnHome.setBackground(getDrawable(R.drawable.bg_nav_default));
        btnHome.setOnClickListener(v -> navigateTo(MainActivity.class));
        nav.addView(btnHome);

        Button btnBill = createNavButton("📊\n账单");
        btnBill.setBackground(getDrawable(R.drawable.bg_nav_default));
        btnBill.setOnClickListener(v -> navigateTo(BillActivity.class));
        nav.addView(btnBill);

        Button btnStock = createNavButton("📦\n进货");
        btnStock.setBackground(getDrawable(R.drawable.bg_nav_default));
        btnStock.setOnClickListener(v -> navigateTo(PurchaseActivity.class));
        nav.addView(btnStock);

        Button btnMy = createNavButton("👤\n我的");
        btnMy.setBackground(getDrawable(R.drawable.bg_nav_selected));
        btnMy.setTextColor(getColor(android.R.color.white));
        btnMy.setOnClickListener(v -> Toast.makeText(this, "已经是我的", Toast.LENGTH_SHORT).show());
        nav.addView(btnMy);

        wrapper.addView(nav);
        return wrapper;
    }


    private void addBottomNav(LinearLayout parent) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setPadding(0, 6, 0, 10);   // 左右 padding 改为 0
        wrapper.setBackgroundColor(getColor(android.R.color.transparent));
        LinearLayout nav = new LinearLayout(this);
        nav.setBackground(getDrawable(R.drawable.bg_card_white));
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(6, 6, 6, 6);
        nav.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button btnHome = createNavButton("🏠\n首页");
        btnHome.setBackground(getDrawable(R.drawable.bg_nav_default));
        btnHome.setOnClickListener(v -> { startActivity(new Intent(this, MainActivity.class)); finish(); });
        nav.addView(btnHome);

        Button btnBill = createNavButton("📊\n账单");
        btnBill.setBackground(getDrawable(R.drawable.bg_nav_default));
        btnBill.setOnClickListener(v -> { startActivity(new Intent(this, BillActivity.class)); finish(); });
        nav.addView(btnBill);

        Button btnStock = createNavButton("📦\n进货");
        btnStock.setBackground(getDrawable(R.drawable.bg_nav_default));
        btnStock.setOnClickListener(v -> { startActivity(new Intent(this, PurchaseActivity.class)); finish(); });
        nav.addView(btnStock);

        Button btnMy = createNavButton("👤\n我的");
        btnMy.setBackground(getDrawable(R.drawable.bg_nav_selected));
        btnMy.setTextColor(getColor(android.R.color.white));
        btnMy.setOnClickListener(v -> Toast.makeText(this, "已经是我的", Toast.LENGTH_SHORT).show());
        nav.addView(btnMy);

        wrapper.addView(nav);
        parent.addView(wrapper);
    }

    private Button createNavButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(11);
        btn.setMaxLines(2);
        btn.setEllipsize(TextUtils.TruncateAt.END);
        btn.setGravity(Gravity.CENTER);
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return btn;
    }

    // ==================== 店铺名称 ====================
    private void addShopNameSection(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(16, 12, 16, 12);
        card.setBackground(getDrawable(R.drawable.bg_card_white));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(12, 0, 12, 8);
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
        btnSave.setTextColor(getColor(android.R.color.white));
        btnSave.setBackground(getDrawable(R.drawable.btn_rounded_primary));
        btnSave.setPadding(16, 12, 16, 12);
        btnSave.setOnClickListener(v -> {
            String name = etShopName.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(this, "店铺名称不能为空", Toast.LENGTH_SHORT).show(); return; }
            prefs.edit().putString("shop_name", name).apply();
            Toast.makeText(this, "店铺名称已保存", Toast.LENGTH_SHORT).show();
        });
        card.addView(btnSave);
        parent.addView(card);
    }

    private void addFixedButton(LinearLayout parent, String text, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(16, 0, 16, 0);
        card.setBackground(getDrawable(R.drawable.bg_card_white));
        // 统一高度 56dp
        int heightDp = (int) (56 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightDp);
        cardParams.setMargins(12, 0, 12, 8);
        card.setLayoutParams(cardParams);

        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(getColor(R.color.text_primary));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tv.setPadding(8, 0, 8, 0);
        card.addView(tv);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(20);
        arrow.setTextColor(getColor(R.color.divider));
        card.addView(arrow);

        card.setOnClickListener(v -> {
            MediaSoundHelper.getInstance().playClick(MyActivity.this);
            listener.onClick(v);
        });
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
            if ("0831".equals(input.getText().toString().trim())) restoreDatabase();
            else { MediaSoundHelper.getInstance().playError(this); Toast.makeText(this, "❌ 授权码错误", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showTestAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 运行测试需要授权");
        final EditText input = new EditText(this);
        input.setHint("请输入授权码");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);
        builder.setPositiveButton("确认", (dialog, which) -> {
            if ("0831".equals(input.getText().toString().trim())) { Toast.makeText(this, "正在生成测试数据...", Toast.LENGTH_SHORT).show(); TestRunner.runAll(this); }
            else { Toast.makeText(this, "❌ 授权码错误", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showInitAuthDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔐 初始化系统需要授权");
        final EditText input = new EditText(this);
        input.setHint("请输入授权码");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);
        builder.setPositiveButton("确认", (dialog, which) -> {
            if ("0831".equals(input.getText().toString().trim())) showInitConfirmDialog();
            else { Toast.makeText(this, "❌ 授权码错误", Toast.LENGTH_SHORT).show(); }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showInitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ 确认初始化")
                .setMessage("此操作将删除所有数据。\n\n此操作不可恢复！")
                .setPositiveButton("确认初始化", (d, w) -> initDatabase())
                .setNegativeButton("取消", null).show();
    }

    private void initDatabase() {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                db.close();
                File dbFile = getDatabasePath("clothing_store.db");
                if (dbFile.exists()) dbFile.delete();
                prefs.edit().clear().apply();
                runOnUiThread(() -> {
                    Toast.makeText(this, "✅ 初始化完成，请重启App", Toast.LENGTH_LONG).show();
                    new android.os.Handler().postDelayed(() -> { finishAffinity(); System.exit(0); }, 3000);
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "❌ 初始化失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void addAboutSection(LinearLayout parent) {
        TextView tv = new TextView(this);
        tv.setText("HenBen_Cat | 为妈妈做的专属记账工具");
        tv.setTextSize(12);
        tv.setTextColor(getColor(R.color.text_hint));
        tv.setPadding(16, 12, 16, 24);
        tv.setGravity(Gravity.CENTER);
        parent.addView(tv);
    }

    // ==================== 菜单项 ====================
    private void loadMenuItems() {
        menuItems.clear();
        menuItems.add(new MenuItem("📋 商品管理", () -> startActivity(new Intent(this, ViewProductsActivity.class))));
        menuItems.add(new MenuItem("➕ 添加商品", () -> startActivity(new Intent(this, AddProductActivity.class))));
        menuItems.add(new MenuItem("🏷️ 属性管理", () -> startActivity(new Intent(this, AttributeGroupsActivity.class))));
        menuItems.add(new MenuItem("📦 库存查看", () -> startActivity(new Intent(this, InventoryActivity.class))));
        menuItems.add(new MenuItem("🏆 热销排行", () -> startActivity(new Intent(this, RankActivity.class))));
        menuItems.add(new MenuItem("📈 利润趋势", () -> startActivity(new Intent(this, ProfitActivity.class))));
        String savedOrder = prefs.getString("menu_order", "");
        if (!savedOrder.isEmpty()) {
            String[] ids = savedOrder.split(",");
            List<MenuItem> sorted = new ArrayList<>();
            for (String id : ids) for (MenuItem item : menuItems) if (item.id.equals(id)) sorted.add(item);
            if (sorted.size() == menuItems.size()) { menuItems.clear(); menuItems.addAll(sorted); }
        }
    }

    private void saveMenuOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < menuItems.size(); i++) { if (i > 0) sb.append(","); sb.append(menuItems.get(i).id); }
        prefs.edit().putString("menu_order", sb.toString()).apply();
    }

    private static class MenuItem { String id; String text; Runnable action; MenuItem(String t, Runnable a) { this.id = t; this.text = t; this.action = a; } }

    private class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        private List<MenuItem> list; MenuAdapter(List<MenuItem> l) { this.list = l; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) { return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_my_menu, parent, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
            MenuItem item = list.get(pos); holder.tvText.setText(item.text);
            holder.itemView.setOnClickListener(v -> { MediaSoundHelper.getInstance().playClick(MyActivity.this); if (item.action != null) item.action.run(); });
        }
        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { TextView tvText; ViewHolder(View v) { super(v); tvText = v.findViewById(R.id.tv_menu_text); } }
    }

    // ==================== 导出/恢复 ====================
    private void exportDatabase() {
        new Thread(() -> {
            try {
                File dbFile = getDatabasePath("clothing_store.db");
                if (!dbFile.exists()) { runOnUiThread(() -> Toast.makeText(this, "数据库文件不存在", Toast.LENGTH_SHORT).show()); return; }
                String name = "服装店数据_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".db";
                File dest = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
                copyFile(dbFile, dest);
                runOnUiThread(() -> Toast.makeText(this, "✅ 已导出：" + name, Toast.LENGTH_LONG).show());
            } catch (IOException e) { runOnUiThread(() -> Toast.makeText(this, "❌ 导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show()); }
        }).start();
    }

    private void restoreDatabase() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "记账备份");
        if (!dir.exists() || dir.listFiles() == null || dir.listFiles().length == 0) { Toast.makeText(this, "没有备份文件", Toast.LENGTH_SHORT).show(); return; }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".db"));
        String[] names = new String[files.length]; for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
        new AlertDialog.Builder(this).setTitle("选择备份").setItems(names, (d, w) -> new AlertDialog.Builder(this).setTitle("确认恢复").setMessage("恢复「" + names[w] + "」？")
                .setPositiveButton("确认", (dd, ww) -> new Thread(() -> {
                    try { AppDatabase.getInstance(this).close(); copyFile(files[w], getDatabasePath("clothing_store.db")); runOnUiThread(() -> Toast.makeText(this, "恢复成功，请重启App", Toast.LENGTH_LONG).show()); }
                    catch (IOException e) { runOnUiThread(() -> Toast.makeText(this, "恢复失败：" + e.getMessage(), Toast.LENGTH_LONG).show()); }
                }).start()).setNegativeButton("取消", null).show()).setNegativeButton("取消", null).show();
    }

    private void copyFile(File s, File d) throws IOException {
        FileInputStream in = new FileInputStream(s); FileOutputStream out = new FileOutputStream(d);
        byte[] buf = new byte[1024]; int len; while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        out.flush(); out.close(); in.close();
    }

    private void addSoundSwitchButton(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(16, 0, 16, 0);
        card.setBackground(getDrawable(R.drawable.bg_card_white));
        int heightDp = (int) (56 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightDp);
        cardParams.setMargins(12, 0, 12, 8);
        card.setLayoutParams(cardParams);

        TextView tv = new TextView(this);
        updateSoundSwitchText(tv);
        tv.setTextSize(16);
        tv.setTextColor(getColor(R.color.text_primary));
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        tv.setPadding(8, 0, 8, 0);
        card.addView(tv);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextSize(20);
        arrow.setTextColor(getColor(R.color.divider));
        card.addView(arrow);

        card.setOnClickListener(v -> {
            boolean cur = MediaSoundHelper.getInstance().isSoundEnabled(this);
            MediaSoundHelper.getInstance().setSoundEnabled(this, !cur);
            updateSoundSwitchText(tv);
            Toast.makeText(this, cur ? "🔇 已关闭" : "🔊 已开启", Toast.LENGTH_SHORT).show();
        });
        parent.addView(card);
    }

    private void updateSoundSwitchText(TextView tv) {
        tv.setText(MediaSoundHelper.getInstance().isSoundEnabled(this) ? "🔊 提示音：开" : "🔇 提示音：关");
    }
}