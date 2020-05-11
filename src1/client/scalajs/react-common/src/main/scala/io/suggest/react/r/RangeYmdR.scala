package io.suggest.react.r

import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.common.html.HtmlConstants.SPACE
import io.suggest.dt.MYmd
import io.suggest.msg.{JsFormatUtil, Messages}
import japgolly.scalajs.react.{BackendScope, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.12.16 11:36
  * Description: Компонент для максимально краткого рендера диапазона дат.
  */
object RangeYmdR {

  final case class Props(
                          capFirst: Boolean,
                          rangeYmdOpt: MRangeYmdOpt
                        )

  class Backend($: BackendScope[Props, Unit]) {

    def render(p: Props): VdomElement = {

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

        v.dateStartOpt.whenDefined { dateStart =>
          <.span(
            // В подсказке содержится день недели.
            YmdR.dowTitle(dateStart),
            // В теле содержится дата:
            dateStart.day,

            // Дата начала может быть отрендерена неполностью. Если год/месяц совпадают с датой окончания:
            <.span(
              SPACE,
              JsFormatUtil.formatMonth( dateStart ),
              <.span(SPACE, dateStart.year)
                .unless( isSameYear )
            )
              .when( !isSameYear || v.dateEndOpt.isEmpty || !v.dateEndOpt.map(_.month).contains( dateStart.month ) ),
            SPACE
          )
        },

        v.dateEndOpt.whenDefined { dateEnd =>
          <.span(
            // до [дата окончания]
            Messages("till._date"),
            SPACE,
            YmdR( dateEnd )()
          )
        },

        // г.
        SPACE,
        Messages("year_abbrevated")
      )
    }

  }

  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .stateless
    .renderBackend[Backend]
    .build

  def apply(props: Props) = component(props)

}
