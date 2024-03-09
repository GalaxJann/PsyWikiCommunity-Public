package com.jan.psywikikt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBar
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.jan.psywikikt.databinding.ActivityBrowserBinding
import com.jan.psywikikt.dialogs.BaseDialog

class BrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowserBinding

    private var searchSite: String = " site:reddit.com"

    // TODO: MenuItem to disable Reddit JS-Injection
    var injectJS: Boolean = true

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar!!.title = "Browser - Standby"

        val urlLocation = if(intent.extras != null && intent.extras!!.containsKey("URL")) {
            intent.extras!!.getString("URL").toString()
        } else {
            "https://startpage.com/"
        }

        // Cookie handling
        val cookieManager: CookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        // Search sites
        val searchSites = arrayOf("Reddit", "Eve-Rave", "Gelbe Liste", "PsychonautWiki", "TripSit")
        val searchModeAdapter: ArrayAdapter<String> = ArrayAdapter<String>(this, androidx.appcompat.R.layout.select_dialog_item_material, searchSites)
        binding.searchSiteMenu.apply {
            threshold = 1
            setAdapter(searchModeAdapter)
            setText("Reddit", false)
        }
        binding.searchSiteMenu.setOnItemClickListener { adapterView, view, pos, id ->
            when(pos) {
                0 -> {
                    searchSite = " site:reddit.com"
                    binding.searchBarMenu.hint = "Search - site:reddit.com"
                } 1 -> {
                    searchSite = " site:eve-rave.ch"
                    binding.searchBarMenu.hint = "Search - site:eve-rave.ch"
                } 2 -> {
                    searchSite = "https://www.gelbe-liste.de/suche?term=%s"
                    binding.searchBarMenu.hint = "Search - gelbe-liste.de"
                } 3 -> {
                    searchSite = "https://psychonautwiki.org/w/index.php?search=%s&title=Special%3ASearch&profile=advanced&fulltext=1"
                    binding.searchBarMenu.hint = "Search - psychonautwiki.org"
                } 4 -> {
                    searchSite = "https://drugs.tripsit.me/"
                    binding.searchBarMenu.hint = "Search - tripsit.me"
                }
            }
        }

        // WebView Settings
        binding.webview.apply {
            background = ColorDrawable(Color.BLACK)
            clearCache(true)
            clearFormData()
            clearHistory()
            clearMatches()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.useWideViewPort = true
            isForceDarkAllowed = true
            webViewClient = CustomWebClient(binding, injectJS, supportActionBar, urlLocation)
            webChromeClient = CustomWebChrome(binding)
            loadUrl(urlLocation)
        }

        // OnBackPressed - WebView
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(binding.webview.canGoBack()) {
                    binding.webview.goBack()
                } else {
                    finish()
                }
            }
        })

        // EditText Handling
        binding.search.setOnEditorActionListener { textView, i, keyEvent ->
            if(i == EditorInfo.IME_ACTION_GO) {
                if(searchSite.contains("https://")) {   // If not startpage, other site selected/used
                    binding.webview.loadUrl(searchSite.replace("%s", binding.search.text.toString()))
                } else {
                    binding.webview.loadUrl("https://startpage.com/sp/search?query=" + Uri.encode(binding.search.text.toString() + searchSite))
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    } override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu
        menuInflater.inflate(R.menu.browser_menu, menu)
        return true
    } override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.itemRedditUnlock -> {
                if(!binding.webview.url!!.contains("old.reddit.com")) {
                    if(binding.webview.url!!.contains("www.reddit.com")) {
                        val newUrl: String = binding.webview.url!!.replace("www", "old")
                        binding.webview.loadUrl(newUrl)
                    } else if(binding.webview.url!!.contains("reddit.com")) {
                        val newUrl: String = binding.webview.url!!.replace("reddit", "old.")
                        binding.webview.loadUrl(newUrl)
                    }
                } else {
                    Toast.makeText(applicationContext, "Keine Reddit-URL erkannt!", Toast.LENGTH_SHORT).show()
                }
            } R.id.itemClose -> {
                onBackPressedDispatcher.onBackPressed()
                finish()
            } R.id.itemReload -> {
                val animation = AnimatedVectorDrawableCompat.create(applicationContext, R.drawable.avd_anim)
                animation.let {
                    item.icon = animation
                }
                animation?.start()

                binding.webview.reload()
            }
        }
        return super.onOptionsItemSelected(item)
    } private class CustomWebChrome(binding: ActivityBrowserBinding) : WebChromeClient() {
        val binding: ActivityBrowserBinding

        init {
            this.binding = binding
        }
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if(newProgress == 100) {
                binding.progressBar.visibility = ProgressBar.GONE
            } else {
                binding.progressBar.visibility = ProgressBar.VISIBLE
            }
            binding.progressBar.progress = newProgress

            super.onProgressChanged(view, newProgress)
        }
    } private class CustomWebClient(binding: ActivityBrowserBinding, injectJS: Boolean, supportActionBar: ActionBar?, intentURL: String?) : WebViewClient() {
        val binding: ActivityBrowserBinding
        val injectJS: Boolean
        val supportActionBar: ActionBar?
        val intentURL: String?

        init {
            this.binding = binding
            this.injectJS = injectJS
            this.supportActionBar = supportActionBar
            this.intentURL = intentURL
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            supportActionBar!!.title = "Browser - Loading..."
            supportActionBar.subtitle = view!!.url

            if(!intentURL!!.contains("psychonautwiki.org/wiki/") && view.url!!.matches("^https://(m\\.|)psychonautwiki\\.org/wiki/.+\$".toRegex())) {
                BaseDialog(
                    binding.root.context as Activity,
                    "Psychonaut substance link found. Do you want to open in application view?",
                    title = "<h2>Substance link found</h2>",
                    onPositive = { _, _ ->
                        val urlSplit: List<String> = view.url!!.split("/")

                        val intent = Intent(binding.root.context as Activity, InfoActivity::class.java)
                        intent.putExtra("substance", urlSplit[4])
                        binding.root.context.startActivity(intent)
                    },
                    onNegative = { _, _ -> }
                ).show()
            }

            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            supportActionBar!!.title = "Browser - Finish"

            if(url!!.contains("old.reddit.com")) {
                if(binding.webview.url!!.contains("gated") && injectJS) {
                    view!!.evaluateJavascript("document.getElementsByClassName(\"c-btn-primary\")[1].click();", null)
                    /*view!!.evaluateJavascript("(function() { return ('<html>'+document.getElementsByClassName(\"c-btn-primary\")[1].innerHTML+'</html>');})();", ValueCallback<String> { html ->
                        Log.e("LOG WEBVIEW", html.toString())
                    })*/
                    Toast.makeText(binding.root.context, "Auto-Click", Toast.LENGTH_SHORT).show()
                } else {
                    view!!.evaluateJavascript("document.getElementsByClassName(\"infobar-toaster-container\")[0].remove();", null)
                }
            } else if(url.contains("drugs.tripsit.me")) {
                Toast.makeText(binding.root.context, "Auto-Enter", Toast.LENGTH_SHORT).show()
                view!!.evaluateJavascript(
                    "document.getElementById(\"DataTables_Table_0_filter\").getElementsByTagName(\"input\")[0].value = \"${binding.search.text.toString()}\";" +
                            "document.getElementById(\"DataTables_Table_0_filter\").getElementsByTagName(\"input\")[0].dispatchEvent(new Event(\"keyup\"));",
                    null)
            }
            super.onPageFinished(view, url)
        }
    }
}