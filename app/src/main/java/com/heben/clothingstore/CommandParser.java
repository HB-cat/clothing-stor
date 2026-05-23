package com.heben.clothingstore;

import android.content.Context;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.ProductDao;
import com.heben.clothingstore.dao.SaleDao;
import com.heben.clothingstore.dao.PurchaseDao;
import com.heben.clothingstore.entity.Product;
import com.heben.clothingstore.entity.Sale;
import com.heben.clothingstore.entity.Purchase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 指令解析器：根据规则匹配用户输入的文字指令，
 * 自动识别操作类型、商品、数量、金额，并执行对应记账操作。
 *
 * 支持的操作类型：
 * - 销售：卖了/卖出/卖掉 + 商品名 + 数量 + 金额
 * - 进货：进了/进货/入库 + 商品名 + 数量 + 金额
 * - 退货：退了/退货/退款 + 商品名 + 数量
 */
public class CommandParser {

    /**
     * 解析结果封装
     */
    public static class ParseResult {
        public String type;        // sale / purchase / refund / unknown
        public String productName; // 匹配到的商品名
        public int quantity;       // 数量
        public double price;       // 单价
        public String message;     // 反馈信息
    }

    /**
     * 解析用户输入的文字指令
     */
    public static ParseResult parse(Context context, String input) {
        ParseResult result = new ParseResult();
        result.type = "unknown";
        result.quantity = 1;
        result.price = 0;

        if (input == null || input.trim().isEmpty()) {
            result.message = "请输入指令，例如：卖了连衣裙一件90块";
            return result;
        }

        String text = input.trim();

        // ===== 第一步：识别操作类型 =====
        if (text.contains("卖了") || text.contains("卖出") || text.contains("卖掉")) {
            result.type = "sale";
        } else if (text.contains("进了") || text.contains("进货") || text.contains("入库")) {
            result.type = "purchase";
        } else if (text.contains("退了") || text.contains("退货") || text.contains("退款")) {
            result.type = "refund";
        } else {
            result.message = "❌ 无法识别操作类型。请用以下格式：\n" +
                    "• 卖了[商品] [数量] [价格]\n" +
                    "• 进了[商品] [数量] [价格]\n" +
                    "• 退了[商品] [数量]";
            return result;
        }

        // ===== 第二步：提取金额（优先从末尾匹配） =====
        result.price = extractPrice(text);

        // ===== 第三步：提取数量 =====
        result.quantity = extractQuantity(text);

        // ===== 第四步：提取商品名（去除操作词、数量、金额后剩余的部分） =====
        String remaining = text
                .replaceAll("卖了|卖出|卖掉|进了|进货|入库|退了|退货|退款", "")
                .replaceAll("\\d+元|\\d+块|\\d+\\.\\d+元|\\d+\\.\\d+块", "")
                .replaceAll("一|两|三|四|五|六|七|八|九|十|\\d+", "")
                .replaceAll("个|件", "")
                .trim();

        result.productName = remaining;

        // ===== 第五步：去数据库匹配商品 =====
        if (!result.productName.isEmpty()) {
            Product matched = findProduct(context, result.productName);
            if (matched != null) {
                result.productName = matched.getName(); // 用数据库中的正式名称
                result.price = result.price > 0 ? result.price : matched.getSellingPrice();
            } else {
                result.message = "❌ 找不到商品「" + result.productName + "」，请先添加商品或检查名称";
                return result;
            }
        } else {
            result.message = "❌ 未识别到商品名，请重试";
            return result;
        }

        // ===== 第六步：执行操作 =====
        if (result.price <= 0 && result.type.equals("sale")) {
            result.message = "❌ 请指定价格，例如：卖了连衣裙一件90块";
            return result;
        }
        if (result.price <= 0 && result.type.equals("purchase")) {
            result.message = "❌ 请指定进货价，例如：进了连衣裙5件60块";
            return result;
        }

        // 执行数据库操作
        executeCommand(context, result);

        String typeName = result.type.equals("sale") ? "卖出" :
                result.type.equals("purchase") ? "进货" : "退货";
        result.message = "✅ " + typeName + "成功！\n" +
                "商品：" + result.productName + "\n" +
                "数量：" + result.quantity + "件" +
                (result.price > 0 ? "  单价：¥" + result.price : "");
        return result;
    }

    /**
     * 提取金额：支持 90元、90块、90
     */
    private static double extractPrice(String text) {
        // 匹配 90元、90块、90.5元 等
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)\\s*[元块]?");
        java.util.regex.Matcher m = p.matcher(text);
        double price = 0;
        while (m.find()) {
            try {
                price = Double.parseDouble(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return price; // 返回最后一个匹配到的金额
    }

    /**
     * 提取数量：支持 一件、两个、3件、3个
     */
    private static int extractQuantity(String text) {
        // 中文数字映射
        java.util.Map<String, Integer> cnNum = new java.util.HashMap<>();
        cnNum.put("一", 1); cnNum.put("两", 2); cnNum.put("三", 3);
        cnNum.put("四", 4); cnNum.put("五", 5); cnNum.put("六", 6);
        cnNum.put("七", 7); cnNum.put("八", 8); cnNum.put("九", 9); cnNum.put("十", 10);

        // 匹配 3件、3个、一件、两个 等
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("([一二两三四五六七八九十\\d]+)\\s*[件个]");
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find()) {
            String numStr = m.group(1);
            if (cnNum.containsKey(numStr)) {
                return cnNum.get(numStr);
            }
            try {
                return Integer.parseInt(numStr);
            } catch (NumberFormatException ignored) {}
        }
        return 1; // 默认1件
    }

    /**
     * 在数据库中模糊匹配商品（支持名称和别名）
     */
    private static Product findProduct(Context context, String keyword) {
        AppDatabase db = AppDatabase.getInstance(context);
        ProductDao dao = db.productDao();
        // 先用关键词搜索
        List<Product> results = dao.searchProducts(keyword);
        if (results != null && !results.isEmpty()) {
            return results.get(0); // 返回第一个匹配的
        }
        // 再尝试搜索所有商品，手动做简单模糊匹配
        List<Product> all = dao.getAllProducts();
        if (all != null) {
            for (Product p : all) {
                if (p.getName().contains(keyword) || keyword.contains(p.getName())) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 执行记账操作
     */
    private static void executeCommand(Context context, ParseResult result) {
        AppDatabase db = AppDatabase.getInstance(context);
        Product product = findProduct(context, result.productName);
        if (product == null) return;

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        switch (result.type) {
            case "sale": {
                Sale sale = new Sale();
                sale.setProductId(product.getId());
                sale.setQuantity(result.quantity);
                sale.setSellingPrice(result.price);
                sale.setCostPrice(product.getCostPrice());
                sale.setSaleDate(today);
                sale.setSaleTime(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
                sale.setRefunded(false);
                sale.setCreatedAt(now);
                db.saleDao().insert(sale);
                break;
            }
            case "purchase": {
                Purchase purchase = new Purchase();
                purchase.setProductId(product.getId());
                purchase.setQuantity(result.quantity);
                purchase.setCostPrice(result.price);
                purchase.setPurchaseDate(today);
                purchase.setAttributeDesc("快捷指令入库");
                purchase.setCreatedAt(now);
                db.purchaseDao().insert(purchase);
                break;
            }
            case "refund": {
                // 查找最近一笔该商品的未退款销售记录
                List<Sale> sales = db.saleDao().getSalesByProduct(product.getId());
                if (sales != null) {
                    for (Sale s : sales) {
                        if (!s.isRefunded() && s.getQuantity() == result.quantity) {
                            s.setRefunded(true);
                            db.saleDao().update(s);
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * 获取可用的指令格式提示（用于输入框占位或快捷提示）
     */
    public static String getHint() {
        return "📢 卖了[商品] [数量] [价格]\n" +
                "例如：卖了连衣裙一件90块";
    }

    /**
     * 获取所有可用的快捷指令示例
     */
    public static String[] getExamples() {
        return new String[]{
                "卖了连衣裙一件90块",
                "卖了T恤两件60",
                "进了连衣裙5件60块",
                "退了连衣裙一件"
        };
    }
}