package com.dante.ui.notice;

import com.dante.ui.update.IBaseUpdate;

/**
 * @author flymegoc
 * @date 2018/1/26
 */

public interface IBaseNotice extends IBaseUpdate {
    void checkNewNotice(int versionCode);
}
