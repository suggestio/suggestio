package models.mext.tw

import models.mext.IPostAttachmentId
import play.api.libs.json._
//import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.04.15 18:45
 * Description: Модель данных по media-аттачменту, загруженному ранее.
 */
object TwMediaAtt {

  /**
   * Парсер модели из JSON.
   * Твиттер после аплоада возвращает что-то типа этого:
   * {
   *   "media_id": 553639437322563584,
   *   "media_id_string": "553639437322563584",
   *   "size": 998865,
   *   "image": {
   *     "w": 2234,
   *     "h": 1873,
   *     "image_type": "image/jpeg"
   *   }
   * }
   * @see [[https://dev.twitter.com/rest/reference/post/media/upload]]
   * @return Экземпляр json-маппера.
   */
  implicit def reads: Reads[TwMediaAtt] = {
    (__ \ "media_id_string")
      .read[String]
      .map(apply)
  }

}


/**
 * Экземпляр модели.
 * @param strId Строковой id аттачмента.
 */
case class TwMediaAtt(strId: String)
  extends IPostAttachmentId
