package com.example.myapplication.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Link 实体类 — 记录笔记内链关系
 * fromNoteId → 指向 toNoteTitle 对应的笔记
 */
@Entity(
    tableName = "link",
    foreignKeys = @ForeignKey(
        entity = Note.class,
        parentColumns = "id",
        childColumns = "from_note_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class Link {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "from_note_id", index = true)
    public int fromNoteId;

    /** 目标笔记的标题（用于跳转查询） */
    @ColumnInfo(name = "to_note_title")
    public String toNoteTitle;

    public Link() {}

    @Ignore
    public Link(int fromNoteId, String toNoteTitle) {
        this.fromNoteId = fromNoteId;
        this.toNoteTitle = toNoteTitle;
    }
}
