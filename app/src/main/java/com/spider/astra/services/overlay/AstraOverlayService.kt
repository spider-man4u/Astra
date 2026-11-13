package com.spider.astra.services.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.spider.astra.MainActivity
import com.spider.astra.R
import com.spider.astra.ui.theme.AstraPalette
import kotlin.math.abs

/**
 * User-enabled floating ASTRA orb.
 *
 * The service only hosts the overlay and a persistent notification. It does not
 * execute commands, inspect other apps, capture the screen, or access microphone data.
 */
class AstraOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var expanded = false
    private var initialRawX = 0f
    private var initialRawY = 0f
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var isDragging = false
    private val touchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(expand = false)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        isRunning = false
        super.onDestroy()
    }

    private fun showOverlay(expand: Boolean) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        removeOverlay()
        expanded = expand

        val view = if (expand) createExpandedPanel() else createOrbView()
        val params = createWindowParams(expand)
        overlayView = view
        windowParams = params

        try {
            windowManager.addView(view, params)
            isRunning = true
        } catch (_: SecurityException) {
            overlayView = null
            windowParams = null
            isRunning = false
            stopSelf()
        } catch (_: WindowManager.BadTokenException) {
            overlayView = null
            windowParams = null
            isRunning = false
            stopSelf()
        }
    }

    private fun createWindowParams(expand: Boolean): WindowManager.LayoutParams {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val width = if (expand) dp(252) else dp(68)
        val height = if (expand) WindowManager.LayoutParams.WRAP_CONTENT else dp(68)
        val params = WindowManager.LayoutParams(
            width,
            height,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val screenWidth = resources.displayMetrics.widthPixels
        params.x = if (expand) (screenWidth - width - dp(14)).coerceAtLeast(dp(8))
        else (screenWidth - width - dp(12)).coerceAtLeast(dp(8))
        params.y = (resources.displayMetrics.heightPixels / 3).coerceAtLeast(dp(90))
        return params
    }

    private fun createOrbView(): View {
        return OrbView(this).apply {
            contentDescription = "ASTRA floating orb. Tap to expand, drag to move."
            setOnTouchListener { _, event -> handleDragTouch(event, openOnTap = true, collapseOnTap = false) }
        }
    }

    private fun createExpandedPanel(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(15))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF10263D.toInt(), 0xFF071321.toInt())
            ).apply {
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), 0x8854E8FF.toInt())
            }
            elevation = dp(18).toFloat()
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val dragTitle = TextView(this).apply {
            text = "ASTRA CORE  ·  MOVE"
            textSize = 9f
            letterSpacing = 0.14f
            setTextColor(0xFF91A5BD.toInt())
            setPadding(0, dp(7), 0, dp(7))
            contentDescription = "Drag to move ASTRA panel"
        }
        header.addView(
            dragTitle,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        dragTitle.setOnTouchListener { _, event ->
            handleDragTouch(event, openOnTap = false, collapseOnTap = false)
        }

        val close = TextView(this).apply {
            text = "×"
            textSize = 23f
            setTextColor(0xFFFF9AAA.toInt())
            gravity = Gravity.CENTER
            background = roundedBackground(0x22FF7185, 0x66FF7185, 18)
            setOnClickListener { stopSelf() }
        }
        header.addView(close, LinearLayout.LayoutParams(dp(34), dp(34)))
        panel.addView(header)

        val orbRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(12))
        }
        val orb = OrbView(this).apply {
            contentDescription = "ASTRA core"
            setOnClickListener { showOverlay(expand = false) }
        }
        orbRow.addView(orb, LinearLayout.LayoutParams(dp(72), dp(72)))
        val status = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        status.addView(label("FLOATING INTERFACE", 9f, 0xFF54E8FF.toInt(), bold = true))
        status.addView(label("Standing by", 15f, 0xFFF2F8FF.toInt(), bold = true))
        status.addView(label("Local UI · No actions active", 10f, 0xFF91A5BD.toInt()))
        orbRow.addView(status, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        panel.addView(orbRow)

        val openApp = actionButton("OPEN ASTRA", 0xFF20D9F4.toInt(), 0xFF04101B.toInt())
        openApp.setOnClickListener {
            val launch = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launch)
        }
        panel.addView(openApp, matchWrap())

        val minimize = actionButton("MINIMIZE TO ORB", 0xFF17334E.toInt(), 0xFFB5C8DC.toInt())
        minimize.setOnClickListener { showOverlay(expand = false) }
        val minimizeParams = matchWrap().apply { topMargin = dp(8) }
        panel.addView(minimize, minimizeParams)

        return panel
    }

    private fun handleDragTouch(
        event: MotionEvent,
        openOnTap: Boolean,
        collapseOnTap: Boolean
    ): Boolean {
        val params = windowParams ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialRawX = event.rawX
                initialRawY = event.rawY
                initialWindowX = params.x
                initialWindowY = params.y
                isDragging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialRawX
                val dy = event.rawY - initialRawY
                if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                    isDragging = true
                }
                if (isDragging) {
                    val screenWidth = resources.displayMetrics.widthPixels
                    val screenHeight = resources.displayMetrics.heightPixels
                    val viewWidth = overlayView?.width?.takeIf { it > 0 } ?: dp(if (expanded) 252 else 68)
                    val viewHeight = overlayView?.height?.takeIf { it > 0 } ?: dp(if (expanded) 280 else 68)
                    params.x = (initialWindowX + dx.toInt()).coerceIn(0, (screenWidth - viewWidth).coerceAtLeast(0))
                    params.y = (initialWindowY + dy.toInt()).coerceIn(0, (screenHeight - viewHeight).coerceAtLeast(0))
                    runCatching { windowManager.updateViewLayout(overlayView, params) }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    when {
                        openOnTap -> showOverlay(expand = true)
                        collapseOnTap -> showOverlay(expand = false)
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return false
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        windowParams = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ASTRA floating interface",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the user-enabled ASTRA floating orb available."
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AstraOverlayService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_astra_launcher)
            .setContentTitle("ASTRA floating orb is active")
            .setContentText("Tap the orb to open its controls.")
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop orb", stopPendingIntent)
            .build()
    }

    private fun label(text: String, sizeSp: Float, color: Int, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(2), 0, dp(2))
        }
    }

    private fun actionButton(text: String, backgroundColor: Int, textColor: Int): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 10f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(textColor)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = roundedBackground(backgroundColor, 0x6654E8FF, 14)
            setPadding(dp(12), dp(14), dp(12), dp(14))
            isClickable = true
            isFocusable = true
        }
    }

    private fun roundedBackground(fill: Int, stroke: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class OrbView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) * 0.40f

            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx,
                cy,
                radius * 1.85f,
                intArrayOf(0x6654E8FF, 0x2220D9F4, 0x003B82F6),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * 1.85f, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = radius * 0.11f
            paint.color = 0x3320D9F4
            canvas.drawCircle(cx, cy, radius * 1.30f, paint)
            paint.strokeWidth = radius * 0.045f
            paint.color = 0x8854E8FF.toInt()
            canvas.drawCircle(cx, cy, radius * 1.08f, paint)

            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(
                cx - radius * 0.24f,
                cy - radius * 0.30f,
                radius * 1.75f,
                intArrayOf(0xFFCCFBFF.toInt(), 0xFF54E8FF.toInt(), 0xFF087FA8.toInt(), 0xFF06182C.toInt()),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, radius * 0.76f, paint)
            paint.shader = null
            paint.color = 0xAAFFFFFF.toInt()
            canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.30f, radius * 0.11f, paint)
        }
    }

    companion object {
        const val ACTION_SHOW = "com.spider.astra.action.SHOW_OVERLAY"
        const val ACTION_STOP = "com.spider.astra.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "astra_floating_interface"
        private const val NOTIFICATION_ID = 4201

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, AstraOverlayService::class.java).setAction(ACTION_SHOW)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AstraOverlayService::class.java))
        }
    }
}
