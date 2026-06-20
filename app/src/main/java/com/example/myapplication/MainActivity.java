package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.ThemeHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainActivity — 首页主列表
 * 功能：展示所有笔记、搜索、分类筛选、排序、快捷操作（分类/删除/置顶）、FAB 新建、侧边栏
 */
public class MainActivity extends AppCompatActivity {

    private NoteAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private TextInputEditText etSearch;
    private RadioGroup rgSort;

    // 侧边栏
    private DrawerLayout drawerLayout;
    private SidebarAdapter sidebarAdapter;

    private String currentKeyword = "";
    private int sortMode = 0; // 0=date, 1=title, 2=category
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ThemeHelper themeHelper;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "notelink_categories";
    private static final String KEY_USER_CATEGORIES = "user_categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeHelper = ThemeHelper.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        themeHelper.applyToActivity(this);

        // 检查是否有外部传入的搜索标签
        if (getIntent().hasExtra("search_tag")) {
            currentKeyword = getIntent().getStringExtra("search_tag");
        }

        // DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        // 设置 Toolbar
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(
                androidx.core.view.GravityCompat.START));

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
                showQuickActionsDialog(note, position);
            }
        });
        adapter.setPalette(themeHelper.getPalette());
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
        if (!currentKeyword.isEmpty()) {
            etSearch.setText(currentKeyword);
        }
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentKeyword = s.toString().trim();
                loadNotes();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 排序控件
        rgSort = findViewById(R.id.rg_sort);
        rgSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_sort_date) {
                sortMode = 0;
            } else if (checkedId == R.id.rb_sort_title) {
                sortMode = 1;
            } else if (checkedId == R.id.rb_sort_category) {
                sortMode = 2;
            }
            loadNotes();
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
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                return true;
            }
            return true;
        });

        // 侧边栏
        setupSidebar();
    }

    /** 初始化侧边栏 */
    private void setupSidebar() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        RecyclerView rvSidebar = findViewById(R.id.rv_sidebar_categories);
        rvSidebar.setLayoutManager(new LinearLayoutManager(this));

        sidebarAdapter = new SidebarAdapter();
        sidebarAdapter.setOnNoteClickListener(note -> {
            drawerLayout.closeDrawers();
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("note_id", note.id);
            startActivity(intent);
        });
        sidebarAdapter.setPalette(themeHelper.getPalette());
        rvSidebar.setAdapter(sidebarAdapter);

        MaterialButton btnAddCategory = findViewById(R.id.btn_add_category);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        loadSidebarData();
    }

    private void loadSidebarData() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Note> allNotes = db.noteDao().getAll();
            Map<String, SidebarAdapter.CategoryGroup> groupMap = new LinkedHashMap<>();

            Set<String> userCats = prefs.getStringSet(KEY_USER_CATEGORIES, new HashSet<>());
            for (String cat : userCats) {
                if (!groupMap.containsKey(cat)) {
                    groupMap.put(cat, new SidebarAdapter.CategoryGroup(cat));
                }
            }

            if (allNotes != null) {
                for (Note note : allNotes) {
                    String cat = (note.category != null && !note.category.isEmpty())
                            ? note.category : "未分类";
                    if (!groupMap.containsKey(cat)) {
                        groupMap.put(cat, new SidebarAdapter.CategoryGroup(cat));
                    }
                    groupMap.get(cat).notes.add(note);
                }
            }

            final List<SidebarAdapter.CategoryGroup> groups =
                    new ArrayList<>(groupMap.values());
            runOnUiThread(() -> sidebarAdapter.setGroups(groups));
        });
    }

    private void showAddCategoryDialog() {
        EditText etInput = new EditText(this);
        etInput.setHint("请输入分类名称");
        etInput.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("新建分类")
                .setView(etInput)
                .setPositiveButton("确定", (d, w) -> {
                    String name = etInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Set<String> userCats = new HashSet<>(
                                prefs.getStringSet(KEY_USER_CATEGORIES, new HashSet<>()));
                        userCats.add(name);
                        prefs.edit().putStringSet(KEY_USER_CATEGORIES, userCats).apply();
                        sidebarAdapter.addCategory(name);
                        Toast.makeText(this, "分类\"" + name + "\"已创建", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        themeHelper.applyToActivity(this);
        adapter.setPalette(themeHelper.getPalette());
        sidebarAdapter.setPalette(themeHelper.getPalette());
        ThemeHelper.Palette p = themeHelper.getPalette();
        MaterialButton btnAddCategory = findViewById(R.id.btn_add_category);
        btnAddCategory.setTextColor(0xFFFFFFFF);
        View sidebarHeader = findViewById(R.id.sidebar_header);
        if (sidebarHeader != null) {
            sidebarHeader.setBackgroundColor(p.primaryDark);
        }
        loadNotes();
        loadSidebarData();
    }

    /** 根据排序模式加载笔记列表 */
    private void loadNotes() {
        swipeRefresh.setRefreshing(true);
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Note> notes;

            if (!currentKeyword.isEmpty()) {
                // 搜索模式下按默认排序（置顶优先+时间倒序）
                notes = db.noteDao().searchByKeyword(currentKeyword);
            } else {
                switch (sortMode) {
                    case 1:
                        notes = db.noteDao().getAllOrderByTitle();
                        break;
                    case 2:
                        notes = db.noteDao().getAllOrderByCategory();
                        break;
                    default:
                        notes = db.noteDao().getAllOrderByDate();
                        break;
                }
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
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Note note = adapter.getNoteAt(pos);
                showDeleteDialogAfterSwipe(note, pos);
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // 禁用长按拖拽，让长按事件传递给 OnLongClickListener
                return false;
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }

    // ==================== 快捷操作对话框 ====================

    /** 长按笔记 → 弹出快捷操作菜单：修改分类 / 置顶 / 删除 */
    private void showQuickActionsDialog(Note note, int position) {
        String title = note.title != null ? note.title : "无标题";
        String[] actions = {
                note.pinned ? "📌 取消置顶" : "📌 置顶",
                "🏷 修改分类",
                "🗑 删除"
        };

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0: // 置顶/取消置顶
                            togglePin(note, position);
                            break;
                        case 1: // 修改分类
                            showChangeCategoryDialog(note, position);
                            break;
                        case 2: // 删除
                            showDeleteDialog(note, position);
                            break;
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 置顶 / 取消置顶 */
    private void togglePin(Note note, int position) {
        note.pinned = !note.pinned;
        executor.execute(() -> {
            AppDatabase.getInstance(this).noteDao().update(note);
            runOnUiThread(() -> {
                Toast.makeText(this,
                        note.pinned ? R.string.msg_pinned : R.string.msg_unpinned,
                        Toast.LENGTH_SHORT).show();
                loadNotes(); // 刷新排序
            });
        });
    }

    /** 修改分类对话框 — 列出所有分类让用户选择 */
    private void showChangeCategoryDialog(Note note, int position) {
        executor.execute(() -> {
            List<String> categories = AppDatabase.getInstance(this)
                    .noteDao().getAllCategories();
            if (categories == null) categories = new ArrayList<>();

            // 合并 SharedPreferences 中的分类
            Set<String> userCats = prefs.getStringSet(KEY_USER_CATEGORIES, new HashSet<>());
            for (String cat : userCats) {
                if (!categories.contains(cat)) categories.add(cat);
            }
            if (!categories.contains("未分类")) categories.add("未分类");

            final List<String> finalCats = categories;
            runOnUiThread(() -> {
                String[] catsArr = finalCats.toArray(new String[0]);
                new AlertDialog.Builder(this)
                        .setTitle("修改分类")
                        .setItems(catsArr, (dialog, which) -> {
                            String newCat = catsArr[which];
                            changeNoteCategory(note, newCat, position);
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        });
    }

    /** 修改笔记的分类 */
    private void changeNoteCategory(Note note, String newCategory, int position) {
        note.category = newCategory;
        executor.execute(() -> {
            AppDatabase.getInstance(this).noteDao().update(note);
            runOnUiThread(() -> {
                Toast.makeText(this, "分类已改为: " + newCategory, Toast.LENGTH_SHORT).show();
                loadNotes();
                loadSidebarData();
            });
        });
    }

    // ==================== 删除 ====================

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
                .setNegativeButton(R.string.action_cancel,
                        (d, w) -> adapter.notifyItemChanged(position))
                .setCancelable(false)
                .show();
    }

    private void deleteNote(Note note, int position) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.linkDao().deleteByFromNoteId(note.id);
            db.noteDao().delete(note);
            runOnUiThread(() -> {
                adapter.removeAt(position);
                if (adapter.getItemCount() == 0) tvEmpty.setVisibility(View.VISIBLE);
                loadSidebarData();
            });
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(
                androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
