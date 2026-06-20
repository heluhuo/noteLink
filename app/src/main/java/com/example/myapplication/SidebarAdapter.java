package com.example.myapplication;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SidebarAdapter — 侧边栏分类+日记树形列表适配器
 * 支持分类展开/折叠，显示每个分类下的日记条目
 */
public class SidebarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_NOTE     = 1;

    /** 列表项（分类 or 日记） */
    public static class Item {
        public static final int KIND_CATEGORY = 0;
        public static final int KIND_NOTE     = 1;

        public int kind;
        public String categoryName;  // 分类名（kind=0）
        public int noteCount;        // 分类下笔记数（kind=0）
        public boolean expanded;     // 是否展开（kind=0）
        public Note note;            // 日记对象（kind=1）

        public static Item category(String name, int count, boolean expanded) {
            Item it = new Item();
            it.kind = KIND_CATEGORY;
            it.categoryName = name;
            it.noteCount = count;
            it.expanded = expanded;
            return it;
        }

        public static Item note(Note n) {
            Item it = new Item();
            it.kind = KIND_NOTE;
            it.note = n;
            return it;
        }
    }

    /** 全部数据（按分类分组，每个分类后跟其笔记） */
    public static class CategoryGroup {
        public String name;
        public List<Note> notes;
        public boolean expanded = true;

        public CategoryGroup(String name) {
            this.name = name;
            this.notes = new ArrayList<>();
        }
    }

    private final List<Item> displayItems = new ArrayList<>();
    private List<CategoryGroup> groups = new ArrayList<>();

    private OnCategoryClickListener categoryClickListener;
    private OnNoteClickListener noteClickListener;
    private ThemeHelper.Palette palette;

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener l) { this.categoryClickListener = l; }
    public void setOnNoteClickListener(OnNoteClickListener l) { this.noteClickListener = l; }

    /** 设置主题色板 — 由 Activity 在 onResume 时调用 */
    public void setPalette(ThemeHelper.Palette palette) {
        this.palette = palette;
        if (!displayItems.isEmpty()) notifyDataSetChanged();
    }

    /** 设置分组数据并重建显示列表 */
    public void setGroups(List<CategoryGroup> groups) {
        this.groups = groups != null ? groups : new ArrayList<>();
        rebuildDisplayItems();
    }

    /** 追加一个新分类 */
    public void addCategory(String name) {
        CategoryGroup g = new CategoryGroup(name);
        groups.add(g);
        rebuildDisplayItems();
    }

    /** 重建显示列表（根据展开状态） */
    private void rebuildDisplayItems() {
        displayItems.clear();
        for (CategoryGroup g : groups) {
            displayItems.add(Item.category(g.name, g.notes.size(), g.expanded));
            if (g.expanded) {
                for (Note n : g.notes) {
                    displayItems.add(Item.note(n));
                }
            }
        }
        notifyDataSetChanged();
    }

    /** 切换分类展开状态 */
    private void toggleCategory(int displayPos) {
        Item item = displayItems.get(displayPos);
        if (item.kind != Item.KIND_CATEGORY) return;

        // 找到对应的 group
        for (CategoryGroup g : groups) {
            if (g.name.equals(item.categoryName)) {
                g.expanded = !g.expanded;
                break;
            }
        }
        rebuildDisplayItems();
    }

    // ==================== RecyclerView ====================

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).kind;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_CATEGORY) {
            View v = inflater.inflate(R.layout.item_sidebar_category, parent, false);
            return new CategoryVH(v);
        } else {
            View v = inflater.inflate(R.layout.item_sidebar_note, parent, false);
            return new NoteVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = displayItems.get(position);
        if (item.kind == Item.KIND_CATEGORY) {
            CategoryVH cvh = (CategoryVH) holder;
            cvh.tvName.setText(item.categoryName);
            cvh.tvCount.setText(String.valueOf(item.noteCount));
            cvh.tvExpand.setText(item.expanded ? "\u25BC" : "\u25B6");

            // 应用主题色
            if (palette != null) {
                cvh.tvName.setTextColor(palette.textPrimary);
                cvh.tvCount.setTextColor(palette.textHint);
                cvh.tvExpand.setTextColor(palette.textSecondary);
            }

            cvh.itemView.setOnClickListener(v -> {
                toggleCategory(holder.getAdapterPosition());
                if (categoryClickListener != null) {
                    categoryClickListener.onCategoryClick(item.categoryName);
                }
            });
        } else {
            NoteVH nvh = (NoteVH) holder;
            Note note = item.note;
            nvh.tvTitle.setText(note.title != null ? note.title : "\u65E0\u6807\u9898");
            nvh.tvDate.setText(note.date != null ? note.date : "");

            // 应用主题色
            if (palette != null) {
                nvh.tvTitle.setTextColor(palette.textSecondary);
                nvh.tvDate.setTextColor(palette.textHint);
            }

            nvh.itemView.setOnClickListener(v -> {
                if (noteClickListener != null) {
                    noteClickListener.onNoteClick(note);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class CategoryVH extends RecyclerView.ViewHolder {
        TextView tvExpand, tvName, tvCount;
        CategoryVH(@NonNull View v) {
            super(v);
            tvExpand = v.findViewById(R.id.tv_expand_icon);
            tvName   = v.findViewById(R.id.tv_category_name);
            tvCount  = v.findViewById(R.id.tv_note_count);
        }
    }

    static class NoteVH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        NoteVH(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tv_note_title);
            tvDate  = v.findViewById(R.id.tv_note_date);
        }
    }
}
