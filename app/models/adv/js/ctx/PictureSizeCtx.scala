package models.adv.js.ctx

import models.MImgSizeT
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.01.15 16:09
 * Description: Объект для представления размеров картинки, которые необходимо рендерить.
 * Он закидывается в context силами js подсистемы ext adv.
 */
object PictureSizeCtx {

  val WIDTH_FN = "width"
  val HEIGHT_FN = "height"

  /** mapper из json. */
  implicit def reads: Reads[PictureSizeCtx] = (
    (__ \ WIDTH_FN).read[Int] and
    (__ \ HEIGHT_FN).read[Int]
  )(PictureSizeCtx.apply _)

  /** unmapper в json. */
  implicit def writes: Writes[PictureSizeCtx] = (
    (__ \ WIDTH_FN).write[Int] and
    (__ \ HEIGHT_FN).write[Int]
  )(unlift(PictureSizeCtx.unapply))

}

case class PictureSizeCtx(width: Int, height: Int) extends MImgSizeT
