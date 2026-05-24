package com.heben.clothingstore;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.SaleDao;
import com.heben.clothingstore.dao.ProductDao;
import com.heben.clothingstore.entity.Sale;
import com.heben.clothingstore.entity.Product;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.net.Uri;
import android.widget.ImageView;
import java.io.File;
import com.heben.clothingstore.entity.Product;

/**
 * 账单页面：支持日/月/年查询，日历选择器，默认隐藏金额。
 * 左滑销售记录可退款（需二次确认）。
 */
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

        // ========== 左滑退款功能 ==========
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Sale sale = saleList.get(position);
                // 立即恢复 item，避免滑动后消失
                adapter.notifyItemChanged(position);

                if (sale.isRefunded()) {
                    Toast.makeText(BillActivity.this, "该笔已退款", Toast.LENGTH_SHORT).show();
                    return;
                }

                double refundAmount = sale.getSellingPrice() * sale.getQuantity();
                new AlertDialog.Builder(BillActivity.this)
                        .setTitle("退款确认")
                        .setMessage("确定要退回这笔 ¥" + refundAmount + " 的交易吗？")
                        .setPositiveButton("确认退款", (dialog, which) -> refundSale(sale, position))
                        .setNegativeButton("取消", null)
                        .show();
                MediaSoundHelper.getInstance().playRefund(BillActivity.this);
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvSales);

        clearSummary();

        btnTabDay.setOnClickListener(v -> switchMode("day"));
        btnTabMonth.setOnClickListener(v -> switchMode("month"));
        btnTabYear.setOnClickListener(v -> switchMode("year"));

        tvSelectedDate.setOnClickListener(v -> showCalendarDialog());

        btnQuery.setOnClickListener(v -> {
            if (selectedDateStr.isEmpty()) {
                Toast.makeText(this, "请先选择日期", Toast.LENGTH_SHORT).show();
                return;
            }
            loadSales();
        });

        switchMode("day");

        // 默认选中今天，但不自动查询
        selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvSelectedDate.setText(selectedDateStr);
    }

    /** 执行退款操作 */
    private void refundSale(Sale sale, int position) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(BillActivity.this);
            sale.setRefunded(true);
            db.saleDao().update(sale);
            runOnUiThread(() -> {
                adapter.notifyItemChanged(position);
                loadSales();   // 刷新汇总数据
                Toast.makeText(BillActivity.this, "退款成功", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void switchMode(String mode) {
        currentMode = mode;
        selectedDateStr = "";
        tvSelectedDate.setText("点击选择");
        clearSummary();
        saleList.clear();
        adapter.notifyDataSetChanged();

        btnTabDay.setBackgroundTintList(getColorStateList(
                mode.equals("day") ? android.R.color.holo_blue_light : android.R.color.white));
        btnTabDay.setTextColor(mode.equals("day") ? 0xFFFFFFFF : 0xFF333333);
        btnTabMonth.setBackgroundTintList(getColorStateList(
                mode.equals("month") ? android.R.color.holo_blue_light : android.R.color.white));
        btnTabMonth.setTextColor(mode.equals("month") ? 0xFFFFFFFF : 0xFF333333);
        btnTabYear.setBackgroundTintList(getColorStateList(
                mode.equals("year") ? android.R.color.holo_blue_light : android.R.color.white));
        btnTabYear.setTextColor(mode.equals("year") ? 0xFFFFFFFF : 0xFF333333);
    }

    private void showCalendarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_calendar, null);
        builder.setView(view);

        tvDialogMonthYear = view.findViewById(R.id.tv_cal_month_year);
        glCalendar = view.findViewById(R.id.gl_calendar);
        Button btnPrev = view.findViewById(R.id.btn_cal_prev_month);
        Button btnNext = view.findViewById(R.id.btn_cal_next_month);

        dialogCal = (Calendar) selectedCal.clone();
        renderCalendar();

        btnPrev.setOnClickListener(v -> {
            if (currentMode.equals("day") || currentMode.equals("month")) {
                dialogCal.add(Calendar.MONTH, -1);
            } else {
                dialogCal.add(Calendar.YEAR, -1);
            }
            renderCalendar();
        });

        btnNext.setOnClickListener(v -> {
            if (currentMode.equals("day") || currentMode.equals("month")) {
                dialogCal.add(Calendar.MONTH, 1);
            } else {
                dialogCal.add(Calendar.YEAR, 1);
            }
            renderCalendar();
        });

        currentDialog = builder.create();
        currentDialog.show();
    }

    private void renderCalendar() {
        glCalendar.removeAllViews();

        if (currentMode.equals("day")) {
            tvDialogMonthYear.setText(new SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(dialogCal.getTime()));
            Calendar cal = (Calendar) dialogCal.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 0; i < firstDayOfWeek; i++) {
                addDayCell("", false);
            }
            for (int day = 1; day <= daysInMonth; day++) {
                final int d = day;
                TextView tv = new TextView(this);
                tv.setText(String.valueOf(day));
                tv.setTextSize(15);
                tv.setTextColor(0xFF333333);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(8, 12, 8, 12);
                tv.setOnClickListener(v -> {
                    Calendar chosen = (Calendar) dialogCal.clone();
                    chosen.set(Calendar.DAY_OF_MONTH, d);
                    selectedCal = chosen;
                    selectedDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.getTime());
                    tvSelectedDate.setText(selectedDateStr);
                    if (currentDialog != null) currentDialog.dismiss();
                });
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
                tv.setLayoutParams(params);
                glCalendar.addView(tv);
            }
        } else if (currentMode.equals("month")) {
            tvDialogMonthYear.setText(new SimpleDateFormat("yyyy年", Locale.getDefault()).format(dialogCal.getTime()));
            for (int m = 1; m <= 12; m++) {
                final int month = m;
                TextView tv = new TextView(this);
                tv.setText(m + "月");
                tv.setTextSize(15);
                tv.setTextColor(0xFF333333);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(12, 16, 12, 16);
                tv.setOnClickListener(v -> {
                    selectedCal = (Calendar) dialogCal.clone();
                    selectedCal.set(Calendar.MONTH, month - 1);
                    selectedDateStr = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(selectedCal.getTime());
                    tvSelectedDate.setText(selectedDateStr);
                    if (currentDialog != null) currentDialog.dismiss();
                });
                glCalendar.addView(tv);
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
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(12, 16, 12, 16);
                tv.setOnClickListener(v -> {
                    selectedCal = Calendar.getInstance();
                    selectedCal.set(Calendar.YEAR, year);
                    selectedDateStr = String.valueOf(year);
                    tvSelectedDate.setText(selectedDateStr);
                    if (currentDialog != null) currentDialog.dismiss();
                });
                glCalendar.addView(tv);
            }
        }
    }

    private void addDayCell(String text, boolean isDay) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(8, 12, 8, 12);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        tv.setLayoutParams(params);
        glCalendar.addView(tv);
    }

    private void clearSummary() {
        tvTotalSales.setText("--");
        tvTotalProfit.setText("--");
        tvSaleCount.setText("--");
    }

    private void loadSales() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(BillActivity.this);
            SaleDao saleDao = db.saleDao();
            List<Sale> sales;

            if (currentMode.equals("day")) {
                sales = saleDao.getSalesByDate(selectedDateStr);
            } else if (currentMode.equals("month")) {
                sales = saleDao.getSalesByDateRange(
                        selectedDateStr + "-01",
                        selectedDateStr + "-31");
            } else {
                sales = saleDao.getSalesByDateRange(
                        selectedDateStr + "-01-01",
                        selectedDateStr + "-12-31");
            }

            double totalSales = 0;
            double totalProfit = 0;
            int count = 0;
            for (Sale s : sales) {
                if (!s.isRefunded()) {
                    totalSales += s.getSellingPrice() * s.getQuantity();
                    totalProfit += (s.getSellingPrice() - s.getCostPrice()) * s.getQuantity();
                    count++;
                }
            }

            double finalSales = totalSales;
            double finalProfit = totalProfit;
            int finalCount = count;

            runOnUiThread(() -> {
                saleList.clear();
                saleList.addAll(sales);
                adapter.notifyDataSetChanged();

                tvTotalSales.setText("¥" + finalSales);
                tvTotalProfit.setText("¥" + finalProfit);
                tvSaleCount.setText(String.valueOf(finalCount));
            });
        }).start();
    }

    // ========== 适配器 ==========
    private class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {

        private List<Sale> list;

        public SaleAdapter(List<Sale> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_sale, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Sale sale = list.get(position);
            holder.tvTime.setText(sale.getSaleTime() != null ?
                    sale.getSaleTime().substring(0, 5) : "");
            loadProductName(sale.getProductId(), holder.tvProduct);
            holder.tvAmount.setText("¥" + (sale.getSellingPrice() * sale.getQuantity()));
            // 点击整行查看详情
            holder.itemView.setOnClickListener(v -> showSaleDetailDialog(sale));

            double profit = (sale.getSellingPrice() - sale.getCostPrice()) * sale.getQuantity();
            holder.tvDetail.setText("数量: " + sale.getQuantity() +
                    "  进价: ¥" + sale.getCostPrice() +
                    "  毛利: ¥" + profit);

            if (sale.isRefunded()) {
                holder.tvRefunded.setVisibility(View.VISIBLE);
                holder.tvAmount.setTextColor(0xFF999999);
            } else {
                holder.tvRefunded.setVisibility(View.GONE);
                holder.tvAmount.setTextColor(0xFFFF5722);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        private void loadProductName(long productId, TextView textView) {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(BillActivity.this);
                ProductDao dao = db.productDao();
                Product product = dao.getById(productId);
                runOnUiThread(() -> {
                    textView.setText(product != null ? product.getName() : "未知商品");
                });
            }).start();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvProduct, tvAmount, tvDetail, tvRefunded;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_sale_time);
                tvProduct = itemView.findViewById(R.id.tv_sale_product);
                tvAmount = itemView.findViewById(R.id.tv_sale_amount);
                tvDetail = itemView.findViewById(R.id.tv_sale_detail);
                tvRefunded = itemView.findViewById(R.id.tv_sale_refunded);
            }
        }
    }
    /**
     * 点击销售记录，弹出详情对话框
     */
    private void showSaleDetailDialog(Sale sale) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(BillActivity.this);
            Product product = db.productDao().getById(sale.getProductId());

            runOnUiThread(() -> {
                if (product == null) {
                    Toast.makeText(BillActivity.this, "无法获取商品信息", Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(BillActivity.this);
                View view = LayoutInflater.from(BillActivity.this).inflate(R.layout.dialog_sale_detail, null);

                ImageView ivPhoto = view.findViewById(R.id.iv_detail_photo);
                TextView tvName = view.findViewById(R.id.tv_detail_name);
                TextView tvInfo = view.findViewById(R.id.tv_detail_info);
                TextView tvTime = view.findViewById(R.id.tv_detail_time);

                // 商品图片
                String imagePath = product.getImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    ivPhoto.setImageURI(Uri.fromFile(new File(imagePath)));
                } else {
                    ivPhoto.setImageResource(android.R.drawable.ic_menu_camera);
                }

                // 商品名
                tvName.setText(product.getName());

                // 数量、售价、进价、毛利
                double profit = (sale.getSellingPrice() - sale.getCostPrice()) * sale.getQuantity();
                tvInfo.setText("数量: " + sale.getQuantity() +
                        "  售价: ¥" + sale.getSellingPrice() +
                        "\n进价: ¥" + sale.getCostPrice() +
                        "  毛利: ¥" + profit);

                // 时间
                tvTime.setText(sale.getSaleDate() + " " + sale.getSaleTime());

                builder.setView(view)
                        .setPositiveButton("关闭", null)
                        .show();
            });
        }).start();
    }
}