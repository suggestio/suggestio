package io.suggest.react.r

import io.suggest.common.html.HtmlConstants
import io.suggest.dt.MYmd
import io.suggest.sjs.common.i18n.JsFormatUtil
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.04.17 16:51
  * Description: React-компонент для рендера даты MYmd.
  */
object YmdR {

  /** Рендер title-аттрибута для даты. */
  def dowTitle(ymd: MYmd): TagMod = {
    ^.title := JsFormatUtil.formatDayOfWeek(ymd)
  }


  val component = ReactComponentB[MYmd]("Ymd")
    .stateless
    .renderPC { (_, mYmd, pc) =>
      <.span(
        // В подсказке содержится день недели.
        dowTitle(mYmd),
        mYmd.day,

        HtmlConstants.SPACE,
        JsFormatUtil.formatMonth( mYmd ),

        HtmlConstants.SPACE,
        mYmd.year,

        pc
      )
    }
    .build

  def apply(mYmd: MYmd)(children: ReactNode*) = component(mYmd, children: _*)

}
