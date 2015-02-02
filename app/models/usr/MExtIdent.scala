package models.usr

import io.suggest.model.EnumMaybeWithName
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import models.MPersonIdent.IdTypes
import models.{MPersonIdent, MPersonIdentSubmodelStatic}
import play.api.libs.json.JsString
import securesocial.core.providers.{TwitterProvider, FacebookProvider, VkProvider}
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.stringParser

import scala.collection.Map

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 11:27
 * Description: ExternalIdent - это ident-модель для хранения данных логина из соц.сетей или от иных провайдеров
 * идентификации пользователей.
 */
object MExtIdent extends MPersonIdentSubmodelStatic with PlayMacroLogsImpl {

  val PERSON_ID_ESFN    = "personId"
  val USER_ID_ESFN      = "key"
  val EMAIL_ESFN        = "value"
  val PROVIDER_ID_ESFN  = "prov"

  override val ES_TYPE_NAME = "exid"

  override type T = MExtIdent

  override def generateMappingProps: List[DocField] = {
    FieldString(PROVIDER_ID_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true) ::
    super.generateMappingProps
  }

  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MExtIdent(
      id          = id,
      versionOpt  = version,
      personId    = stringParser(m(PERSON_ID_ESFN)),
      provider    = IdProviders.withName( stringParser(m(PROVIDER_ID_ESFN)) ),
      userId      = stringParser(m(USER_ID_ESFN)),
      email       = m.get(EMAIL_ESFN).map(stringParser)
    )
  }

}


import MExtIdent._


case class MExtIdent(
  personId      : String,
  provider      : IdProvider,
  userId        : String,
  email         : Option[String] = None,
  id            : Option[String] = None,
  versionOpt    : Option[Long] = None
) extends MPersonIdent {

  override type T         = this.type
  override def companion  = MExtIdent
  override def idType     = IdTypes.EXT_ID

  /** Ключём модели является userId. */
  override def key = userId
  override def value = email

  /** isVerified писать в хранилище не нужно, потому мы не управляем проверкой юзера. */
  override def writeVerifyInfo = false
  override def isVerified = true

  /** Сериализация json-экземпляра. */
  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    PROVIDER_ID_ESFN -> JsString(provider.toString) ::
    super.writeJsonFields(acc)
  }
}


/** Поддерживаемые провайдеры идентификации. */
object IdProviders extends Enumeration with EnumMaybeWithName {
  override type T = Value

  val Facebook: T   = Value(FacebookProvider.Facebook)
  val Vkontakte: T  = Value(VkProvider.Vk)
  val Twitter: T    = Value(TwitterProvider.Twitter)
}

