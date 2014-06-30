package io.suggest.ym.model.common

import io.suggest.model._
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.{JsString, JsArray}
import java.{lang => jl}
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.06.14 18:45
 * Description: Поле для картинной галереи. Хранит последовательность img id, сохраняя её порядок.
 * Дедубликация картинок -- на совести клиента.
 */
object EMImgGallery {

  val IMG_GALLERY_ESFN = "ig"

}


import EMImgGallery._


trait EMImgGalleryStatic extends EsModelStaticT {
  override type T <: EMImgGalleryMut

  abstract override def generateMappingProps: List[DocField] = {
    val acc0 = super.generateMappingProps
    FieldString(IMG_GALLERY_ESFN, index = FieldIndexingVariants.no, include_in_all = false) :: acc0
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (IMG_GALLERY_ESFN, v: jl.Iterable[_]) =>
        acc.imgGallery = v
          .foldLeft(List.empty[String]) {
            (acc, e) => EsModel.stringParser(e) :: acc
          }
          .reverse
    }
  }
}

trait EMImgGallery extends EsModelT {
  override type T <: EMImgGallery
  def imgGallery: List[String]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc1 = super.writeJsonFields(acc)
    if (imgGallery.isEmpty) {
      acc1
    } else {
      val v = JsArray( imgGallery.map(JsString.apply) )
      IMG_GALLERY_ESFN -> v :: acc1
    }
  }
}

trait EMImgGalleryMut extends EMImgGallery {
  override type T <: EMImgGalleryMut
  var imgGallery: List[String]
}
