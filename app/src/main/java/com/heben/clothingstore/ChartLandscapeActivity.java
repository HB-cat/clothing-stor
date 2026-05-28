package com.heben.clothingstore;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.BusinessAdvisor;
import com.heben.clothingstore.entity.MonthProfit;
import com.heben.clothingstore.entity.ProductRank;
import com.heben.clothingstore.entity.Product;
import com.heben.clothingstore.entity.ProductStock;

import java.util.ArrayList;
import java.util.List;

public class ChartLandscapeActivity extends BaseActivity {

    private FrameLayout flChart;
    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart_landscape);

        flChart = findViewById(R.id.fl_chart_landscape);
        type = getIntent().getStringExtra("type");

        loadChart();
    }

    private void loadChart() {
        new Thread(() -> {
            switch (type) {
                case "trend": showTrendChart(); break;
                case "hot": showHotChart(); break;
                case "slow": showSlowList(); break;
                case "restock": showRestockList(); break;
                case "peak": showPeakChart(); break;
            }
        }).start();
    }

    private void showTrendChart() {
        List<MonthProfit> profits = AppDatabase.getInstance(this).saleDao().getMonthProfit();
        runOnUiThread(() -> {
            LineChart lineChart = new LineChart(this);
            lineChart.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < profits.size(); i++) {
                entries.add(new Entry(i, (float) profits.get(i).getTotalSales()));
                labels.add(profits.get(i).getMonth());
            }
            LineDataSet dataSet = new LineDataSet(entries, "销售额");
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setDrawValues(true);
            dataSet.setColor(Color.parseColor("#43A047"));
            dataSet.setCircleColor(Color.parseColor("#2E7D32"));
            dataSet.setLineWidth(2f);
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            lineChart.getDescription().setEnabled(false);
            lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float v) {
                    int idx = (int) v;
                    return idx >= 0 && idx < labels.size() ? labels.get(idx) : "";
                }
            });
            lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            flChart.addView(lineChart);
            lineChart.invalidate();
        });
    }

    private void showHotChart() {
        List<ProductRank> ranks = AppDatabase.getInstance(this).saleDao().getProductRank();
        runOnUiThread(() -> {
            HorizontalBarChart barChart = new HorizontalBarChart(this);
            barChart.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            List<BarEntry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < ranks.size(); i++) {
                entries.add(new BarEntry(i, ranks.get(i).getTotalQty()));
                labels.add(ranks.get(i).getName());
            }
            BarDataSet dataSet = new BarDataSet(entries, "销量");
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setDrawValues(true);
            dataSet.setColor(Color.parseColor("#43A047"));
            BarData barData = new BarData(dataSet);
            barChart.setData(barData);
            barChart.getDescription().setEnabled(false);
            barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float v) {
                    int idx = (int) v;
                    return idx >= 0 && idx < labels.size() ? labels.get(idx) : "";
                }
            });
            flChart.addView(barChart);
            barChart.invalidate();
        });
    }

    private void showSlowList() {
        List<Product> slowList = (List<Product>) BusinessAdvisor.getSlowMoving(this).data;
        runOnUiThread(() -> {
            LinearLayout listLayout = new LinearLayout(this);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setPadding(16, 16, 16, 16);
            if (slowList.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("暂无滞销商品");
                tv.setTextSize(18);
                tv.setTextColor(getColor(R.color.text_secondary));
                listLayout.addView(tv);
            } else {
                for (Product p : slowList) {
                    TextView tv = new TextView(this);
                    tv.setText("❄️ " + p.getName());
                    tv.setTextSize(16);
                    tv.setTextColor(getColor(R.color.text_secondary));
                    tv.setPadding(4, 12, 4, 12);
                    listLayout.addView(tv);
                }
            }
            flChart.addView(listLayout);
        });
    }

    private void showRestockList() {
        List<ProductStock> stockList = (List<ProductStock>) BusinessAdvisor.getRestockAdvice(this).data;
        runOnUiThread(() -> {
            LinearLayout listLayout = new LinearLayout(this);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            listLayout.setPadding(16, 16, 16, 16);
            if (stockList.isEmpty()) {
                TextView tv = new TextView(this);
                tv.setText("库存充足，无需补货");
                tv.setTextSize(18);
                tv.setTextColor(getColor(R.color.text_secondary));
                listLayout.addView(tv);
            } else {
                for (ProductStock ps : stockList) {
                    TextView tv = new TextView(this);
                    tv.setText("🔴 " + ps.getName() + " — 库存 " + ps.getCurrentStock() + " 件");
                    tv.setTextSize(16);
                    tv.setTextColor(ps.getCurrentStock() <= 3 ? 0xFFC62828 : 0xFFFF9800);
                    tv.setPadding(4, 12, 4, 12);
                    listLayout.addView(tv);
                }
            }
            flChart.addView(listLayout);
        });
    }

    private void showPeakChart() {
        int[] hourCount = (int[]) BusinessAdvisor.getPeakHours(this).data;
        runOnUiThread(() -> {
            BarChart barChart = new BarChart(this);
            barChart.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            List<BarEntry> entries = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                entries.add(new BarEntry(i, hourCount[i]));
            }
            BarDataSet dataSet = new BarDataSet(entries, "订单数");
            dataSet.setValueTextSize(10f);
            dataSet.setValueTextColor(Color.BLACK);
            dataSet.setDrawValues(true);
            dataSet.setColor(Color.parseColor("#43A047"));
            BarData barData = new BarData(dataSet);
            barChart.setData(barData);
            barChart.getDescription().setEnabled(false);
            barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float v) {
                    return ((int) v) + "时";
                }
            });
            flChart.addView(barChart);
            barChart.invalidate();
        });
    }
}