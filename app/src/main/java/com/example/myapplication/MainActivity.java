package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity — 首页主列表
 * 功能：展示所有笔记、搜索、分类筛选、滑动删除、FAB 新建
 */
public class MainActivity extends AppCompatActivity {

    private NoteAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private TextInputEditText etSearch;

    private String currentKeyword = "";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置 Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recycler_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NoteAdapter(new NoteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Note note) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("note_id", note.id);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(Note note, int position) {
                showDeleteDialog(note, position);
            }
        });
        recyclerView.setAdapter(adapter);

        // 滑动删除
        setupSwipeToDelete(recyclerView);

        // SwipeRefresh
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(this::loadNotes);

        // 空状态
        tvEmpty = findViewById(R.id.tv_empty);

        // 搜索框
        etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s.toString().trim();
                loadNotes();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // FAB
        FloatingActionButton fab = findViewById(R.id.fab_new_note);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditActivity.class);
            startActivity(intent);
        });

        // 底部导航
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    /** 从 Room 加载笔记列表 */
    private void loadNotes() {
        swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Note> notes;
            if (currentKeyword.isEmpty()) {
                notes = db.noteDao().getAll();
            } else {
                notes = db.noteDao().searchByKeyword(currentKeyword);
            }
            final List<Note> result = notes != null ? notes : new ArrayList<>();
            runOnUiThread(() -> {
                adapter.setNotes(result);
                tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
                swipeRefresh.setRefreshing(false);
            });
        });
    }

    /** 设置左滑删除 */
    private void setupSwipeToDelete(RecyclerView recyclerView) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Note note = adapter.getNoteAt(pos);
                showDeleteDialogAfterSwipe(note, pos);
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void showDeleteDialog(Note note, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.msg_delete_confirm)
                .setPositiveButton(R.string.action_confirm, (d, w) -> deleteNote(note, position))
                .setNegativeButton(R.string.action_cancel, (d, w) -> {})
                .show();
    }

    private void showDeleteDialogAfterSwipe(Note note, int position) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.msg_delete_confirm)
                .setPositiveButton(R.string.action_confirm, (d, w) -> deleteNote(note, position))
                .setNegativeButton(R.string.action_cancel, (d, w) -> adapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    private void deleteNote(Note note, int position) {
        executor.execute(() -> {
            AppDatabase.getInstance(this).noteDao().delete(note);
            runOnUiThread(() -> {
                adapter.removeAt(position);
                if (adapter.getItemCount() == 0) tvEmpty.setVisibility(View.VISIBLE);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
