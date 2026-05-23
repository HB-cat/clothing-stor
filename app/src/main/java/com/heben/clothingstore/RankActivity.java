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
import com.heben.clothingstore.entity.ProductRank;

import java.util.ArrayList;
import java.util.List;

public class RankActivity extends AppCompatActivity {

    private RecyclerView rvRank;
    private RankAdapter adapter;
    private List<ProductRank> rankList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        rvRank = findViewById(R.id.rv_rank);
        rvRank.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RankAdapter(rankList);
        rvRank.setAdapter(adapter);

        loadRank();
    }

    private void loadRank() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(RankActivity.this);
            SaleDao saleDao = db.saleDao();
            List<ProductRank> list = saleDao.getProductRank();

            runOnUiThread(() -> {
                rankList.clear();
                rankList.addAll(list);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private class RankAdapter extends RecyclerView.Adapter<RankAdapter.ViewHolder> {

        private List<ProductRank> list;

        public RankAdapter(List<ProductRank> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_rank, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ProductRank item = list.get(position);
            int rank = position + 1;

            holder.tvNum.setText(String.valueOf(rank));
            holder.tvName.setText(item.getName());
            holder.tvQty.setText(item.getTotalQty() + "件");
            holder.tvAmount.setText("¥" + (int)item.getTotalAmount());
            holder.tvProfit.setText("¥" + (int)item.getTotalProfit());

            // 前三名特殊颜色
            if (rank == 1) {
                holder.tvNum.setTextColor(0xFFFF9800);
            } else if (rank == 2) {
                holder.tvNum.setTextColor(0xFF2196F3);
            } else if (rank == 3) {
                holder.tvNum.setTextColor(0xFF8BC34A);
            } else {
                holder.tvNum.setTextColor(0xFF999999);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNum, tvName, tvQty, tvAmount, tvProfit;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNum = itemView.findViewById(R.id.tv_rank_num);
                tvName = itemView.findViewById(R.id.tv_rank_name);
                tvQty = itemView.findViewById(R.id.tv_rank_qty);
                tvAmount = itemView.findViewById(R.id.tv_rank_amount);
                tvProfit = itemView.findViewById(R.id.tv_rank_profit);
            }
        }
    }
}