package io.suggest.sjs.dt.period.r

import diode.react.ModelProxy
import io.suggest.adv.AdvConstants
import io.suggest.common.qs.QsConstants
import io.suggest.dt.MYmd
import japgolly.scalajs.react.vdom.prefix_<^._
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.lk.r.PropTable
import io.suggest.lk.r.Forms.InputCont
import io.suggest.sjs.dt.period.m.{SetDateStartEnd, DtpInputFn}
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB, ReactElement, ReactEventI}

/** Поле ввода даты. */
object DateInput {

  case class Props(fn: DtpInputFn, value: ModelProxy[MYmd])

  protected class Backend($: BackendScope[Props, Unit]) {

    def onChange(e: ReactEventI): Callback = {
      val newStr = e.target.value
      $.props >>= { props =>
        props.value.dispatchCB(
          SetDateStartEnd(
            fn      = props.fn,
            ymdStr  = newStr
          )
        )
      }
    }

    def render(props: Props): ReactElement = {
      PropTable.Outer(
        PropTable.Row(
          Messages( "Date." + props.fn.strId ),

          InputCont(
            <.input(
              ^.`type`    := "text",
              ^.name      := (AdvConstants.PERIOD_FN :: AdvConstants.DtPeriod.DATES_INTERVAL_FN :: props.fn :: Nil).mkString( QsConstants.KEY_PARTS_DELIM_STR ),
              ^.value     := props.value().toString,
              ^.onChange ==> onChange
            )
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("DateInput")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
