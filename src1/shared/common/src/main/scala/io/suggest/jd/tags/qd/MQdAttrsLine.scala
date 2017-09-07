package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.primo.ISetUnset
import io.suggest.text.MTextAlign
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.09.17 16:02
  * Description: Аттрибуты строки.
  */
object MQdAttrsLine {

  /** Поддержка play-json. */
  implicit val QD_ATTRS_LINE_FORMAT: OFormat[MQdAttrsLine] = (
    (__ \ "h").formatNullable[ISetUnset[Int]] and
    (__ \ "t").formatNullable[ISetUnset[MQdListType]] and
    (__ \ "n").formatNullable[ISetUnset[Int]] and
    (__ \ "c").formatNullable[ISetUnset[Boolean]] and
    (__ \ "b").formatNullable[ISetUnset[Boolean]] and
    (__ \ "a").formatNullable[ISetUnset[MTextAlign]]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MQdAttrsLine] = UnivEq.derive

}


case class MQdAttrsLine(
                         header      : Option[ISetUnset[Int]]           = None,
                         list        : Option[ISetUnset[MQdListType]]   = None,
                         indent      : Option[ISetUnset[Int]]           = None,
                         codeBlock   : Option[ISetUnset[Boolean]]       = None,
                         blockQuote  : Option[ISetUnset[Boolean]]       = None,
                         align       : Option[ISetUnset[MTextAlign]]    = None
                       )
  extends EmptyProduct
