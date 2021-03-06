package pl.brokenpipe.timeboxing.screens.timer

import android.databinding.BaseObservable
import android.databinding.Bindable
import android.text.Spannable
import pl.brokenpipe.timeboxing.BR
import pl.brokenpipe.timeboxing.ui.clock.Side
import pl.brokenpipe.timeboxing.ui.clock.Side.LEFT
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber

class TimerViewModel(val timerViewActions: TimerViewActions) : BaseObservable() {

    private val compositeSubscription = CompositeSubscription()

    fun subscribeChanges() {
        compositeSubscription.add(timerViewActions.getTimerSecondsObservable()
                                      .observeOn(AndroidSchedulers.mainThread())
                                      .doOnNext {
                                          timeInSec = it
                                          time = secondsToTime(it)
                                          timerLeftValueVisibility = time.hours > 0L || time.minutes > 0L
                                          pauseButtonVisibility = !isClockRunning && time.hours + time.minutes + time.seconds > 0L
                                          if (it == 0L && isClockRunning) {
                                              timerViewActions.playEndSound()
                                              pauseTimer()
                                          } else {
                                              if (isClockRunning) {
                                                  try {
                                                      timerViewActions.animateTimeFlow()
                                                  } catch (e: IllegalStateException) {
                                                      Timber.d(e)
                                                  }
                                              }
                                          }
                                      }
                                      .onErrorReturn { timeInSec }
                                      .subscribe())
    }

    @get:Bindable
    var isClockRunning = false
        set(value) {
            field = value
            notifyPropertyChanged(BR.clockRunning)
            pauseButtonVisibility = !isClockRunning && time.hours + time.minutes + time.seconds > 0L
            if(value) {
                timerViewActions.keepScreenOn()
            } else {
                timerViewActions.letScreenOff()
            }
        }

    private var timeInSec = 0L
    var clockSpinSide: Side = LEFT

    @get:Bindable
    var time: Time = Time(0, 0, 0)
        set(value) {
            field = value
            notifyPropertyChanged(BR.time)
            notifyPropertyChanged(BR.timeLeftValue)
            notifyPropertyChanged(BR.timeRightValue)
        }

    @get:Bindable
    var timeLeftValue: String = "00"
        get() {
            if (time.hours > 0) return time.hoursToString()
            else return time.minutesToString()
        }

    @get:Bindable
    var timeRightValue: String = "00"
        get() {
            if (time.hours > 0)
                return time.minutesToString()
            else
                return time.secondsToString()
        }

    @get:Bindable
    var timerLeftValueVisibility: Boolean = false
    set(value) {
        field = value
        notifyPropertyChanged(BR.timerLeftValueVisibility)
    }

    @get:Bindable
    var pauseButtonVisibility: Boolean = false
    set(value) {
        field = value
        notifyPropertyChanged(BR.pauseButtonVisibility)
    }

    fun onStartPauseClick() {
        if (isClockRunning) pauseTimer() else startTimer()
        timerViewActions.stopEndSound()
    }

    private fun pauseTimer() {
        timerViewActions.pauseTimer()
    }

    private fun startTimer() {
        if (timeInSec > 0L) {
            timerViewActions.startTimer()
        }
    }

    private fun secondsToTime(seconds: Long): Time {
        val sec = seconds % 60
        val min = (seconds - sec) / 60 % 60
        val hour = seconds.div(3600)

        return Time(hour, min, sec)
    }

    fun subscribeClockState(stateObservable: Observable<Boolean>) {
        compositeSubscription.add(stateObservable.subscribe({ isClockRunning = it }))
    }

    fun dispose() {
        compositeSubscription.clear()
    }
}