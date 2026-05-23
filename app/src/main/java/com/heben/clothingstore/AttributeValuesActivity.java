package com.heben.clothingstore;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.AttributeValueDao;
import com.heben.clothingstore.entity.AttributeValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttributeValuesActivity extends AppCompatActivity {

    private RecyclerView rvValues;
    private EditText etValueName;
    private Button btnAddValue;
    private TextView tvGroupTitle;
    private ValueAdapter adapter;
    private List<AttributeValue> valueList = new ArrayList<>();

    private long groupId;
    private String groupName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute_values);

        // 获取从上一页传来的属性组信息
        groupId = getIntent().getLongExtra("group_id", 0);
        groupName = getIntent().getStringExtra("group_name");

        tvGroupTitle = findViewById(R.id.tv_group_title);
        tvGroupTitle.setText(groupName != null ? groupName : "属性值");

        etValueName = findViewById(R.id.et_value_name);
        btnAddValue = findViewById(R.id.btn_add_value);
        rvValues = findViewById(R.id.rv_attribute_values);
        rvValues.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ValueAdapter(valueList);
        rvValues.setAdapter(adapter);

        loadValues();

        btnAddValue.setOnClickListener(view -> addValue());
    }

    private void loadValues() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeValuesActivity.this);
            AttributeValueDao dao = db.attributeValueDao();
            List<AttributeValue> values = dao.getByGroupId(groupId);

            runOnUiThread(() -> {
                valueList.clear();
                valueList.addAll(values);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void addValue() {
        String value = etValueName.getText().toString().trim();
        if (value.isEmpty()) {
            Toast.makeText(this, "请输入属性值", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeValuesActivity.this);
            AttributeValueDao dao = db.attributeValueDao();

            AttributeValue attrValue = new AttributeValue();
            attrValue.setGroupId(groupId);
            attrValue.setValue(value);
            attrValue.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            long id = dao.insert(attrValue);

            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(AttributeValuesActivity.this, "属性值添加成功！", Toast.LENGTH_SHORT).show();
                    etValueName.setText("");
                    loadValues();
                } else {
                    Toast.makeText(AttributeValuesActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void deleteValue(AttributeValue value) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeValuesActivity.this);
            AttributeValueDao dao = db.attributeValueDao();
            dao.delete(value);

            runOnUiThread(() -> {
                Toast.makeText(AttributeValuesActivity.this, "已删除: " + value.getValue(), Toast.LENGTH_SHORT).show();
                loadValues();
            });
        }).start();
    }

    // ========== 适配器 ==========
    private class ValueAdapter extends RecyclerView.Adapter<ValueAdapter.ViewHolder> {

        private List<AttributeValue> list;

        public ValueAdapter(List<AttributeValue> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attribute_value, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttributeValue value = list.get(position);
            holder.tvName.setText(value.getValue());
            holder.btnDelete.setOnClickListener(view -> deleteValue(value));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            Button btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_value_name);
                btnDelete = itemView.findViewById(R.id.btn_delete_value);
            }
        }
    }
}