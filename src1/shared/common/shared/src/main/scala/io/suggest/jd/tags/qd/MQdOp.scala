package io.suggest.jd.tags.qd

import io.suggest.jd.MJdEdgeId
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 14:38
  * Description: jd-тег, кодирующий одну операцию в quill-delta.
  *
  * Содержимое кодируется эджами, id которого лежит в edgeInfo.
  * predicate=Text - значит текст
  * predicate=Image|Video|... - значит embed.
  */
object MQdOp {

  implicit def QD_OP_FORMAT: OFormat[MQdOp] = (
    (__ \ "y").format[MQdOpType] and
    (__ \ "g").formatNullable[MJdEdgeId] and
    (__ \ "i").formatNullable[Int] and
    (__ \ "a").formatNullable[MQdAttrsText] and
    (__ \ "l").formatNullable[MQdAttrsLine] and
    (__ \ "e").formatNullable[MQdAttrsEmbed]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MQdOp] = UnivEq.derive

  def opType      = GenLens[MQdOp](_.opType)
  def edgeInfo    = GenLens[MQdOp](_.edgeInfo)
  def index       = GenLens[MQdOp](_.index)
  def attrsText   = GenLens[MQdOp](_.attrsText)
  def attrsLine   = GenLens[MQdOp](_.attrsLine)
  def attrsEmbed  = GenLens[MQdOp](_.attrsEmbed)

}


/** Класс модели описания одной quill-delta операции.
  *
  * @param opType Тип операции: insert, delete, retain.
  * @param edgeInfo embed, живущий среди эджей узла.
  * @param index Индекс для retain/delete операций.
  * @param attrsText Аттрибуты рендера текста.
  * @param attrsLine Аттрибуты рендера текущей строки.
  */
case class MQdOp(
                  opType     : MQdOpType,
                  edgeInfo   : Option[MJdEdgeId]      = None,
                  index      : Option[Int]            = None,
                  attrsText  : Option[MQdAttrsText]   = None,
                  attrsLine  : Option[MQdAttrsLine]   = None,
                  attrsEmbed : Option[MQdAttrsEmbed]  = None
               )
