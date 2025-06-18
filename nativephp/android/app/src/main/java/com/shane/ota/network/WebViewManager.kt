package com.shane.ota.network

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.*
import android.widget.Toast
import com.acsbendi.requestinspectorwebview.RequestInspectorWebViewClient
import com.shane.ota.bridge.PHPBridge
import com.shane.ota.ui.MainActivity
import org.json.JSONObject
import com.shane.ota.security.LaravelSecurity

class WebViewManager(
    private val context: Context,
    private val webView: WebView,
    private val phpBridge: PHPBridge
) {
    private val TAG = "PHPMonitor"

    companion object {
        var shared: WebViewManager? = null
    }

    fun setup() {
        configureWebViewSettings()
        setupCookieManager()
        setupWebViewClient()
        setupJavaScriptInterfaces()
        WebViewManager.shared = this // 👈 make this instance globally accessible
    }

    private fun configureWebViewSettings() {
        webView.clearCache(true)
        webView.clearHistory()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadsImagesAutomatically = true
            blockNetworkImage = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        WebView.setWebContentsDebuggingEnabled(true)
    }

    private fun setupCookieManager() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    private fun setupWebViewClient() {
        webView.webChromeClient = createWebChromeClient()
        webView.webViewClient = createCustomWebViewClient()
    }

    private fun createWebChromeClient(): WebChromeClient {
        return object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "$TAG-Console",
                    "${consoleMessage.message()} -- From line ${consoleMessage.lineNumber()}"
                )
                return true
            }
        }
    }

    private fun createCustomWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            private val requestInspector = RequestInspectorWebViewClient(webView)
            private val phpHandler = PHPWebViewClient(phpBridge, context as MainActivity)

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val url = request.url.toString()
                val method = request.method
                Log.d("$TAG-DEBUG", "URL: $url, Method: $method")
                Log.d(TAG, "⬆️ shouldOverrideUrlLoading: $url")

                if (url.startsWith("nativephp://")) {
                    Log.d("WebView", "🔗 Intercepted deep link inside WebView: $url")

                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, "No app can handle this link", Toast.LENGTH_SHORT).show()
                    }

                    return true // prevent WebView from loading it
                }

                if ((url.startsWith("http://") || url.startsWith("https://")) &&
                    !url.contains("127.0.0.1") &&
                    !url.contains("localhost") &&
                    request.isForMainFrame
                ) {
                    // This is a navigation request to an external site - open in browser
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    view.context.startActivity(intent)
                    return true
                }

                // Handle relative URLs (convert to php://)
                if (url.startsWith("/")) {
                    val uri = request.url
                    val fullUrl = "http://127.0.0.1${uri.encodedPath}" +
                        (uri.encodedQuery?.let { "?$it" } ?: "")

                    Log.d(TAG, "🛠️ Rewriting relative URL with query: $fullUrl")
                    view.loadUrl(fullUrl)
                    return true
                }

                return false
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString()
                val method = request.method

                Log.d(TAG, "🔄 Intercepting $method request to $url")

                request.requestHeaders.forEach { (key, value) ->
                    Log.d("$TAG-Headers", "📋 $key: $value")
                }

                val inspectorResponse = requestInspector.shouldInterceptRequest(view, request)

                if (url.startsWith("http://") && !url.contains(".") && !url.contains("127.0.0.1") && !url.contains("localhost")) {
                    val host = url.substring("http://".length).substringBefore("/")
                    val path = if (url.contains("/")) "/${url.substringAfter("/")}" else "/"
                    val correctedUrl = "http://127.0.0.1/$host$path"

                    Log.d(TAG, "🔄 Correcting malformed URL from $url to $correctedUrl")

                    // Create a modified request with the corrected URL
                    val correctedUri = Uri.parse(correctedUrl)
                    val correctedRequest = object : WebResourceRequest {
                        override fun getUrl(): Uri = correctedUri
                        override fun isForMainFrame(): Boolean = request.isForMainFrame
                        override fun isRedirect(): Boolean = request.isRedirect
                        override fun hasGesture(): Boolean = request.hasGesture()
                        override fun getMethod(): String = request.method
                        override fun getRequestHeaders(): Map<String, String> = request.requestHeaders
                    }

                    // Handle this corrected request normally
                    return shouldInterceptRequest(view, correctedRequest)
                }

                if (!url.contains("127.0.0.1") && !url.contains("localhost")) {
                    // This is an external resource - let the WebView handle it directly
                    Log.d(TAG, "📡 External resource - passing to system: $url")
                    return null // Returning null lets the WebView load it normally
                }

                return when {
                    isStaticAssetExtension(url) ||
                            url.contains("_assets") ||
                            url.contains("/js/") ||
                            url.contains("/css/") ||
                            url.contains("/fonts/") ||
                            url.contains("/images/") -> {
                        Log.d(TAG, "🖼️ Handling asset request")
                        phpHandler.handleAssetRequest(url)
                    }
                    // Regular PHP requests
                    url.contains("127.0.0.1") -> {
                        Log.d(TAG, "🌐 Handling PHP request")
                        phpHandler.handlePHPRequest(request, phpBridge.getLastPostData())
                    }
                    else -> {
                        Log.d(TAG, "↪️ Delegating to system handler: $url")
                        inspectorResponse
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "✅ Page finished loading: $url")

                // Inject JavaScript to capture form submissions and AJAX requests
                injectJavaScript(view)
            }
        }
    }

    private fun injectJavaScript(view: WebView) {
        val jsCode = """
        (function() {
            // 🌐 Native event bridge
            const listeners = {};

            const Native = {
                on: function(eventName, callback) {
                    if (!listeners[eventName]) {
                        listeners[eventName] = [];
                    }
                    listeners[eventName].push(callback);
                },
                off: function(eventName, callback) {
                    if (listeners[eventName]) {
                        listeners[eventName] = listeners[eventName].filter(cb => cb !== callback);
                    }
                },
                dispatch: function(eventName, payload) {
                    const cbs = listeners[eventName] || [];
                    cbs.forEach(cb => cb(payload, eventName));
                }
            };

            window.Native = Native;

            document.addEventListener("native-event", function (e) {
                const eventName = e.detail.event;
                const payload = e.detail.payload;

                Native.dispatch(eventName, payload);


            });

            // Capture form submissions
            document.addEventListener('submit', function(e) {
                var form = e.target;
                var method = form.method.toLowerCase();
                if (["post", "patch", "put"].includes(method)) {
                    var formData = new FormData(form);
                    var urlEncodedData = new URLSearchParams();
                    for (var pair of formData.entries()) {
                        urlEncodedData.append(pair[0], pair[1]);
                    }

                    AndroidPOST.logPostData(urlEncodedData.toString(), form.action, "Content-Type: application/x-www-form-urlencoded");
                }
            });

            // Capture XHR/AJAX requests
            var originalXHROpen = XMLHttpRequest.prototype.open;
            var originalXHRSend = XMLHttpRequest.prototype.send;

            XMLHttpRequest.prototype.open = function(method, url) {
                this._method = method;
                this._url = url;
                return originalXHROpen.apply(this, arguments);
            };

            XMLHttpRequest.prototype.send = function(data) {
                if (["post", "patch", "put"].includes(this._method.toLowerCase()) && data) {
                    var headers = "";

                    if (this.getAllResponseHeaders) {
                        headers = this.getAllResponseHeaders();
                    }

                    AndroidPOST.logPostData(data, this._url, headers);
                }
                return originalXHRSend.apply(this, arguments);
            };

            // Capture fetch() requests
            var originalFetch = window.fetch;

            window.fetch = function(url, options) {
                if (options && options.method && ["post", "patch", "put"].includes(options.method.toLowerCase()) && options.body) {
                    var headerString = "";

                    if (options.headers) {
                        if (options.headers instanceof Headers) {
                            options.headers.forEach(function(value, key) {
                                headerString += key + ": " + value + "\\n";
                            });
                        } else if (typeof options.headers === 'object') {
                            Object.keys(options.headers).forEach(function(key) {
                                headerString += key + ": " + options.headers[key] + "\\n";
                            });
                        }
                    }

                    var bodyStr = options.body;
                    if (options.body instanceof FormData) {
                        var formObj = {};
                        options.body.forEach(function(value, key) {
                            formObj[key] = value;
                        });
                        bodyStr = JSON.stringify(formObj);
                    } else if (typeof options.body === 'object') {
                        bodyStr = JSON.stringify(options.body);
                    }

                    AndroidPOST.logPostData(bodyStr, url, headerString);
                }
                return originalFetch.apply(this, arguments);
            };

            // Find CSRF token
            function findAndSendCsrfToken() {
                var tokenField = document.querySelector('input[name="_token"]');
                if (tokenField) {
                    AndroidPOST.storeCsrfToken(tokenField.value);
                    return;
                }

                if (window.livewire && window.livewire.csrfToken) {
                    AndroidPOST.storeCsrfToken(window.livewire.csrfToken);
                }
            }

            findAndSendCsrfToken();

            var observer = new MutationObserver(function() {
                findAndSendCsrfToken();
            });

            observer.observe(document.body, {
                childList: true,
                subtree: true
            });

            return "POST+PATCH+PUT interception installed";
        })();
    """.trimIndent()

        view.evaluateJavascript(jsCode) { result ->
            Log.d(TAG, "JavaScript injection result: $result")
        }
    }


    private fun setupJavaScriptInterfaces() {
        webView.addJavascriptInterface(JSBridge(phpBridge, TAG), "AndroidPOST")
    }

    // Helper methods
    fun isStaticAssetExtension(url: String): Boolean {
        val staticExtensions = listOf(
            ".js", ".css", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".woff",
            ".woff2", ".ttf", ".eot", ".ico", ".json", ".map"
        )
        return staticExtensions.any { url.endsWith(it) || url.contains("$it?") }
    }
}

class JSBridge(private val phpBridge: PHPBridge, private val TAG: String) {
    @JavascriptInterface
    fun logPostData(data: String, url: String, headers: String) {

        // Create a unique key for this request
        val requestKey = "$url-${System.currentTimeMillis()}"
        Log.d("$TAG-JS", "📦 RequestKey: $data")

        // Store in phpBridge with the key
        phpBridge.storeRequestData(requestKey, data)

        // Set as current request
//        phpBridge.storeCurrentRequestKey(requestKey)

        // Try to extract CSRF token
        LaravelSecurity.extractFromPostBody(data)
    }

    @JavascriptInterface
    fun storeCsrfToken(token: String) {
        Log.d("$TAG-CSRF", "🔑 JS provided token: $token")
        LaravelSecurity.set(token)
    }

    private fun extractCsrfToken(postData: String?) {
        if (postData.isNullOrEmpty()) return

        try {
            // Check if it's JSON
            if (postData.startsWith("{")) {
                val jsonObj = JSONObject(postData)

                // Look for _token field
                if (jsonObj.has("_token")) {
                    val token = jsonObj.getString("_token")
                    Log.d("$TAG-CSRF", "🔑 Extracted token from POST data: $token")
                    LaravelSecurity.set(token)
                }
            }
            // Check for form data format
            else if (postData.contains("_token=")) {
                val parts = postData.split("&")
                for (part in parts) {
                    if (part.startsWith("_token=")) {
                        val token = part.substring("_token=".length)
                        Log.d("$TAG-CSRF", "🔑 Extracted token from form data: $token")
                        LaravelSecurity.set(token)
                        break
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("$TAG-CSRF", "⚠️ Error extracting CSRF token: ${e.message}")
        }
    }
}