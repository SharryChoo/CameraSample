# CameraSample
相机预览的 Sample

# 相机引擎
- Camera1
- CameraX

# 渲染视图
TextureView + OpenGL ES 2.0

# 实现功能
- 根据比例选取最合适的预览分辨率
- 根据比例计算 TextureView 的宽高
- 支持渲染数据的 CenterCrop 的全屏展示
- 支持横竖屏切换渲染效果的统一
- Renderer 中添加了缓冲帧 FBO
