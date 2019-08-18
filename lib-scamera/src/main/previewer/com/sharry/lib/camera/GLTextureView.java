package com.sharry.lib.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import javax.microedition.khronos.opengles.GL10;

/**
 * 利用 TextureView 实现对外来 SurfaceTexture 的加工绘制
 *
 * @author Sharry <a href="sharrychoochn@gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2019-07-27
 */
public class GLTextureView extends TextureView {

    private static final String TAG = GLTextureView.class.getSimpleName();

    /**
     * 渲染器
     */
    protected ITextureRenderer mRenderer;

    /**
     * 用于渲染的纹理
     */
    private SurfaceTexture mBufferTexture;

    /**
     * 用于渲染的线程
     */
    private RendererThread mRendererThread;

    public GLTextureView(Context context) {
        this(context, null);
    }

    public GLTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(new SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (mRendererThread != null) {
                    Log.e(TAG, "Renderer thread already launched.");
                    return;
                }
                // do launch
                mRendererThread = new RendererThread("Renderer Thread",
                        new WeakReference<>(GLTextureView.this));
                mRendererThread.start();
                // invoke renderer lifecycle sequence.
                if (mRenderer != null) {
                    mRendererThread.handleRenderChanged();
                }
                mRendererThread.handleSizeChanged();
                if (mBufferTexture != null) {
                    mRendererThread.handleTextureChanged();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                if (mRendererThread != null) {
                    mRendererThread.handleSizeChanged();
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mRendererThread != null) {
                    mRendererThread.quitSafely();
                    mRendererThread = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }

        });
    }

    /**
     * 设置渲染器
     */
    public void setRenderer(@NonNull IPreviewerRenderer renderer) {
        if (mRenderer == renderer) {
            return;
        }
        this.mRenderer = renderer;
        if (mRendererThread != null) {
            mRendererThread.handleRenderChanged();
        }
    }

    /**
     * 设置外部纹理数据
     */
    public void setBufferTexture(@NonNull SurfaceTexture dataSource) {
        if (mBufferTexture == dataSource) {
            Log.i(TAG, "Data source not changed.");
            return;
        }
        // update data source
        this.mBufferTexture = dataSource;
        if (mRendererThread != null) {
            mRendererThread.handleTextureChanged();
        }
    }

    private static class RendererThread extends HandlerThread
            implements SurfaceTexture.OnFrameAvailableListener, Handler.Callback {

        private static final int MSG_CREATE_EGL_CONTEXT = 0;
        private static final int MSG_RENDERER_CHANGED = 1;
        private static final int MSG_SURFACE_SIZE_CHANGED = 2;
        private static final int MSG_TEXTURE_CHANGED = 3;
        private static final int MSG_DRAW_FRAME = 4;

        private final WeakReference<GLTextureView> mWkRef;
        private final float[] mTextureMatrix = new float[16];
        private final EglCore mEglCore = new EglCore();
        private int mOESTextureId;
        private Handler mRendererHandler;

        private RendererThread(String name, WeakReference<GLTextureView> view) {
            super(name);
            mWkRef = view;
        }

        @Override
        public synchronized void start() {
            super.start();
            mRendererHandler = new Handler(getLooper(), this);
            mRendererHandler.sendEmptyMessage(MSG_CREATE_EGL_CONTEXT);
        }

        @Override
        public boolean quitSafely() {
            release();
            return super.quitSafely();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                // 创建 EGL 上下文
                case MSG_CREATE_EGL_CONTEXT:
                    preformCreateEGL();
                    break;
                // 渲染器变更
                case MSG_RENDERER_CHANGED:
                    performRenderChanged();
                    break;
                // 画布尺寸变更
                case MSG_SURFACE_SIZE_CHANGED:
                    performSurfaceSizeChanged();
                    break;
                // 纹理变更
                case MSG_TEXTURE_CHANGED:
                    performTextureChanged();
                    break;
                // 绘制数据帧
                case MSG_DRAW_FRAME:
                    performDrawTexture();
                    break;
                default:
                    break;
            }
            return false;
        }

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (mRendererHandler != null) {
                mRendererHandler.sendEmptyMessage(MSG_DRAW_FRAME);
            }
        }

        void handleRenderChanged() {
            if (mRendererHandler != null) {
                mRendererHandler.sendEmptyMessage(MSG_RENDERER_CHANGED);
            }
        }

        void handleSizeChanged() {
            if (mRendererHandler != null) {
                mRendererHandler.sendEmptyMessage(MSG_SURFACE_SIZE_CHANGED);
            }
        }

        void handleTextureChanged() {
            if (mRendererHandler != null) {
                mRendererHandler.sendEmptyMessage(MSG_TEXTURE_CHANGED);
            }
        }

        private void preformCreateEGL() {
            GLTextureView view = mWkRef.get();
            if (view == null) {
                return;
            }
            // Create egl context
            mEglCore.initialize(view.getSurfaceTexture(), null);
        }

        private void performRenderChanged() {
            GLTextureView view = mWkRef.get();
            if (view == null) {
                return;
            }
            view.mRenderer.onEglContextCreated(mEglCore.getContext());
        }

        private void performSurfaceSizeChanged() {
            GLTextureView view = mWkRef.get();
            if (view == null) {
                return;
            }
            ITextureRenderer renderer = view.mRenderer;
            renderer.onSurfaceSizeChanged(view.getWidth(), view.getHeight());
        }

        private void performTextureChanged() {
            // 为这个纹理绑定 textureId
            GLTextureView view = mWkRef.get();
            if (view == null) {
                return;
            }
            // 更新纹理数据
            SurfaceTexture bufferTexture = view.mBufferTexture;
            try {
                // 确保这个 Texture 没有绑定其他的纹理 id
                bufferTexture.detachFromGLContext();
            } catch (Throwable e) {
                // ignore.
            } finally {
                /*
                 CameraX 切换摄像头返回新的 SurfaceTexture 时, 会导致 SurfaceTexture 的 transform matrix 旋转角度改变, 从而引发跳闪
                 这里通过创建新的 textureId 解决
                */
                // 创建纹理
                mOESTextureId = createOESTextureId();
                // 绑定纹理
                bufferTexture.attachToGLContext(mOESTextureId);
                // 设置监听器
                bufferTexture.setOnFrameAvailableListener(this);
            }
        }

        private void performDrawTexture() {
            GLTextureView view = mWkRef.get();
            if (view == null) {
                return;
            }
            // 设置当前的环境
            mEglCore.makeCurrent();
            // 更新纹理数据
            SurfaceTexture bufferTexture = view.mBufferTexture;
            ITextureRenderer renderer = view.mRenderer;
            if (bufferTexture != null) {
                bufferTexture.updateTexImage();
                bufferTexture.getTransformMatrix(mTextureMatrix);
            }
            // 执行渲染器的绘制
            if (renderer != null) {
                renderer.drawTexture(mOESTextureId, mTextureMatrix);
            }
            // 将 EGL 绘制的数据, 输出到 View 的 preview 中
            mEglCore.swapBuffers();
        }

        private int createOESTextureId() {
            int[] tex = new int[1];
            GLES20.glGenTextures(1, tex, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MIN_FILTER, (float) GL10.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MAG_FILTER, (float) GL10.GL_LINEAR);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_S, (float) GL10.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_T, (float) GL10.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            return tex[0];
        }

        private void release() {
            if (mRendererHandler != null) {
                mRendererHandler.removeMessages(MSG_CREATE_EGL_CONTEXT);
                mRendererHandler.removeMessages(MSG_SURFACE_SIZE_CHANGED);
                mRendererHandler.removeMessages(MSG_TEXTURE_CHANGED);
                mRendererHandler.removeMessages(MSG_DRAW_FRAME);
            }
            mEglCore.release();
        }

    }

}
