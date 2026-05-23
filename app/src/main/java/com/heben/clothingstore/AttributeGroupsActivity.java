package com.heben.clothingstore;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.heben.clothingstore.database.AppDatabase;
import com.heben.clothingstore.dao.AttributeGroupDao;
import com.heben.clothingstore.dao.AttributeValueDao;
import com.heben.clothingstore.entity.AttributeGroup;
import com.heben.clothingstore.entity.AttributeValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 属性管理页面：展示所有属性组（如风格、面料、颜色），
 * 每个组下方显示已有的属性值标签，可点击标签删除单个值，
 * 点击 + 按钮可批量添加新值。
 */
public class AttributeGroupsActivity extends AppCompatActivity {

    // ========== UI 组件 ==========
    private RecyclerView rvGroups;
    private EditText etGroupName;
    private Button btnAddGroup;

    // ========== 数据 ==========
    private GroupAdapter adapter;
    private List<AttributeGroup> groupList = new ArrayList<>();                // 所有属性组
    private Map<Long, List<AttributeValue>> valuesMap = new HashMap<>();      // 属性组ID → 该组下的属性值列表

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attribute_groups);

        // 绑定视图
        etGroupName = findViewById(R.id.et_group_name);
        btnAddGroup = findViewById(R.id.btn_add_group);
        rvGroups = findViewById(R.id.rv_attribute_groups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        // 初始化适配器
        adapter = new GroupAdapter(groupList, valuesMap);
        rvGroups.setAdapter(adapter);

        // 加载数据
        loadAllData();

        // 点击添加属性组
        btnAddGroup.setOnClickListener(view -> addGroup());
    }

    // ==================== 数据加载 ====================

    /** 加载所有属性组及其属性值，更新界面 */
    private void loadAllData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeGroupsActivity.this);
            AttributeGroupDao groupDao = db.attributeGroupDao();
            AttributeValueDao valueDao = db.attributeValueDao();

            List<AttributeGroup> groups = groupDao.getAllGroups();
            Map<Long, List<AttributeValue>> map = new HashMap<>();
            for (AttributeGroup g : groups) {
                map.put(g.getId(), valueDao.getByGroupId(g.getId()));
            }

            runOnUiThread(() -> {
                groupList.clear();
                groupList.addAll(groups);
                valuesMap.clear();
                valuesMap.putAll(map);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    // ==================== 添加属性组 ====================

    /** 在输入框中输入名称，创建新属性组（如“季节”） */
    private void addGroup() {
        String name = etGroupName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入属性组名", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeGroupsActivity.this);
            AttributeGroupDao dao = db.attributeGroupDao();

            AttributeGroup group = new AttributeGroup();
            group.setName(name);
            group.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            long id = dao.insert(group);

            runOnUiThread(() -> {
                if (id > 0) {
                    Toast.makeText(AttributeGroupsActivity.this, "属性组添加成功！", Toast.LENGTH_SHORT).show();
                    etGroupName.setText("");
                    loadAllData();   // 重新加载，让新组出现在列表中
                } else {
                    Toast.makeText(AttributeGroupsActivity.this, "添加失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    // ==================== 删除属性组 ====================

    /** 删除整个属性组（会级联删除其下所有属性值，数据库已设置外键级联） */
    private void deleteGroup(AttributeGroup group) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeGroupsActivity.this);
            db.attributeGroupDao().delete(group);
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除: " + group.getName(), Toast.LENGTH_SHORT).show();
                loadAllData();
            });
        }).start();
    }

    // ==================== 删除单个属性值（新增） ====================

    /** 删除某一个具体的属性值（如删除“休闲风”） */
    private void deleteValue(AttributeValue value) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(AttributeGroupsActivity.this);
            db.attributeValueDao().delete(value);
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除: " + value.getValue(), Toast.LENGTH_SHORT).show();
                loadAllData();   // 刷新列表
            });
        }).start();
    }

    // ==================== 批量添加属性值 ====================

    /** 弹出对话框，支持一次输入多个值（用逗号分隔），提示文字会动态匹配属性组名称 */
    private void showAddValueDialog(AttributeGroup group) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("为「" + group.getName() + "」添加属性值");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_values, null);
        EditText etValues = view.findViewById(R.id.et_values);
        // 动态设置提示：例如颜色组显示“输入新颜色，多个用逗号隔开”
        etValues.setHint("输入新" + group.getName() + "，多个用逗号隔开");
        builder.setView(view);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String text = etValues.getText().toString().trim();
            if (text.isEmpty()) return;

            // 支持中文逗号、英文逗号、顿号分隔
            String[] parts = text.split("[,，、]");
            List<String> valueList = new ArrayList<>();
            for (String s : parts) {
                String v = s.trim();
                if (!v.isEmpty()) valueList.add(v);
            }
            if (valueList.isEmpty()) return;

            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(AttributeGroupsActivity.this);
                AttributeValueDao valueDao = db.attributeValueDao();
                String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                for (String v : valueList) {
                    AttributeValue av = new AttributeValue();
                    av.setGroupId(group.getId());
                    av.setValue(v);
                    av.setCreatedAt(now);
                    valueDao.insert(av);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "已添加 " + valueList.size() + " 个属性值", Toast.LENGTH_SHORT).show();
                    loadAllData();
                });
            }).start();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    // ==================== 列表适配器 ====================

    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {

        private List<AttributeGroup> list;
        private Map<Long, List<AttributeValue>> map;

        public GroupAdapter(List<AttributeGroup> list, Map<Long, List<AttributeValue>> map) {
            this.list = list;
            this.map = map;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attribute_group, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttributeGroup group = list.get(position);
            holder.tvName.setText(group.getName());

            // 点击整个条目跳转到属性值管理页面（旧功能保留）
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AttributeGroupsActivity.this, AttributeValuesActivity.class);
                intent.putExtra("group_id", group.getId());
                intent.putExtra("group_name", group.getName());
                startActivity(intent);
            });

            // 删除整个属性组
            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(AttributeGroupsActivity.this)
                        .setTitle("删除属性组")
                        .setMessage("确定要删除「" + group.getName() + "」及其所有属性值吗？")
                        .setPositiveButton("删除", (dialog, which) -> deleteGroup(group))
                        .setNegativeButton("取消", null)
                        .show();
            });

            // ========== 填充属性值标签（可点击删除单个值） ==========
            LinearLayout tagsLayout = holder.llValueTags;
            tagsLayout.removeAllViews();
            List<AttributeValue> values = map.get(group.getId());
            if (values != null) {
                for (AttributeValue value : values) {
                    TextView tag = new TextView(AttributeGroupsActivity.this);
                    tag.setText(value.getValue());
                    tag.setTextSize(13);
                    tag.setTextColor(0xFFFFFFFF);
                    tag.setBackgroundColor(0xFF2196F3);
                    tag.setPadding(16, 6, 16, 6);
                    LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    tagParams.setMargins(0, 0, 8, 0);
                    tag.setLayoutParams(tagParams);

                    // 点击标签直接删除该属性值（核心改动）
                    tag.setOnClickListener(v -> {
                        new AlertDialog.Builder(AttributeGroupsActivity.this)
                                .setTitle("删除属性值")
                                .setMessage("确定要删除「" + value.getValue() + "」吗？")
                                .setPositiveButton("删除", (dialog, which) -> deleteValue(value))
                                .setNegativeButton("取消", null)
                                .show();
                    });

                    tagsLayout.addView(tag);
                }
            }

            // 添加属性值按钮
            holder.btnAddValue.setOnClickListener(v -> showAddValueDialog(group));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            Button btnDelete, btnAddValue;
            LinearLayout llValueTags;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_group_name);
                btnDelete = itemView.findViewById(R.id.btn_delete_group);
                btnAddValue = itemView.findViewById(R.id.btn_add_value);
                llValueTags = itemView.findViewById(R.id.ll_value_tags);
            }
        }
    }
}