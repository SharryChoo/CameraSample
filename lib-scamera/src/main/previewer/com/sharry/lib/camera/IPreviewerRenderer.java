package com.sharry.lib.camera;

import android.opengl.EGLContext;

import androidx.annotation.UiThread;

/**
 * 相机预览器的 Renderer
 * <p>
 * 对 ITextureRenderer 的增强, 拓展 matrix 功能
 *
 * @author Sharry <a href="sharrychoochn@gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2019-07-27
 */
public interface IPreviewerRenderer extends ITextureRenderer {

    @UiThread
    EGLContext getEGLContext();

    @UiThread
    int getPreviewerTextureId();

    @UiThread
    void resetMatrix();

    @UiThread
    void rotate(int degrees);

    @UiThread
    void centerCrop(boolean isLandscape, Size surfaceSize, Size textureSize);

    @UiThread
    void transformMatrix();

}
