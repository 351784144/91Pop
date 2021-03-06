package com.dante.ui.history;

import android.support.annotation.NonNull;

import com.hannesdorfmann.mosby3.mvp.MvpBasePresenter;
import com.orhanobut.logger.Logger;
import com.dante.data.DataManager;
import com.dante.data.model.UnLimit91PornItem;

import java.util.List;

import javax.inject.Inject;

/**
 * 浏览历史，只有观看视频，并解析出视频地址保存之后才会被记录
 *
 * @author flymegoc
 * @date 2017/12/22
 */

public class HistoryPresenter extends MvpBasePresenter<HistoryView> implements IHistory {

    private static final String TAG = HistoryPresenter.class.getSimpleName();
    private DataManager dataManager;
    private int page = 1;
    private int pageSize = 10;

    @Inject
    public HistoryPresenter(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void loadHistoryData(boolean pullToRefresh) {
        //如果刷新则重置页数
        if (pullToRefresh) {
            page = 1;
        }
        final List<UnLimit91PornItem> unLimit91PornItemList = dataManager.loadHistoryData(page, pageSize);
        ifViewAttached(new ViewAction<HistoryView>() {
            @Override
            public void run(@NonNull HistoryView view) {
                if (page == 1) {
                    Logger.t(TAG).d("加载首页");
                    view.setData(unLimit91PornItemList);
                } else {
                    Logger.t(TAG).d("加载更多");
                    view.setMoreData(unLimit91PornItemList);
                    view.loadMoreDataComplete();
                }
                page++;
                if (unLimit91PornItemList.size() == 0 || unLimit91PornItemList.size() < pageSize) {
                    Logger.t(TAG).d("没有更多");
                    view.noMoreData();
                }
            }
        });
    }
}
