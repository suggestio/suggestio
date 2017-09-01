package io.suggest.jd.tags.qd

import io.suggest.common.empty.EmptyProduct
import io.suggest.model.n2.node.meta.colors.MColorData
import io.suggest.primo.ISetUnset
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 15:45
  * Description: Модель аттрибутов текста.
  */
object MQdAttrs {

  /** Поддержка play-json. */
  implicit val QD_ATTRS_FORMAT: OFormat[MQdAttrs] = (
    (__ \ "b").formatNullable[ISetUnset[Boolean]] and
    (__ \ "i").formatNullable[ISetUnset[Boolean]] and
    (__ \ "u").formatNullable[ISetUnset[Boolean]] and
    (__ \ "c").formatNullable[ISetUnset[MColorData]] and
    (__ \ "l").formatNullable[ISetUnset[String]] and
    (__ \ "h").formatNullable[ISetUnset[Int]] and
    (__ \ "s").formatNullable[ISetUnset[String]] and
    (__ \ "t").formatNullable[ISetUnset[MQdListType]]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MQdAttrs] = UnivEq.derive

}


/** Класс модели аттрибутов quill-delta-операции. */
case class MQdAttrs(
                     bold        : Option[ISetUnset[Boolean]]       = None,
                     italic      : Option[ISetUnset[Boolean]]       = None,
                     underline   : Option[ISetUnset[Boolean]]       = None,
                     color       : Option[ISetUnset[MColorData]]    = None,
                     link        : Option[ISetUnset[String]]        = None,
                     header      : Option[ISetUnset[Int]]           = None,
                     src         : Option[ISetUnset[String]]        = None,
                     list        : Option[ISetUnset[MQdListType]]   = None
                   )
  extends EmptyProduct
