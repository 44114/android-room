package com.chatroom.app.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Cloudflare Turnstile verification widget rendered in a WebView.
 *
 * ## How it works
 *
 * 1. The server exposes `GET /config` which returns the **public** Turnstile site key.
 *    (The site key is intentionally public — it's visible in the web app's HTML source.)
 *    The **secret key** never leaves the server; it is only used server-side to verify tokens.
 *
 * 2. This composable loads a minimal HTML page containing the Turnstile widget,
 *    with the site key injected from the server config.
 *
 * 3. When the user completes the challenge, the widget calls a JavaScript callback,
 *    which invokes `TurnstileBridge.onTokenReceived(token)` via Android's
 *    `addJavascriptInterface`.
 *
 * 4. The token is passed back to the parent composable via `onTokenReceived`.
 *    The parent sends this token along with the login/register request to
 *    `POST /auth/login` or `POST /auth/register`, where the server verifies it
 *    using its private secret key.
 *
 * ## No secrets in the APK
 *
 * The site key is fetched at runtime from the server. No Turnstile credentials
 * are baked into the APK. If the server changes its site key, the app picks it up
 * automatically the next time `/config` is fetched.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileWebView(
    siteKey: String,
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (siteKey.isBlank()) {
        // Site key not yet loaded from server config
        Box(modifier = modifier.height(65.dp), contentAlignment = Alignment.Center) {
            Text("等待验证组件加载...", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary)
        }
        return
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false

                // Critical: set a realistic mobile Chrome user-agent.
                // The default WebView UA is clearly recognizable as an embedded
                // browser, which Cloudflare Turnstile flags as suspicious.
                // Using a standard Chrome Android UA dramatically improves pass rates.
                settings.userAgentString = (
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36"
                )

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?,
                    ) {
                        onError("Turnstile 加载失败: ${description ?: "未知错误"}")
                    }
                }

                // Bridge: WebView JavaScript → Kotlin
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onTokenReceived(token: String) {
                        // Post to main thread since this is called from WebView's thread
                        post { onTokenReceived(token) }
                    }

                    @JavascriptInterface
                    fun onExpired() {
                        post { onError("验证已过期，请重新验证") }
                    }

                    @JavascriptInterface
                    fun onError() {
                        post { onError("人机验证失败，请重试") }
                    }
                }, "TurnstileBridge")

                loadDataWithBaseURL(
                    "https://challenges.cloudflare.com",
                    TURNSTILE_HTML.replace("SITE_KEY_PLACEHOLDER", siteKey),
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        modifier = modifier.height(75.dp),
    )
}

/**
 * Minimal HTML page hosting the Cloudflare Turnstile widget.
 *
 * The site key placeholder is replaced at runtime with the key received
 * from the server's GET /config endpoint.
 */
private val TURNSTILE_HTML = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <script src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit" async defer></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { display: flex; justify-content: center; align-items: center; min-height: 70px; }
        #turnstile-container { min-height: 65px; }
    </style>
</head>
<body>
    <div id="turnstile-container"></div>
    <script>
        // Render Turnstile explicitly after API loads
        window.onloadTurnstileCallback = function() {
            if (window.turnstile) {
                turnstile.render('#turnstile-container', {
                    sitekey: 'SITE_KEY_PLACEHOLDER',
                    callback: function(token) {
                        if (window.TurnstileBridge) {
                            TurnstileBridge.onTokenReceived(token);
                        }
                    },
                    'expired-callback': function() {
                        if (window.TurnstileBridge) {
                            TurnstileBridge.onExpired();
                        }
                    },
                    'error-callback': function() {
                        if (window.TurnstileBridge) {
                            TurnstileBridge.onError();
                        }
                    },
                    theme: 'light',
                    size: 'normal',
                });
            }
        };
        // Also try auto-load in case the API already loaded
        if (window.turnstile) {
            onloadTurnstileCallback();
        }
    </script>
</body>
</html>
""".trimIndent()
