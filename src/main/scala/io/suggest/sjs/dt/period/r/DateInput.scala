package io.suggest.sjs.dt.period.r

import io.suggest.adv.AdvConstants
import io.suggest.common.qs.QsConstants
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.lk.r.PropTable
import io.suggest.lk.r.Forms.InputCont
import japgolly.scalajs.react.ReactComponentB

/** Поле ввода даты. */
object DateInput {

  case class Props(fn: String, value0: Option[String])

  val component = ReactComponentB[Props]("DtpDateInput")
    .initialState_P(_.value0.getOrElse(""))
    .renderPS { (_, props, state) =>
      PropTable.Outer(
        PropTable.Row(
          Messages( "Date." + props.fn ),

          InputCont(
            <.input(
              ^.`type` := "text",
              ^.name   := (AdvConstants.PERIOD_FN + QsConstants.KEY_PARTS_DELIM_STR + AdvConstants.DtPeriod.DATES_INTERVAL_FN + QsConstants.KEY_PARTS_DELIM_STR + props.fn),
              ^.value  := state
            )
          )
        )
      )
    }
    .build

  def apply(props: Props) = component(props)

}
