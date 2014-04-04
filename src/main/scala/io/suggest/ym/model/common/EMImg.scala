package io.suggest.ym.model.common

import io.suggest.util.SioEsUtil._
import io.suggest.model.{EsModelT, EsModelStaticT}
import org.elasticsearch.common.xcontent.XContentBuilder

/**
 * Suggest.io
 * Userи : Konstantin обязательной Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:20
 * Description: Поле основной и обязательной картинки.
 */

object EMImg {
  val IMG_ESFN = "img"
  def esMappingField = FieldObject(IMG_ESFN, enabled = false, properties = Nil)
}

import EMImg._


trait EMImgStatic[T <: EMImg[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    esMappingField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (IMG_ESFN, value)  =>
        acc.img = MImgInfo.convertFrom(value)
    }
  }
}

trait EMImg[T <: EMImg[T]] extends EsModelT[T] {

  var img: MImgInfo

  abstract override def writeJsonFields(acc: XContentBuilder): Unit = {
    super.writeJsonFields(acc)
    acc.rawField(IMG_ESFN, img.toJson.getBytes)
  }
}