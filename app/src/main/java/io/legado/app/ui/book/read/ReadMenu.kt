package io.legado.app.ui.book.read

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ViewReadMenuBinding
import io.legado.app.help.*
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.*
import io.legado.app.model.BookRead
import io.legado.app.ui.web.WebViewActivity
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.*
import splitties.views.onClick
import splitties.views.onLongClick

/**
 * 阅读界面菜单
 */
class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    var cnaShowMenu: Boolean = false
    private val callBack: CallBack get() = activity as CallBack
    private val binding = ViewReadMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation
    private val bgColor: Int = context.bottomBackground
    private val textColor: Int = context.getPrimaryTextColor(ColorUtils.isColorLight(bgColor))
    private val bottomBackgroundList: ColorStateList = Selector.colorBuild()
        .setDefaultColor(bgColor)
        .setPressedColor(ColorUtils.darkenColor(bgColor))
        .create()
    private var onMenuOutEnd: (() -> Unit)? = null
    private val showBrightnessView
        get() = context.getPrefBoolean(
            PreferKey.showBrightnessView,
            true
        )
    private val sourceMenu by lazy {
        PopupMenu(context, binding.tvSourceAction).apply {
            inflate(R.menu.book_read_source)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_edit_source -> callBack.openSourceEditActivity()
                    R.id.menu_disable_source -> callBack.disableSource()
                }
                true
            }
        }
    }

    init {
        initView()
        upBrightnessState()
        bindEvent()
    }

    private fun initView() = binding.run {
        if (AppConfig.isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        initAnimation()
        val brightnessBackground = GradientDrawable()
        brightnessBackground.cornerRadius = 5F.dp
        brightnessBackground.setColor(ColorUtils.adjustAlpha(bgColor, 0.5f))
        llBrightness.background = brightnessBackground
        llBottomBg.setBackgroundColor(bgColor)
        vwNavigationBar.setBackgroundColor(bgColor)
        fabSearch.backgroundTintList = bottomBackgroundList
        fabSearch.setColorFilter(textColor)
        fabAutoPage.backgroundTintList = bottomBackgroundList
        fabAutoPage.setColorFilter(textColor)
        fabReplaceRule.backgroundTintList = bottomBackgroundList
        fabReplaceRule.setColorFilter(textColor)
        fabNightTheme.backgroundTintList = bottomBackgroundList
        fabNightTheme.setColorFilter(textColor)
        tvPre.setTextColor(textColor)
        tvNext.setTextColor(textColor)
        ivCatalog.setColorFilter(textColor)
        tvCatalog.setTextColor(textColor)
        ivReadAloud.setColorFilter(textColor)
        tvReadAloud.setTextColor(textColor)
        ivFont.setColorFilter(textColor)
        tvFont.setTextColor(textColor)
        ivSetting.setColorFilter(textColor)
        tvSetting.setTextColor(textColor)
        vwBg.setOnClickListener(null)
        vwNavigationBar.setOnClickListener(null)
        llBrightness.setOnClickListener(null)
        seekBrightness.post {
            seekBrightness.progress = AppConfig.readBrightness
        }
    }

    fun upBrightnessState() {
        if (brightnessAuto()) {
            binding.ivBrightnessAuto.setColorFilter(context.accentColor)
            binding.seekBrightness.isEnabled = false
        } else {
            binding.ivBrightnessAuto.setColorFilter(context.buttonDisabledColor)
            binding.seekBrightness.isEnabled = true
        }
        setScreenBrightness(AppConfig.readBrightness)
    }

    /**
     * 设置屏幕亮度
     */
    private fun setScreenBrightness(value: Int) {
        var brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (!brightnessAuto()) {
            brightness = value.toFloat()
            if (brightness < 1f) brightness = 1f
            brightness /= 255f
        }
        val params = activity?.window?.attributes
        params?.screenBrightness = brightness
        activity?.window?.attributes = params
    }

    fun runMenuIn() {
        this.visible()
        binding.titleBar.visible()
        binding.bottomMenu.visible()
        binding.titleBar.startAnimation(menuTopIn)
        binding.bottomMenu.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            binding.titleBar.startAnimation(menuTopOut)
            binding.bottomMenu.startAnimation(menuBottomOut)
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView
    }

    private fun bindEvent() = binding.run {
        val chapterViewClickListener = OnClickListener {
            BookRead.bookSource ?: return@OnClickListener
            if (AppConfig.readUrlInBrowser) {
                context.openUrl(tvChapterUrl.text.toString().substringBefore(",{"))
            } else {
                context.startActivity<WebViewActivity> {
                    val url = tvChapterUrl.text.toString()
                    putExtra("title", tvChapterName.text)
                    putExtra("url", url)
                    IntentData.put(url, BookRead.bookSource?.getHeaderMap(true))
                }
            }
        }
        val chapterViewLongClickListener = OnLongClickListener {
            BookRead.bookSource ?: return@OnLongClickListener true
            context.alert(R.string.Open_fan) {
                setMessage(R.string.use_browser_open)
                okButton {
                    AppConfig.readUrlInBrowser = true
                }
                noButton {
                    AppConfig.readUrlInBrowser = false
                }
            }
            true
        }
        tvChapterName.setOnClickListener(chapterViewClickListener)
        tvChapterName.setOnLongClickListener(chapterViewLongClickListener)
        tvChapterUrl.setOnClickListener(chapterViewClickListener)
        tvChapterUrl.setOnLongClickListener(chapterViewLongClickListener)
        //登录
        tvLogin.setOnClickListener {
            callBack.showLogin()
        }
        //购买
        tvPay.setOnClickListener {
            callBack.payAction()
        }
        //书源操作
        tvSourceAction.onClick {
            sourceMenu.show()
        }
        //亮度跟随
        ivBrightnessAuto.setOnClickListener {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seekBrightness.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setScreenBrightness(progress)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                AppConfig.readBrightness = seekBar.progress
            }

        })

        //阅读进度
        seekReadPage.setOnSeekBarChangeListener(object : SeekBarChangeListener {

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                BookRead.skipToPage(seekBar.progress)
            }

        })

        //搜索
        fabSearch.setOnClickListener {
            runMenuOut {
                callBack.openSearchActivity(null)
            }
        }

        //自动翻页
        fabAutoPage.setOnClickListener {
            runMenuOut {
                callBack.autoPage()
            }
        }

        //替换
        fabReplaceRule.setOnClickListener { callBack.openReplaceRule() }

        //夜间模式
        fabNightTheme.setOnClickListener {
            AppConfig.isNightTheme = !AppConfig.isNightTheme
            ThemeConfig.applyDayNight(context)
        }

        //上一章
        tvPre.setOnClickListener { BookRead.moveToPrevChapter(upContent = true, toLast = false) }

        //下一章
        tvNext.setOnClickListener { BookRead.moveToNextChapter(true) }

        //目录
        llCatalog.setOnClickListener {
            runMenuOut {
                callBack.openChapterList()
            }
        }

        //朗读
        llReadAloud.setOnClickListener {
            runMenuOut {
                callBack.onClickReadAloud()
            }
        }
        llReadAloud.onLongClick {
            runMenuOut { callBack.showReadAloudDialog() }
        }
        //界面
        llFont.setOnClickListener {
            runMenuOut {
                callBack.showReadStyle()
            }
        }

        //设置
        llSetting.setOnClickListener {
            runMenuOut {
                callBack.showMoreSetting()
            }
        }
    }

    private fun initAnimation() {
        //显示菜单
        menuTopIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.tvLogin.isGone = BookRead.bookSource?.loginUrl.isNullOrEmpty()
                binding.tvPay.isGone = BookRead.bookSource?.loginUrl.isNullOrEmpty()
                        || BookRead.curTextChapter?.isVip != true
                        || BookRead.curTextChapter?.isPay == true
                callBack.upSystemUiVisibility()
                binding.llBrightness.visible(showBrightnessView)
            }

            override fun onAnimationEnd(animation: Animation) {
                binding.vwMenuBg.setOnClickListener { runMenuOut() }
                binding.vwNavigationBar.run {
                    layoutParams = layoutParams.apply {
                        height = if (ReadBookConfig.hideNavigationBar) {
                            activity?.navigationBarHeight ?: 0
                        } else {
                            0
                        }
                    }
                }
                if (!LocalConfig.readMenuHelpVersionIsLast) {
                    callBack.showReadMenuHelp()
                }
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })

        //隐藏菜单
        menuTopOut = AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_top_out)
        menuBottomOut =
            AnimationUtilsSupport.loadAnimation(context, R.anim.anim_readbook_bottom_out)
        menuTopOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                binding.vwMenuBg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                binding.titleBar.invisible()
                binding.bottomMenu.invisible()
                cnaShowMenu = false
                onMenuOutEnd?.invoke()
                callBack.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) = Unit
        })
    }

    fun upBookView() {
        binding.titleBar.title = BookRead.book?.name
        BookRead.curTextChapter?.let {
            binding.tvChapterName.text = it.title
            binding.tvChapterName.visible()
            if (!BookRead.isLocalBook) {
                binding.tvChapterUrl.text = it.url
                binding.tvChapterUrl.visible()
            } else {
                binding.tvChapterUrl.gone()
            }
            binding.seekReadPage.max = it.pageSize.minus(1)
            binding.seekReadPage.progress = BookRead.durPageIndex()
            binding.tvPre.isEnabled = BookRead.durChapterIndex != 0
            binding.tvNext.isEnabled = BookRead.durChapterIndex != BookRead.chapterSize - 1
        } ?: let {
            binding.tvChapterName.gone()
            binding.tvChapterUrl.gone()
        }
    }

    fun setSeekPage(seek: Int) {
        binding.seekReadPage.progress = seek
    }

    fun setAutoPage(autoPage: Boolean) = binding.run {
        if (autoPage) {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page_stop)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
        fabAutoPage.setColorFilter(textColor)
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showReadMenuHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
    }

}
