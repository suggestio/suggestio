package io.suggest.sjs.dt.period.r

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.dt.interval.MRangeYmd
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.react.r.RangeYmdR
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:49
  * Description: Реализация виджета выбора периода размещения на базе react.js + diode.
  * Шаблон и некоторая логика в одном флаконе.
  *
  * Эксплуатировать сий код надо надо с помощью modelProxy.connect().
  */


/** Отчёт о датах размещения. */
object DtpResult {

  /** Статическая визуальная обёртка для [[DtpResult]].component. */
  val Outer = ReactComponentB[Unit]("DtpResOut")
    .stateless
    .render_C { propsChildren =>
      <.div(
        ^.`class` := Css.Dt.RESULT,

        // Статический заголовок по рассчётным датам размещения:
        <.div(
          ^.`class` := (Css.Block.BLOCK :: Css.Margin.S :: Nil).mkString(SPACE),
          Messages("Your.ad.will.adv")
        ),

        propsChildren
      )
    }
    .build

  type Props = ModelProxy[MRangeYmd]

  // Этот компонент надо использовать через proxy.connect().
  val component = ReactComponentB[Props]("DtpRes")
    .stateless
    .render_P { p =>
      val v = p()

      <.div(
        ^.`class` := Css.Dt.RESULT_VALUE,

        // с [дата начала]
        SPACE,
        RangeYmdR(
          RangeYmdR.Props(
            capFirst    = false,
            rangeYmdOpt = v.toRangeYmdOpt,
            Messages    = Messages
          )
        )
      )
    }
    .build

  def apply(props: Props) = component(props)

}

