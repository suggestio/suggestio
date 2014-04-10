package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import play.api.libs.json._
import io.suggest.model.EsModel.FieldsJsonAcc

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:43
 * Description: ta-поле, используемое для характеристики ориентации текста в публикуемых материалах.
 */

object EMTextAlign {
  val TEXT_ALIGN_ESFN = "textAlign"

  val ALIGN_TOP_ESFN = "alignTop"
  val ALIGN_BOTTOM_ESFN = "alignBottom"
  val ALIGN_ESFN = "align"
  val PHONE_ESFN = "phone"
  val TABLET_ESFN = "tablet"
}

import EMTextAlign._


trait EMTextAlignStatic[T <: EMTextAlignMut[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(TEXT_ALIGN_ESFN,  enabled = false,  properties = Nil) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (TEXT_ALIGN_ESFN, value) =>
        acc.textAlign = Option(JacksonWrapper.convert[TextAlign](value))
    }
  }
}

trait EMTextAlign[T <: EMTextAlign[T]] extends EsModelT[T] {

  def textAlign: Option[TextAlign]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (textAlign.isDefined)
      TEXT_ALIGN_ESFN -> textAlign.get.toPlayJson :: acc0
    else
      acc0
  }
}

trait EMTextAlignMut[T <: EMTextAlignMut[T]] extends EMTextAlign[T] {
  var textAlign: Option[TextAlign]
}


case class TextAlign(phone: TextAlignPhone, tablet: TextAlignTablet) {
  def toPlayJson: JsObject = {
    JsObject(Seq(
      PHONE_ESFN  -> phone.toPlayJson,
      TABLET_ESFN -> tablet.toPlayJson
    ))
  }
}
case class TextAlignTablet(alignTop: String, alignBottom: String) {
  def toPlayJson: JsObject = {
    JsObject(Seq(
      ALIGN_TOP_ESFN -> JsString(alignTop),
      ALIGN_BOTTOM_ESFN -> JsString(alignBottom)
    ))
  }
}
case class TextAlignPhone(align: String) {
  def toPlayJson: JsObject = {
    JsObject(Seq(
      ALIGN_ESFN -> JsString(align)
    ))
  }
}


/** Допустимые значения textAlign-полей. */
// TODO Пока используется только в веб-морде. Может туда её и переместить?
object AOTextAlignValues extends Enumeration {
  type TextAlignValue = Value
  val left, right = Value

  def maybeWithName(n: String): Option[TextAlignValue] = {
    try {
      Some(withName(n))
    } catch {
      case _: Exception => None
    }
  }
}
