package com.heben.clothingstore;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.heben.clothingstore.entity.ProductStock;
import android.app.AlertDialog;

import java.util.List;
import com.heben.clothingstore.entity.Product;


public class AnalysisActivity extends BaseActivity {

    private TextView tvReportSales, tvReportProfit, tvReportOrders, tvReportAvg;
    private LinearLayout llTrendInsight, llHotInsight, llSlowInsight, llRestockInsight, llPeakInsight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        // 今日快报四卡片
        tvReportSales = findViewById(R.id.tv_report_sales);
        tvReportProfit = findViewById(R.id.tv_report_profit);
        tvReportOrders = findViewById(R.id.tv_report_orders);
        tvReportAvg = findViewById(R.id.tv_report_avg);

        // 各模块结论容器
        llTrendInsight = findViewById(R.id.ll_trend_insight);
        llHotInsight = findViewById(R.id.ll_hot_insight);
        llSlowInsight = findViewById(R.id.ll_slow_insight);
        llRestockInsight = findViewById(R.id.ll_restock_insight);
        llPeakInsight = findViewById(R.id.ll_peak_insight);

        // 加载所有数据
        loadDailyReport();
        loadTrendInsight();
        loadHotInsight();
        loadSlowInsight();
        loadRestockInsight();
        loadPeakInsight();

        // 今日快报 → 跳转到账单页（因为没有单独图表，跳转到账单看明细）
        Button btnDaily = findViewById(R.id.btn_daily_chart);
        btnDaily.setOnClickListener(v ->
                startActivity(new Intent(this, BillActivity.class)));

        // 趋势 → 横屏折线图
        Button btnTrend = findViewById(R.id.btn_trend_chart);
        btnTrend.setOnClickListener(v ->
                startActivity(new Intent(this, ChartLandscapeActivity.class)
                        .putExtra("type", "trend")));

        // 热销 → 横屏柱状图
        Button btnHot = findViewById(R.id.btn_hot_chart);
        btnHot.setOnClickListener(v ->
                startActivity(new Intent(this, ChartLandscapeActivity.class)
                        .putExtra("type", "hot")));

        // 滞销 → 弹窗列表
        Button btnSlow = findViewById(R.id.btn_slow_chart);
        btnSlow.setOnClickListener(v -> showSlowListDialog());

        // 补货 → 弹窗列表
        Button btnRestock = findViewById(R.id.btn_restock_chart);
        btnRestock.setOnClickListener(v -> showRestockListDialog());

        // 时段 → 横屏柱状图
        Button btnPeak = findViewById(R.id.btn_peak_chart);
        btnPeak.setOnClickListener(v ->
                startActivity(new Intent(this, ChartLandscapeActivity.class)
                        .putExtra("type", "peak")));
    }

    private void showInsightText(LinearLayout container, String text) {
        TextView tv = new TextView(this);
        tv.setText("📌 " + text);
        tv.setTextSize(15);
        tv.setTextColor(getColor(R.color.text_secondary));
        tv.setPadding(0, 4, 0, 4);
        container.addView(tv);
    }

    // ==================== 今日快报 ====================
    private void loadDailyReport() {
        new Thread(() -> {
            BusinessAdvisor.DailyReport report = BusinessAdvisor.getDailyReport(AnalysisActivity.this);
            runOnUiThread(() -> {
                tvReportSales.setText("¥" + (int) report.totalSales);
                tvReportProfit.setText("¥" + (int) report.totalProfit);
                tvReportOrders.setText(String.valueOf(report.orderCount));
                tvReportAvg.setText("¥" + (int) report.avgOrderValue);
            });
        }).start();
    }

    // ==================== 趋势结论 ====================
    private void loadTrendInsight() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getSalesTrend(AnalysisActivity.this);
            runOnUiThread(() -> {
                for (BusinessAdvisor.Insight ins : result.insights) {
                    showInsightText(llTrendInsight, ins.detail);
                }
            });
        }).start();
    }

    // ==================== 热销结论 ====================
    private void loadHotInsight() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getHotProducts(AnalysisActivity.this);
            runOnUiThread(() -> {
                for (BusinessAdvisor.Insight ins : result.insights) {
                    showInsightText(llHotInsight, ins.detail);
                }
            });
        }).start();
    }

    // ==================== 滞销结论 ====================
    private void loadSlowInsight() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getSlowMoving(AnalysisActivity.this);
            runOnUiThread(() -> {
                for (BusinessAdvisor.Insight ins : result.insights) {
                    showInsightText(llSlowInsight, ins.detail);
                }
            });
        }).start();
    }

    // ==================== 补货结论 ====================
    private void loadRestockInsight() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getRestockAdvice(AnalysisActivity.this);
            runOnUiThread(() -> {
                for (BusinessAdvisor.Insight ins : result.insights) {
                    showInsightText(llRestockInsight, ins.detail);
                }
            });
        }).start();
    }

    // ==================== 时段结论 ====================
    private void loadPeakInsight() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getPeakHours(AnalysisActivity.this);
            runOnUiThread(() -> {
                for (BusinessAdvisor.Insight ins : result.insights) {
                    showInsightText(llPeakInsight, ins.detail);
                }
            });
        }).start();
    }

    /**
     * 弹窗展示滞销商品列表
     */
    private void showSlowListDialog() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getSlowMoving(AnalysisActivity.this);
            List<Product> slowList = (List<Product>) result.data;
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(AnalysisActivity.this);
                builder.setTitle("❄️ 滞销商品列表");

                LinearLayout layout = new LinearLayout(AnalysisActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(24, 16, 24, 16);

                if (slowList.isEmpty()) {
                    TextView tv = new TextView(AnalysisActivity.this);
                    tv.setText("暂无滞销商品");
                    tv.setTextSize(15);
                    tv.setTextColor(getColor(R.color.text_secondary));
                    layout.addView(tv);
                } else {
                    for (Product p : slowList) {
                        TextView tv = new TextView(AnalysisActivity.this);
                        tv.setText("❄️ " + p.getName());
                        tv.setTextSize(15);
                        tv.setTextColor(getColor(R.color.text_secondary));
                        tv.setPadding(0, 8, 0, 8);
                        layout.addView(tv);
                    }
                }

                builder.setView(layout);
                builder.setPositiveButton("关闭", null);
                builder.show();
            });
        }).start();
    }

    //弹窗展示补货建议列表
    private void showRestockListDialog() {
        new Thread(() -> {
            BusinessAdvisor.AnalysisResult result = BusinessAdvisor.getRestockAdvice(AnalysisActivity.this);
            List<ProductStock> stockList = (List<ProductStock>) result.data;
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(AnalysisActivity.this);
                builder.setTitle("⚠️ 补货建议列表");

                LinearLayout layout = new LinearLayout(AnalysisActivity.this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(24, 16, 24, 16);

                if (stockList.isEmpty()) {
                    TextView tv = new TextView(AnalysisActivity.this);
                    tv.setText("库存充足，无需补货");
                    tv.setTextSize(15);
                    tv.setTextColor(getColor(R.color.text_secondary));
                    layout.addView(tv);
                } else {
                    for (ProductStock ps : stockList) {
                        TextView tv = new TextView(AnalysisActivity.this);
                        String icon = ps.getCurrentStock() <= 3 ? "🔴" : "🟡";
                        tv.setText(icon + " " + ps.getName() + " — 库存 " + ps.getCurrentStock() + " 件");
                        tv.setTextSize(15);
                        tv.setTextColor(ps.getCurrentStock() <= 3 ? 0xFFC62828 : 0xFFFF9800);
                        tv.setPadding(0, 8, 0, 8);
                        layout.addView(tv);
                    }
                }

                builder.setView(layout);
                builder.setPositiveButton("关闭", null);
                builder.show();
            });
        }).start();
    }
}