package io.suggest.sjs.dt.period.r

import com.github.hacker0x01.react.date.picker.{DatePickerPropsR, DatePickerR, Date_t, PopperPlacements}
import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.adv.AdvConstants
import io.suggest.common.qs.QsConstants
import io.suggest.css.Css
import io.suggest.dt.{MAdvPeriod, MYmd}
import io.suggest.dt.interval.{MRangeYmd, QuickAdvPeriod, QuickAdvPeriods}
import io.suggest.dt.moment.MomentJsUtil.Implicits._
import io.suggest.lk.r.PropTableR
import io.suggest.lk.r.Forms.InputCont
import io.suggest.msg.Messages
import io.suggest.react.ReactCommonUtil
import io.suggest.spa.OptFastEq.Plain
import io.suggest.sjs.dt.period.m.{DtpInputFn, DtpInputFns, SetDateStartEnd, SetQap}
import japgolly.scalajs.react._

import java.time.LocalDate
//import japgolly.scalajs.react.vdom.Implicits._
import japgolly.scalajs.react.vdom.html_<^._
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.react.ReactCommonUtil.cbFun2ToJsCb
import io.suggest.i18n.MsgCodes
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
// TODO import io.suggest.sjs.common.dt.JsDateUtil.MRangeYmdFastEq + OptFastEq.Wrapped

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
    def onQapChange(e: ReactEventFromInput): Callback = {
      val v = e.target.value
      val qap = QuickAdvPeriods.withValue(v)
      dispatchOnProxyScopeCB($, SetQap(qap))
    }

    def onCustomDateChange(fn: DtpInputFn, newDate: LocalDate): Callback = {
      val msg = SetDateStartEnd(
        fn      = fn,
        date  = newDate,
      )
      dispatchOnProxyScopeCB($, msg)
    }


    def render(state: State): VdomElement = {
      <.div(
        ^.`class` := Css.Dt.OPTIONS,

        // Выбор периода размещения...
        PropTableR.Outer(
          PropTableR.Row(
            // Пояснение по сути
            Messages( MsgCodes.`Advertising.period` )
          )(
            // Селект
            InputCont( Css.Size.S )(
              state.qapConn { qapProxy =>
                <.select(
                  ^.name      := ( AdvConstants.PERIOD_FN + QsConstants.KEY_PARTS_DELIM_STR + AdvConstants.DtPeriod.QUICK_PERIOD_FN ),
                  ^.`class`   := Css.CLICKABLE,
                  ^.value     := qapProxy().value,
                  ^.onChange ==> onQapChange,

                  // Отрендерить опшены: сначала quick-периоды, затем кастомные режимы дат.
                  QuickAdvPeriods.values.toVdomArray { qap =>
                    <.option(
                      ^.key   := qap.value,
                      ^.value := qap.value,
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
          val momentLocale = Messages( MsgCodes.`locale.momentjs` )
          <.div(
            ReactCommonUtil.maybeEl(customRangeOpt.nonEmpty) {
              val children: Seq[TagMod] = for {
                (fn, dateOptConn) <- Seq[(DtpInputFn, ReactConnectProxy[Option[MYmd]])](
                  DtpInputFns.start -> state.dateStartConn,
                  DtpInputFns.end   -> state.dateEndConn
                )
              } yield {
                dateOptConn { dateOptProx =>
                  dateOptProx().whenDefinedEl { ymd =>
                    PropTableR.Outer.withKey(fn.value)(
                      PropTableR.Row(
                        Messages(
                          MsgCodes.`Date.suffixed`(fn.value)
                        )
                      )(
                        InputCont(Css.Size.S)(
                          DatePickerR(
                            new DatePickerPropsR {
                              override val locale: UndefOr[String] = momentLocale

                              override val selected: js.UndefOr[Date_t] = ymd.to[Date_t]
                              override val minDate: UndefOr[Date_t] = fn.minDate(customRangeOpt).toMoment

                              // date range через два поля:
                              override val selectsStart: UndefOr[Boolean] = fn.selectsStart
                              override val selectsEnd: UndefOr[Boolean] = fn.selectsEnd
                              override val startDate: UndefOr[Date_t] = {
                                customRangeOpt
                                  .map(_.dateStart.to[Date_t])
                                  .toUndef
                              }
                              override val endDate: UndefOr[Date_t] = {
                                customRangeOpt
                                  .map(_.dateEnd.to[Date_t])
                                  .toUndef
                              }

                              // TODO Opt инстансы callback-функций можно прооптимизировать, вынеся в val-карту функций или в state, например.
                              override val onChange: js.UndefOr[js.Function2[Date_t, ReactEvent, Unit]] = js.defined {
                                cbFun2ToJsCb { (newDate, _) =>
                                  onCustomDateChange(fn, newDate.toLocalDate)
                                }
                              }
                              // Для end-даты показывать сразу два месяца.
                              override val monthsShown: UndefOr[Int] = fn.monthsShown
                              override val todayButton: UndefOr[String] = {
                                if (fn.withTodayBtn)
                                  Messages(MsgCodes.`Today`)
                                else
                                  js.undefined
                              }
                              override val maxDate: UndefOr[Date_t] = fn.maxDate.toMoment
                              override val popperPlacement: UndefOr[String] = PopperPlacements.`bottom-end`
                            }
                          )
                        )

                      )
                    )
                  } // for ymd
                }: TagMod
              }  // for (fn, dateOptConn)
              <.div(
                children: _*
              )
            }
          )
        } // customRangeProxy

      )
    } // render()

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { props =>
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
