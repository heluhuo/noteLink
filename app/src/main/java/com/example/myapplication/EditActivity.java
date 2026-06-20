package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.data.dao.LinkDao;
import com.example.myapplication.data.database.AppDatabase;
import com.example.myapplication.data.entity.Link;
import com.example.myapplication.data.entity.Note;
import com.example.myapplication.util.LinkParser;
import com.example.myapplication.util.ThemeHelper;
import com.example.myapplication.util.ThemedArrayAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
 * EditActivity — 新建 / 编辑笔记页
 * 功能：富文本输入、内链实时检测浮窗、日期选择、图片插入、侧边栏导航、保存
 */
public class EditActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 1001;

    private TextInputEditText etTitle, etDate, etTags;
    private Spinner spinnerCategory;
    private EditText etContent;
    private View tvLinkHint;
    private TextView tvCharCount;
    private int editNoteId = -1;
    private final java.util.List<String> categoryList = new ArrayList<>();

    // 图片相关
    private final List<String> selectedImagePaths = new ArrayList<>();
    private HorizontalScrollView scrollImagesPreview;
    private LinearLayout layoutImagesPreview;

    // 内链浮窗
    private PopupWindow linkPopup;
    private RecyclerView recyclerSuggestions;
    private LinkSuggestionAdapter suggestionAdapter;
    private final List<Note> suggestionNotes = new ArrayList<>();
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private boolean isPopupShowing = false;

    // 侧边栏
    private DrawerLayout drawerLayout;
    private SidebarAdapter sidebarAdapter;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "notelink_categories";
    private static final String KEY_USER_CATEGORIES = "user_categories";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ThemeHelper themeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeHelper = ThemeHelper.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        themeHelper.applyToActivity(this);

        // DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(
                androidx.core.view.GravityCompat.START));

        // 视图引用
        etTitle    = findViewById(R.id.et_title);
        etDate     = findViewById(R.id.et_date);
        spinnerCategory = findViewById(R.id.spinner_category);
        etTags     = findViewById(R.id.et_tags);
        etContent  = findViewById(R.id.et_content);
        tvLinkHint = findViewById(R.id.tv_link_hint);
        tvCharCount = findViewById(R.id.tv_char_count);
        scrollImagesPreview = findViewById(R.id.scroll_images_preview);
        layoutImagesPreview = findViewById(R.id.layout_images_preview);

        // 初始化字数统计
        updateCharCount();

        // 初始化分类 Spinner
        setupCategorySpinner();

        // 日期选择器
        etDate.setFocusable(false);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etDate.setText(today);
        etDate.setOnClickListener(v -> showDatePicker());

        // 内链浮窗：监听内容输入
        etContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasLink = s.toString().contains("[[");
                tvLinkHint.setVisibility(hasLink ? View.VISIBLE : View.GONE);
                updateCharCount();
                handleWordMatching(s.toString(), etContent.getSelectionStart());
            }
        });

        etContent.setOnClickListener(v -> {
            handleWordMatching(
                    etContent.getText() != null ? etContent.getText().toString() : "",
                    etContent.getSelectionStart());
        });

        initLinkPopup();

        // 保存按钮
        MaterialButton btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveNote());

        // 回到主页按钮
        MaterialButton btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        // 插入图片按钮
        MaterialButton btnInsertImage = findViewById(R.id.btn_insert_image);
        btnInsertImage.setOnClickListener(v -> pickImage());

        // 检查是否为编辑模式
        editNoteId = getIntent().getIntExtra("note_id", -1);
        if (editNoteId != -1) {
            toolbar.setTitle(R.string.title_edit);
            loadNoteForEdit(editNoteId);
        } else {
            toolbar.setTitle(R.string.title_new_note);
            if (getIntent().hasExtra("preset_date")) {
                etDate.setText(getIntent().getStringExtra("preset_date"));
            }
        }

        // 初始化侧边栏
        setupSidebar();
    }

    // ==================== 图片功能 ====================

    /** 从相册选取图片 */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        try {
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.msg_no_gallery, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                saveImageToInternal(imageUri);
            }
        }
    }

    /** 将选取的图片复制到应用内部存储 */
    private void saveImageToInternal(Uri imageUri) {
        executor.execute(() -> {
            try {
                // 读取图片数据
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) return;

                // 创建目标文件
                File dir = new File(getFilesDir(), "images");
                if (!dir.exists()) dir.mkdirs();
                String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                File destFile = new File(dir, fileName);

                // 复制文件
                FileOutputStream outputStream = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.close();
                inputStream.close();

                final String path = destFile.getAbsolutePath();
                runOnUiThread(() -> addImageToPreview(path));
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "图片保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    /** 添加图片到预览区 */
    private void addImageToPreview(String path) {
        selectedImagePaths.add(path);
        refreshImagePreview();
    }

    /** 刷新图片预览横条 */
    private void refreshImagePreview() {
        layoutImagesPreview.removeAllViews();
        if (selectedImagePaths.isEmpty()) {
            scrollImagesPreview.setVisibility(View.GONE);
            return;
        }
        scrollImagesPreview.setVisibility(View.VISIBLE);

        for (int i = 0; i < selectedImagePaths.size(); i++) {
            final String path = selectedImagePaths.get(i);
            final int index = i;

            // 缩略图容器
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 0, dpToPx(8), 0);

            ImageView imageView = new ImageView(this);
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                // 缩放到 120dp 宽
                int thumbW = dpToPx(120);
                int thumbH = (int) (thumbW * ((float) bitmap.getHeight() / bitmap.getWidth()));
                Bitmap thumb = Bitmap.createScaledBitmap(bitmap, thumbW, thumbH, true);
                imageView.setImageBitmap(thumb);
            }
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                    dpToPx(120), dpToPx(90));
            imgParams.bottomMargin = dpToPx(2);
            itemLayout.addView(imageView, imgParams);

            // 删除按钮
            TextView tvRemove = new TextView(this);
            tvRemove.setText("× 移除");
            tvRemove.setTextSize(10);
            tvRemove.setTextColor(0xFFFF4444);
            tvRemove.setGravity(Gravity.CENTER);
            tvRemove.setOnClickListener(v -> {
                selectedImagePaths.remove(index);
                refreshImagePreview();
            });
            itemLayout.addView(tvRemove);

            layoutImagesPreview.addView(itemLayout);
        }
    }

    // ==================== 侧边栏 ====================

    private void setupSidebar() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        RecyclerView rvSidebar = findViewById(R.id.rv_sidebar_categories);
        rvSidebar.setLayoutManager(new LinearLayoutManager(this));

        sidebarAdapter = new SidebarAdapter();
        sidebarAdapter.setOnNoteClickListener(note -> {
            drawerLayout.closeDrawers();
            // 在编辑页点击侧边栏笔记 → 打开详情页
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
                    new java.util.ArrayList<>(groupMap.values());
            runOnUiThread(() -> sidebarAdapter.setGroups(groups));
        });
    }

    private void showAddCategoryDialog() {
        EditText etInput = new EditText(this);
        etInput.setHint("请输入分类名称");
        etInput.setPadding(48, 24, 48, 24);

        new androidx.appcompat.app.AlertDialog.Builder(this)
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

    // ==================== 内链浮窗 ====================

    private void initLinkPopup() {
        View popupView = LayoutInflater.from(this)
                .inflate(R.layout.popup_link_suggestions, null);
        recyclerSuggestions = popupView.findViewById(R.id.recycler_link_suggestions);
        recyclerSuggestions.setLayoutManager(new LinearLayoutManager(this));

        suggestionAdapter = new LinkSuggestionAdapter(suggestionNotes, note -> {
            insertLinkAtCursor(note.title);
            dismissLinkPopup();
        });
        recyclerSuggestions.setAdapter(suggestionAdapter);

        linkPopup = new PopupWindow(popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        linkPopup.setOutsideTouchable(true);
        linkPopup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        linkPopup.setOnDismissListener(() -> isPopupShowing = false);
    }

    private void handleWordMatching(String text, int cursorPos) {
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }

        debounceRunnable = () -> {
            String currentWord = extractCurrentWord(text, cursorPos);
            if (currentWord == null || currentWord.length() < 2) {
                dismissLinkPopup();
                return;
            }

            executor.execute(() -> {
                List<Note> matched = AppDatabase.getInstance(this)
                        .noteDao().searchByKeyword(currentWord);
                List<Note> filtered = new ArrayList<>();
                if (matched != null) {
                    for (Note n : matched) {
                        if (editNoteId == -1 || n.id != editNoteId) {
                            filtered.add(n);
                        }
                    }
                    if (filtered.size() > 5) {
                        filtered = filtered.subList(0, 5);
                    }
                }

                final List<Note> finalList = filtered;
                runOnUiThread(() -> {
                    if (finalList.isEmpty()) {
                        dismissLinkPopup();
                    } else {
                        showLinkPopup(finalList);
                    }
                });
            });
        };

        debounceHandler.postDelayed(debounceRunnable, 300);
    }

    private String extractCurrentWord(String text, int cursorPos) {
        if (text == null || text.isEmpty()) return null;
        if (cursorPos < 0) cursorPos = 0;
        if (cursorPos > text.length()) cursorPos = text.length();

        int start = cursorPos;
        while (start > 0 && !isWordBoundary(text.charAt(start - 1))) {
            start--;
        }

        int end = cursorPos;
        while (end < text.length() && !isWordBoundary(text.charAt(end))) {
            end++;
        }

        if (start == end) return null;
        return text.substring(start, end).trim();
    }

    private boolean isWordBoundary(char c) {
        return Character.isWhitespace(c) || c == ',' || c == '.' || c == '!' || c == '?'
                || c == ';' || c == ':' || c == '(' || c == ')' || c == '[' || c == ']'
                || c == '{' || c == '}' || c == '"' || c == '\'' || c == '、' || c == '。'
                || c == '，' || c == '！' || c == '？' || c == '；' || c == '：'
                || c == '（' || c == '）' || c == '【' || c == '】' || c == '\n' || c == '\r';
    }

    private void showLinkPopup(List<Note> notes) {
        suggestionNotes.clear();
        suggestionNotes.addAll(notes);
        suggestionAdapter.notifyDataSetChanged();

        if (linkPopup.isShowing()) {
            return;
        }

        int[] location = new int[2];
        etContent.getLocationOnScreen(location);
        int popupY = location[1] - 220;

        int popupWidth = etContent.getWidth();
        if (popupWidth <= 0) popupWidth = ViewGroup.LayoutParams.MATCH_PARENT;

        linkPopup.setWidth(popupWidth);
        linkPopup.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

        View rootView = findViewById(android.R.id.content);
        linkPopup.showAtLocation(rootView, Gravity.TOP | Gravity.START,
                location[0] + etContent.getPaddingLeft(),
                Math.max(popupY, 0));

        isPopupShowing = true;
    }

    private void dismissLinkPopup() {
        if (linkPopup != null && linkPopup.isShowing()) {
            linkPopup.dismiss();
        }
        isPopupShowing = false;
    }

    private void insertLinkAtCursor(String title) {
        int start = etContent.getSelectionStart();
        String text = etContent.getText() != null ? etContent.getText().toString() : "";

        String currentWord = extractCurrentWord(text, start);
        if (currentWord != null && !currentWord.isEmpty()) {
            int wordStart = start;
            while (wordStart > 0 && !isWordBoundary(text.charAt(wordStart - 1))) {
                wordStart--;
            }
            int wordEnd = start;
            while (wordEnd < text.length() && !isWordBoundary(text.charAt(wordEnd))) {
                wordEnd++;
            }
            String before = text.substring(0, wordStart);
            String after = text.substring(wordEnd);
            String newText = before + "[[" + title + "]]" + after;
            etContent.setText(newText);
            etContent.setSelection(wordStart + title.length() + 4);
        } else {
            String before = text.substring(0, Math.max(start, 0));
            String after = text.substring(Math.max(start, 0));
            String newText = before + "[[" + title + "]]" + after;
            etContent.setText(newText);
            etContent.setSelection(Math.max(start, 0) + title.length() + 4);
        }
    }

    // ==================== 分类 Spinner ====================

    private void setupCategorySpinner() {
        executor.execute(() -> {
            java.util.List<String> cats = AppDatabase.getInstance(this).noteDao().getAllCategories();
            final java.util.List<String> list = new ArrayList<>();
            if (cats != null && !cats.isEmpty()) {
                list.addAll(cats);
            }

            SharedPreferences sp = getSharedPreferences("notelink_categories", MODE_PRIVATE);
            java.util.Set<String> userCats = sp.getStringSet("user_categories", new java.util.HashSet<>());
            for (String cat : userCats) {
                if (!list.contains(cat)) {
                    list.add(cat);
                }
            }

            if (!list.contains("日记")) list.add(0, "日记");

            runOnUiThread(() -> {
                categoryList.clear();
                categoryList.addAll(list);
                ThemeHelper.Palette p = themeHelper.getPalette();
                ThemedArrayAdapter<String> adapter = new ThemedArrayAdapter<>(
                        EditActivity.this, p, categoryList);
                spinnerCategory.setAdapter(adapter);
                spinnerCategory.setPopupBackgroundDrawable(
                        ThemeHelper.createSpinnerPopupBg(p));
            });
        });
    }

    private String getSelectedCategory() {
        if (spinnerCategory.getSelectedItem() != null) {
            return spinnerCategory.getSelectedItem().toString();
        }
        return "日记";
    }

    private void selectCategory(String category) {
        if (category == null || category.isEmpty()) return;
        for (int i = 0; i < categoryList.size(); i++) {
            if (categoryList.get(i).equals(category)) {
                spinnerCategory.setSelection(i);
                return;
            }
        }
        categoryList.add(category);
        ThemeHelper.Palette p = themeHelper.getPalette();
        ThemedArrayAdapter<String> adapter = new ThemedArrayAdapter<>(
                this, p, categoryList);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setPopupBackgroundDrawable(
                ThemeHelper.createSpinnerPopupBg(p));
        spinnerCategory.setSelection(categoryList.size() - 1);
    }

    // ==================== 其他功能 ====================

    private void showDatePicker() {
        String current = etDate.getText() != null ? etDate.getText().toString() : "";
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            cal.setTime(sdf.parse(current));
        } catch (Exception ignored) {}
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            etDate.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadNoteForEdit(int noteId) {
        executor.execute(() -> {
            Note note = AppDatabase.getInstance(this).noteDao().findById(noteId);
            if (note != null) {
                runOnUiThread(() -> {
                    etTitle.setText(note.title);
                    etDate.setText(note.date);
                    etTags.setText(note.tags);
                    etContent.setText(note.content);
                    spinnerCategory.postDelayed(() -> selectCategory(note.category), 300);

                    // 加载已有图片
                    if (note.imagePaths != null && !note.imagePaths.isEmpty()) {
                        String[] paths = note.imagePaths.split(",");
                        for (String p : paths) {
                            String trimmed = p.trim();
                            if (!trimmed.isEmpty() && new File(trimmed).exists()) {
                                selectedImagePaths.add(trimmed);
                            }
                        }
                        refreshImagePreview();
                    }
                });
            }
        });
    }

    private void updateCharCount() {
        if (tvCharCount == null || etContent == null) return;
        String text = etContent.getText() != null ? etContent.getText().toString() : "";
        int count = text.length();
        tvCharCount.setText("字数: " + count);
    }

    private void saveNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, R.string.msg_title_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        String content  = etContent.getText() != null ? etContent.getText().toString() : "";
        String date     = etDate.getText() != null ? etDate.getText().toString() : "";
        String category = getSelectedCategory();
        String tags     = etTags.getText() != null ? etTags.getText().toString().trim() : "";
        if (category.isEmpty()) category = "日记";
        if (date.isEmpty()) {
            date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        // 图片路径
        final String imagePathsStr = String.join(",", selectedImagePaths);

        final String finalDate     = date;
        final String finalCategory = category;

        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            com.example.myapplication.data.dao.NoteDao noteDao = db.noteDao();
            LinkDao linkDao = db.linkDao();

            Note note;
            if (editNoteId == -1) {
                note = new Note(title, content, finalDate, finalCategory, tags);
                note.imagePaths = imagePathsStr;
                int newId = (int) db.noteDao().insert(note);
                note.id = newId;
            } else {
                note = db.noteDao().findById(editNoteId);
                if (note == null) return;
                note.title    = title;
                note.content  = content;
                note.date     = finalDate;
                note.category = finalCategory;
                note.tags     = tags;
                note.imagePaths = imagePathsStr;
                note.updatedAt = System.currentTimeMillis();
                db.noteDao().update(note);
                linkDao.deleteByFromNoteId(editNoteId);
            }

            List<String> linkTitles = LinkParser.extractLinkTitles(content);
            for (String lt : linkTitles) {
                linkDao.insert(new Link(note.id, lt));
            }

            runOnUiThread(() -> {
                Toast.makeText(this, R.string.msg_save_success, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    public void onBackPressed() {
        dismissLinkPopup();
        if (drawerLayout != null && drawerLayout.isDrawerOpen(
                androidx.core.view.GravityCompat.START)) {
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeHelper.applyToActivity(this);
        if (sidebarAdapter != null) {
            sidebarAdapter.setPalette(themeHelper.getPalette());
        }
        ThemeHelper.Palette p = themeHelper.getPalette();
        MaterialButton btnAddCat = findViewById(R.id.btn_add_category);
        if (btnAddCat != null) btnAddCat.setTextColor(0xFFFFFFFF);
        View sidebarHeader = findViewById(R.id.sidebar_header);
        if (sidebarHeader != null) sidebarHeader.setBackgroundColor(p.primaryDark);

        if (spinnerCategory != null) {
            spinnerCategory.setPopupBackgroundDrawable(ThemeHelper.createSpinnerPopupBg(p));
        }
        loadSidebarData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLinkPopup();
        debounceHandler.removeCallbacksAndMessages(null);
        executor.shutdown();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ==================== 内链建议适配器 ====================

    private static class LinkSuggestionAdapter
            extends RecyclerView.Adapter<LinkSuggestionAdapter.VH> {

        private final List<Note> notes;
        private final OnNoteClickListener listener;

        interface OnNoteClickListener {
            void onClick(Note note);
        }

        LinkSuggestionAdapter(List<Note> notes, OnNoteClickListener listener) {
            this.notes = notes;
            this.listener = listener;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_link_suggestion, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Note note = notes.get(position);
            holder.tvTitle.setText(note.title != null ? note.title : "");
            String location = "";
            if (note.date != null && !note.date.isEmpty()) {
                location = note.date;
            }
            if (note.category != null && !note.category.isEmpty()) {
                if (!location.isEmpty()) location += " · ";
                location += note.category;
            }
            holder.tvLocation.setText(location);
            holder.itemView.setOnClickListener(v -> listener.onClick(note));
        }

        @Override
        public int getItemCount() {
            return notes.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation;
            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tv_suggestion_title);
                tvLocation = itemView.findViewById(R.id.tv_suggestion_location);
            }
        }
    }
}
