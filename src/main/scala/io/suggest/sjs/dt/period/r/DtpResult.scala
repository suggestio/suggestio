package io.suggest.sjs.dt.period.r

import io.suggest.css.Css
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.dt.period.m.{IDateInfo, IDatesPeriodInfo}
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:49
  * Description: Реализация виджета выбора периода размещения на базе react.js.
  * Шаблон и логика в одном флаконе.
  *
  * Эксплуатировать сий код надо вот так:
  * {{{
  *   DtpCont(
  *     DtpOptions(...),  // (1) Передать исходные параметры.
  *     DtpResult(...)    // (2) Значения пока что обсчитываются и рендерятся на сервере на основе (1)
  *                       // вместе с ценой (которая обитает вне этого модуля).
  *   )
  * }}}
  */



/** Отчёт о датах размещения. */
object DtpResult {

  val ResValDate = ReactComponentB[IDateInfo]("DtpResDate")
    .stateless
    .render_P { props =>
      <.span(
        ^.title := props.dow,
        props.date
      )
    }
    .build


  val component = ReactComponentB[IDatesPeriodInfo]("DtpResVal")
    .stateless
    .render_P { props =>
      // Контейнер
      <.div(
        ^.`class` := Css.Dt.RESULT,

        // Статический заголовок по рассчётным датам размещения:
        <.div(
          ^.`class` := (Css.Block.BLOCK :: Css.Margin.S :: Nil).mkString(" "),
          Messages( "Your.ad.will.adv" )
        ),

        // Содержимое
        <.div(
          ^.`class` := Css.Dt.RESULT_VALUE,

          // с [дата начала]
          Messages( "from._date" ),
          ResValDate(props.start),

          // до [дата окончания]
          Messages( "till._date" ),
          ResValDate(props.end)
        )
      )
    }
    .build

  def apply(props: IDatesPeriodInfo) = component(props)

}

