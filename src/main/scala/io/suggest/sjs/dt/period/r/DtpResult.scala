package io.suggest.sjs.dt.period.r

import diode.react.ModelProxy
import io.suggest.css.Css
import io.suggest.dt.interval.MRangeYmd
import io.suggest.dt.MYmd
import io.suggest.lk.vm.LkMessagesWindow.Messages
import io.suggest.sjs.common.dt.MYmdJs
import io.suggest.common.html.HtmlConstants.SPACE
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


  /** Рендер title-аттрибута для даты. */
  private def _dateTitle(ymd: MYmd) = {
    val jsDate = MYmdJs.toJsDate(ymd)
    val sioDow = jsDate.getDay() + 1
    ^.title := Messages("DayOfWeek.N." + sioDow)
  }

  /** Рендер месяца в человеко-читабельном виде. */
  private def _dateMonth(ymd: MYmd) = Messages("ofMonth.N." + ymd.month)

  type Props = ModelProxy[MRangeYmd]

  // Этот компонент надо использовать через proxy.connect().
  val component = ReactComponentB[Props]("DtpRes")
    .stateless
    .render_P { p =>
      val v = p()
      val sameYear = v.dateStart.year == v.dateEnd.year

      <.div(
        ^.`class` := Css.Dt.RESULT_VALUE,

        // с [дата начала]
        SPACE,
        Messages( "from._date" ),
        SPACE,

        <.span(
          // В подсказке содержится день недели.
          _dateTitle(v.dateStart),
          // В теле содержится дата:
          v.dateStart.day,

          // Дата начала может быть отрендерена неполностью. Если год/месяц совпадают с датой окончания:
          (!sameYear || v.dateStart.month != v.dateEnd.month) ?= <.span(
            SPACE,
            _dateMonth(v.dateStart),
            !sameYear ?= <.span(SPACE, v.dateStart.year)
          )
        ),

        // до [дата окончания]
        SPACE,
        Messages( "till._date" ),
        SPACE,
        <.span(
          // В подсказке содержится день недели.
          _dateTitle(v.dateEnd),
          v.dateEnd.day,
          SPACE,
          _dateMonth(v.dateEnd),
          SPACE,
          v.dateEnd.year
        ),

        // г.
        SPACE,
        Messages("year_abbrevated")
      )
    }
    .build

  def apply(props: Props) = component(props)

}

