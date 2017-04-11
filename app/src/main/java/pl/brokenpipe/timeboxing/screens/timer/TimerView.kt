package pl.brokenpipe.timeboxing.screens.timer

import android.animation.ValueAnimator
import android.graphics.Typeface
import android.media.AudioManager
import android.media.SoundPool
import android.media.SoundPool.Builder
import android.os.Build.VERSION
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import kotlinx.android.synthetic.main.timer_view.clockFace
import kotlinx.android.synthetic.main.timer_view.rlClockCenter
import kotlinx.android.synthetic.main.timer_view.tvClockTimeLeft
import kotlinx.android.synthetic.main.timer_view.tvClockTimeMiddle
import kotlinx.android.synthetic.main.timer_view.tvClockTimeRight
import kotlinx.android.synthetic.main.timer_view.vClockCenterBackground
import pl.brokenpipe.timeboxing.R
import pl.brokenpipe.timeboxing.base.BaseView
import pl.brokenpipe.timeboxing.base.Layout
import pl.brokenpipe.timeboxing.R.layout
import pl.brokenpipe.timeboxing.R.raw
import pl.brokenpipe.timeboxing.databinding.TimerViewBinding
import rx.Observable
import timber.log.Timber

@Layout(layout.timer_view)
class TimerView : BaseView<TimerViewBinding>(), TimerViewActions {

    private val SOUND_FADE_OUT_DURATION = 200L
    private var timeFlowAnimation = AnimationSet(true)

    lateinit var viewModel: TimerViewModel
    @Suppress("DEPRECATION")
    val soundPool: SoundPool = if (VERSION.SDK_INT >= 21) Builder().build()
    else SoundPool(2, AudioManager.STREAM_MUSIC, 0)

    var soundId: Int = 0

    init {
        with(timeFlowAnimation) {
            val animationDuration = 800L
            interpolator = DecelerateInterpolator()
            val scale = ScaleAnimation(
                1f, 1.2f, 1f, 1.2f, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f)
            scale.duration = animationDuration

            val fade = AlphaAnimation(1f, 0f)
            fade.duration = animationDuration

            addAnimation(scale)
            addAnimation(fade)

            duration = animationDuration
            repeatCount = 1
        }
    }

    override fun startTimer() {
        activity.clockFace.start()
    }

    override fun pauseTimer() {
        activity.clockFace.pause()
        activity.rlClockCenter.clearAnimation()

    }

    override fun animateTimeFlow() {
        activity.vClockCenterBackground.startAnimation(timeFlowAnimation)
    }

    override fun onViewBound(binding: TimerViewBinding) {
        viewModel = TimerViewModel(this)
        binding.viewModel = viewModel
        soundId = soundPool.load(activity, raw.alarm3, 1)
        viewModel.subscribeClockState(activity.clockFace.getStateObservable())

        setupFonts()
    }

    private fun setupFonts() {
        val colonTypeface = Typeface.createFromAsset(activity.assets, "Roboto-Thin.ttf")
        activity.tvClockTimeMiddle.typeface = colonTypeface
        activity.tvClockTimeLeft.setTypeface(activity.tvClockTimeRight.typeface, Typeface.BOLD)
        activity.tvClockTimeRight.setTypeface(activity.tvClockTimeRight.typeface, Typeface.BOLD)
    }

    override fun getTimerSecondsObservable(): Observable<Long> {
        return activity.clockFace.getTimerObservable()
    }

    override fun playEndSound() {
        val playResult = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        Timber.i("Sound result with: %d", playResult)
    }

    override fun stopEndSound() {
        val animation = ValueAnimator.ofFloat(1f, 0f)
        animation.duration = SOUND_FADE_OUT_DURATION
        animation.repeatCount = 1
        animation.addUpdateListener {
            soundPool.setVolume(soundId, it.animatedValue as Float, it.animatedValue as Float)
            if (it.animatedValue as Float <= 0) {
                it.removeAllUpdateListeners()
            }
        }
        soundPool.stop(soundId)
    }

    override fun keepScreenOn() {
        activity.window.addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun letScreenOff() {
        activity.window.clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}