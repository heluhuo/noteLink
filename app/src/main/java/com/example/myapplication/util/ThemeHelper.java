package com.example.myapplication.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.MonthCalendarView;
import com.example.myapplication.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 主题 / 风格切换管理器
 * 支持多种预设配色方案，可应用到 Activity 的所有视图
 */
public class ThemeHelper {

    private static final String PREFS_NAME = "notelink_theme";
    private static final String KEY_THEME = "theme_id";
    private static final String KEY_NIGHT = "night_mode";  // 日夜模式标志

    // 预设主题
    public static final String[] THEME_NAMES = {
            "森林绿 (默认)", "海洋蓝", "落日橙", "星空紫"
    };

    public static final String[] THEME_IDS = {
            "green", "blue", "orange", "purple"
    };

    // ---- 主题色板 ----
    public static class Palette {
        public final int primary;
        public final int primaryDark;
        public final int accent;
        public final int bg;
        public final int surface;
        public final int card;
        public final int link;
        public final int tagBg;
        public final int textPrimary;
        public final int textSecondary;
        public final int textHint;

        Palette(int p, int pd, int a, int b, int s, int c, int l, int t,
                int tp, int ts, int th) {
            primary = p; primaryDark = pd; accent = a;
            bg = b; surface = s; card = c; link = l; tagBg = t;
            textPrimary = tp; textSecondary = ts; textHint = th;
        }
    }

    // 文字颜色 - 根据日夜模式动态选择
    private static final int TXT_PRIMARY_LIGHT   = 0xDE000000; // 87% 黑
    private static final int TXT_SECONDARY_LIGHT = 0x99000000; // 60% 黑
    private static final int TXT_HINT_LIGHT      = 0x61000000; // 38% 黑

    private static final int TXT_PRIMARY_DARK   = 0xDEFFFFFF; // 87% 白
    private static final int TXT_SECONDARY_DARK = 0x99FFFFFF; // 60% 白
    private static final int TXT_HINT_DARK      = 0x61FFFFFF; // 38% 白

    // 浅色基础调色板（日模式）
    private static final Palette LIGHT_BASE = new Palette(
        Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"),
        Color.parseColor("#81C784"), Color.parseColor("#F5F5F5"),
        Color.parseColor("#FFFFFFFF"), Color.parseColor("#FFFFFFFF"),
        Color.parseColor("#64B5F6"), Color.parseColor("#1A4CAF50"),
        TXT_PRIMARY_LIGHT, TXT_SECONDARY_LIGHT, TXT_HINT_LIGHT
    );

    // 深色基础调色板（夜模式）
    private static final Palette DARK_BASE = new Palette(
        Color.parseColor("#4CAF50"), Color.parseColor("#388E3C"),
        Color.parseColor("#81C784"), Color.parseColor("#121212"),
        Color.parseColor("#1E1E1E"), Color.parseColor("#252525"),
        Color.parseColor("#64B5F6"), Color.parseColor("#1A4CAF50"),
        TXT_PRIMARY_DARK, TXT_SECONDARY_DARK, TXT_HINT_DARK
    );

    // 风格主色调（primary, primaryDark, accent, link, tagBg）
    private static final int[] STYLE_PRIMARY    = { 0xFF4CAF50, 0xFF2196F3, 0xFFFF7043, 0xFF7C4DFF };
    private static final int[] STYLE_PRIMARY_DARK = { 0xFF388E3C, 0xFF1976D2, 0xFFE64A19, 0xFF651FFF };
    private static final int[] STYLE_ACCENT      = { 0xFF81C784, 0xFF64B5F6, 0xFFFFAB91, 0xFFB388FF };
    private static final int[] STYLE_LINK         = { 0xFF64B5F6, 0xFF81D4FA, 0xFFFFCC80, 0xFFB39DDB };
    private static final int[] STYLE_TAG_BG      = {
        0x1A4CAF50, 0x1A2196F3, 0x1AFF7043, 0x1A7C4DFF
    };

    // View tag key: stores the background color role ("bg","surface","card", etc.)
    // 必须使用 R.id 资源 ID，View.setTag(int key, Object tag) 不接受动态生成的 ID
    private static final int TAG_BG_ROLE = R.id.bg_role;

    private static ThemeHelper instance;
    private final SharedPreferences prefs;

    private ThemeHelper(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeHelper(context.getApplicationContext());
        }
        return instance;
    }

    /** 获取当前主题索引 */
    public int getCurrentThemeIndex() {
        String id = prefs.getString(KEY_THEME, "green");
        for (int i = 0; i < THEME_IDS.length; i++) {
            if (THEME_IDS[i].equals(id)) return i;
        }
        return 0;
    }

    /** 设置主题 */
    public void setTheme(int index) {
        if (index >= 0 && index < THEME_IDS.length) {
            prefs.edit().putString(KEY_THEME, THEME_IDS[index]).apply();
        }
    }

    /** 获取日夜模式 */
    public boolean isNightMode() {
        return prefs.getBoolean(KEY_NIGHT, true); // 默认夜间模式
    }

    /** 设置日夜模式 */
    public void setNightMode(boolean night) {
        prefs.edit().putBoolean(KEY_NIGHT, night).apply();
    }

    /**
     * 获取当前 Palette
     * 根据当前风格主题 + 日/夜模式合并生成
     */
    public Palette getPalette() {
        int idx = getCurrentThemeIndex();
        boolean night = isNightMode();
        return buildPalette(idx, night);
    }

    /** 构建合并后的 Palette（风格色 + 日夜底色） */
    private Palette buildPalette(int styleIdx, boolean night) {
        if (styleIdx < 0 || styleIdx >= STYLE_PRIMARY.length) styleIdx = 0;

        int primary    = STYLE_PRIMARY[styleIdx];
        int primaryDark = STYLE_PRIMARY_DARK[styleIdx];
        int accent     = STYLE_ACCENT[styleIdx];
        int link       = STYLE_LINK[styleIdx];
        int tagBg      = STYLE_TAG_BG[styleIdx];

        if (night) {
            // 夜间模式：使用深色背景/表面，但保持风格主色调
            return new Palette(primary, primaryDark, accent,
                    DARK_BASE.bg, DARK_BASE.surface, DARK_BASE.card, link, tagBg,
                    DARK_BASE.textPrimary, DARK_BASE.textSecondary, DARK_BASE.textHint);
        } else {
            // 日间模式：使用浅色背景/表面，保持风格主色调，链接色改为深色系
            int lightLink = adjustLinkForLight(link);
            return new Palette(primary, primaryDark, accent,
                    LIGHT_BASE.bg, LIGHT_BASE.surface, LIGHT_BASE.card, lightLink, tagBg,
                    LIGHT_BASE.textPrimary, LIGHT_BASE.textSecondary, LIGHT_BASE.textHint);
        }
    }

    /** 日间模式下，将链接色调整为可读性更好的深色版本 */
    private int adjustLinkForLight(int darkLink) {
        // 在亮色背景上，链接色需要更深
        float[] hsv = new float[3];
        Color.colorToHSV(darkLink, hsv);
        hsv[1] = Math.min(1f, hsv[1] + 0.2f); // 增加饱和度
        hsv[2] = Math.max(0.3f, hsv[2] - 0.3f); // 降低亮度
        return Color.HSVToColor(hsv);
    }

    /** 获取指定索引的 Palette（兼容旧代码） */
    public static Palette getPaletteAt(int index) {
        ThemeHelper helper = instance;
        if (helper == null) {
            // 无实例时返回默认深色
            return new Palette(
                0xFF4CAF50, 0xFF388E3C, 0xFF81C784,
                0xFF121212, 0xFF1E1E1E, 0xFF252525,
                0xFF64B5F6, 0x1A4CAF50,
                0xDEFFFFFF, 0x99FFFFFF, 0x61FFFFFF
            );
        }
        return helper.buildPalette(index, helper.isNightMode());
    }

    // ==================== 主题应用 ====================

    /**
     * 将当前主题完整应用到一个 Activity
     * 在 setContentView() 之后调用
     */
    public void applyToActivity(AppCompatActivity activity) {
        Palette p = getPalette();

        // ---- 窗口 ----
        activity.getWindow().setStatusBarColor(p.surface);
        activity.getWindow().setNavigationBarColor(p.surface);

        // ---- 根布局 ----
        View root = activity.findViewById(android.R.id.content);
        if (root != null) {
            root.setBackgroundColor(p.bg);
            // 也设置直接子视图的背景（覆盖 XML 硬编码的背景色）
            if (root instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) root;
                if (vg.getChildCount() > 0) {
                    vg.getChildAt(0).setBackgroundColor(p.bg);
                }
            }
        }
        activity.getWindow().getDecorView().setBackgroundColor(p.bg);

        // ---- 遍历视图树应用主题 ----
        applyToView(root, p);
    }

    /**
     * 递归应用主题色到视图
     */
    private void applyToView(View view, Palette p) {
        if (view == null) return;

        // MonthCalendarView → 自管理子 View 颜色，只设背景色，不递归覆盖子 View
        else if (view instanceof MonthCalendarView) {
            // 只设背景色（由 setPalette 负责内部子 View）
            view.setBackgroundColor(p.bg);
            // 不递归遍历子 View，避免覆盖日历单元格的精心设置的样式
            return;
        }
        // MaterialToolbar → 主色深色背景
        if (view instanceof MaterialToolbar) {
            view.setBackgroundColor(p.primaryDark);
        }
        // DrawerLayout → 设置遮罩颜色
        else if (view instanceof androidx.drawerlayout.widget.DrawerLayout) {
            ((androidx.drawerlayout.widget.DrawerLayout) view)
                    .setScrimColor(0x99000000); // 保持通用半透明黑遮罩
        }
        // MaterialCardView → 用 ColorStateList 设置卡片背景色
        else if (view instanceof MaterialCardView) {
            MaterialCardView card = (MaterialCardView) view;
            card.setCardBackgroundColor(
                    android.content.res.ColorStateList.valueOf(p.card));
            card.setStrokeColor(
                    android.content.res.ColorStateList.valueOf(p.tagBg));
            card.setStrokeWidth(1);
            card.invalidate(); // 强制重绘（ScrollView 中尤其需要）
        }
        // BottomNavigationView → 表面色背景 + 动态图标/文字颜色
        else if (view instanceof BottomNavigationView) {
            BottomNavigationView bnv = (BottomNavigationView) view;
            bnv.setBackgroundColor(p.surface);
            // 动态设置选中/未选中颜色（覆盖 XML 中静态的 nl_nav_item_color）
            int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] {}
            };
            int[] iconColors = new int[] { p.primary, p.textHint };
            bnv.setItemIconTintList(new ColorStateList(states, iconColors));
            bnv.setItemTextColor(new ColorStateList(states, iconColors));
        }
        // FloatingActionButton → 主色
        else if (view instanceof FloatingActionButton) {
            ((FloatingActionButton) view).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(p.primary));
            ((FloatingActionButton) view).setImageTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        }
        // MaterialButton → 主色文字 + 描边 + 背景色
        else if (view instanceof MaterialButton) {
            MaterialButton btn = (MaterialButton) view;
            btn.setStrokeColor(ColorStateList.valueOf(p.primary));
            
            // 判断按钮类型：有背景着色 = 填充按钮，否则 = 描边/文字按钮
            ColorStateList bgTint = btn.getBackgroundTintList();
            if (bgTint != null && btn.getStrokeWidth() == 0) {
                // Filled button: apply primary background + bg-colored text
                btn.setBackgroundTintList(ColorStateList.valueOf(p.primary));
                btn.setTextColor(p.bg);
            } else {
                // Outlined / Text button: keep transparent bg, primary text
                btn.setTextColor(p.primary);
            }
        }
        // Spinner → 表面色背景
        else if (view instanceof Spinner) {
            view.setBackgroundColor(p.surface);
        }
        // CalendarView → 动态设置日期文字样式
        else if (view instanceof CalendarView) {
            applyCalendarViewStyle((CalendarView) view, p);
        }
        // TextInputLayout → 设置盒子背景和提示色
        else if (view instanceof TextInputLayout) {
            TextInputLayout til = (TextInputLayout) view;
            til.setBoxStrokeColorStateList(createInputStrokeColor(p));
            til.setHintTextColor(createInputHintColor(p));
            // 设置输入框盒子背景色（覆盖默认的白色/透明）
            til.setBoxBackgroundColor(p.surface);
        }
        // TextInputEditText → 文本颜色（它也是 TextView，需在通用 TextView 之前处理）
        else if (view instanceof TextInputEditText) {
            TextInputEditText et = (TextInputEditText) view;
            et.setTextColor(p.textPrimary);
            et.setHintTextColor(p.textHint);
        }
        // SwitchMaterial → 无需额外处理（自动跟随 theme）
        // TextView → 强制设置文字颜色
        else if (view instanceof TextView && !(view instanceof TextInputEditText)) {
            TextView tv = (TextView) view;
            tv.setTextColor(p.textPrimary);
            tv.setHintTextColor(p.textHint);
            if (!tv.isEnabled()) {
                tv.setTextColor(p.textSecondary);
            }
        }
        // 通用回退：匹配已知背景色角色
        else {
            tryApplyRoleBackground(view, p);
        }

        // 递归处理子视图
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                applyToView(vg.getChildAt(i), p);
            }
        }
    }

    /**
     * 通用回退：检测 View 的 ColorDrawable 背景色，匹配已知角色并替换。
     * 首次匹配时将角色存入 View tag，后续直接从 tag 读取，保证重新应用主题时正确。
     */
    private void tryApplyRoleBackground(View view, Palette p) {
        Drawable bg = view.getBackground();
        if (!(bg instanceof ColorDrawable)) return;

        // 已有角色 tag → 直接应用
        Object tag = view.getTag(TAG_BG_ROLE);
        if (tag instanceof String) {
            applyRoleColor(view, (String) tag, p);
            return;
        }

        // 首次：根据颜色值分类
        int color = ((ColorDrawable) bg).getColor();
        String role = classifyColor(color);
        if (role != null) {
            view.setTag(TAG_BG_ROLE, role);
            applyRoleColor(view, role, p);
        }
    }

    /**
     * 根据颜色值判断其"语义角色"
     * 覆盖 XML 硬编码的 nl_* 静态默认色（深色模式 + 绿色风格）
     * 以及日间模式首次应用后的 LIGHT_BASE 颜色
     */
    private String classifyColor(int color) {
        // Dark mode XML 默认色（nl_bg / nl_surface / nl_card）
        if (color == 0xFF121212) return "bg";
        if (color == 0xFF1E1E1E) return "surface";
        if (color == 0xFF252525) return "card";

        // 绿色风格主色（XML 默认值）
        if (color == 0xFF4CAF50) return "primary";
        if (color == 0xFF388E3C) return "primaryDark";
        if (color == 0xFF81C784) return "accent";
        if (color == 0xFF64B5F6) return "link";
        if (color == 0x1A4CAF50) return "tagBg";

        // 其他风格的主色
        if (color == 0xFF2196F3) return "primary";  // 海洋蓝
        if (color == 0xFF1976D2) return "primaryDark";
        if (color == 0xFF64B5F6) return "accent";   // 海洋蓝 accent
        if (color == 0xFF81D4FA) return "link";      // 海洋蓝 link
        if (color == 0x1A2196F3) return "tagBg";     // 海洋蓝 tagBg
        
        if (color == 0xFFFF7043) return "primary";   // 落日橙
        if (color == 0xFFE64A19) return "primaryDark";
        if (color == 0xFFFFAB91) return "accent";
        if (color == 0xFFFFCC80) return "link";
        if (color == 0x1AFF7043) return "tagBg";
        
        if (color == 0xFF7C4DFF) return "primary";   // 星空紫
        if (color == 0xFF651FFF) return "primaryDark";
        if (color == 0xFFB388FF) return "accent";
        if (color == 0xFFB39DDB) return "link";
        if (color == 0x1A7C4DFF) return "tagBg";

        // 日间模式 LIGHT_BASE 颜色（重新应用时匹配）
        if (color == 0xFFF5F5F5) return "bg";
        if (color == 0xFFFFFFFF) return "surface"; // 日间 surface/card 同色

        // 匹配未识别到的 — 返回 null 跳过
        return null;
    }

    /** 根据语义角色应用对应的 palette 颜色 */
    private void applyRoleColor(View view, String role, Palette p) {
        switch (role) {
            case "bg":        view.setBackgroundColor(p.bg); break;
            case "surface":   view.setBackgroundColor(p.surface); break;
            case "card":      view.setBackgroundColor(p.card); break;
            case "primary":   view.setBackgroundColor(p.primary); break;
            case "primaryDark": view.setBackgroundColor(p.primaryDark); break;
            case "accent":    view.setBackgroundColor(p.accent); break;
            case "link":      view.setBackgroundColor(p.link); break;
            case "tagBg":     view.setBackgroundColor(p.tagBg); break;
        }
    }

    /**
     * 为 CalendarView 动态设置日期文字样式
     * CalendarView 原生组件不支持 XML 颜色引用覆盖
     */
    private void applyCalendarViewStyle(CalendarView cv, Palette p) {
        try {
            // 设置聚焦日期文字颜色
            cv.setFocusedMonthDateColor(p.primary);
            // 设置非聚焦月份日期文字颜色
            cv.setUnfocusedMonthDateColor(p.textSecondary);
            // 选中的日期文字颜色
            cv.setSelectedDateVerticalBar(p.primary);
            // 星期标题颜色
            cv.setWeekDayTextAppearance(android.R.style.TextAppearance_DeviceDefault_Small);
        } catch (Exception ignored) {
            // 不同 Android 版本的 CalendarView API 可能有差异
        }
    }

    /** 创建 TextInputLayout 输入框描边颜色状态列表 */
    private ColorStateList createInputStrokeColor(Palette p) {
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_focused },
            new int[] { android.R.attr.state_hovered },
            new int[] {}
        };
        int[] colors = new int[] { p.primary, p.primary, p.tagBg };
        return new ColorStateList(states, colors);
    }

    /** 创建 TextInputLayout 提示文字颜色状态列表 */
    private ColorStateList createInputHintColor(Palette p) {
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_focused },
            new int[] {}
        };
        int[] colors = new int[] { p.primary, p.textHint };
        return new ColorStateList(states, colors);
    }

    /**
     * 创建 Spinner 下拉弹窗背景 Drawable（圆角 + 动态调色板颜色）
     * 代替硬编码的 spinner_popup_background.xml
     */
    public static android.graphics.drawable.Drawable createSpinnerPopupBg(Palette p) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(p.card);
        gd.setCornerRadius(24f); // 8dp in pixels roughly, exact dp converted at call site
        return gd;
    }
}
