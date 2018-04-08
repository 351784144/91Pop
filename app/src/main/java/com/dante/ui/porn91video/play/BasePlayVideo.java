package com.dante.ui.porn91video.play;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.loadmore.LoadMoreView;
import com.dante.R;
import com.dante.adapter.VideoCommentAdapter;
import com.dante.custom.TastyToast;
import com.dante.data.DataManager;
import com.dante.data.model.UnLimit91PornItem;
import com.dante.data.model.VideoComment;
import com.dante.data.model.VideoResult;
import com.dante.service.DownloadVideoService;
import com.dante.ui.MvpActivity;
import com.dante.ui.porn91video.author.AuthorActivity;
import com.dante.ui.user.UserLoginActivity;
import com.dante.utils.AddressHelper;
import com.dante.utils.AppUtils;
import com.dante.utils.DialogUtils;
import com.dante.utils.LoadHelperUtils;
import com.dante.utils.UserHelper;
import com.dante.utils.constants.Keys;
import com.github.rubensousa.floatingtoolbar.FloatingToolbar;
import com.helper.loadviewhelper.help.OnLoadViewListener;
import com.helper.loadviewhelper.load.LoadViewHelper;
import com.jaeger.library.StatusBarUtil;
import com.orhanobut.logger.Logger;
import com.qmuiteam.qmui.util.QMUIDisplayHelper;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * @author flymegoc
 */
public abstract class BasePlayVideo extends MvpActivity<PlayVideoView, PlayVideoPresenter> implements PlayVideoView {

    private final String TAG = BasePlayVideo.class.getSimpleName();
    protected UnLimit91PornItem unLimit91PornItem;
    @Inject
    protected AddressHelper addressHelper;
    @Inject
    protected PlayVideoPresenter playVideoPresenter;
    @Inject
    protected DataManager dataManager;
    @BindView(R.id.recyclerView_video_comment)
    RecyclerView recyclerViewVideoComment;
    @BindView(R.id.root)
    CoordinatorLayout root;
    @BindView(R.id.floatingToolbar)
    FloatingToolbar floatingToolbar;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.app_bar_layout)
    AppBarLayout appBarLayout;
    @BindView(R.id.tv_play_video_title)
    TextView tvPlayVideoTitle;
    @BindView(R.id.tv_play_video_author)
    TextView tvPlayVideoAuthor;
    @BindView(R.id.tv_play_video_add_date)
    TextView tvPlayVideoAddDate;
    @BindView(R.id.tv_play_video_info)
    TextView tvPlayVideoInfo;
    @BindView(R.id.fl_load_holder)
    FrameLayout flLoadHolder;
    @BindView(R.id.et_video_comment)
    AppCompatEditText etVideoComment;
    @BindView(R.id.iv_video_comment_send)
    ImageView ivVideoCommentSend;
    @BindView(R.id.et_comment_input_layout)
    LinearLayout etCommentInputLayout;
    @BindView(R.id.comment_swipeRefreshLayout)
    SwipeRefreshLayout commentSwipeRefreshLayout;
    @BindView(R.id.userinfo_layout)
    LinearLayout userinfoLayout;
    @BindView(R.id.videoplayer_container)
    FrameLayout videoplayerContainer;
    private AlertDialog mAlertDialog;
    private AlertDialog favoriteDialog;
    private AlertDialog commentVideoDialog;
    private LoadViewHelper helper;
    private VideoCommentAdapter videoCommentAdapter;
    private boolean isVideoError = true;
    private boolean isComment = true;
    private VideoComment videoComment;
    private boolean commentLayoutShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_play_video);
        ButterKnife.bind(this);
        setVideoViewHeight(videoplayerContainer);
        initPlayerView();
        unLimit91PornItem = (UnLimit91PornItem) getIntent().getSerializableExtra(Keys.KEY_INTENT_UNLIMIT91PORNITEM);
        TransitionManager.beginDelayedTransition(root);
        initListener();
        initDialog();
        initLoadHelper();
        initVideoComments();
        initData();
        initBottomMenu();
        setLoadingViewVideoCommentHeight();
    }

    /**
     * 初始化视频引擎视图
     */
    public abstract void initPlayerView();

    private void initListener() {
        ivVideoCommentSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String comment = etVideoComment.getText().toString().trim();
                commentOrReplyVideo(comment);
            }
        });
        commentSwipeRefreshLayout.setEnabled(false);
        AppUtils.setColorSchemeColors(this, commentSwipeRefreshLayout);
        commentSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (unLimit91PornItem.getVideoResultId() == 0) {
                    commentSwipeRefreshLayout.setRefreshing(false);
                    return;
                }
                String videoId = unLimit91PornItem.getVideoResult().getVideoId();
                presenter.loadVideoComment(videoId, unLimit91PornItem.getViewKey(), true);
            }
        });
        userinfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isComment = true;
                videoCommentAdapter.setClickPosition(-1);
                videoCommentAdapter.notifyDataSetChanged();
                etVideoComment.setHint(R.string.comment_video_hint_tip);
            }
        });
    }

    /**
     * 评论视频或者回复评论
     *
     * @param comment 留言内容
     */
    private void commentOrReplyVideo(String comment) {
        if (TextUtils.isEmpty(comment)) {
            showMessage("评论内容不能为空", TastyToast.INFO);
            return;
        }

        if (!UserHelper.isUserInfoComplete(user)) {
            showMessage("请先登录帐号", TastyToast.INFO);
            goToLogin();
            return;
        }
        if (unLimit91PornItem.getVideoResultId() == 0) {
            showMessage("视频还未解析完成，无法评论", TastyToast.WARNING);
            return;
        }
        String vid = unLimit91PornItem.getVideoResult().getVideoId();
        String uid = String.valueOf(user.getUserId());
        if (isComment) {
            commentVideoDialog.show();
            presenter.commentVideo(comment, uid, vid, unLimit91PornItem.getViewKey());
        } else {
            if (videoComment == null) {
                showMessage("请先点击需要回复的评论", TastyToast.WARNING);
                return;
            }
            commentVideoDialog.show();
            String username = videoComment.getuName();
            String commentId = videoComment.getReplyId();
            presenter.replyComment(comment, username, vid, commentId, unLimit91PornItem.getViewKey());
        }
    }

    private void initData() {
        UnLimit91PornItem tmp = dataManager.findUnLimit91PornItemByViewKey(unLimit91PornItem.getViewKey());
        if (tmp == null || tmp.getVideoResult() == null) {
            presenter.loadVideoUrl(unLimit91PornItem);
        } else {
            unLimit91PornItem = tmp;
            videoplayerContainer.setVisibility(View.VISIBLE);
            Logger.t(TAG).d("使用已有播放地址");
            //浏览历史
            unLimit91PornItem.setViewHistoryDate(new Date());
            dataManager.updateUnLimit91PornItem(unLimit91PornItem);
            VideoResult videoResult = unLimit91PornItem.getVideoResult();
            setToolBarLayoutInfo(unLimit91PornItem);
            playVideo(unLimit91PornItem.getTitle(), videoResult.getVideoUrl(), videoResult.getVideoName(), videoResult.getThumbImgUrl());
            //加载评论
            presenter.loadVideoComment(videoResult.getVideoId(), unLimit91PornItem.getViewKey(), true);
        }
    }

    private void setToolBarLayoutInfo(final UnLimit91PornItem unLimit91PornItem) {
        if (unLimit91PornItem.getVideoResultId() == 0) {
            return;
        }
        String searchTitleTag = "...";
        VideoResult videoResult = unLimit91PornItem.getVideoResult();
        if (unLimit91PornItem.getTitle().contains(searchTitleTag) || unLimit91PornItem.getTitle().endsWith(searchTitleTag)) {
            tvPlayVideoTitle.setText(videoResult.getVideoName());
        } else {
            tvPlayVideoTitle.setText(unLimit91PornItem.getTitle());
        }
        tvPlayVideoAuthor.setText(String.format("作者：%s", videoResult.getOwnnerName()));
        tvPlayVideoAddDate.setText(videoResult.getAddDate());
        tvPlayVideoInfo.setText(videoResult.getUserOtherInfo());
        tvPlayVideoAuthor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UserHelper.isUserInfoComplete(user)) {
                    goToLogin();
                    showMessage("请先登录", TastyToast.WARNING);
                    return;
                }
                if (unLimit91PornItem.getVideoResultId() == 0) {
                    showMessage("视频还未解析成功", TastyToast.WARNING);
                    return;
                }
                Intent intent = new Intent(BasePlayVideo.this, AuthorActivity.class);
                intent.putExtra(Keys.KEY_INTENT_UID, unLimit91PornItem.getVideoResult().getOwnnerId());
                startActivityForResultWithAnimotion(intent, 1);
            }
        });
    }

    private void initLoadHelper() {
        helper = new LoadViewHelper(flLoadHolder);
        helper.setListener(new OnLoadViewListener() {
            @Override
            public void onRetryClick() {
                if (isVideoError) {
                    presenter.loadVideoUrl(unLimit91PornItem);
                } else {
                    //加载评论
                    if (unLimit91PornItem.getVideoResultId() == 0) {
                        return;
                    }
                    presenter.loadVideoComment(unLimit91PornItem.getVideoResult().getVideoId(), unLimit91PornItem.getViewKey(), true);
                }
            }
        });
    }

    private void initDialog() {
        mAlertDialog = DialogUtils.initLodingDialog(this, "视频地址解析中...");
        favoriteDialog = DialogUtils.initLodingDialog(this, "收藏中...");
        commentVideoDialog = DialogUtils.initLodingDialog(this, "提交评论中...");
    }

    /**
     * 测量高度，为了让loading居中显示
     */
    private void setLoadingViewVideoCommentHeight() {
        int height = dataManager.getAppBarLayoutHeight();
        if (height > 0) {
            setLoadingViewHeight(height);
        }
        appBarLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                appBarLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int height = appBarLayout.getMeasuredHeight();
                int screenHeight = QMUIDisplayHelper.getScreenHeight(BasePlayVideo.this);
                int remainHeight = screenHeight - height - QMUIDisplayHelper.getScreenWidth(BasePlayVideo.this) * 9 / 16;
                if (remainHeight != height) {
                    dataManager.setAppBarLayoutHeight(remainHeight);
                    setLoadingViewHeight(remainHeight);
                }
            }
        });
    }

    /**
     * 控制高度
     */
    private void setLoadingViewHeight(int height) {
        Logger.t(TAG).d("height:" + height);
        ViewGroup.LayoutParams layoutParams = flLoadHolder.getLayoutParams();
        layoutParams.height = height;
        flLoadHolder.setLayoutParams(layoutParams);
        flLoadHolder.postInvalidate();
    }

    private void initVideoComments() {
        List<VideoComment> videoCommentList = new ArrayList<>();
        videoCommentAdapter = new VideoCommentAdapter(this, R.layout.item_video_comment, videoCommentList);

        recyclerViewVideoComment.setLayoutManager(new LinearLayoutManager(this));
//        recyclerViewVideoComment.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerViewVideoComment.setAdapter(videoCommentAdapter);
        videoCommentAdapter.setLoadMoreView(new LoadMoreView() {
            @Override
            public int getLayoutId() {
                return R.layout.loading;
            }

            @Override
            protected int getLoadingViewId() {
                return R.id.loading;
            }

            @Override
            protected int getLoadFailViewId() {
                return R.id.loadFailed;
            }

            @Override
            protected int getLoadEndViewId() {
                return R.id.loadEnd;
            }
        });
        videoCommentAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                //加载评论
                if (unLimit91PornItem.getVideoResultId() == 0) {
                    videoCommentAdapter.loadMoreFail();
                    return;
                }
                presenter.loadVideoComment(unLimit91PornItem.getVideoResult().getVideoId(), unLimit91PornItem.getViewKey(), false);
            }
        }, recyclerViewVideoComment);
        videoCommentAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (floatingToolbar.isShowing()) {
                    floatingToolbar.hide();
                }
                if (commentLayoutShown) {
                    if (videoCommentAdapter.getClickPosition() == position) {
                        hideCommentLayout();
                    }
                } else {
                    showCommentLayout();
                }
                isComment = false;
                videoComment = (VideoComment) adapter.getData().get(position);
                etVideoComment.setHint("回复：" + videoComment.getuName());
                videoCommentAdapter.setClickPosition(position);
            }
        });
        recyclerViewVideoComment.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0)
                    fab.hide();
                else if (dy < -5)
                    fab.show();
            }
        });
    }

    private void showCommentLayout() {
        if (!commentLayoutShown) {
            etCommentInputLayout.setVisibility(View.VISIBLE);
            etCommentInputLayout.animate().scaleX(1).setInterpolator(new LinearOutSlowInInterpolator()).setStartDelay(0).setListener(null).start();
            fab.hide();
        }
        commentLayoutShown = true;
    }

    private void hideCommentLayout() {
        if (commentLayoutShown) {
            etCommentInputLayout.animate().scaleX(0).setInterpolator(new FastOutLinearInInterpolator()).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    etCommentInputLayout.setVisibility(View.GONE);
                    fab.show();
                }
            }).start();
        }
        commentLayoutShown = false;
    }

    private void initBottomMenu() {
        floatingToolbar.attachFab(fab);
        floatingToolbar.attachRecyclerView(recyclerViewVideoComment);
        floatingToolbar.setClickListener(new FloatingToolbar.ItemClickListener() {
            @Override
            public void onItemClick(MenuItem item) {
                onOptionsItemSelected(item);
            }

            @Override
            public void onItemLongClick(MenuItem item) {

            }
        });
    }

    /**
     * 根据屏幕宽度信息重设videoview宽高为16：9比例
     */
    protected void setVideoViewHeight(View playerView) {
        AppBarLayout.LayoutParams layoutParams = (AppBarLayout.LayoutParams) playerView.getLayoutParams();
        layoutParams.height = QMUIDisplayHelper.getScreenWidth(this) * 9 / 16;
        playerView.setLayoutParams(layoutParams);
    }

    /**
     * 开始播放视频
     *
     * @param title      视频标题
     * @param videoUrl   视频链接
     * @param name       视频名字
     * @param thumImgUrl 视频缩略图
     */
    public abstract void playVideo(String title, String videoUrl, String name, String thumImgUrl);


    @NonNull
    @Override
    public PlayVideoPresenter createPresenter() {
        getActivityComponent().inject(this);
        return playVideoPresenter;
    }

    @Override
    public void showParsingDialog() {
        if (mAlertDialog == null) {
            return;
        }
        mAlertDialog.show();
    }

    @Override
    public void playVideo(UnLimit91PornItem unLimit91PornItem) {
        this.unLimit91PornItem = unLimit91PornItem;
        videoplayerContainer.setVisibility(View.VISIBLE);
        dismissDialog();
        setToolBarLayoutInfo(unLimit91PornItem);
        VideoResult videoResult = unLimit91PornItem.getVideoResult();
        playVideo(unLimit91PornItem.getTitle(), videoResult.getVideoUrl(), "", videoResult.getThumbImgUrl());
        helper.showContent();
        presenter.loadVideoComment(videoResult.getVideoId(), unLimit91PornItem.getViewKey(), true);
        boolean neverAskForWatchDownloadTip = dataManager.isNeverAskForWatchDownloadTip();
        if (!neverAskForWatchDownloadTip) {
            showWatchDownloadVideoTipDialog();
        }
    }

    private void showWatchDownloadVideoTipDialog() {
        QMUIDialog.MessageDialogBuilder builder = new QMUIDialog.MessageDialogBuilder(this);
        builder.setTitle("温馨提示");
        builder.setMessage("1. 通常无法在线观看视频就意味着你也无法下载视频\n" +
                "2. 若在线观看速度慢可以选择下载后再观看，有时候比在线观看要快\n" +
                "3. 如果想要更好的观看和下载，请使用VPN（不是设置中的HTTP代理）；\n" +
                "4. 点击作者名字可查看该作者其他视频（需登录）");
        builder.addAction("我知道了", new QMUIDialogAction.ActionListener() {
            @Override
            public void onClick(QMUIDialog dialog, int index) {
                dialog.dismiss();
            }
        });
        builder.setLeftAction("不再提示", new QMUIDialogAction.ActionListener() {
            @Override
            public void onClick(QMUIDialog dialog, int index) {
                dialog.dismiss();
                dataManager.setNeverAskForWatchDownloadTip(true);
            }
        });
        builder.show();
    }

    @Override
    public void errorParseVideoUrl(String errorMessage) {
        dismissDialog();
        isVideoError = true;
        helper.showError();
        LoadHelperUtils.setErrorText(helper.getLoadError(), R.id.tv_error_text, "解析视频地址失败，点击重试");
        showMessage(errorMessage, TastyToast.ERROR);
    }

    @Override
    public void favoriteSuccess() {
        dataManager.setFavoriteNeedRefresh(true);
        showMessage("收藏成功", TastyToast.SUCCESS);
    }

    @Override
    public void setVideoCommentData(List<VideoComment> videoCommentList, boolean pullToRefresh) {
        if (pullToRefresh) {
            recyclerViewVideoComment.smoothScrollToPosition(0);
        }
        videoCommentAdapter.setNewData(videoCommentList);
        commentSwipeRefreshLayout.setEnabled(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fab.show();
            }
        }, 300);
    }

    @Override
    public void setMoreVideoCommentData(List<VideoComment> videoCommentList) {
        videoCommentAdapter.loadMoreComplete();
        videoCommentAdapter.addData(videoCommentList);
    }

    @Override
    public void noMoreVideoCommentData(String message) {
        videoCommentAdapter.loadMoreEnd(true);
        //showMessage(message, TastyToast.INFO);
    }

    @Override
    public void loadMoreVideoCommentError(String message) {
        videoCommentAdapter.loadMoreFail();
    }

    @Override
    public void loadVideoCommentError(String message) {
        isVideoError = false;
        helper.showError();
        LoadHelperUtils.setErrorText(helper.getLoadError(), R.id.tv_error_text, "加载评论失败，点击重试");
        //showMessage(message, TastyToast.Error);
    }

    @Override
    public void commentVideoSuccess(String message) {
        cleanVideoCommentInput();
        reFreshData();
        showMessage(message, TastyToast.SUCCESS);
    }

    @Override
    public void commentVideoError(String message) {
        showMessage(message, TastyToast.ERROR);
    }

    @Override
    public void replyVideoCommentSuccess(String message) {
        cleanVideoCommentInput();
        isComment = true;
        etVideoComment.setHint(R.string.comment_video_hint_tip);
        videoCommentAdapter.setClickPosition(-1);
        reFreshData();
        showMessage(message, TastyToast.SUCCESS);
    }

    private void reFreshData() {
        if (unLimit91PornItem.getVideoResultId() == 0) {
            return;
        }
        //刷新
        commentSwipeRefreshLayout.setRefreshing(true);
        String videoId = unLimit91PornItem.getVideoResult().getVideoId();
        presenter.loadVideoComment(videoId, unLimit91PornItem.getViewKey(), true);
    }

    @Override
    public void replyVideoCommentError(String message) {
        showMessage(message, TastyToast.ERROR);
    }

    private void cleanVideoCommentInput() {
        etVideoComment.setText("");
    }

    @Override
    public void showError(String message) {
        showMessage(message, TastyToast.ERROR);
        dismissDialog();
    }

    @Override
    public void showLoading(boolean pullToRefresh) {
        helper.showLoading();
        LoadHelperUtils.setLoadingText(helper.getLoadIng(), R.id.tv_loading_text, "拼命加载评论中...");
    }

    @Override
    public void showContent() {
        if (videoCommentAdapter.getData().size() == 0) {
            isVideoError = false;
            helper.showEmpty();
            LoadHelperUtils.setEmptyText(helper.getLoadEmpty(), R.id.tv_empty_info, "暂无评论");
        } else {
            flLoadHolder.setVisibility(View.GONE);
            helper.showContent();
        }
        commentSwipeRefreshLayout.setRefreshing(false);
        dismissDialog();
    }

    @Override
    public void showMessage(String msg, int type) {
        super.showMessage(msg, type);
        dismissDialog();
    }

    private void dismissDialog() {
        if (mAlertDialog != null && mAlertDialog.isShowing() && !isFinishing()) {
            mAlertDialog.dismiss();
        }
        if (favoriteDialog != null && favoriteDialog.isShowing() && !isFinishing()) {
            favoriteDialog.dismiss();
        }
        if (commentVideoDialog != null && commentVideoDialog.isShowing() && !isFinishing()) {
            commentVideoDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.play_video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_play_collect) {
            favoriteVideo();
            return true;
        } else if (id == R.id.menu_play_download) {
            startDownloadVideo();
            return true;
        } else if (id == R.id.menu_play_share) {
            shareVideoUrl();
            return true;
        } else if (id == R.id.menu_play_comment) {
            showCommentLayout();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fab.hide();
                }
            }, 500);
            return true;
        } else if (id == R.id.menu_play_close) {
            floatingToolbar.hide();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void startDownloadVideo() {
        boolean isDownloadNeedWifi = dataManager.isDownloadVideoNeedWifi();
        presenter.downloadVideo(unLimit91PornItem, isDownloadNeedWifi, false);
        Intent intent = new Intent(this, DownloadVideoService.class);
        startService(intent);
    }

    private void favoriteVideo() {
        if (unLimit91PornItem == null || unLimit91PornItem.getVideoResultId() == 0) {
            showMessage("解析完视频才能收藏哦", TastyToast.WARNING);
            return;
        }
        VideoResult videoResult = unLimit91PornItem.getVideoResult();
        if (!UserHelper.isUserInfoComplete(user)) {
            goToLogin();
            showMessage("请先登录", TastyToast.SUCCESS);
            return;
        }
        if (Integer.parseInt(videoResult.getOwnnerId()) == user.getUserId()) {
            showMessage("不能收藏自己的视频哦", TastyToast.INFO);
            return;
        }
        favoriteDialog.show();
        presenter.favorite(String.valueOf(user.getUserId()), videoResult.getVideoId(), videoResult.getOwnnerId());
    }

    private void shareVideoUrl() {
        if (unLimit91PornItem == null || unLimit91PornItem.getVideoResultId() == 0) {
            showMessage("解析完视频才能分享哦", TastyToast.WARNING);
            return;
        }
        String url = unLimit91PornItem.getVideoResult().getVideoUrl();
        if (TextUtils.isEmpty(url)) {
            showMessage("解析完视频才能分享哦", TastyToast.WARNING);
            return;
        }
        Intent textIntent = new Intent(Intent.ACTION_SEND);
        textIntent.setType("text/plain");
        textIntent.putExtra(Intent.EXTRA_TEXT, "链接：" + url);
        startActivity(Intent.createChooser(textIntent, "分享视频地址"));
    }

    private void goToLogin() {
        Intent intent = new Intent(this, UserLoginActivity.class);
        startActivityWithAnimotion(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == AuthorActivity.AUTHORACTIVITY_RESULT_CODE) {
            unLimit91PornItem = (UnLimit91PornItem) data.getSerializableExtra(Keys.KEY_INTENT_UNLIMIT91PORNITEM);
            recyclerViewVideoComment.smoothScrollToPosition(0);
            videoCommentAdapter.getData().clear();
            videoCommentAdapter.notifyDataSetChanged();
            initData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
            //这里没必要，因为我们使用的是setColorForSwipeBack，并不会有这个虚拟的view，而是设置的padding
            StatusBarUtil.hideFakeStatusBarView(this);
        } else if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
    }
}
