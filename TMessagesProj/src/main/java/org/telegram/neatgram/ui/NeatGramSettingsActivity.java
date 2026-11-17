package org.telegram.neatgram.ui;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.neatgram.config.NeatGramConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextColorCell;
import org.telegram.ui.Components.ColorPicker;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * NeatGram Settings screen built with native Telegram components.
 * Contains Ghost Mode settings with expandable controls and master switch,
 * plus core neatGram toggles.
 */
public class NeatGramSettingsActivity extends UniversalFragment {

    private static final int ID_GHOST_MASTER = 100;
    private static final int ID_GHOST_DONT_READ_MESSAGES = 101;
    private static final int ID_GHOST_DONT_READ_STORIES = 102;
    private static final int ID_GHOST_DONT_SEND_ONLINE = 103;
    private static final int ID_GHOST_DONT_SEND_TYPING = 104;
    private static final int ID_GHOST_AUTO_OFFLINE = 105;
    private static final int ID_GHOST_READ_AFTER_ACTION = 106;
    private static final int ID_GHOST_USE_SCHEDULE = 107;

    private static final int ID_ADS_DISABLE = 201;
    private static final int ID_BOOST_DOWNLOAD = 202;
    private static final int ID_RAM_PILL = 203;
    private static final int ID_BACKGROUND_PUSH = 204;

    private static final int ID_HISTORY_SAVE_DELETED = 301;
    private static final int ID_HISTORY_SAVE_EDITS = 302;
    private static final int ID_HISTORY_SAVE_CHANNELS = 303;
    private static final int ID_HISTORY_DIM_DELETED = 304;
    private static final int ID_HISTORY_ICON = 305;
    private static final int ID_HISTORY_COLOR = 306;
    private static final int ID_HISTORY_SAVE_TTL = 307;

    private boolean ghostCollapsed = true;

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        fragmentView = super.createView(context);
        if (listView != null) {
            listView.setSections();
            listView.setSectionsDrawBackground(true);
        }
        return fragmentView;
    }

    @Override
    protected CharSequence getTitle() {
        return "Настройки neatGram";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int activeGhostCount = 0;
        if (NeatGramConfig.isGhostDontReadMessages(currentAccount)) activeGhostCount++;
        if (NeatGramConfig.isGhostDontReadStories(currentAccount)) activeGhostCount++;
        if (NeatGramConfig.isGhostDontSendOnline(currentAccount)) activeGhostCount++;
        if (NeatGramConfig.isGhostDontSendTyping(currentAccount)) activeGhostCount++;
        if (NeatGramConfig.isGhostAutoOffline(currentAccount)) activeGhostCount++;

        items.add(UItem.asHeader("Режим призрака"));
        items.add(
            UItem.asExpandableSwitch(ID_GHOST_MASTER, "Режим Призрака", activeGhostCount + "/5")
                .setChecked(NeatGramConfig.isGhostModeEnabled(currentAccount))
                .setCollapsed(ghostCollapsed)
                .setClickCallback(v -> {
                    boolean newState = !NeatGramConfig.isGhostModeEnabled(currentAccount);
                    NeatGramConfig.setGhostModeEnabled(currentAccount, newState);
                    if (listView != null && listView.adapter != null) {
                        listView.adapter.update(true);
                    }
                })
        );

        if (!ghostCollapsed) {
            items.add(
                UItem.asRoundCheckbox(ID_GHOST_DONT_READ_MESSAGES, "Не читать сообщения")
                    .setChecked(NeatGramConfig.isGhostDontReadMessages(currentAccount))
                    .setPad(1)
            );
            items.add(
                UItem.asRoundCheckbox(ID_GHOST_DONT_READ_STORIES, "Не читать истории")
                    .setChecked(NeatGramConfig.isGhostDontReadStories(currentAccount))
                    .setPad(1)
            );
            items.add(
                UItem.asRoundCheckbox(ID_GHOST_DONT_SEND_ONLINE, "Не отправлять «онлайн»")
                    .setChecked(NeatGramConfig.isGhostDontSendOnline(currentAccount))
                    .setPad(1)
            );
            items.add(
                UItem.asRoundCheckbox(ID_GHOST_DONT_SEND_TYPING, "Не отправлять «печатает»")
                    .setChecked(NeatGramConfig.isGhostDontSendTyping(currentAccount))
                    .setPad(1)
            );
            items.add(
                UItem.asRoundCheckbox(ID_GHOST_AUTO_OFFLINE, "Автоматический «офлайн»")
                    .setChecked(NeatGramConfig.isGhostAutoOffline(currentAccount))
                    .setPad(1)
            );
        }

        items.add(
            UItem.asCheck(ID_GHOST_READ_AFTER_ACTION, "Читать после действия")
                .setChecked(NeatGramConfig.isGhostReadAfterAction(currentAccount))
        );
        items.add(
            UItem.asCheck(ID_GHOST_USE_SCHEDULE, "Использовать отложку")
                .setChecked(NeatGramConfig.isGhostUseSchedule(currentAccount))
        );
        items.add(UItem.asShadow("Отправка сообщения, реакции или голос в опросе прочитает чат и моментально вернёт статус офлайн."));

        items.add(UItem.asHeader("Основные"));
        items.add(
            UItem.asButtonCheck(ID_BACKGROUND_PUSH, "Фоновая служба уведомлений", "Мгновенные пуши без Google-сервисов")
                .setChecked(NeatGramConfig.isBackgroundPushService())
        );
        items.add(
            UItem.asCheck(ID_ADS_DISABLE, "Отключить рекламу")
                .setChecked(NeatGramConfig.isDisableAds())
        );
        items.add(
            UItem.asCheck(ID_BOOST_DOWNLOAD, "Ускорение загрузки (Premium)")
                .setChecked(NeatGramConfig.isPremiumDownloadBoost(currentAccount))
        );
        items.add(
            UItem.asCheck(ID_RAM_PILL, "Виджет памяти в поиске")
                .setChecked(NeatGramConfig.isRamPillEnabled())
        );
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader("История сообщений"));
        items.add(
            UItem.asCheck(ID_HISTORY_SAVE_DELETED, "Сохранять удалённые сообщения")
                .setChecked(NeatGramConfig.isSaveDeletedMessages(currentAccount))
        );
        items.add(
            UItem.asButtonCheck(ID_HISTORY_SAVE_TTL, "Сохранять одноразовые медиа (TTL)", "Фото, видео, голосовые и кружки с таймером после просмотра")
                .setChecked(NeatGramConfig.isSaveTtlMedia(currentAccount))
        );
        items.add(
            UItem.asCheck(ID_HISTORY_SAVE_EDITS, "История редактирования")
                .setChecked(NeatGramConfig.isSaveEditHistory(currentAccount))
        );
        items.add(
            UItem.asButtonCheck(ID_HISTORY_SAVE_CHANNELS, "Сохранять в каналах", "Отключено по умолчанию для экономии памяти")
                .setChecked(NeatGramConfig.isSaveHistoryInChannels(currentAccount))
        );
        items.add(
            UItem.asCheck(ID_HISTORY_DIM_DELETED, "Затемнять удалённые сообщения")
                .setChecked(NeatGramConfig.isDimDeletedMessages(currentAccount))
        );

        String[] iconNames = new String[]{"Корзина", "Крестик", "Зачёркнутый глаз", "Призрак"};
        int iconIndex = NeatGramConfig.getDeletedMessageIcon(currentAccount);
        String selectedIconName = iconIndex >= 0 && iconIndex < iconNames.length ? iconNames[iconIndex] : iconNames[0];
        items.add(
            UItem.asButton(ID_HISTORY_ICON, "Иконка удалённых сообщений", selectedIconName)
        );

        Context context = getContext();
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }
        if (context != null) {
            TextColorCell colorCell = new TextColorCell(context, getResourceProvider());
            int currentColor = NeatGramConfig.getDeletedIconColor(currentAccount);
            colorCell.setTextAndColor("Цвет иконки", currentColor, false);
            items.add(UItem.asCustom(ID_HISTORY_COLOR, colorCell));
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_GHOST_MASTER:
                ghostCollapsed = !ghostCollapsed;
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_DONT_READ_MESSAGES:
                NeatGramConfig.setGhostDontReadMessages(currentAccount, !NeatGramConfig.isGhostDontReadMessages(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_DONT_READ_STORIES:
                NeatGramConfig.setGhostDontReadStories(currentAccount, !NeatGramConfig.isGhostDontReadStories(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_DONT_SEND_ONLINE:
                NeatGramConfig.setGhostDontSendOnline(currentAccount, !NeatGramConfig.isGhostDontSendOnline(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_DONT_SEND_TYPING:
                NeatGramConfig.setGhostDontSendTyping(currentAccount, !NeatGramConfig.isGhostDontSendTyping(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_AUTO_OFFLINE:
                NeatGramConfig.setGhostAutoOffline(currentAccount, !NeatGramConfig.isGhostAutoOffline(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_READ_AFTER_ACTION:
                NeatGramConfig.setGhostReadAfterAction(currentAccount, !NeatGramConfig.isGhostReadAfterAction(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_GHOST_USE_SCHEDULE:
                NeatGramConfig.setGhostUseSchedule(currentAccount, !NeatGramConfig.isGhostUseSchedule(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_ADS_DISABLE:
                NeatGramConfig.setDisableAds(!NeatGramConfig.isDisableAds());
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_BOOST_DOWNLOAD:
                NeatGramConfig.setPremiumDownloadBoost(currentAccount, !NeatGramConfig.isPremiumDownloadBoost(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_RAM_PILL:
                NeatGramConfig.setRamPillEnabled(!NeatGramConfig.isRamPillEnabled());
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_BACKGROUND_PUSH:
                boolean newPushState = !NeatGramConfig.isBackgroundPushService();
                NeatGramConfig.setBackgroundPushService(newPushState);
                ApplicationLoader.startPushService();
                for (int i = 0; i < org.telegram.messenger.UserConfig.MAX_ACCOUNT_COUNT; i++) {
                    if (org.telegram.messenger.UserConfig.getInstance(i).isClientActivated()) {
                        org.telegram.tgnet.ConnectionsManager.getInstance(i).setPushConnectionEnabled(newPushState);
                    }
                }
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_HISTORY_SAVE_DELETED:
                NeatGramConfig.setSaveDeletedMessages(currentAccount, !NeatGramConfig.isSaveDeletedMessages(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_HISTORY_SAVE_TTL:
                NeatGramConfig.setSaveTtlMedia(currentAccount, !NeatGramConfig.isSaveTtlMedia(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_HISTORY_SAVE_EDITS:
                NeatGramConfig.setSaveEditHistory(currentAccount, !NeatGramConfig.isSaveEditHistory(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_HISTORY_SAVE_CHANNELS:
                NeatGramConfig.setSaveHistoryInChannels(currentAccount, !NeatGramConfig.isSaveHistoryInChannels(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_HISTORY_DIM_DELETED:
                NeatGramConfig.setDimDeletedMessages(currentAccount, !NeatGramConfig.isDimDeletedMessages(currentAccount));
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
                break;
            case ID_HISTORY_ICON:
                openIconDialog();
                break;
            case ID_HISTORY_COLOR:
                openColorDialog();
                break;
        }
    }

    private void openIconDialog() {
        if (getParentActivity() == null) {
            return;
        }
        int[] icons = new int[]{
            R.drawable.ic_neatgram_del_trash,
            R.drawable.ic_neatgram_del_cross,
            R.drawable.ic_neatgram_del_eye_off,
            R.drawable.ic_neatgram_del_ghost
        };
        CharSequence[] names = new CharSequence[]{"Корзина", "Крестик", "Зачёркнутый глаз", "Призрак"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Иконка удалённых сообщений");
        builder.setItems(names, icons, (dialog, which) -> {
            NeatGramConfig.setDeletedMessageIcon(currentAccount, which);
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openColorDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] colorOptions = new CharSequence[]{
            "Авто (цвет времени)",
            "Красный (#FF5252)",
            "Оранжевый (#FF9800)",
            "Графитовый (#8E929B)",
            "Белый (#FFFFFF)",
            "Голубой (#40A7E3)",
            "Свой цвет..."
        };
        int[] colorValues = new int[]{
            0,
            0xFFFF5252,
            0xFFFF9800,
            0xFF8E929B,
            0xFFFFFFFF,
            0xFF40A7E3
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Цвет иконки");
        builder.setItems(colorOptions, (dialog, which) -> {
            if (which < colorValues.length) {
                NeatGramConfig.setDeletedIconColor(currentAccount, colorValues[which]);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            } else {
                openColorPicker();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void openColorPicker() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        int currentColor = NeatGramConfig.getDeletedIconColor(currentAccount);
        ColorPicker colorPicker = new ColorPicker(context, false, new ColorPicker.ColorPickerDelegate() {
            @Override
            public void setColor(int color, int num, boolean applyNow) {
                NeatGramConfig.setDeletedIconColor(currentAccount, color);
                if (listView != null && listView.adapter != null) {
                    listView.adapter.update(true);
                }
            }
        }) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(300), MeasureSpec.EXACTLY));
            }
        };
        colorPicker.setColor(currentColor != 0 ? currentColor : 0xFF40A7E3, 0);
        colorPicker.setType(-1, true, 1, 1, false, 0, false);
        BottomSheet bottomSheet = new BottomSheet(context, false);
        bottomSheet.setCustomView(colorPicker);
        bottomSheet.show();
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
