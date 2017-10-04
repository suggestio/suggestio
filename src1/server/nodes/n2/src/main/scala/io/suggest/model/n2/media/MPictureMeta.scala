package io.suggest.model.n2.media

import io.suggest.common.geom.d2.ISize2di
import io.suggest.es.model.IGenEsMappingProps
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


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    def _n(fn: String) = {
      // TODO Тут index = false почему-то, хз.
      FieldNumber(fn, fieldType = DocFieldTypes.integer, index = false, include_in_all = false)
    }
    List(
      _n(WIDTH_FN),
      _n(HEIGHT_FN)
    )
  }

  // TODO Удалить этот днищще-метод. wh должно быть полем этой модели.
  def apply(sz2d: ISize2di): MPictureMeta = {
    MPictureMeta(
      width   = sz2d.width,
      height  = sz2d.height
    )
  }

}


case class MPictureMeta(
  override val width   : Int,
  override val height  : Int
)
  extends ISize2di
