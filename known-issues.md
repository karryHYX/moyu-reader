# 已知问题与后续边界

1. **物理机性能复测**：模拟器已完成三尺寸三轮视觉回归、Startup benchmark 与 profile 采集；最终 TTID、FrameTiming 和峰值 PSS 仍以发布物理机为准。
2. **100 MB 设备端 PSS**：JVM 512 MiB test worker 已在 1.9 秒完成 100 MiB 流式解析；Android 设备端导入耗时、PSS 与搜索耗时仍需 release/benchmark 变体复测。
3. **EPUB CSS 版式不保留**：V1 提取语义正文并用墨屿 Reader 排版；复杂表格、数学公式、脚注弹窗和媒体 overlay 不在 V1 范围。
4. **DRM EPUB 不处理**：产品边界内明确排除。
5. **笔记/高亮编辑 UI**：数据库 schema 已保留 annotations，V1 核心交付先完成书签与搜索定位；完整标注编辑器列入后续版本。
6. **自动滚动**：分页点击/横滑、连续滚动与 Android 本地 TTS 已完成；自动滚动仍在后续范围。
7. **系统 TTS 语音包**：朗读依赖设备已安装的 Android TTS 引擎与中文语音包；App 会在面板内显示就绪状态。
8. **应用商店渠道审核**：Release APK 已使用项目固定证书签名；不同品牌应用商店仍可能要求各自的开发者上架审核。

