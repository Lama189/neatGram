package org.telegram.neatgram.ui;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Debug;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.neatgram.config.NeatGramConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * Compact RAM info capsule widget (RAM Pill) displayed in the search bar header of DialogsActivity.
 * Renders an equalizer-like load indicator icon and memory usage in tabular digits.
 * Updates periodically only when attached and visible to ensure zero background battery drain.
 */
public class RamInfoPillView extends View implements Theme.Colorable {

    private static final long UPDATE_INTERVAL_MS = 3000L;
    private static final float GB_FACTOR = 1024f * 1024f * 1024f;

    private final Theme.ResourcesProvider resourcesProvider;
    private final WeakReference<BaseFragment> fragmentRef;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private final RectF rectF = new RectF();
    private final RectF barRectF = new RectF();

    private final ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
    private ActivityManager activityManager;

    private String text = "";
    private float textWidth;

    private boolean isPaused;
    private boolean isRunning;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!shouldRunUpdates()) {
                isRunning = false;
                return;
            }
            updateMemoryInfo();
            AndroidUtilities.runOnUIThread(this, UPDATE_INTERVAL_MS);
        }
    };

    public RamInfoPillView(Context context) {
        this(context, null, null);
    }

    public RamInfoPillView(Context context, Theme.ResourcesProvider resourcesProvider) {
        this(context, resourcesProvider, null);
    }

    public RamInfoPillView(Context context, Theme.ResourcesProvider resourcesProvider, BaseFragment fragment) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.fragmentRef = fragment != null ? new WeakReference<>(fragment) : null;

        setClickable(true);
        setFocusable(false);

        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setTextSize(AndroidUtilities.dp(11.5f));
        textPaint.setFontFeatureSettings("tnum");

        updateColors();
        updateMemoryInfo();
    }

    private int getThemedColor(int key) {
        return resourcesProvider != null ? resourcesProvider.getColor(key) : Theme.getColor(key);
    }

    @Override
    public void updateColors() {
        boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
        int textColor = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);

        if (isDark) {
            bgPaint.setColor(Theme.multAlpha(textColor, isPressed() ? 0.22f : 0.14f));
        } else {
            bgPaint.setColor(Theme.multAlpha(textColor, isPressed() ? 0.15f : 0.08f));
        }

        iconPaint.setColor(Theme.multAlpha(textColor, 0.75f));
        textPaint.setColor(Theme.multAlpha(textColor, 0.90f));

        invalidate();
    }

    private void updateMemoryInfo() {
        if (activityManager == null) {
            Context context = getContext();
            if (context != null) {
                activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            }
        }
        if (activityManager == null) {
            return;
        }

        activityManager.getMemoryInfo(memoryInfo);
        long total = memoryInfo.totalMem;
        long avail = memoryInfo.availMem;
        long used = Math.max(0L, total - avail);

        float usedGb = (float) used / GB_FACTOR;
        float totalGb = (float) total / GB_FACTOR;

        String newText = String.format(Locale.US, "%.1f GB / %.1f GB", usedGb, totalGb);
        if (!newText.equals(text)) {
            text = newText;
            float newTextWidth = textPaint.measureText(text);
            boolean widthChanged = Math.abs(newTextWidth - textWidth) > 0.5f;
            textWidth = newTextWidth;
            if (widthChanged) {
                requestLayout();
            } else {
                invalidate();
            }
        }
    }

    private boolean shouldRunUpdates() {
        return !isPaused
                && isAttachedToWindow()
                && getWindowVisibility() == VISIBLE
                && getVisibility() == VISIBLE
                && isShown()
                && NeatGramConfig.isRamPillEnabled();
    }

    private void checkRunning() {
        boolean shouldRun = shouldRunUpdates();
        if (shouldRun != isRunning) {
            isRunning = shouldRun;
            if (shouldRun) {
                AndroidUtilities.cancelRunOnUIThread(updateRunnable);
                updateMemoryInfo();
                AndroidUtilities.runOnUIThread(updateRunnable, UPDATE_INTERVAL_MS);
            } else {
                AndroidUtilities.cancelRunOnUIThread(updateRunnable);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = AndroidUtilities.dp(24);
        float barWidth = AndroidUtilities.dpf2(2.0f);
        float barGap = AndroidUtilities.dpf2(1.5f);
        float iconWidth = 3 * barWidth + 2 * barGap;

        float padLeft = AndroidUtilities.dp(7.0f);
        float iconGap = AndroidUtilities.dp(5.0f);
        float padRight = AndroidUtilities.dp(7.0f);

        if (textWidth <= 0 && !text.isEmpty()) {
            textWidth = textPaint.measureText(text);
        }

        int width = (int) Math.ceil(padLeft + iconWidth + iconGap + textWidth + padRight);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        float radius = h / 2.0f;
        rectF.set(0, 0, w, h);
        canvas.drawRoundRect(rectF, radius, radius, bgPaint);

        float centerY = h / 2.0f;
        float barWidth = AndroidUtilities.dpf2(2.0f);
        float barRadius = barWidth / 2.0f;
        float barGap = AndroidUtilities.dpf2(1.5f);

        float padLeft = AndroidUtilities.dp(7.0f);
        float iconGap = AndroidUtilities.dp(5.0f);
        float maxBarHeight = AndroidUtilities.dpf2(10.5f);
        float bottomY = centerY + maxBarHeight / 2.0f;

        float h1 = AndroidUtilities.dpf2(7.0f);
        float h2 = AndroidUtilities.dpf2(10.5f);
        float h3 = AndroidUtilities.dpf2(4.5f);

        boolean isRtl = LocaleController.isRTL;
        float iconStartX = isRtl
                ? (w - padLeft - (3 * barWidth + 2 * barGap))
                : padLeft;

        // Draw 3 vertical bars: bar 1 (medium), bar 2 (high), bar 3 (low)
        barRectF.set(iconStartX, bottomY - h1, iconStartX + barWidth, bottomY);
        canvas.drawRoundRect(barRectF, barRadius, barRadius, iconPaint);

        float b2Left = iconStartX + (barWidth + barGap);
        barRectF.set(b2Left, bottomY - h2, b2Left + barWidth, bottomY);
        canvas.drawRoundRect(barRectF, barRadius, barRadius, iconPaint);

        float b3Left = iconStartX + 2 * (barWidth + barGap);
        barRectF.set(b3Left, bottomY - h3, b3Left + barWidth, bottomY);
        canvas.drawRoundRect(barRectF, barRadius, barRadius, iconPaint);

        // Draw text
        if (!text.isEmpty()) {
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float baseline = centerY - (fm.ascent + fm.descent) / 2.0f;
            float textStartX = isRtl
                    ? (iconStartX - iconGap - textWidth)
                    : (iconStartX + (3 * barWidth + 2 * barGap) + iconGap);
            canvas.drawText(text, textStartX, baseline, textPaint);
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        boolean changed = isPressed() != pressed;
        super.setPressed(pressed);
        if (changed) {
            updateColors();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                return true;
            case MotionEvent.ACTION_UP:
                if (isPressed()) {
                    setPressed(false);
                    performClick();
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                setPressed(false);
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        handlePillClick();
        return true;
    }

    private void handlePillClick() {
        ImageLoader.getInstance().clearMemory();
        updateMemoryInfo();
        invalidate();

        Utilities.globalQueue.postRunnable(() -> {
            long pss = Debug.getPss();
            AndroidUtilities.runOnUIThread(() -> {
                if (!isAttachedToWindow()) {
                    return;
                }
                showClearCacheBulletin(pss);
            });
        });
    }

    private void showClearCacheBulletin(long pss) {
        BaseFragment fragment = fragmentRef != null ? fragmentRef.get() : null;
        BulletinFactory factory;
        if (fragment != null && BulletinFactory.canShowBulletin(fragment)) {
            factory = BulletinFactory.of(fragment);
        } else {
            factory = BulletinFactory.global();
        }

        String pssText = pss > 0 ? AndroidUtilities.formatFileSize(pss) : null;
        if (pssText == null) {
            long heap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            pssText = AndroidUtilities.formatFileSize(heap);
        }

        CharSequence title = LocaleController.getString(R.string.ClearCache);
        CharSequence subtitle = "neatGram PSS: " + pssText;
        factory.createSimpleBulletin(R.raw.ic_delete, title, subtitle).show();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkRunning();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AndroidUtilities.cancelRunOnUIThread(updateRunnable);
        isRunning = false;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        checkRunning();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        checkRunning();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        checkRunning();
        if (getParent() instanceof View) {
            ((View) getParent()).requestLayout();
        }
    }

    public void onPause() {
        isPaused = true;
        AndroidUtilities.cancelRunOnUIThread(updateRunnable);
        isRunning = false;
    }

    public void onResume() {
        isPaused = false;
        checkRunning();
    }

    public void destroy() {
        isPaused = true;
        AndroidUtilities.cancelRunOnUIThread(updateRunnable);
        isRunning = false;
    }
}
