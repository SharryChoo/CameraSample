package com.sharry.lib.camera;

import android.opengl.EGLContext;

import androidx.annotation.WorkerThread;

/**
 * OpenGL ES 基础的 Texture Renderer
 *
 * @author Sharry <a href="xiaoyu.zhu@1hai.cn">Contact me.</a>
 * @version 1.0
 * @since 2019-08-08 14:10
 */
public interface ITextureRenderer {

    @WorkerThread
    void onEglContextCreated(EGLContext eglContext);

    @WorkerThread
    void onSurfaceSizeChanged(int width, int height);

    @WorkerThread
    void drawTexture(int textureId, float[] textureMatrix);

}
