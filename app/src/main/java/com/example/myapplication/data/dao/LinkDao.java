package com.example.myapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.myapplication.data.entity.Link;

import java.util.List;

/**
 * LinkDao — 内链关系数据访问接口
 */
@Dao
public interface LinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Link link);

    @Delete
    void delete(Link link);

    /** 获取某条笔记的所有出链 */
    @Query("SELECT * FROM link WHERE from_note_id = :fromNoteId")
    List<Link> getLinksFrom(int fromNoteId);

    /** 删除某条笔记的所有出链（笔记更新前调用） */
    @Query("DELETE FROM link WHERE from_note_id = :fromNoteId")
    void deleteByFromNoteId(int fromNoteId);
}
