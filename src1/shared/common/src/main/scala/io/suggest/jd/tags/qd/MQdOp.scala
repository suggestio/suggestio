package io.suggest.jd.tags.qd

import io.suggest.color.MColorData
import io.suggest.jd.MJdEdgeId
import io.suggest.jd.tags.IJdElement
import io.suggest.primo.SetVal
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scalaz.Tree

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

  implicit val QD_OP_FORMAT: OFormat[MQdOp] = (
    (__ \ "y").format[MQdOpType] and
    (__ \ "g").formatNullable[MJdEdgeId] and
    (__ \ "i").formatNullable[Int] and
    (__ \ "a").formatNullable[MQdAttrsText] and
    (__ \ "l").formatNullable[MQdAttrsLine] and
    (__ \ "e").formatNullable[MQdAttrsEmbed]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MQdOp] = UnivEq.derive

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
  extends IJdElement
{

  def withAttrsText(attrsText: Option[MQdAttrsText]) = copy(attrsText = attrsText)


  override def deepElMap(f: (IJdElement, IJdElement) => IJdElement): MQdOp = {
    f(this, this)
      .asInstanceOf[MQdOp]
  }

  override def bgImgEdgeId = edgeInfo

  override def setBgColor(bgColor: Option[MColorData]): MQdOp = {
    val attrsText0 = attrsText.getOrElse(MQdAttrsText.empty)
    withAttrsText(
      Some(
        attrsText0.withBackground(
          bgColor.map { SetVal.apply }
        )
      )
    )
  }

  override def toScalazTree: Tree[IJdElement] = {
    Tree.Leaf(this)
  }

}

