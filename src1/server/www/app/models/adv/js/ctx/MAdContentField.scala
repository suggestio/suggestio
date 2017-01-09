package models.adv.js.ctx

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.adv.ext.model.ctx.MAdContentField._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.11.15 16:24
 * Description: Модель поля с контентом карточки.
 */

object MAdContentField {

  implicit def reads: Reads[MAdContentField] = {
    (__ \ TEXT_FN)
      .read[String]
      .map(apply)
  }

  implicit def writes: Writes[MAdContentField] = {
    (__ \ TEXT_FN)
      .write[String]
      .contramap(_.text)
  }

}


case class MAdContentField(
  text: String
)
