package io.suggest.img

import io.suggest.common.geom.d2.ISize2di
import io.suggest.err.ErrorConstants
import io.suggest.img.crop.MCrop
import io.suggest.jd.{MJdEdgeId, MJdEdgeVldInfo}
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.scalaz.ScalazUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scalaz.{Validation, ValidationNel}
import scalaz.syntax.apply._

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


  /** Валидация инстанса [[MImgEdgeWithOps]] с помощью остальных данных.
    *
    * @param m Проверяемый инстанс [[MImgEdgeWithOps]].
    * @param edges Карта допустимых эджей.
    * @param imgContSzOpt Размер контейнера изображения (для проверки кропа).
    * @return Результат валидации.
    */
  def validate(
                m          : MImgEdgeWithOps,
                edges      : Map[EdgeUid_t, MJdEdgeVldInfo],
                imgContSzOpt  : Option[ISize2di]
              ): ValidationNel[String, MImgEdgeWithOps] = {
    val edgeInfoOpt = edges.get( m.imgEdge.edgeUid )
    (
      Validation.liftNel(m.imgEdge)( { _ => !edgeInfoOpt.exists(_.img.exists(_.isImg)) }, ErrorConstants.emsgF("img")("e") ) |@|
      ScalazUtil.liftNelOpt(m.crop) { mcrop =>
        ScalazUtil.someValidationOrFail("crop.args") {
          for {
            contSz   <- imgContSzOpt
            edgeInfo <- edgeInfoOpt
            img      <- edgeInfo.img
          } yield {
            MCrop.validate(mcrop, tgContSz = contSz, imgWh = img.imgWh)
          }
        }
      }
    ){ MImgEdgeWithOps(_, _) }
  }

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
  def withCrop(crop: Option[MCrop])   = copy(crop = crop)

}
