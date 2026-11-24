package io.github.sdnick484.heyclaude.spike

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The phone side of the spike: three buttons and the event log.
 *
 * It can also be driven from a terminal, which is handy while the phone is
 * tethered to the head unit:
 *
 *     adb shell am start -n <package>/io.github.sdnick484.heyclaude.spike.MainActivity --es post loud
 *
 * where the value is "loud" or "quiet".
 */
class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var log: TextView
    private val refresh: () -> Unit = { runOnUiThread { render() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildLayout())
        if (!Notifier.canPost(this)) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onStart() {
        super.onStart()
        EventLog.addListener(refresh)
        render()
    }

    override fun onStop() {
        EventLog.removeListener(refresh)
        super.onStop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }

    private fun handle(intent: Intent?) {
        when (intent?.getStringExtra("post")) {
            "loud" -> Turns.postStandingMessage(this, quiet = false)
            "quiet" -> Turns.postStandingMessage(this, quiet = true)
        }
    }

    private fun render() {
        status.text = buildString {
            append(getString(R.string.app_name)).append("  (").append(packageName).append(")\n")
            append(getString(R.string.flavor_summary)).append("\n")
            append(
                if (Notifier.canPost(this@MainActivity)) {
                    "Notifications: allowed"
                } else {
                    "Notifications: NOT allowed. Nothing can be posted until they are."
                },
            )
        }
        log.text = EventLog.text().ifEmpty { "Nothing has happened yet." }
    }

    private fun buildLayout(): View {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f }
        column.addView(status, MATCH_PARENT, WRAP_CONTENT)

        column.addView(button("Post \"${Conversation.SEED}\" (heads-up)") {
            Turns.postStandingMessage(this, quiet = false)
        })
        column.addView(button("Post \"${Conversation.SEED}\" (quiet)") {
            Turns.postStandingMessage(this, quiet = true)
        })
        column.addView(button("Allow notifications") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        })

        column.addView(TextView(this).apply {
            text = "Event log (newest first)"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, pad, 0, pad / 2)
        })
        log = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            textSize = 13f
            setTextIsSelectable(true)
        }
        column.addView(log, MATCH_PARENT, WRAP_CONTENT)

        return ScrollView(this).apply {
            // Android 15 and later draw apps edge to edge. This keeps the content
            // clear of the status and navigation bars.
            fitsSystemWindows = true
            addView(column, MATCH_PARENT, WRAP_CONTENT)
        }
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        }
}
