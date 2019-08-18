package com.sharry.lib.camera;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * 相机预览器的抽象描述
 *
 * @author Sharry <a href="xiaoyu.zhu@1hai.cn">Contact me.</a>
 * @version 1.0
 * @since 2019-08-08 10:49
 */
public interface IPreviewer {

    /**
     * 设置要预览的数据源
     */
    void setDataSource(@NonNull SurfaceTexture bufferTexture);

    /**
     * 设置 previewer 的渲染器
     */
    void setRender(@NonNull IPreviewerRenderer renderer);

    /**
     * 设置监听器
     */
    void setWatcher(Watcher watcher);

    /**
     * 获取用于预览的 view
     */
    View getView();

    /**
     * 设置 previewer 的渲染器
     */
    IPreviewerRenderer getRenderer();

    /**
     * 获取渲染器的尺寸
     */
    Size getSize();

    Bitmap getBitmap();

    interface Watcher {

        void onSizeChanged(int previewerWidth, int previewerHeight);

        void onRenderChanged(IPreviewerRenderer renderer);

    }

}
