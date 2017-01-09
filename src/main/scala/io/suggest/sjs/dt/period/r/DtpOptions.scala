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
import io.suggest.sjs.dt.period.m.{DtpInputFn, DtpInputFns, SetQap}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

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

    def render(state: State): ReactElement = {
      <.div(
        ^.`class` := Css.Dt.OPTIONS,
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

        state.customRangeConn { customRangeProxy =>
          <.div(
            customRangeProxy().nonEmpty ?= <.div(
              _renderDateInput(DtpInputFns.start, state.dateStartConn),
              _renderDateInput(DtpInputFns.end,   state.dateEndConn)
            )
          )
        }
      )
    }

  }

  /** Дедублицированный код рендера инпута одной даты. */
  private def _renderDateInput(fn: DtpInputFn, dateConn: ReactConnectProxy[Option[MYmd]]) = {
    dateConn { dateProxy =>
      dateProxy().fold[ReactElement](null) { _ =>
        DateInput(
          DateInput.Props(fn, dateProxy.zoom(_.get))
        )
      }
    }
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
