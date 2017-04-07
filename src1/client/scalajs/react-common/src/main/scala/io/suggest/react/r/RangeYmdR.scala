package io.suggest.react.r

import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.dt.MYmd
import io.suggest.sjs.common.i18n.{JsFormatUtil, Messages}
import japgolly.scalajs.react.{BackendScope, ReactComponentB, ReactElement}
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.12.16 11:36
  * Description: Компонент для максимально краткого рендера диапазона дат.
  */
object RangeYmdR {

  final case class Props(capFirst: Boolean, rangeYmdOpt: MRangeYmdOpt)

  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): ReactElement = {

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
            YmdR.dowTitle(dateStart),
            // В теле содержится дата:
            dateStart.day,

            // Дата начала может быть отрендерена неполностью. Если год/месяц совпадают с датой окончания:
            (!isSameYear || v.dateEndOpt.isEmpty || !v.dateEndOpt.map(_.month).contains( dateStart.month ) ) ?= <.span(
              SPACE,
              JsFormatUtil.formatMonth( dateStart ),
              !isSameYear ?= <.span(SPACE, dateStart.year)
            ),
            SPACE
          ): ReactElement
        },

        for (dateEnd <- v.dateEndOpt) yield {
          <.span(
            // до [дата окончания]
            Messages("till._date"),
            SPACE,
            YmdR( dateEnd )()
          ): ReactElement
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
