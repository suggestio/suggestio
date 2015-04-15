package models.usr

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.{EsModelJMXBase, EsModelJMXMBeanI}
import io.suggest.model.EsModel.FieldsJsonAcc
import io.suggest.util.SioEsUtil._
import models.mext.ILoginProvider
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders}
import play.api.libs.json.JsString
import securesocial.core.IProfileDflt
import util.PlayMacroLogsImpl
import io.suggest.model.EsModel.stringParser

import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}

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
      versionOpt  = version,
      personId    = stringParser(m(PERSON_ID_ESFN)),
      provider    = ILoginProvider.maybeWithName( stringParser(m(PROVIDER_ID_ESFN)) ).get,
      userId      = stringParser(m(USER_ID_ESFN)),
      email       = m.get(EMAIL_ESFN).map(stringParser)
    )
  }

  def userIdQuery(userId: String) = QueryBuilders.termQuery(USER_ID_ESFN, userId)
  def providerIdFilter(prov: ILoginProvider) = FilterBuilders.termFilter(PROVIDER_ID_ESFN, prov.ssProvName)

  /** Генерация id модели. */
  def genId(prov: ILoginProvider, userId: String): String = {
    // TODO Может надо делать toLowerCase?
    s"$prov~$userId"
  }

  /**
   * Поиск документа по userId и провайдеру.
   * @param prov Провайдер идентификации.
   * @param userId id юзера в рамках провайдера.
   * @return Результат, если есть.
   */
  def getByUserIdProv(prov: ILoginProvider, userId: String)(implicit client: Client, ec: ExecutionContext): Future[Option[T]] = {
    val id = genId(prov, userId)
    getById(id)
  }

}


import MExtIdent._


case class MExtIdent(
  personId            : String,
  provider            : ILoginProvider,
  userId              : String,
  override val email  : Option[String] = None,
  versionOpt          : Option[Long] = None
) extends MPersonIdent with IProfileDflt {

  override type T         = this.type
  override def companion  = MExtIdent
  override def idType     = IdTypes.EXT_ID

  /** Ключём модели является userId. */
  override def key = userId
  override def value = email

  /** Форсируем уникальность в рамках одного провайдера */
  override def id: Option[String] = Some(genId(provider, userId))

  /** isVerified писать в хранилище не нужно, потому мы не управляем проверкой юзера. */
  override def writeVerifyInfo = false
  override def isVerified = true

  /** Сериализация json-экземпляра. */
  override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    PROVIDER_ID_ESFN -> JsString(provider.ssProvName) ::
    super.writeJsonFields(acc)
  }

  /** Реализация IProfile. */
  override def providerId = provider.ssProvName
  override def authMethod = provider.ssAuthMethod
}



// Поддержка JMX.
trait MExtIdentJmxMBean extends EsModelJMXMBeanI
final class MExtIdentJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MExtIdentJmxMBean
{
  override def companion = MExtIdent
}


