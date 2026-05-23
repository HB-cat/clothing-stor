package com.heben.clothingstore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.SaleDao;
import com.heben.clothingstore.entity.MonthProfit;

import java.util.ArrayList;
import java.util.List;

public class ProfitActivity extends AppCompatActivity {

    private RecyclerView rvProfit;
    private ProfitAdapter adapter;
    private List<MonthProfit> profitList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profit);

        rvProfit = findViewById(R.id.rv_profit);
        rvProfit.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ProfitAdapter(profitList);
        rvProfit.setAdapter(adapter);

        loadProfit();
    }

    private void loadProfit() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(ProfitActivity.this);
            SaleDao saleDao = db.saleDao();
            List<MonthProfit> list = saleDao.getMonthProfit();

            runOnUiThread(() -> {
                profitList.clear();
                profitList.addAll(list);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private class ProfitAdapter extends RecyclerView.Adapter<ProfitAdapter.ViewHolder> {

        private List<MonthProfit> list;

        public ProfitAdapter(List<MonthProfit> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_profit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MonthProfit item = list.get(position);
            holder.tvMonth.setText(item.getMonth());
            holder.tvSales.setText("¥" + (int)item.getTotalSales());
            holder.tvProfit.setText("¥" + (int)item.getTotalProfit());
            holder.tvCount.setText(item.getSaleCount() + "笔");
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMonth, tvSales, tvProfit, tvCount;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMonth = itemView.findViewById(R.id.tv_profit_month);
                tvSales = itemView.findViewById(R.id.tv_profit_sales);
                tvProfit = itemView.findViewById(R.id.tv_profit_profit);
                tvCount = itemView.findViewById(R.id.tv_profit_count);
            }
        }
    }
}