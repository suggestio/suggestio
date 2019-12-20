package io.suggest.stat.m

import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.09.16 14:11
  * Description: json-модель данных по viewport'у.
  */
object MViewPort
  extends IEsMappingProps
{

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


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val fInt = FNumber(
      typ   = DocFieldTypes.Integer,
      index = someTrue,
    )
    val F = Fields

    Json.obj(
      F.WIDTH_PX_FN  -> fInt,
      F.HEIGHT_PX_FN -> fInt,
      F.PX_RATIO_FN  -> FNumber(
        typ   = DocFieldTypes.Float,
        index = someTrue,
      ),
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
