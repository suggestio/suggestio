package io.suggest.stat.m

import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 14:11
  * Description: json-модель данных по viewport'у.
  */
object MViewPort extends IGenEsMappingProps {

  object Fields {

    val WIDTH_PX_FN   = "width"
    val HEIGHT_PX_FN  = "height"
    val PX_RATIO_FN   = "pxRatio"

  }


  import Fields._

  implicit val FORMAT: OFormat[MViewPort] = (
    (__ \ WIDTH_PX_FN).format[Int] and
    (__ \ HEIGHT_PX_FN).format[Int] and
    (__ \ PX_RATIO_FN).formatNullable[Float]
  )(apply, unlift(unapply))


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldNumber(WIDTH_PX_FN,  fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldNumber(HEIGHT_PX_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldNumber(PX_RATIO_FN,  fieldType = DocFieldTypes.float, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/**
  * Модель статистических данных viewport'а.
  * @param widthPx Ширина в пикселях.
  * @param heightPx Высота в пикселях.
  * @param pxRatio Плотность пискелей.
  */
case class MViewPort(
  widthPx   : Int,
  heightPx  : Int,
  pxRatio   : Option[Float]
)
