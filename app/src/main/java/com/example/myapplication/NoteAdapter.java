package com.example.myapplication;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
 * NoteAdapter — RecyclerView 适配器
 * 展示笔记列表，支持点击和长按事件
 */
public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Note note);
        void onItemLongClick(Note note, int position);
    }

    private List<Note> notes = new ArrayList<>();
    private OnItemClickListener listener;
    private ThemeHelper.Palette palette;

    public NoteAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    /** 设置主题色板 — 由 Activity 在 onResume 时调用 */
    public void setPalette(ThemeHelper.Palette palette) {
        this.palette = palette;
        if (!notes.isEmpty()) notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setNotes(List<Note> newNotes) {
        this.notes = newNotes != null ? newNotes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < notes.size()) {
            notes.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Note getNoteAt(int position) {
        return notes.get(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.tvTitle.setText(note.title != null ? note.title : "（无标题）");
        holder.tvDate.setText(note.date != null ? note.date : "");
        holder.tvCategory.setText(note.category != null ? note.category : "");

        // 内容预览：去除内链语法符号
        String preview = note.content != null
                ? note.content.replaceAll("\\[\\[(.+?)\\]\\]", "$1")
                : "";
        holder.tvPreview.setText(preview);

        // 置顶标识
        if (note.pinned) {
            holder.tvTitle.setText("📌 " + holder.tvTitle.getText());
        }

        // 标签
        if (note.tags != null && !note.tags.isEmpty()) {
            holder.tvTags.setText("🏷 " + note.tags);
            holder.tvTags.setVisibility(View.VISIBLE);
        } else {
            holder.tvTags.setVisibility(View.GONE);
        }

        // 应用主题色
        applyTheme(holder);

        // 点击事件
        holder.itemView.setOnClickListener(v -> listener.onItemClick(note));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(note, holder.getAdapterPosition());
            return true;
        });
    }

    /** 将当前主题色板应用到 item 视图 */
    private void applyTheme(ViewHolder holder) {
        if (palette == null) return;

        // item 卡片背景
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(palette.card);
        cardBg.setCornerRadius(dpToPx(holder.itemView.getContext(), 8));
        holder.itemView.setBackground(cardBg);

        // 文字颜色
        holder.tvTitle.setTextColor(palette.textPrimary);
        holder.tvPreview.setTextColor(palette.textSecondary);
        holder.tvDate.setTextColor(palette.textHint);
        holder.tvTags.setTextColor(palette.textHint);

        // 分类标签背景
        GradientDrawable tagBg = new GradientDrawable();
        tagBg.setColor(palette.tagBg);
        tagBg.setCornerRadius(dpToPx(holder.itemView.getContext(), 4));
        holder.tvCategory.setBackground(tagBg);
        holder.tvCategory.setTextColor(palette.primary);
    }

    private static float dpToPx(android.content.Context ctx, float dp) {
        return dp * ctx.getResources().getDisplayMetrics().density;
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvPreview, tvDate, tvCategory, tvTags;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tv_title);
            tvPreview  = itemView.findViewById(R.id.tv_preview);
            tvDate     = itemView.findViewById(R.id.tv_date);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvTags     = itemView.findViewById(R.id.tv_tags);
        }
    }
}
