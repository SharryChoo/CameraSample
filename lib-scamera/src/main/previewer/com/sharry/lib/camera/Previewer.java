package com.sharry.lib.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

/**
 * Camera 预览器
 * <p>
 * 使用 TextureView 渲染硬件相机输出的 SurfaceTexture
 *
 * @author Sharry <a href="sharrychoochn@gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2019-04-24
 */
@SuppressLint("ViewConstructor")
public final class Previewer extends GLTextureView implements IPreviewer {

    private IPreviewerRenderer mRenderer;
    private Watcher mWatcher;

    Previewer(Context context, FrameLayout parent) {
        super(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        parent.addView(this, params);
        // set default renderer
        setRender(new PreviewRenderer(context));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mWatcher != null) {
            mWatcher.onSizeChanged(getWidth(), getHeight());
        }
    }

    @Override
    public void setDataSource(@NonNull SurfaceTexture dataSource) {
        super.setBufferTexture(dataSource);
    }

    @Override
    public void setRender(@NonNull IPreviewerRenderer renderer) {
        this.mRenderer = renderer;
        setRenderer(mRenderer);
        if (mWatcher != null) {
            mWatcher.onRenderChanged(mRenderer);
        }
    }

    @Override
    public void setWatcher(Watcher watcher) {
        this.mWatcher = watcher;
        // call at once.
        if (mWatcher != null) {
            mWatcher.onSizeChanged(getWidth(), getHeight());
            mWatcher.onRenderChanged(mRenderer);
        }
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public IPreviewerRenderer getRenderer() {
        return mRenderer;
    }

    @Override
    public Size getSize() {
        return new Size(getWidth(), getHeight());
    }

    @Override
    public Bitmap getBitmap() {
        return super.getBitmap();
    }
}
