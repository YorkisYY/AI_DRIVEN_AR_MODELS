package com.example.ibm_ai_weather_art_android;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.filament.Filament; 
import com.example.ibm_ai_weather_art_android.model.GLBReader;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    private boolean isVuforiaInitialized() {
    return vuforiaCoreManager != null && vuforiaCoreManager.isVuforiaInitialized();
}

    
    static {
        Filament.init();
        try {
            System.loadLibrary("gltfio-jni");
        } catch (UnsatisfiedLinkError e) {
            Log.e("MainActivity", "gltfio library not found", e);
        }
    }
    
    static {
        try {
            System.loadLibrary("Vuforia");
            Log.d("MainActivity", "✅ libVuforia.so loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e("MainActivity", "❌ Failed to load libVuforia.so: " + e.getMessage());
        }
    }

    // ✅ 修正：統一使用一個 VuforiaCoreManager 實例
    private VuforiaCoreManager vuforiaCoreManager;
    private FilamentRenderer filamentRenderer;
    private GLBReader glbReader;
    
    // UI 組件
    private FrameLayout cameraContainer;
    private SurfaceView filamentSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // ✅ 修正：只創建一次，傳入 Activity 實例
        vuforiaCoreManager = new VuforiaCoreManager(this);
        
        // 初始化視圖
        initViews();
        
        // 初始化核心組件
        initializeComponents();
        
        // ✅ 強制每次都重新請求相機權限（解決 Vuforia 11.3 Bug）
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    private void initializeComponents() {
        try {
            Log.d(TAG, "Initializing core components...");
            
            // ✅ 修正：不重複創建，直接使用已有的實例
            // vuforiaCoreManager 已經在 onCreate 中創建了
            
            // 初始化 Filament 渲染器
            filamentRenderer = new FilamentRenderer(this);
            
            // 初始化 GLB 讀取器
            glbReader = new GLBReader(this);
            
            // 設定回調
            setupVuforiaCallbacks();
            
            Log.d(TAG, "Core components initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing components", e);
        }
    }
    
    private void setupVuforiaCallbacks() {
        // 初始化回調
        vuforiaCoreManager.setInitializationCallback(new VuforiaCoreManager.InitializationCallback() {
            @Override
            public void onVuforiaInitialized(boolean success) {
                if (success) {
                    Log.d(TAG, "✅ Vuforia initialized successfully");
                    loadGLBModel();
                } else {
                    Log.e(TAG, "❌ Vuforia initialization failed");
                    showError("Vuforia 初始化失敗");
                }
            }
        });
        
        // 模型載入回調
        vuforiaCoreManager.setModelLoadingCallback(new VuforiaCoreManager.ModelLoadingCallback() {
            @Override
            public void onModelLoaded(boolean success) {
                Log.d(TAG, "📦 GLB model loaded: " + (success ? "成功" : "失敗"));
                if (success) {
                    startARSession();
                } else {
                    showError("3D 模型載入失敗");
                }
            }
        });
        
        // 目標檢測回調
        vuforiaCoreManager.setTargetDetectionCallback(new VuforiaCoreManager.TargetDetectionCallback() {
            @Override
            public void onTargetFound(String targetName) {
                Log.d(TAG, "🎯 Target found: " + targetName);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "發現目標: " + targetName, Toast.LENGTH_SHORT).show();
                });
                
                // 觸發 Filament 渲染
                if (filamentRenderer != null) {
                    // 顯示 3D 模型
                }
            }
            
            @Override
            public void onTargetLost(String targetName) {
                Log.d(TAG, "❌ Target lost: " + targetName);
                // 隱藏 3D 模型
            }
            
            @Override
            public void onTargetTracking(String targetName, float[] modelViewMatrix) {
                Log.d(TAG, "📡 Target tracking: " + targetName);
                // 更新 3D 模型位置
                if (filamentRenderer != null) {
                    // 更新變換矩陣
                }
            }
        });
    }
    
    private void initViews() {
        Log.d(TAG, "初始化視圖組件");
        
        cameraContainer = findViewById(R.id.cameraContainer);
        if (cameraContainer == null) {
            Log.e(TAG, "Camera container not found");
            return;
        }
        
        filamentSurface = findViewById(R.id.filamentSurface);
        if (filamentSurface == null) {
            Log.e(TAG, "Filament SurfaceView not found");
            return;
        }
        
        Log.d(TAG, "視圖組件初始化完成");
    }
    
    private void initializeAR() {
        Log.d(TAG, "開始初始化 AR 系統");
        
        try {
            // 1. 設定 Filament Surface
            if (filamentRenderer != null && filamentSurface != null) {
                filamentRenderer.setupSurface(filamentSurface);
            }
            
            // 2. 檢查 GLB 檔案
            checkGLBFiles();
            
            // 3. 初始化 Vuforia (會觸發 onVuforiaInitialized 回調)
            vuforiaCoreManager.setupVuforia();
            
        } catch (Exception e) {
            Log.e(TAG, "AR 初始化錯誤: " + e.getMessage());
            showError("AR 初始化失敗");
        }
    }
    
    private void checkGLBFiles() {
        String[] glbFiles = glbReader.listAvailableGLBFiles();
        Log.d(TAG, "可用的 GLB 檔案: " + java.util.Arrays.toString(glbFiles));
        
        GLBReader.GLBFileInfo giraffeInfo = glbReader.getGLBFileInfo("giraffe_voxel.glb");
        Log.d(TAG, "長頸鹿模型資訊: " + giraffeInfo.fileName + " - 有效: " + giraffeInfo.isValid + " - 大小: " + giraffeInfo.fileSize);
    }
    
    private void loadGLBModel() {
        try {
            GLBReader.GLBFileInfo glbInfo = glbReader.getGLBFileInfo("giraffe_voxel.glb");
            if (glbInfo.isValid) {
                Log.d(TAG, "Loading GLB model: " + glbInfo.fileName);
                vuforiaCoreManager.loadGiraffeModel(); // 會觸發 onModelLoaded 回調
            } else {
                showError("GLB 檔案無效: " + glbInfo.fileName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading GLB model", e);
            showError("GLB 模型載入錯誤");
        }
    }
    
    private void startARSession() {
        try {
            Log.d(TAG, "Starting AR session...");
            
            // 啟動目標檢測
            if (vuforiaCoreManager.startTargetDetection()) {
                Log.d(TAG, "🎉 AR 會話啟動成功！");
                Toast.makeText(this, "AR 系統就緒", Toast.LENGTH_SHORT).show();
            } else {
                showError("目標檢測啟動失敗");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting AR session", e);
            showError("AR 會話啟動失敗");
        }
    }
    
    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }
    
    // ==================== 生命週期管理 ====================
    
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause - 暫停 AR 組件");
        
        // ✅ 修正：統一使用 vuforiaCoreManager
        try {
            if (vuforiaCoreManager != null) {
                vuforiaCoreManager.pauseVuforia();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during Vuforia pause", e);
        }
    }
    // 替换您的 onResume 方法为以下代码：

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - 恢復 AR 組件");
        
        // ✅ 修正：如果 Vuforia 尚未初始化且有权限，尝试初始化
        if (allPermissionsGranted() && !isVuforiaInitialized()) {
            Log.d(TAG, "🔄 Vuforia not initialized, attempting initialization in onResume");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!isVuforiaInitialized()) {  // 双重检查
                    Log.d(TAG, "🚀 Starting delayed AR initialization from onResume");
                    initializeAR();
                }
            }, 500);
        }
    
    // ✅ 恢复已初始化的 Vuforia
    try {
        if (vuforiaCoreManager != null && isVuforiaInitialized()) {
            vuforiaCoreManager.resumeVuforia();
        }
    } catch (Exception e) {
        Log.e(TAG, "Error during Vuforia resume", e);
    }
}
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy - 清理 AR 資源");
        
        // ✅ 修正：統一使用 vuforiaCoreManager
        if (vuforiaCoreManager != null) {
            vuforiaCoreManager.cleanupManager();
        }
        
        if (filamentRenderer != null) {
            filamentRenderer.destroy();
        }
    }

    // ==================== 權限檢查 ====================
    
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // ✅ 修正：添加延迟初始化 - Vuforia 11.x workaround
                Log.d(TAG, "🔄 Camera permission granted, delayed AR initialization...");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "🚀 Starting delayed AR initialization (Vuforia 11.x workaround)");
                    initializeAR();
                }, 1000); // 延迟 1 秒让系统处理权限状态
            } else {
                Toast.makeText(this, "需要相机权限", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    }