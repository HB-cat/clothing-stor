package com.heben.clothingstore;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.SaleDao;
import com.heben.clothingstore.entity.Sale;
import com.heben.clothingstore.entity.Product;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;



public class BillActivity extends BaseActivity {

    private RecyclerView rvSales;
    private TextView tvSelectedDate, tvTotalSales, tvTotalProfit, tvSaleCount;
    private Button btnTabDay, btnTabMonth, btnTabYear, btnQuery;
    private SaleAdapter adapter;
    private List<Sale> saleList = new ArrayList<>();

    private String currentMode = "day";
    private Calendar selectedCal = Calendar.getInstance();
    private String selectedDateStr = "";

    private Calendar dialogCal = Calendar.getInstance();
    private TextView tvDialogMonthYear;
    private GridLayout glCalendar;
    private AlertDialog currentDialog;

    private NumberPicker npWheel;
    private LinearLayout llCalendarContent;
    private LinearLayout llWeekHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);

        rvSales = findViewById(R.id.rv_sales);
        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvTotalSales = findViewById(R.id.tv_total_sales);
        tvTotalProfit = findViewById(R.id.tv_total_profit);
        tvSaleCount = findViewById(R.id.tv_sale_count);
        btnTabDay = findViewById(R.id.btn_tab_day);
        btnTabMonth = findViewById(R.id.btn_tab_month);
        btnTabYear = findViewById(R.id.btn_tab_year);
        btnQuery = findViewById(R.id.btn_query);

        rvSales.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SaleAdapter(saleList);
        rvSales.setAdapter(adapter);

        // 左滑退款
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Sale sale = saleList.get(position);
                adapter.notifyItemChanged(position);
                if (sale.isRefunded()) { Toast.makeText(BillActivity.this, "该笔已退款", Toast.LENGTH_SHORT).show(); return; }
                new AlertDialog.Builder(BillActivity.this)
                        .setTitle("退款确认")
                        .setMessage("确定要退回 ¥" + (sale.getSellingPrice() * sale.getQuantity()) + " 吗？")
                        .setPositiveButton("确认退款", (d, w) -> refundSale(sale, position))
                        .setNegativeButton("取消", null).show();
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvSales);

        // 底部导航
        findViewById(R.id.btn_nav_home).setOnClickListener(v -> navigateTo(MainActivity.class));
        findViewById(R.id.btn_nav_bill).setOnClickListener(v -> Toast.makeText(this, "已经是账单", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_nav_stock).setOnClickListener(v -> navigateTo(PurchaseActivity.class));
        findViewById(R.id.btn_nav_my).setOnClickListener(v -> navigateTo(MyActivity.class));

        clearSummary();
        btnTabDay.setOnClickListener(v -> switchMode("day"));
        btnTabMonth.setOnClickListener(v -> switchMode("month"));
        btnTabYear.setOnClickListener(v -> switchMode("year"));
        tvSelectedDate.setOnClickListener(v -> showCalendarDialog());
        btnQuery.setOnClickListener(v -> {
            if (selectedDateStr.isEmpty()) { Toast.makeText(this, "请先选择日期", Toast.LENGTH_SHORT).show(); return; }
            loadSales();
        });

        // 默认切换到“日”模式，自动设置当天日期
        switchMode("day");
        selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvSelectedDate.setText(selectedDateStr);
    }

    private void refundSale(Sale sale, int position) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(BillActivity.this);
            sale.setRefunded(true);
            db.saleDao().update(sale);
            runOnUiThread(() -> { adapter.notifyItemChanged(position); loadSales(); Toast.makeText(this, "退款成功", Toast.LENGTH_SHORT).show(); });
        }).start();
    }

    private void switchMode(String mode) {
        currentMode = mode;
        selectedDateStr = "";
        tvSelectedDate.setText("点击选择");
        clearSummary();
        saleList.clear();
        adapter.notifyDataSetChanged();
        btnTabDay.setBackgroundTintList(getColorStateList(mode.equals("day") ? android.R.color.holo_green_light : android.R.color.white));
        btnTabDay.setTextColor(mode.equals("day") ? getColor(android.R.color.white) : getColor(R.color.text_secondary));
        btnTabMonth.setBackgroundTintList(getColorStateList(mode.equals("month") ? android.R.color.holo_green_light : android.R.color.white));
        btnTabMonth.setTextColor(mode.equals("month") ? getColor(android.R.color.white) : getColor(R.color.text_secondary));
        btnTabYear.setBackgroundTintList(getColorStateList(mode.equals("year") ? android.R.color.holo_green_light : android.R.color.white));
        btnTabYear.setTextColor(mode.equals("year") ? getColor(android.R.color.white) : getColor(R.color.text_secondary));
    }

    private void showCalendarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_calendar, null);
        tvDialogMonthYear = view.findViewById(R.id.tv_cal_month_year);
        llCalendarContent = view.findViewById(R.id.ll_calendar_content);
        llWeekHeader = view.findViewById(R.id.ll_week_header);
        npWheel = view.findViewById(R.id.np_wheel);
        Button btnPrev = view.findViewById(R.id.btn_cal_prev_month);
        Button btnNext = view.findViewById(R.id.btn_cal_next_month);

        dialogCal = (Calendar) selectedCal.clone();
        renderCalendar();

        btnPrev.setOnClickListener(v -> {
            if (currentMode.equals("day")) dialogCal.add(Calendar.MONTH, -1);
            else if (currentMode.equals("month")) dialogCal.add(Calendar.YEAR, -1);
            else dialogCal.add(Calendar.YEAR, -1);
            renderCalendar();
        });
        btnNext.setOnClickListener(v -> {
            if (currentMode.equals("day")) dialogCal.add(Calendar.MONTH, 1);
            else if (currentMode.equals("month")) dialogCal.add(Calendar.YEAR, 1);
            else dialogCal.add(Calendar.YEAR, 1);
            renderCalendar();
        });

        // 滚轮监听
        npWheel.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (currentMode.equals("month")) {
                selectedCal = (Calendar) dialogCal.clone();
                selectedCal.set(Calendar.MONTH, newVal - 1);
                selectedDateStr = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedCal.getTime());
            } else if (currentMode.equals("year")) {
                selectedCal = Calendar.getInstance();
                selectedCal.set(Calendar.YEAR, newVal);
                selectedDateStr = String.valueOf(newVal);
            }
        });

        builder.setView(view);
        // 确认按钮：将滚轮选中的值同步到日期显示
        builder.setPositiveButton("确定", (d, w) -> {
            if (currentMode.equals("month") || currentMode.equals("year")) {
                tvSelectedDate.setText(selectedDateStr);
            }
        });
        builder.setNegativeButton("取消", null);

        currentDialog = builder.create();
        currentDialog.show();
    }

    private void renderCalendar(LinearLayout container) {
        container.removeAllViews();

        if (currentMode.equals("day")) {
            tvDialogMonthYear.setText(new SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(dialogCal.getTime()));
            Calendar cal = (Calendar) dialogCal.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            int totalCells = firstDayOfWeek + daysInMonth;
            int rows = (int) Math.ceil(totalCells / 7.0);
            int cellIndex = 0;

            for (int r = 0; r < rows; r++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                for (int c = 0; c < 7; c++) {
                    TextView tv = new TextView(this);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextSize(15);
                    tv.setPadding(8, 12, 8, 12);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    tv.setLayoutParams(params);

                    if (cellIndex < firstDayOfWeek || cellIndex >= firstDayOfWeek + daysInMonth) {
                        tv.setText("");
                        tv.setClickable(false);
                    } else {
                        final int day = cellIndex - firstDayOfWeek + 1;
                        tv.setText(String.valueOf(day));
                        tv.setTextColor(0xFF333333);
                        tv.setOnClickListener(v -> {
                            Calendar chosen = (Calendar) dialogCal.clone();
                            chosen.set(Calendar.DAY_OF_MONTH, day);
                            selectedCal = chosen;
                            selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.getTime());
                            tvSelectedDate.setText(selectedDateStr);
                            if (currentDialog != null) currentDialog.dismiss();
                        });
                    }
                    row.addView(tv);
                    cellIndex++;
                }
                container.addView(row);
            }
        } else if (currentMode.equals("month")) {
            tvDialogMonthYear.setText(new SimpleDateFormat("yyyy年", Locale.getDefault()).format(dialogCal.getTime()));
            for (int m = 1; m <= 12; m++) {
                final int month = m;
                TextView tv = new TextView(this);
                tv.setText(m + "月");
                tv.setTextSize(15);
                tv.setTextColor(0xFF333333);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(12, 16, 12, 16);
                tv.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setOnClickListener(v -> {
                    selectedCal = (Calendar) dialogCal.clone();
                    selectedCal.set(Calendar.MONTH, month - 1);
                    selectedDateStr = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedCal.getTime());
                    tvSelectedDate.setText(selectedDateStr);
                    if (currentDialog != null) currentDialog.dismiss();
                });
                container.addView(tv);
            }
        } else if (currentMode.equals("year")) {
            tvDialogMonthYear.setText("选择年份");
            int thisYear = dialogCal.get(Calendar.YEAR);
            for (int y = thisYear - 5; y <= thisYear + 5; y++) {
                final int year = y;
                TextView tv = new TextView(this);
                tv.setText(String.valueOf(year));
                tv.setTextSize(15);
                tv.setTextColor(0xFF333333);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(12, 16, 12, 16);
                tv.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                tv.setOnClickListener(v -> {
                    selectedCal = Calendar.getInstance();
                    selectedCal.set(Calendar.YEAR, year);
                    selectedDateStr = String.valueOf(year);
                    tvSelectedDate.setText(selectedDateStr);
                    if (currentDialog != null) currentDialog.dismiss();
                });
                container.addView(tv);
            }
        }
    }

    private void renderCalendar() {
        if (currentMode.equals("day")) {
            // 日模式：显示日历网格
            llCalendarContent.setVisibility(View.VISIBLE);
            llWeekHeader.setVisibility(View.VISIBLE);
            npWheel.setVisibility(View.GONE);

            tvDialogMonthYear.setText(new SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(dialogCal.getTime()));
            llCalendarContent.removeAllViews();

            Calendar cal = (Calendar) dialogCal.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int totalCells = firstDayOfWeek + daysInMonth;
            int rows = (int) Math.ceil(totalCells / 7.0);
            int cellIndex = 0;

            for (int r = 0; r < rows; r++) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                for (int c = 0; c < 7; c++) {
                    TextView tv = new TextView(this);
                    tv.setGravity(Gravity.CENTER);
                    tv.setTextSize(15);
                    tv.setPadding(4, 12, 4, 12);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    tv.setLayoutParams(params);

                    if (cellIndex < firstDayOfWeek || cellIndex >= firstDayOfWeek + daysInMonth) {
                        tv.setText("");
                        tv.setClickable(false);
                    } else {
                        final int day = cellIndex - firstDayOfWeek + 1;
                        tv.setText(String.valueOf(day));
                        tv.setTextColor(0xFF333333);
                        tv.setOnClickListener(v -> {
                            Calendar chosen = (Calendar) dialogCal.clone();
                            chosen.set(Calendar.DAY_OF_MONTH, day);
                            selectedCal = chosen;
                            selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.getTime());
                            tvSelectedDate.setText(selectedDateStr);
                            if (currentDialog != null) currentDialog.dismiss();
                        });
                    }
                    row.addView(tv);
                    cellIndex++;
                }
                llCalendarContent.addView(row);
            }
        } else if (currentMode.equals("month")) {
            // 月模式：显示滚轮
            llCalendarContent.setVisibility(View.GONE);
            llWeekHeader.setVisibility(View.GONE);
            npWheel.setVisibility(View.VISIBLE);

            tvDialogMonthYear.setText(new SimpleDateFormat("yyyy年", Locale.getDefault()).format(dialogCal.getTime()));
            npWheel.setMinValue(1);
            npWheel.setMaxValue(12);
            npWheel.setValue(dialogCal.get(Calendar.MONTH) + 1);
            npWheel.setDisplayedValues(new String[]{
                    "1月", "2月", "3月", "4月", "5月", "6月",
                    "7月", "8月", "9月", "10月", "11月", "12月"
            });
        } else if (currentMode.equals("year")) {
            // 年模式：显示滚轮
            llCalendarContent.setVisibility(View.GONE);
            llWeekHeader.setVisibility(View.GONE);
            npWheel.setVisibility(View.VISIBLE);

            tvDialogMonthYear.setText("选择年份");
            int thisYear = dialogCal.get(Calendar.YEAR);
            int startYear = thisYear - 5;
            int endYear = thisYear + 5;
            String[] yearStrings = new String[endYear - startYear + 1];
            for (int i = 0; i < yearStrings.length; i++) {
                yearStrings[i] = (startYear + i) + "年";
            }
            npWheel.setMinValue(startYear);
            npWheel.setMaxValue(endYear);
            npWheel.setValue(thisYear);
            npWheel.setDisplayedValues(yearStrings);
        }
    }

    private void addDayCell(String text, boolean isDay) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(8, 12, 8, 12);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        tv.setLayoutParams(params);
        glCalendar.addView(tv);
    }

    private void clearSummary() { tvTotalSales.setText("--"); tvTotalProfit.setText("--"); tvSaleCount.setText("--"); }

    private void loadSales() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(BillActivity.this);
            SaleDao saleDao = db.saleDao();
            List<Sale> sales;
            if (currentMode.equals("day")) sales = saleDao.getSalesByDate(selectedDateStr);
            else if (currentMode.equals("month")) sales = saleDao.getSalesByDateRange(selectedDateStr + "-01", selectedDateStr + "-31");
            else sales = saleDao.getSalesByDateRange(selectedDateStr + "-01-01", selectedDateStr + "-12-31");
            double totalSales = 0, totalProfit = 0; int count = 0;
            for (Sale s : sales) { if (!s.isRefunded()) { totalSales += s.getSellingPrice() * s.getQuantity(); totalProfit += (s.getSellingPrice() - s.getCostPrice()) * s.getQuantity(); count++; } }
            double finalSales = totalSales, finalProfit = totalProfit; int finalCount = count;
            runOnUiThread(() -> { saleList.clear(); saleList.addAll(sales); adapter.notifyDataSetChanged(); tvTotalSales.setText("¥" + finalSales); tvTotalProfit.setText("¥" + finalProfit); tvSaleCount.setText(String.valueOf(finalCount)); });
        }).start();
    }

    private class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {
        private List<Sale> list; SaleAdapter(List<Sale> list) { this.list = list; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false)); }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Sale sale = list.get(position); holder.tvTime.setText(sale.getSaleTime() != null ? sale.getSaleTime().substring(0, 5) : ""); loadProductName(sale.getProductId(), holder.tvProduct);
            holder.tvAmount.setText("¥" + (sale.getSellingPrice() * sale.getQuantity())); double profit = (sale.getSellingPrice() - sale.getCostPrice()) * sale.getQuantity();
            holder.tvDetail.setText("数量: " + sale.getQuantity() + "  进价: ¥" + sale.getCostPrice() + "  毛利: ¥" + profit);
            if (sale.isRefunded()) { holder.tvRefunded.setVisibility(View.VISIBLE); holder.tvAmount.setTextColor(0xFF999999); } else { holder.tvRefunded.setVisibility(View.GONE); holder.tvAmount.setTextColor(0xFFFF5722); }
            holder.itemView.setOnClickListener(v -> showSaleDetailDialog(sale));
        }
        @Override public int getItemCount() { return list.size(); }
        private void loadProductName(long productId, TextView textView) { new Thread(() -> { Product p = AppDatabase.getInstance(BillActivity.this).productDao().getById(productId); runOnUiThread(() -> textView.setText(p != null ? p.getName() : "未知商品")); }).start(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvProduct, tvAmount, tvDetail, tvRefunded; ViewHolder(@NonNull View itemView) { super(itemView); tvTime = itemView.findViewById(R.id.tv_sale_time); tvProduct = itemView.findViewById(R.id.tv_sale_product); tvAmount = itemView.findViewById(R.id.tv_sale_amount); tvDetail = itemView.findViewById(R.id.tv_sale_detail); tvRefunded = itemView.findViewById(R.id.tv_sale_refunded); }
        }
    }

    private void showSaleDetailDialog(Sale sale) {
        new Thread(() -> {
            Product product = AppDatabase.getInstance(BillActivity.this).productDao().getById(sale.getProductId());
            runOnUiThread(() -> {
                if (product == null) return;
                AlertDialog.Builder builder = new AlertDialog.Builder(BillActivity.this);
                View view = LayoutInflater.from(BillActivity.this).inflate(R.layout.dialog_sale_detail, null);
                ImageView ivPhoto = view.findViewById(R.id.iv_detail_photo); TextView tvName = view.findViewById(R.id.tv_detail_name); TextView tvInfo = view.findViewById(R.id.tv_detail_info); TextView tvTime = view.findViewById(R.id.tv_detail_time);
                String imagePath = product.getImagePath(); if (imagePath != null && !imagePath.isEmpty()) ivPhoto.setImageURI(Uri.fromFile(new File(imagePath))); else ivPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                tvName.setText(product.getName());
                double profit = (sale.getSellingPrice() - sale.getCostPrice()) * sale.getQuantity();
                tvInfo.setText("数量: " + sale.getQuantity() + "  售价: ¥" + sale.getSellingPrice() + "\n进价: ¥" + sale.getCostPrice() + "  毛利: ¥" + profit);
                tvTime.setText(sale.getSaleDate() + " " + sale.getSaleTime());
                builder.setView(view).setPositiveButton("关闭", null).show();
            });
        }).start();
    }
}