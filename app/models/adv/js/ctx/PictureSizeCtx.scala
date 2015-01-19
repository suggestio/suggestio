package models.adv.js.ctx

import models.MImgSizeT
import play.api.libs.json.{JsNumber, JsObject, Reads, __}
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

  /** Парсер из json. */
  implicit def pscReads: Reads[PictureSizeCtx] = {
    val s =
      (__ \ WIDTH_FN).read[Int] and
      (__ \ HEIGHT_FN).read[Int]
    s.apply(PictureSizeCtx.apply _)
  }

}


import PictureSizeCtx._


case class PictureSizeCtx(width: Int, height: Int) extends MImgSizeT {

  def toPlayJson = {
    JsObject(Seq(
      HEIGHT_FN -> JsNumber(height),
      WIDTH_FN  -> JsNumber(width)
    ))
  }

}
