package com.heben.clothingstore;

import android.content.Context;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.entity.Product;
import com.heben.clothingstore.entity.ProductRank;
import com.heben.clothingstore.entity.ProductStock;
import com.heben.clothingstore.entity.MonthProfit;
import com.heben.clothingstore.entity.Sale;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BusinessAdvisor {

    // ==================== 数据结构 ====================

    public static class Insight {
        public String detail;

        public Insight(String detail) {
            this.detail = detail;
        }
    }

    public static class DailyReport {
        public double totalSales;
        public double totalProfit;
        public int orderCount;
        public double avgOrderValue;
    }

    public static class AnalysisResult {
        public List<Insight> insights = new ArrayList<>();
        public Object data;
        public String chartType;
    }

    // ==================== 今日快报 ====================

    public static DailyReport getDailyReport(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DailyReport report = new DailyReport();
        Double sales = db.saleDao().getDailyTotal(today);
        Double profit = db.saleDao().getDailyProfit(today);
        report.totalSales = sales != null ? sales : 0;
        report.totalProfit = profit != null ? profit : 0;
        List<Sale> todaySales = db.saleDao().getSalesByDate(today);
        report.orderCount = todaySales != null ? todaySales.size() : 0;
        report.avgOrderValue = report.orderCount > 0 ? report.totalSales / report.orderCount : 0;
        return report;
    }

    // ==================== 热销分析 ====================

    public static AnalysisResult getHotProducts(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<ProductRank> allRanks = db.saleDao().getProductRank();

        AnalysisResult result = new AnalysisResult();
        result.chartType = "horizontal_bar";
        result.data = allRanks;

        if (allRanks != null && !allRanks.isEmpty()) {
            ProductRank top = allRanks.get(0);
            int totalQty = 0;
            for (ProductRank r : allRanks) totalQty += r.getTotalQty();
            double ratio = totalQty > 0 ? (top.getTotalQty() * 100.0 / totalQty) : 0;
            result.insights.add(new Insight("「" + top.getName() + "」是本月销冠，占总销量 " + String.format("%.0f", ratio) + "%"));
            if (allRanks.size() >= 3) {
                StringBuilder sb = new StringBuilder("本月销量 Top 3：");
                for (int i = 0; i < 3 && i < allRanks.size(); i++) {
                    sb.append(allRanks.get(i).getName()).append("(").append(allRanks.get(i).getTotalQty()).append("件)");
                    if (i < 2) sb.append("、");
                }
                result.insights.add(new Insight(sb.toString()));
            }
        } else {
            result.insights.add(new Insight("暂无销售数据"));
        }
        return result;
    }

    // ==================== 滞销分析 ====================

    public static AnalysisResult getSlowMoving(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<Product> allProducts = db.productDao().getAllProducts();
        List<Product> slowList = new ArrayList<>();

        for (Product p : allProducts) {
            List<Sale> sales = db.saleDao().getSalesByProduct(p.getId());
            boolean hasRecent = false;
            if (sales != null) {
                long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
                for (Sale s : sales) {
                    try {
                        if (new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(s.getSaleDate()).getTime() >= thirtyDaysAgo) {
                            hasRecent = true; break;
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (!hasRecent && sales != null && !sales.isEmpty()) slowList.add(p);
        }

        AnalysisResult result = new AnalysisResult();
        result.chartType = "list";
        result.data = slowList;

        if (!slowList.isEmpty()) {
            result.insights.add(new Insight("有 " + slowList.size() + " 件商品近30天未售出，建议关注"));
            StringBuilder sb = new StringBuilder("建议清仓：");
            for (int i = 0; i < Math.min(3, slowList.size()); i++) {
                sb.append(slowList.get(i).getName());
                if (i < Math.min(2, slowList.size() - 1)) sb.append("、");
            }
            result.insights.add(new Insight(sb.toString()));
        } else {
            result.insights.add(new Insight("所有商品近30天均有销售"));
        }
        return result;
    }

    // ==================== 补货建议 ====================

    public static AnalysisResult getRestockAdvice(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<ProductStock> stockList = db.productDao().getProductsWithStock();
        List<ProductStock> lowStockList = new ArrayList<>();
        for (ProductStock ps : stockList) {
            if (ps.getCurrentStock() <= 5) lowStockList.add(ps);
        }

        AnalysisResult result = new AnalysisResult();
        result.chartType = "list";
        result.data = lowStockList;

        if (!lowStockList.isEmpty()) {
            result.insights.add(new Insight("有 " + lowStockList.size() + " 件商品库存不足，建议及时补货"));
            StringBuilder urgent = new StringBuilder("最紧急：");
            for (int i = 0; i < Math.min(3, lowStockList.size()); i++) {
                ProductStock ps = lowStockList.get(i);
                urgent.append(ps.getName()).append("(").append(ps.getCurrentStock()).append("件)");
                if (i < Math.min(2, lowStockList.size() - 1)) urgent.append("、");
            }
            result.insights.add(new Insight(urgent.toString()));
        } else {
            result.insights.add(new Insight("所有商品库存充足"));
        }
        return result;
    }

    // ==================== 趋势分析 ====================

    public static AnalysisResult getSalesTrend(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<MonthProfit> profits = db.saleDao().getMonthProfit();

        AnalysisResult result = new AnalysisResult();
        result.chartType = "line";
        result.data = profits;

        if (profits != null && profits.size() >= 2) {
            MonthProfit latest = profits.get(profits.size() - 1);
            MonthProfit prev = profits.get(profits.size() - 2);
            double change = prev.getTotalSales() > 0 ? (latest.getTotalSales() - prev.getTotalSales()) / prev.getTotalSales() * 100 : 0;
            String dir = change >= 0 ? "增长" : "下降";
            result.insights.add(new Insight("本月销售额比上月" + dir + " " + String.format("%.1f", Math.abs(change)) + "%"));
            double maxSales = 0;
            String maxMonth = "";
            for (MonthProfit mp : profits) {
                if (mp.getTotalSales() > maxSales) { maxSales = mp.getTotalSales(); maxMonth = mp.getMonth(); }
            }
            result.insights.add(new Insight("最高月 " + maxMonth + " 为 ¥" + (int)maxSales));
        } else {
            result.insights.add(new Insight("需要更多数据才能分析趋势"));
        }
        return result;
    }

    // ==================== 时段分析 ====================

    public static AnalysisResult getPeakHours(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        List<Sale> allSales = db.saleDao().getAllSales();
        int[] hourCount = new int[24];
        for (Sale s : allSales) {
            if (s.getSaleTime() != null && s.getSaleTime().length() >= 2) {
                try {
                    int hour = Integer.parseInt(s.getSaleTime().substring(0, 2));
                    if (hour >= 0 && hour < 24) hourCount[hour]++;
                } catch (NumberFormatException ignored) {}
            }
        }

        AnalysisResult result = new AnalysisResult();
        result.chartType = "bar";
        result.data = hourCount;

        int maxHour = 0, minHour = 0, maxCount = 0, minCount = Integer.MAX_VALUE;
        for (int i = 0; i < 24; i++) {
            if (hourCount[i] > maxCount) { maxCount = hourCount[i]; maxHour = i; }
            if (hourCount[i] < minCount) { minCount = hourCount[i]; minHour = i; }
        }
        result.insights.add(new Insight("下午 " + maxHour + ":00 左右订单最多"));
        result.insights.add(new Insight("上午 " + minHour + ":00 左右订单最少"));
        return result;
    }
}