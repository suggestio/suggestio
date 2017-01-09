package models.mtag

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.common.tags.edit.TagsEditConstants.ReplyOk._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.09.15 16:33
 * Description: Модель для сборки и сериализации положительного JSON ответа по добавлению тега.
 */
object MAddTagReplyOk {

  /** Сериализатор в JSON. */
  implicit def writes: Writes[MAddTagReplyOk] = (
    (__ \ EXIST_TAGS_FN).write[String] and
    (__ \ ADD_FORM_FN).write[String]
  )(unlift(unapply))

}

case class MAddTagReplyOk(
  existHtml: String,
  addFormHtml: String
)
