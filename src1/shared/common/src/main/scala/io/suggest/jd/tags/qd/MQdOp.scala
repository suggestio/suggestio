package io.suggest.jd.tags.qd

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:38
  * Description: jd-тег, кодирующий одну операцию в quill-delta.
  */
object MQdOp {

  implicit val QD_OP_FORMAT: OFormat[MQdOp] = (
    (__ \ "y").format[MQdOpType] and
    (__ \ "g").formatNullable[MQdEdgeInfo] and
    (__ \ "x").formatNullable[MEmbedExt] and
    (__ \ "i").formatNullable[Int] and
    (__ \ "a").formatNullable[MQdAttrsText] and
    (__ \ "l").formatNullable[MQdAttrsLine]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MQdOp] = UnivEq.derive

}


/** Класс модели описания одной quill-delta операции.
  *
  * @param opType Тип операции: insert, delete, retain.
  * @param edgeInfo embed, живущий среди эджей узла.
  * @param extEmbed Внешний embed, описанный прямо здесь.
  * @param index Индекс для retain/delete операций.
  * @param attrsText Аттрибуты рендера текста.
  * @param attrsLine Аттрибуты рендера текущей строки.
  */
case class MQdOp(
                  opType     : MQdOpType,
                  edgeInfo   : Option[MQdEdgeInfo]  = None,
                  extEmbed   : Option[MEmbedExt]    = None,
                  index      : Option[Int]          = None,
                  attrsText  : Option[MQdAttrsText] = None,
                  attrsLine  : Option[MQdAttrsLine] = None
               )

