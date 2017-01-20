package io.suggest.sjs.dt.period.r

import com.github.hacker0x01.react.date.picker.{DatePickerPropsR, DatePickerR, Date_t}
import com.momentjs.Moment
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.AdvConstants
import io.suggest.common.qs.QsConstants
import io.suggest.css.Css
import io.suggest.dt.{MAdvPeriod, MYmd}
import io.suggest.dt.interval.{MRangeYmd, QuickAdvPeriod, QuickAdvPeriods}
import io.suggest.lk.r.PropTable
import io.suggest.lk.r.Forms.InputCont
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.spa.OptFastEq
import io.suggest.sjs.dt.period.m.{DtpInputFn, DtpInputFns, SetDateStartEnd, SetQap}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.react.ReactCommonUtil.Implicits.reactElOpt2reactEl
import io.suggest.react.ReactCommonUtil.cbFun2TojsCallback
import io.suggest.dt.moment.MomentJsUtil.Implicits.MomentDateExt
import io.suggest.sjs.common.empty.JsOptionUtil.opt2undef

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/** Рендер Options-раздела целиком. */
object DtpOptions {

  type Props = ModelProxy[MAdvPeriod]

  case class State(
                    qapConn         : ReactConnectProxy[QuickAdvPeriod],
                    customRangeConn : ReactConnectProxy[Option[MRangeYmd]],
                    dateStartConn   : ReactConnectProxy[Option[MYmd]],
                    dateEndConn     : ReactConnectProxy[Option[MYmd]]
                  )


  class Backend($: BackendScope[Props, State]) {

    /** Реакция на смену значения в селекте периода размещения. */
    def onQapChange(e: ReactEventI): Callback = {
      val v = e.target.value
      val qap = QuickAdvPeriods.withName(v)
      $.props >>= { props =>
        props.dispatchCB( SetQap(qap) )
      }
    }

    def onCustomDateChange(fn: DtpInputFn, newDate: Moment): Callback = {
      $.props >>= { props =>
        props.dispatchCB(
          SetDateStartEnd(
            fn      = fn,
            moment  = newDate
          )
        )
      }
    }


    def render(state: State): ReactElement = {
      <.div(
        ^.`class` := Css.Dt.OPTIONS,

        // Выбор периода размещения...
        PropTable.Outer(
          PropTable.Row(
            // Пояснение по сути
            Messages( "Advertising.period" ),

            // Селект
            InputCont(
              // Props
              Some( Css.Size.S ),

              // Children
              state.qapConn { qapProxy =>
                <.select(
                  ^.name := ( AdvConstants.PERIOD_FN + QsConstants.KEY_PARTS_DELIM_STR + AdvConstants.DtPeriod.QUICK_PERIOD_FN ),
                  ^.value := qapProxy().strId,
                  ^.onChange ==> onQapChange,

                  // Отрендерить опшены: сначала quick-периоды, затем кастомные режимы дат.
                  for (qap <- QuickAdvPeriods.values) yield {
                    <.option(
                      ^.key   := qap.strId,
                      ^.value := qap.strId,
                      Messages( qap.messagesCode )
                    )
                  }
                )
              }
            )
          )
        ),

        // Выбор диапазона дат размещения в случае кастомности исходного периода:
        state.customRangeConn { customRangeOptProxy =>
          val customRangeOpt = customRangeOptProxy()
          val momentLocale = Messages("locale.momentjs")
          <.div(
            customRangeOpt.nonEmpty ?= <.div(
              for {
                (fn, dateOptConn) <- Seq [(DtpInputFn, ReactConnectProxy[Option[MYmd]])] (
                  DtpInputFns.start -> state.dateStartConn,
                  DtpInputFns.end   -> state.dateEndConn
                )
              } yield {
                dateOptConn { dateOptProx =>
                  for (ymd <- dateOptProx()) yield {
                    PropTable.Outer.withKey(fn.strId)(
                      PropTable.Row(
                        Messages( "Date." + fn.strId ),

                        InputCont(
                          DatePickerR(
                            new DatePickerPropsR {
                              override val locale: UndefOr[String] = momentLocale

                              override val selected: js.UndefOr[Date_t] = ymd.to[Moment]
                              override val minDate: UndefOr[Date_t] = fn.minDate(customRangeOpt)

                              // date range через два поля:
                              override val selectsStart: UndefOr[Boolean] = fn.selectsStart
                              override val selectsEnd: UndefOr[Boolean] = fn.selectsEnd
                              override val startDate: UndefOr[Date_t] = {
                                customRangeOpt.map(_.dateStart.to[Moment])
                              }
                              override val endDate: UndefOr[Date_t] = {
                                customRangeOpt.map(_.dateEnd.to[Moment])
                              }

                              // TODO Opt инстансы callback-функций можно прооптимизировать, вынеся в val-карту функций или в state, например.
                              override val onChange: js.UndefOr[js.Function2[Date_t, ReactEvent, Unit]] = js.defined {
                                cbFun2TojsCallback { (newDate, _) =>
                                  onCustomDateChange(fn, newDate)
                                }
                              }
                              // Для end-даты показывать сразу два месяца.
                              override val monthsShown: UndefOr[Int] = fn.monthsShown
                              override val todayButton: UndefOr[String] = {
                                if (fn.withTodayBtn)
                                  Messages("Today")
                                else
                                  js.undefined
                              }
                              override val maxDate: UndefOr[Date_t] = fn.maxDate
                            }
                          )()
                        )

                      )
                    )
                  }  // for ymd

                }
              }   // for (fn, dateOptConn)

            )
          )
        } // customRangeProxy

      )
    } // render()

  }


  val component = ReactComponentB[Props]("DtpOptions")
    .initialState_P { props =>
      import OptFastEq.optFastEqImpl
      State(
        qapConn             = props.connect(_.info.quickAdvPeriod),
        customRangeConn     = props.connect(_.info.customRangeOpt),
        dateStartConn       = props.connect(_.info.customRangeOpt.map(_.dateStart)),
        dateEndConn         = props.connect(_.info.customRangeOpt.map(_.dateEnd))
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
