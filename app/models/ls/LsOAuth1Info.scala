package models.ls

// ЭТОТ import НУЖЕН!
import models.usr.OAuthReqTokUtil.{reads => oartReads, writes => oartWrites}
import org.joda.time.DateTime
import play.api.libs.oauth.RequestToken
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.04.15 19:08
 * Description: Модель для хранения данных access_token'а в localStorage.
 */

object LsOAuth1Info {

  def ACCESS_TOKEN_FN   = "a"
  def PERSON_ID_FN      = "p"
  def TIMESTAMP_FN      = "t"
  def LAST_VERIFIED_FN  = "l"

  /** Десериализация из JSON с валидацией типа. */
  implicit def reads: Reads[LsOAuth1Info] = (
    LsDataTypes.readsKv and
    (__ \ ACCESS_TOKEN_FN).read[RequestToken] and
    (__ \ PERSON_ID_FN).read[String] and
    (__ \ TIMESTAMP_FN).read[DateTime] and
    (__ \ LAST_VERIFIED_FN).readNullable[DateTime]
  ) {
    (lsdt, acTok, personId, timestamp, lastVerified) =>
      // Обязательно делать проверку id модели от считываемых данных. Дабы юзеры не подсовывали сюда данные из других моделей.
      if (lsdt != LsDataTypes.OAuth1AccessToken)
        throw new IllegalArgumentException("Unexpected ls data type: " + lsdt)
      else
        apply(acTok, personId, timestamp, lastVerified)
    }

  /** Сериализация в JSON с записью типа. */
  implicit def writes: Writes[LsOAuth1Info] = (
    LsDataTypes.writesKv and
    (__ \ ACCESS_TOKEN_FN).write[RequestToken] and
    (__ \ PERSON_ID_FN).write[String] and
    (__ \ TIMESTAMP_FN).write[DateTime] and
    (__ \ LAST_VERIFIED_FN).writeNullable[DateTime]
  ) { info: LsOAuth1Info =>
    // Записывать идентификатор модели в сериализуемые данные.
    (info.lsDataType, info.acTok, info.personId, info.created, info.verified)
  }

}


/**
 * Экземпляр модели.
 * @param acTok хранимый токен oauth1.
 * @param personId нужен для привязки к юзеру, чтобы не было соблазна подсовывать чужой ворованный ключ от своего имени.
 * @param created нужен для TTL.
 */
case class LsOAuth1Info(
  acTok     : RequestToken,
  personId  : String,
  created   : DateTime          = DateTime.now(),
  verified  : Option[DateTime]  = None
) extends ILsModel {

  override def lsDataType = LsDataTypes.OAuth1AccessToken

}
