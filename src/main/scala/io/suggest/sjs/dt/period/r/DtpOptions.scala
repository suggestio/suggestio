package io.suggest.sjs.dt.period.r

import io.suggest.adv.AdvConstants
import io.suggest.common.qs.QsConstants
import io.suggest.css.Css
import io.suggest.lk.r.PropTable
import io.suggest.lk.r.Forms.InputCont
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.dt.interval.{QuickAdvPeriod, QuickAdvPeriods}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/** Рендер Options-раздела целиком. */
object DtpOptions {

  case class State(
                    qap         : QuickAdvPeriod,
                    customDates : Option[(String, String)] = None
                  )

  case class Props(
                    onChange  : () => Unit,
                    state0    : State = State(QuickAdvPeriods.default)
                  )


  class Backend($: BackendScope[Props, State]) {

    /** Реакция на смену значения в селекте периода размещения. */
    def onQapChange(e: ReactEventI): Callback = {
      QuickAdvPeriods.maybeWithName( e.target.value )
        .fold(Callback.empty) { qap2 =>
          for {
            _     <- $.modState { _.copy(qap = qap2) }
            props <- $.props
          } yield {
            props.onChange()
          }
        }
    }

    def render(state: State) = {
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
              <.select(
                ^.name := ( AdvConstants.PERIOD_FN + QsConstants.KEY_PARTS_DELIM_STR + AdvConstants.DtPeriod.QUICK_PERIOD_FN ),
                ^.value := state.qap.strId,
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
            )
          )
        ),

        (state.qap == QuickAdvPeriods.Custom) ?= <.div(
          DateInput(
            DateInput.Props("start", state.customDates.map(_._1))
          ),
          DateInput(
            DateInput.Props("end", state.customDates.map(_._2))
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("DtpOptions")
    .initialState_P( _.state0 )
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
