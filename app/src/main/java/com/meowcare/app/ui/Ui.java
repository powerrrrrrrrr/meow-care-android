package com.meowcare.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import com.google.android.material.button.MaterialButton;
import com.meowcare.app.R;

public final class Ui {
    private Ui() {}
    public static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    public static TextView text(Context c, String text, int size, int color) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(size); v.setTextColor(c.getColor(color));
        v.setLineSpacing(dp(c, 3), 1); return v;
    }
    public static void heading(LinearLayout parent, String title) {
        TextView t = text(parent.getContext(), title, 20, R.color.ink); t.setTypeface(null, Typeface.BOLD);
        t.setPadding(0, dp(parent.getContext(), 18), 0, dp(parent.getContext(), 12)); parent.addView(t);
    }
    public static LinearLayout panel(LinearLayout parent) {
        Context c = parent.getContext(); LinearLayout panel = new LinearLayout(c); panel.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16); panel.setPadding(p,p,p,p); panel.setBackground(outlinedBackground(c, R.color.surface, R.color.line, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin = dp(c,12); parent.addView(panel,lp); return panel;
    }
    public static GradientDrawable background(Context c, int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(c.getColor(color)); d.setCornerRadius(dp(c,radius)); return d;
    }
    public static GradientDrawable outlinedBackground(Context c, int color, int stroke, int radius) {
        GradientDrawable d=background(c,color,radius);d.setStroke(dp(c,1),c.getColor(stroke));return d;
    }
    public static GradientDrawable circle(Context c,int color) {
        GradientDrawable d=new GradientDrawable();d.setShape(GradientDrawable.OVAL);d.setColor(c.getColor(color));return d;
    }
    public static View divider(Context c) {
        View line=new View(c);line.setBackgroundColor(c.getColor(R.color.line));
        line.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(c,1)));return line;
    }
    public static ImageView icon(Context c,int drawable) {
        ImageView icon=new ImageView(c);icon.setImageResource(drawable);icon.setColorFilter(c.getColor(R.color.primary));
        icon.setPadding(dp(c,6),dp(c,6),dp(c,6),dp(c,6));icon.setBackgroundResource(R.drawable.bg_icon_circle);return icon;
    }
    public static int taskIcon(String title) {
        if(title.contains("饮水"))return R.drawable.ic_water_bowl;
        if(title.contains("饭盆"))return R.drawable.ic_food_bowl;
        if(title.contains("更换"))return R.drawable.ic_litter_box;
        return R.drawable.ic_scoop;
    }
    public static MaterialButton iconButton(Context c,int drawable,String description,View.OnClickListener listener) {
        MaterialButton button=new MaterialButton(c);button.setText("");button.setIconResource(drawable);button.setIconTint(ColorStateList.valueOf(c.getColor(R.color.primary)));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);button.setIconPadding(0);button.setMinWidth(dp(c,48));button.setMinimumWidth(dp(c,48));button.setMinHeight(dp(c,48));
        button.setContentDescription(description);button.setBackgroundTintList(ColorStateList.valueOf(c.getColor(R.color.surface)));button.setElevation(0);button.setStateListAnimator(null);button.setOnClickListener(listener);return button;
    }
    public static MaterialButton button(Context c, String label, View.OnClickListener listener) {
        MaterialButton b = new MaterialButton(c); b.setText(label); b.setTextSize(14); b.setCornerRadius(dp(c,12));
        b.setMinHeight(dp(c,48)); b.setOnClickListener(listener); return b;
    }
    public static void secondary(MaterialButton b) { Context c=b.getContext(); b.setBackgroundTintList(ColorStateList.valueOf(c.getColor(R.color.soft))); b.setTextColor(c.getColor(R.color.primary)); }
}
