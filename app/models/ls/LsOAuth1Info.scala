package models.ls

import models.usr.OAuthReqTokUtil
import play.api.libs.oauth.RequestToken
import play.api.libs.json._
import play.api.libs.functional.syntax._
import LsDataTypes.LS_DATA_TYPE_FN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 19:08
 * Description: Модель для хранения данных access_token'а в localStorage.
 */

object LsOAuth1Info {

  def ACCESS_TOKEN_FN = "at"
  def PERSON_ID_FN    = "p"
  def TIMESTAMP_FN    = "t"

  implicit def reads: Reads[LsOAuth1Info] = (
    //LsDataTypes.readsKv and   // TODO Делать проверку id модели от считываемых данных.
    (__ \ ACCESS_TOKEN_FN).read(OAuthReqTokUtil.reads) and
    (__ \ PERSON_ID_FN).read[String] and
    (__ \ TIMESTAMP_FN).read[Long]
  )(apply _)

  implicit def writes: Writes[LsOAuth1Info] = (
    //LsDataTypes.writesKv and   // TODO Записывать id модели от считываемых данных.
    (__ \ ACCESS_TOKEN_FN).write(OAuthReqTokUtil.writes) and
    (__ \ PERSON_ID_FN).write[String] and
    (__ \ TIMESTAMP_FN).write[Long]
  )(unlift(unapply))


}

/**
 * Экземпляр модели.
 * @param acTok хранимый токен oauth1.
 * @param personId нужен для привязки к юзеру, чтобы не было соблазна подсовывать чужой ворованный ключ от своего имени.
 * @param timestamp нужен для TTL.
 */
case class LsOAuth1Info(
  acTok     : RequestToken,
  personId  : String,
  timestamp : Long
) extends ILsModel {

  override def lsDataType = LsDataTypes.OAuth1AccessToken

}
