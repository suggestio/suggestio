package io.suggest.img

import io.suggest.img.crop.MCrop
import io.suggest.jd.MJdEdgeId
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.09.17 12:40
  * Description: Модель описания изображения с накладываемыми на него операциями.
  */
object MImgEdgeWithOps {

  /** Поддержка play-json. */
  implicit val MIMG_EDGE_WITH_OPS_FORMAT: OFormat[MImgEdgeWithOps] = (
    (__ \ "e").format[MJdEdgeId] and
    (__ \ "c").formatNullable[MCrop]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MImgEdgeWithOps] = UnivEq.derive

}


/** Класс модели данных по картинке и её модификациям.
  *
  * @param imgEdge Данные доступа к эджу изображения.
  * @param crop Кроп текущего изображения, если есть.
  */
case class MImgEdgeWithOps(
                            imgEdge   : MJdEdgeId,
                            crop      : Option[MCrop]     = None
                          ) {

  def withImgEdge(imgEdge: MJdEdgeId) = copy(imgEdge = imgEdge)
  def withCrop(crop: Option[MCrop])     = copy(crop = crop)

}
