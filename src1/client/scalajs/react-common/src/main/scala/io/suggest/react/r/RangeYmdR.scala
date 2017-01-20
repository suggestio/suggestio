package io.suggest.react.r

import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.dt.MYmd
import io.suggest.sjs.common.dt.MYmdJs
import io.suggest.sjs.common.i18n.JsMessagesSingleLang
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.12.16 11:36
  * Description: Компонент для максимально краткого рендера диапазона дат.
  */
object RangeYmdR {

  final case class Props(capFirst: Boolean, rangeYmdOpt: MRangeYmdOpt, Messages: JsMessagesSingleLang)


  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {
      import p.Messages

      /** Рендер title-аттрибута для даты. */
      def _dateTitle(ymd: MYmd) = {
        val jsDate = MYmdJs.toJsDate(ymd)
        val sioDow = jsDate.getDay() + 1
        ^.title := Messages("DayOfWeek.N." + sioDow)
      }

      def _dateMonth(ymd: MYmd) = Messages("ofMonth.N." + ymd.month)

      val v = p.rangeYmdOpt

      val yearF = { ymd: MYmd => ymd.year }
      val startYearOpt = v.dateStartOpt.map(yearF)
      val endYearOpt = v.dateEndOpt.map(yearF)
      val isSameYear = startYearOpt == endYearOpt

      <.span(

        // с [дата начала]
        SPACE,
        Messages( (if (p.capFirst) "F" else "f") + "rom._date" ),
        SPACE,

        for (dateStart <- v.dateStartOpt) yield {
          <.span(
            // В подсказке содержится день недели.
            _dateTitle(dateStart),
            // В теле содержится дата:
            dateStart.day,

            // Дата начала может быть отрендерена неполностью. Если год/месяц совпадают с датой окончания:
            (!isSameYear || v.dateEndOpt.isEmpty || !v.dateEndOpt.map(_.month).contains( dateStart.month ) ) ?= <.span(
              SPACE,
              _dateMonth(dateStart),
              !isSameYear ?= <.span(SPACE, dateStart.year)
            ),
            // до [дата окончания]
            SPACE
          )
        },

        for (dateEnd <- v.dateEndOpt) yield {
          <.span(
            Messages("till._date"),
            SPACE,
            // В подсказке содержится день недели.
            _dateTitle(dateEnd),
            dateEnd.day,
            SPACE,
            _dateMonth(dateEnd),
            SPACE,
            dateEnd.year
          )
        },

        // г.
        SPACE,
        Messages("year_abbrevated")
      )
    }

  }

  val component = ReactComponentB[Props]("RangeYmd")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
