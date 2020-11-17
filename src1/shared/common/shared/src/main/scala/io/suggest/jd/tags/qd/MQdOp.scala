package io.suggest.jd.tags.qd

import io.suggest.jd.MJdEdgeId
import io.suggest.text.StringUtil
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

  object Fields {
    def OP_TYPE = "y"
    def EDGE_INFO = "g"
    def INDEX = "i"
    def ATTRS_TEXT = "a"
    def ATTRS_LINE = "l"
    def ATTRS_EMBED = "e"
  }

  implicit def QD_OP_FORMAT: OFormat[MQdOp] = {
    val F = Fields
    (
      (__ \ F.OP_TYPE).format[MQdOpType] and
      (__ \ F.EDGE_INFO).formatNullable[MJdEdgeId] and
      (__ \ F.INDEX).formatNullable[Int] and
      (__ \ F.ATTRS_TEXT).formatNullable[MQdAttrsText] and
      (__ \ F.ATTRS_LINE).formatNullable[MQdAttrsLine] and
      (__ \ F.ATTRS_EMBED).formatNullable[MQdAttrsEmbed]
    )(apply, unlift(unapply))
  }

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
  * @param edgeInfo Данные эджа до текста или embed'а, живущего среди эджей узла.
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
               ) {

  override def toString: String = {
    StringUtil.toStringHelper(this, 128) { renderF =>
      val F = MQdOp.Fields
      val render0 = renderF("")
      render0(opType)
      edgeInfo foreach render0
      index foreach renderF( F.INDEX )
      attrsText foreach render0
      attrsLine foreach render0
      attrsEmbed foreach render0
    }
  }

}
