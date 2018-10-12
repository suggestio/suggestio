package io.suggest.sys.mdr

import io.suggest.common.html.HtmlConstants
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.18 18:01
  * Description: Модель инфы по длине очереди на модерацию.
  */
object MMdrQueueReport {

  /** Поддержка play-json. */
  implicit def mMdrQueueReportFormat: OFormat[MMdrQueueReport] = (
    (__ \ "l").format[Int] and
    (__ \ "m").format[Boolean]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MMdrQueueReport] = UnivEq.derive

}

case class MMdrQueueReport(
                            len         : Int,
                            hasMore     : Boolean
                          ) {

  /** Рендер в человеко-читабельную строку. */
  def toHumanReadableString: String = {
    var s = len.toString
    if (hasMore)
      s += HtmlConstants.PLUS
    s
  }

  override def toString: String =
    toHumanReadableString

  def withLen(len: Int) = copy(len = len)

}
