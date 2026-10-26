package com.edirear.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefresh;
    private LinearLayout mErrorView;
    private FrameLayout mCustomViewContainer;

    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;

    private ValueCallback<Uri[]> mFilePathCallback;
    private Uri mCameraPhotoUri;
    private Uri mCameraVideoUri;

    private long mLastBackPressTime = 0;
    private String mCurrentUrl = Config.DEFAULT_URL;

    // ActivityResultLauncher for handling modern file chooser
    private final ActivityResultLauncher<Intent> mFileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (mFilePathCallback == null) return;
                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && (data.getData() != null || data.getClipData() != null)) {
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            results = new Uri[count];
                            for (int i = 0; i < count; i++) {
                                results[i] = data.getClipData().getItemAt(i).getUri();
                            }
                        } else if (data.getData() != null) {
                            results = new Uri[]{data.getData()};
                        }
                    } else {
                        // Check if photo was captured
                        if (mCameraPhotoUri != null) {
                            results = new Uri[]{mCameraPhotoUri};
                        } else if (mCameraVideoUri != null) {
                            results = new Uri[]{mCameraVideoUri};
                        }
                    }
                }
                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
                mCameraPhotoUri = null;
                mCameraVideoUri = null;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        setupSwipeRefresh();
        setupBackNavigation();
        checkAndRequestPermissions();

        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            loadAppUrl(mCurrentUrl);
        }
    }

    private void initViews() {
        mWebView = findViewById(R.id.webView);
        mProgressBar = findViewById(R.id.progressBar);
        mSwipeRefresh = findViewById(R.id.swipeRefreshLayout);
        mErrorView = findViewById(R.id.errorView);
        mCustomViewContainer = findViewById(R.id.customViewContainer);
        Button btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(v -> {
            mErrorView.setVisibility(View.GONE);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.reload();
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptThirdPartyCookies(mWebView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        String userAgent = settings.getUserAgentString();
        settings.setUserAgentString(userAgent + Config.USER_AGENT_SUFFIX);

        mWebView.setWebViewClient(new EdirearWebViewClient());
        mWebView.setWebChromeClient(new EdirearWebChromeClient());

        // File download listener
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file from " + Config.APP_NAME);
                    request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));

                    DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(getApplicationContext(), "Download started...", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        mSwipeRefresh.setColorSchemeResources(R.color.accent_green);
        mSwipeRefresh.setProgressBackgroundColorSchemeResource(R.color.card_dark);
        mSwipeRefresh.setOnRefreshListener(() -> mWebView.reload());
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mCustomView != null) {
                    // Exit fullscreen video
                    hideCustomView();
                } else if (mWebView.canGoBack()) {
                    mWebView.goBack();
                } else {
                    if (System.currentTimeMillis() - mLastBackPressTime < 2000) {
                        finish();
                    } else {
                        mLastBackPressTime = System.currentTimeMillis();
                        Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void loadAppUrl(String url) {
        if (!isNetworkAvailable()) {
            showError();
            return;
        }
        mWebView.loadUrl(url);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void showError() {
        mSwipeRefresh.setRefreshing(false);
        mProgressBar.setVisibility(View.GONE);
        mWebView.setVisibility(View.GONE);
        mErrorView.setVisibility(View.VISIBLE);
    }

    private void checkAndRequestPermissions() {
        List<String> list = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            list.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                list.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (!list.isEmpty()) {
            ActivityCompat.requestPermissions(this, list.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    // ==========================================
    // WebChromeClient: Uploads, Fullscreen Video, Permissions
    // ==========================================
    private class EdirearWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                mProgressBar.setVisibility(View.GONE);
                mSwipeRefresh.setRefreshing(false);
            } else {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setProgress(newProgress);
            }
        }

        // WebRTC Camera/Microphone Permission Handler for Live Streaming
        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            });
        }

        // Modern File Chooser for Video, Photo, and File Uploads
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = null;
            Intent takeVideoIntent = null;

            boolean allowCamera = true;
            if (fileChooserParams.getAcceptTypes() != null) {
                for (String type : fileChooserParams.getAcceptTypes()) {
                    if (type.contains("video")) {
                        takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        try {
                            File videoFile = createMediaFile(".mp4");
                            mCameraVideoUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", videoFile);
                            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraVideoUri);
                        } catch (IOException e) {
                            takeVideoIntent = null;
                        }
                    } else if (type.contains("image")) {
                        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        try {
                            File photoFile = createMediaFile(".jpg");
                            mCameraPhotoUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", photoFile);
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraPhotoUri);
                        } catch (IOException e) {
                            takePictureIntent = null;
                        }
                    }
                }
            }

            Intent contentSelectionIntent = fileChooserParams.createIntent();
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

            List<Intent> intentList = new ArrayList<>();
            if (takePictureIntent != null) intentList.add(takePictureIntent);
            if (takeVideoIntent != null) intentList.add(takeVideoIntent);

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select Upload File");
            if (!intentList.isEmpty()) {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toArray(new Intent[0]));
            }

            try {
                mFileChooserLauncher.launch(chooserIntent);
            } catch (Exception e) {
                mFilePathCallback = null;
                return false;
            }
            return true;
        }

        // HTML5 Fullscreen Video Handling
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (mCustomView != null) {
                onHideCustomView();
                return;
            }
            mCustomView = view;
            mCustomViewCallback = callback;
            mCustomViewContainer.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            mCustomViewContainer.setVisibility(View.VISIBLE);
            mSwipeRefresh.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        @Override
        public void onHideCustomView() {
            hideCustomView();
        }
    }

    private void hideCustomView() {
        if (mCustomView == null) return;
        mCustomViewContainer.removeView(mCustomView);
        mCustomView = null;
        mCustomViewContainer.setVisibility(View.GONE);
        mSwipeRefresh.setVisibility(View.VISIBLE);
        if (mCustomViewCallback != null) {
            mCustomViewCallback.onCustomViewHidden();
            mCustomViewCallback = null;
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private File createMediaFile(String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Edirear_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, extension, storageDir);
    }

    // ==========================================
    // WebViewClient: Navigation & Error Handling
    // ==========================================
    private class EdirearWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost();
            String defaultHost = Uri.parse(Config.DEFAULT_URL).getHost();

            if (host != null && (host.equalsIgnoreCase(defaultHost) || host.contains("localhost") || host.contains("192.168.") || host.contains("10.0.2.2"))) {
                // Internal route: stay inside app WebView
                return false;
            }

            // External URLs (mailto, tel, whatsapp, social links): open in system browser
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mProgressBar.setVisibility(View.GONE);
            mSwipeRefresh.setRefreshing(false);
            CookieManager.getInstance().flush();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showError();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }
}
