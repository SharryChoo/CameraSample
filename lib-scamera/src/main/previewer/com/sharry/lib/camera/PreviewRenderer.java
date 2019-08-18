package com.sharry.lib.camera;

import android.content.Context;
import android.opengl.EGLContext;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.glGetUniformLocation;

/**
 * @author Sharry <a href="sharrychoochn@gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2019-07-28
 */
public class PreviewRenderer implements IPreviewerRenderer {

    private static final String TAG = PreviewRenderer.class.getSimpleName();

    private final float[] mVertexCoordinate = new float[]{
            -1f, 1f,  // 左上
            -1f, -1f, // 左下
            1f, 1f,   // 右上
            1f, -1f   // 右下
    };
    private final float[] mTextureCoordinate = new float[]{
            0f, 1f,   // 左上
            0f, 0f,   // 左下
            1f, 1f,   // 右上
            1f, 0f    // 右下
    };
    private final FloatBuffer mVertexBuffer = GlUtil.createFloatBuffer(mVertexCoordinate);
    private final FloatBuffer mTextureBuffer = GlUtil.createFloatBuffer(mTextureCoordinate);
    private final Context mContext;
    private final PreviewerFramebufferRenderer mFramebufferRenderer;
    private EGLContext mEglContext;

    /**
     * 着色器相关
     */
    private int mProgram;
    private int aVertexCoordinate;
    private int aTextureCoordinate;
    private int uTextureMatrix;
    private int uVertexMatrix;
    private int uTexture;

    /**
     * Vertex buffer object 相关
     */
    private int mVboId;

    /**
     * Matrix
     */
    private final float[] mProjectionMatrix = new float[16];      // 投影矩阵
    private final float[] mRotationMatrix = new float[16];        // 裁剪矩阵
    private final float[] mFinalMatrix = new float[16];           // 裁剪矩阵

    PreviewRenderer(Context context) {
        mContext = context;
        mFramebufferRenderer = new PreviewerFramebufferRenderer();
    }

    @Override
    public void onEglContextCreated(EGLContext eglContext) {
        this.mFramebufferRenderer.onEglContextCreated();
        this.mEglContext = eglContext;
        // 上下文变更了, 重置数据
        reset();
        // 配置着色器
        setupShaders();
        // 配置顶点和纹理坐标
        setupCoordinates();
    }

    @Override
    public void onSurfaceSizeChanged(int width, int height) {
        mFramebufferRenderer.onSurfaceSizeChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void drawTexture(int OESTextureId, float[] textureMatrix) {
        mFramebufferRenderer.bindFramebuffer();
        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        // 激活着色器
        GLES20.glUseProgram(mProgram);
        // 绑定纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, OESTextureId);

        /*
         顶点着色器
         */
        // 顶点坐标赋值
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        GLES20.glEnableVertexAttribArray(aVertexCoordinate);
        GLES20.glVertexAttribPointer(aVertexCoordinate, 2, GL_FLOAT, false,
                8, 0);
        // 纹理坐标赋值
        GLES20.glEnableVertexAttribArray(aTextureCoordinate);
        GLES20.glVertexAttribPointer(aTextureCoordinate, 2, GL_FLOAT, false,
                8, mVertexCoordinate.length * 4);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        // 顶点变换矩阵赋值
        GLES20.glUniformMatrix4fv(uVertexMatrix, 1, false, mFinalMatrix, 0);
        // 纹理变换矩阵赋值
        GLES20.glUniformMatrix4fv(uTextureMatrix, 1, false, textureMatrix, 0);

        /*
         片元着色器, 为 uTexture 赋值
         */
        GLES20.glUniform1i(uTexture, 0);

        // 执行渲染管线
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 解绑纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        // 解绑 fbo
        mFramebufferRenderer.unbindFramebuffer();

        // 输出到屏幕上
        mFramebufferRenderer.drawToDisplay();
    }

    @Override
    public EGLContext getEGLContext() {
        return mEglContext;
    }

    @Override
    public int getPreviewerTextureId() {
        return mFramebufferRenderer.getFboTextureId();
    }

    @Override
    public void resetMatrix() {
        Matrix.setIdentityM(mProjectionMatrix, 0);
        Matrix.setIdentityM(mRotationMatrix, 0);
        Matrix.setIdentityM(mFinalMatrix, 0);
    }

    @Override
    public void rotate(int degrees) {
        Matrix.rotateM(mRotationMatrix, 0, degrees, 0, 0, 1);
    }

    @Override
    public void centerCrop(boolean isLandscape, Size surfaceSize, Size textureSize) {
        // 设置正交投影
        float aspectPlane = surfaceSize.getWidth() / (float) surfaceSize.getHeight();
        float aspectTexture = isLandscape ? textureSize.getWidth() / (float) textureSize.getHeight()
                : textureSize.getHeight() / (float) textureSize.getWidth();
        float left, top, right, bottom;
        // 1. 纹理比例 > 投影平面比例
        if (aspectTexture > aspectPlane) {
            left = -aspectPlane / aspectTexture;
            right = -left;
            top = 1;
            bottom = -1;
        }
        // 2. 纹理比例 < 投影平面比例
        else {
            left = -1;
            right = 1;
            top = 1 / aspectPlane * aspectTexture;
            bottom = -top;
        }
        Matrix.orthoM(
                mProjectionMatrix, 0,
                left, right, bottom, top,
                1, -1
        );
        Log.e(TAG, "preview size = " + surfaceSize + ", camera size = " + textureSize);
    }

    @Override
    public void transformMatrix() {
        // 使裁剪矩阵合并旋转矩阵
        Matrix.multiplyMM(mFinalMatrix, 0, mProjectionMatrix, 0,
                mRotationMatrix, 0);
    }

    private void reset() {
        this.mProgram = 0;
        this.mVboId = 0;
    }

    private void setupShaders() {
        if (mProgram != 0) {
            return;
        }
        // 加载着色器
        String vertexSource = GlUtil.getGLResource(mContext, R.raw.camera_vertex_shader);
        String fragmentSource = GlUtil.getGLResource(mContext, R.raw.camera_fragment_shader);
        mProgram = GlUtil.createProgram(vertexSource, fragmentSource);
        // 加载 Program 中的变量
        aVertexCoordinate = GLES20.glGetAttribLocation(mProgram, "aVertexCoordinate");
        aTextureCoordinate = GLES20.glGetAttribLocation(mProgram, "aTextureCoordinate");
        uVertexMatrix = glGetUniformLocation(mProgram, "uVertexMatrix");
        uTextureMatrix = glGetUniformLocation(mProgram, "uTextureMatrix");
        uTexture = glGetUniformLocation(mProgram, "uTexture");
    }

    private void setupCoordinates() {
        if (mVboId != 0) {
            return;
        }
        // 创建 vbo
        int vboSize = 1;
        int[] vboIds = new int[vboSize];
        GLES20.glGenBuffers(vboSize, vboIds, 0);
        // 将顶点坐标写入 vbo
        mVboId = vboIds[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVboId);
        // 开辟 VBO 空间
        GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                (mVertexCoordinate.length + mTextureCoordinate.length) * 4,
                null,
                GLES20.GL_STATIC_DRAW
        );
        // 写入顶点坐标
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER,
                0,
                (mVertexCoordinate.length) * 4,
                mVertexBuffer
        );
        // 写入纹理坐标
        GLES20.glBufferSubData(
                GLES20.GL_ARRAY_BUFFER,
                (mVertexCoordinate.length) * 4,
                (mTextureCoordinate.length) * 4,
                mTextureBuffer
        );
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

}
