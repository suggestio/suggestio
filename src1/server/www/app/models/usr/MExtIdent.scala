package models.usr

import io.suggest.util.JacksonParsing.FieldsJsonAcc
import io.suggest.es.util.SioEsUtil._
import models.mext.ILoginProvider
import io.suggest.util.JacksonParsing.stringParser
import javax.inject.{Inject, Singleton}
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.02.15 11:27
 * Description: ExternalIdent - это ident-модель для хранения данных логина из соц.сетей или от иных провайдеров
 * идентификации пользователей.
 */
@Singleton
class MExtIdents @Inject() (
                             esModel: EsModel,
                           )
  extends MPersonIdentSubmodelStatic
    with MacroLogsImpl
    with EsmV2Deserializer
{

  override val ES_TYPE_NAME = "exid"

  override type T = MExtIdent

  import MExtIdent._

  override def generateMappingProps: List[DocField] = {
    FieldKeyword(PROVIDER_ID_ESFN, index = true, include_in_all = true) ::
    super.generateMappingProps
  }

  @deprecated("Delete it, deserializeOne2() is ready here", "2015.sep.08")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MExtIdent(
      versionOpt  = version,
      personId    = stringParser(m(PERSON_ID_ESFN)),
      provider    = ILoginProvider.maybeWithName( stringParser(m(PROVIDER_ID_ESFN)) ).get,
      userId      = stringParser(m(USER_ID_ESFN)),
      email       = m.get(EMAIL_ESFN).map(stringParser)
    )
  }

  /**
   * Поиск документа по userId и провайдеру.
   * @param prov Провайдер идентификации.
   * @param userId id юзера в рамках провайдера.
   * @return Результат, если есть.
   */
  def getByUserIdProv(prov: ILoginProvider, userId: String): Future[Option[T]] = {
    import esModel.api._
    val id = genId(prov, userId)
    this.getById(id)
  }

  /** Кешируем почти готовый immutable json mapper тут. */
  private val _reads0 = {
    (__ \ PERSON_ID_ESFN).read[String] and
    (__ \ PROVIDER_ID_ESFN).read[ILoginProvider] and
    (__ \ USER_ID_ESFN).read[String] and
    (__ \ EMAIL_ESFN).readNullable[String]
  }
  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    _reads0 {
      (personId, provider, userId, email) =>
        MExtIdent(personId, provider, userId, email, meta.version)
    }
  }

  /** Сериализация json-экземпляра. */
  override def writeJsonFields(m: T, acc: FieldsJsonAcc): FieldsJsonAcc = {
    PROVIDER_ID_ESFN -> JsString(m.provider.ssProvName) ::
      super.writeJsonFields(m, acc)
  }

}


object MExtIdent {

  val PERSON_ID_ESFN    = "personId"
  val USER_ID_ESFN      = "key"
  val EMAIL_ESFN        = "value"
  val PROVIDER_ID_ESFN  = "prov"


  /** Генерация id модели. */
  def genId(prov: ILoginProvider, userId: String): String = {
    // TODO Может надо делать toLowerCase?
    s"$prov~$userId"
  }

}


case class MExtIdent(
  personId            : String,
  provider            : ILoginProvider,
  userId              : String,
  email               : Option[String] = None,
  versionOpt          : Option[Long] = None
)
  extends MPersonIdent
{

  /** Ключём модели является userId. */
  override def key = userId
  override def value = email

  /** Форсируем уникальность в рамках одного провайдера */
  override def id: Option[String] = Some(MExtIdent.genId(provider, userId))

  /** isVerified писать в хранилище не нужно, потому мы не управляем проверкой юзера. */
  override def writeVerifyInfo = false
  override def isVerified = true

}



// Поддержка JMX.
trait MExtIdentJmxMBean extends EsModelJMXMBeanI
final class MExtIdentJmx @Inject() (
                                     override val companion      : MExtIdents,
                                     override val esModelJmxDi   : EsModelJmxDi,
                                   )
  extends EsModelJMXBaseImpl
  with MExtIdentJmxMBean
{
  override type X = MExtIdent
}


