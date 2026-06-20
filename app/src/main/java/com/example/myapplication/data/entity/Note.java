package com.example.myapplication.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Note 实体类 — 对应 Room 数据库中的 note 表
 * 存储笔记/日记的全部字段
 */
@Entity(tableName = "note")
public class Note {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "title")
    public String title;

    /** 正文，可包含 [[内链]] 语法 */
    @ColumnInfo(name = "content")
    public String content;

    /** 日期，格式 yyyy-MM-dd */
    @ColumnInfo(name = "date")
    public String date;

    /** 分类，如：日记、学习、工作等 */
    @ColumnInfo(name = "category")
    public String category;

    /** 标签，逗号分隔，如：tag1,tag2 */
    @ColumnInfo(name = "tags")
    public String tags;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    /** 是否置顶 */
    @ColumnInfo(name = "pinned")
    public boolean pinned;

    /** 图片路径列表，逗号分隔，如 /storage/emulated/0/Pictures/img1.jpg,/storage/.../img2.png */
    @ColumnInfo(name = "image_paths")
    public String imagePaths;

    public Note() {}

    @Ignore
    public Note(String title, String content, String date, String category, String tags) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.category = (category != null && !category.isEmpty()) ? category : "日记";
        this.tags = tags != null ? tags : "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.pinned = false;
        this.imagePaths = "";
    }
}
