package models.adv.js.ctx

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.01.15 16:09
 * Description: Объект для представления размеров картинки, которые необходимо рендерить.
 * Он закидывается в context силами js подсистемы ext adv.
 */
object PictureSizeCtx {

  /** Парсер из json. */
  implicit def pscReads: Reads[PictureSizeCtx] = {
    val s =
      (JsPath \ "width").read[Int] and
      (JsPath \ "height").read[Int]
    s.apply(PictureSizeCtx.apply _)
  }

}


case class PictureSizeCtx(width: Int, height: Int)
