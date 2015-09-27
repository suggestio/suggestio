package io.suggest.model.n2.media

import io.suggest.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 18:47
 * Description: Модель метаданных по картинке в рамках модели [[MMedia]].
 * Модель может использоваться как характеристики для файлов изображений, так и для видео-файлов.
 */
object MPictureMeta extends IGenEsMappingProps {

  val WIDTH_FN    = "w"
  val HEIGHT_FN   = "h"

  /** Поддержка JSON сериализации-десериализации. */
  implicit val FORMAT: OFormat[MPictureMeta] = (
    (__ \ WIDTH_FN).format[Int] and
    (__ \ HEIGHT_FN).format[Int]
  )(apply, unlift(unapply))


  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    def _n(fn: String) = {
      FieldNumber(fn, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.no, include_in_all = false)
    }
    List(
      _n(WIDTH_FN),
      _n(HEIGHT_FN)
    )
  }

}


case class MPictureMeta(
  width   : Int,
  height  : Int
)
