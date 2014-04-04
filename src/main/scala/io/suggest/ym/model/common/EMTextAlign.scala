package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:43
 * Description: ta-поле, используемое для характеристики ориентации текста в публикуемых материалах.
 */

object EMTextAlign {
  val TEXT_ALIGN_ESFN = "textAlign"
}

import EMTextAlign._


trait EMTextAlignStatic[T <: EMTextAlign[T]] extends EsModelStaticT[T] {
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

  var textAlign    : Option[TextAlign]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (textAlign.isDefined)
      acc.rawField(TEXT_ALIGN_ESFN, JacksonWrapper.serialize(textAlign.get).getBytes)
  }
}


case class TextAlign(phone: TextAlignPhone, tablet: TextAlignTablet)
case class TextAlignTablet(alignTop: String, alignBottom: String)
case class TextAlignPhone(align: String)
