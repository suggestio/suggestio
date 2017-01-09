package io.suggest.react.r

import io.suggest.dt.interval.MRangeYmd
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

  final case class Props(capFirst: Boolean, rangeYmd: MRangeYmd, Messages: JsMessagesSingleLang)


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

      val v = p.rangeYmd
      val sameYear = v.dateStart.year == v.dateEnd.year
      <.span(

        // с [дата начала]
        SPACE,
        Messages( (if (p.capFirst) "F" else "f") + "rom._date" ),
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

  }

  val component = ReactComponentB[Props]("RangeYmd")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
