package org.telegram.neatgram.ui;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.neatgram.config.NeatGramConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
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
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
