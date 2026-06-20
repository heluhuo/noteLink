package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.LinkParser;
import com.example.myapplication.util.ThemeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DetailActivity — 笔记详情/阅读页
 * 功能：渲染笔记内容，内链高亮可点击，支持跳转编辑，侧边栏，图片展示
 */
public class DetailActivity extends AppCompatActivity {

    private int noteId = -1;
    private Note currentNote;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ThemeHelper themeHelper;

    // 侧边栏
    private DrawerLayout drawerLayout;
    private SidebarAdapter sidebarAdapter;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "notelink_categories";
    private static final String KEY_USER_CATEGORIES = "user_categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeHelper = ThemeHelper.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        themeHelper.applyToActivity(this);

        // DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // 汉堡菜单打开侧边栏
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(
                androidx.core.view.GravityCompat.START));

        // 编辑按钮
        MaterialButton btnEdit = findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(v -> {
            if (currentNote != null) {
                Intent intent = new Intent(this, EditActivity.class);
                intent.putExtra("note_id", currentNote.id);
                startActivity(intent);
            }
        });

        // 删除按钮
        MaterialButton btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> {
            if (currentNote != null) {
                showDeleteDialog();
            }
        });

        // 回到主页按钮
        MaterialButton btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        noteId = getIntent().getIntExtra("note_id", -1);

        // 初始化侧边栏
        setupSidebar();
        loadNote();
    }

    /** 初始化侧边栏 */
    private void setupSidebar() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        RecyclerView rvSidebar = findViewById(R.id.rv_sidebar_categories);
        rvSidebar.setLayoutManager(new LinearLayoutManager(this));

        sidebarAdapter = new SidebarAdapter();
        sidebarAdapter.setOnNoteClickListener(note -> {
            drawerLayout.closeDrawers();
            // 跳转到选中的笔记
            noteId = note.id;
            currentNote = null;
            loadNote();
        });
        sidebarAdapter.setPalette(themeHelper.getPalette());
        rvSidebar.setAdapter(sidebarAdapter);

        // 新建分类按钮
        MaterialButton btnAddCategory = findViewById(R.id.btn_add_category);
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        loadSidebarData();
    }

    /** 加载侧边栏分组数据 */
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
                    new java.util.ArrayList<>(groupMap.values());
            runOnUiThread(() -> sidebarAdapter.setGroups(groups));
        });
    }

    /** 弹出新建分类对话框 */
    private void showAddCategoryDialog() {
        android.widget.EditText etInput = new android.widget.EditText(this);
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
        themeHelper.applyToActivity(this);
        if (sidebarAdapter != null) {
            sidebarAdapter.setPalette(themeHelper.getPalette());
        }

        // 侧边栏头部
        ThemeHelper.Palette p = themeHelper.getPalette();
        MaterialButton btnAddCat = findViewById(R.id.btn_add_category);
        if (btnAddCat != null) btnAddCat.setTextColor(0xFFFFFFFF);
        View sidebarHeader = findViewById(R.id.sidebar_header);
        if (sidebarHeader != null) sidebarHeader.setBackgroundColor(p.primaryDark);

        loadNote();
        loadSidebarData();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(
                androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    private void loadNote() {
        if (noteId == -1) { finish(); return; }
        executor.execute(() -> {
            Note note = AppDatabase.getInstance(this).noteDao().findById(noteId);
            if (note == null) { finish(); return; }
            currentNote = note;
            runOnUiThread(() -> bindNote(note));
        });
    }

    private void bindNote(Note note) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(note.title != null ? note.title : "");

        TextView tvTitle    = findViewById(R.id.tv_detail_title);
        TextView tvDate     = findViewById(R.id.tv_detail_date);
        TextView tvCat      = findViewById(R.id.tv_detail_category);
        TextView tvTags     = findViewById(R.id.tv_detail_tags);
        TextView tvContent  = findViewById(R.id.tv_detail_content);
        TextView tvModified = findViewById(R.id.tv_detail_modified);
        LinearLayout layoutImages = findViewById(R.id.layout_images);

        tvTitle.setText(note.title);
        tvDate.setText(note.date != null ? note.date : "");
        tvCat.setText(note.category != null ? note.category : "");

        // 标签可点击
        if (note.tags != null && !note.tags.isEmpty()) {
            tvTags.setText("\uD83C\uDFF7 " + note.tags);
            tvTags.setTextColor(themeHelper.getPalette().link);
            tvTags.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("search_tag", note.tags);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        } else {
            tvTags.setText("");
            tvTags.setOnClickListener(null);
        }

        // 修改时间
        if (note.updatedAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            tvModified.setText("修改于 " + sdf.format(new Date(note.updatedAt)));
            tvModified.setVisibility(TextView.VISIBLE);
        } else {
            tvModified.setVisibility(TextView.GONE);
        }

        // 显示图片
        showImages(layoutImages, note.imagePaths);

        // 内链解析
        int linkColor = themeHelper.getPalette().link;
        tvContent.setText(LinkParser.parse(this, note.content, linkColor));
        tvContent.setMovementMethod(LinkMovementMethod.getInstance());
    }

    /** 在 layoutImages 中显示图片 */
    private void showImages(LinearLayout container, String imagePaths) {
        container.removeAllViews();
        if (imagePaths == null || imagePaths.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        String[] paths = imagePaths.split(",");
        boolean hasAny = false;
        for (String path : paths) {
            String trimmed = path.trim();
            if (trimmed.isEmpty()) continue;
            File imgFile = new File(trimmed);
            if (!imgFile.exists()) continue;

            hasAny = true;
            Bitmap bitmap = BitmapFactory.decodeFile(trimmed);
            if (bitmap == null) continue;

            ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setMaxHeight(dpToPx(400));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dpToPx(8);
            container.addView(imageView, params);

            // 点击查看大图
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(imgFile), "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try { startActivity(intent); } catch (Exception ignored) {}
            });
        }

        container.setVisibility(hasAny ? View.VISIBLE : View.GONE);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.msg_delete_confirm)
                .setPositiveButton(R.string.action_confirm, (d, w) -> deleteNote())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteNote() {
        if (currentNote == null) return;
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.linkDao().deleteByFromNoteId(currentNote.id);
            db.noteDao().delete(currentNote);
            runOnUiThread(() -> {
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
