package com.example.myapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.data.entity.Note;

import java.util.List;

/**
 * NoteDao — 笔记数据访问接口
 */
@Dao
public interface NoteDao {

    // -------- 基本增删改 --------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("DELETE FROM note WHERE id = :id")
    void deleteById(int id);

    // -------- 查询 --------

    /** 获取所有笔记（按修改时间倒序） */
    @Query("SELECT * FROM note ORDER BY updated_at DESC")
    List<Note> getAll();

    /** 按日期查询 */
    @Query("SELECT * FROM note WHERE date = :date ORDER BY updated_at DESC")
    List<Note> queryByDate(String date);

    /** 关键词全文搜索（标题 + 内容） */
    @Query("SELECT * FROM note WHERE title LIKE '%' || :keyword || '%' OR content LIKE '%' || :keyword || '%' ORDER BY updated_at DESC")
    List<Note> searchByKeyword(String keyword);

    /** 按分类查询 */
    @Query("SELECT * FROM note WHERE category = :category ORDER BY updated_at DESC")
    List<Note> queryByCategory(String category);

    /** 按标题精确查询（内链跳转用） */
    @Query("SELECT * FROM note WHERE title = :title LIMIT 1")
    Note findByTitle(String title);

    /** 按 id 查询 */
    @Query("SELECT * FROM note WHERE id = :id LIMIT 1")
    Note findById(int id);

    /** 获取有笔记的所有日期（日历角标用） */
    @Query("SELECT DISTINCT date FROM note ORDER BY date DESC")
    List<String> getAllDates();

    /** 获取所有分类（去重） */
    @Query("SELECT DISTINCT category FROM note WHERE category IS NOT NULL AND category != '' ORDER BY category ASC")
    List<String> getAllCategories();
}
