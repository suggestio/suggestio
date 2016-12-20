package io.suggest.sjs.dt.period.r

import diode.react.{ModelProxy, ReactConnectProxy}
import io.suggest.css.Css
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.dt.period.m.{IDateInfo, IDatesPeriodInfo}
import japgolly.scalajs.react.ReactComponentB
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.16 17:49
  * Description: Реализация виджета выбора периода размещения на базе react.js + diode.
  * Шаблон и некоторая логика в одном флаконе.
  *
  * Эксплуатировать сий код надо надо с помощью wrap, т.е. как-то так:
  * {{{
  *   DtpCont(
  *     m.wrap(_.periodInfo)( DtpResult.apply )
  *   )
  * }}}
  */



/** Отчёт о датах размещения. */
object DtpResult {

  val ResValDate = ReactComponentB[ModelProxy[IDateInfo]]("DtpResDate")
    .stateless
    .render_P { props =>
      val v = props()
      <.span(
        ^.title := v.dow,
        v.date
      )
    }
    .build

  def _renderDate(conn: ReactConnectProxy[IDateInfo]) = conn( ResValDate(_) )


  type Props = ModelProxy[IDatesPeriodInfo]

  /** Состояние содержит model-коннекшены до значений начальной и конечной дат. */
  case class State(
                    dateStartConn : ReactConnectProxy[IDateInfo],
                    dateEndConn   : ReactConnectProxy[IDateInfo]
                  )

  val component = ReactComponentB[Props]("DtpRes")
    .initialState_P { props =>
      State(
        dateStartConn = props.connect(_.start),
        dateEndConn   = props.connect(_.end)
      )
    }
    .render_S { state =>
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
          _renderDate( state.dateStartConn ),

          // до [дата окончания]
          Messages( "till._date" ),
          _renderDate( state.dateEndConn )
        )
      )
    }
    .build

  def apply(props: Props) = component(props)

}

