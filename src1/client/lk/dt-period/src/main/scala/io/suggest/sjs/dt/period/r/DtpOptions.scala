package io.suggest.sjs.dt.period.r

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

    def onCustomDateChange(fn: DtpInputFn)(e: ReactEventI): Callback = {
      val newStr = e.target.value
      $.props >>= { props =>
        props.dispatchCB(
          SetDateStartEnd(
            fn      = fn,
            ymdStr  = newStr
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
        state.customRangeConn { customRangeProxy =>
          <.div(
            customRangeProxy().nonEmpty ?= <.div(
              for {
                (fn, dateOptConn) <- Seq [(DtpInputFn, ReactConnectProxy[Option[MYmd]])] (
                  DtpInputFns.start -> state.dateStartConn,
                  DtpInputFns.end   -> state.dateEndConn
                )
              } yield {
                dateOptConn { dateOptProx =>
                  for (ymd <- dateOptProx()) yield {
                    PropTable.Outer(
                      PropTable.Row(
                        Messages( "Date." + fn.strId ),

                        InputCont(
                          // TODO DatePickerR
                          <.input(
                            ^.`type`  := "text",
                            ^.name    := (AdvConstants.PERIOD_FN :: AdvConstants.DtPeriod.DATES_INTERVAL_FN :: fn :: Nil)
                              .mkString(QsConstants.KEY_PARTS_DELIM_STR),
                            ^.value   := ymd.toString,
                            ^.onChange ==> onCustomDateChange(fn)
                          )
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
        qapConn             = props.connect(_.quickAdvPeriod),
        customRangeConn     = props.connect(_.customRange),
        dateStartConn       = props.connect(_.customRange.map(_.dateStart)),
        dateEndConn         = props.connect(_.customRange.map(_.dateEnd))
      )
    }
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
