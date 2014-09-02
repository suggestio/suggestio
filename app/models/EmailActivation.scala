package models

import io.suggest.model._
import models.MPersonIdent.IdTypes
import EsModel._
import io.suggest.util.SioEsUtil._
import play.api.Play.current
import io.suggest.util.StringUtil
import scala.collection.Map
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import util.PlayMacroLogsImpl
import io.suggest.event.SioNotifierStaticClientI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 15:08
 * Description:
 */


/** Статическая часть модели [[EmailActivation]].
  * Модель нужна для хранения ключей для проверки/активации почтовых ящиков. */
object EmailActivation extends EsModelStaticIdentT with PlayMacroLogsImpl {

  override type T = EmailActivation

  val ES_TYPE_NAME: String = "mpiEmailAct"

  /** Длина генерируемых ключей для активации. */
  val KEY_LEN = current.configuration.getInt("ident.email.act.key.len") getOrElse 16

  /** Период ttl, если иное не указано в документе. Записывается в маппинг. */
  val TTL_DFLT = current.configuration.getString("ident.email.act.ttl.period") getOrElse "2d"


  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    EmailActivation(
      key = stringParser(m(KEY_ESFN)),
      email = stringParser(m(PERSON_ID_ESFN))
    )
  }

  /** Сгенерить новый рандомный ключ активации.
    * @return Строка из символов [a-zA-Z0-9].
    */
  def randomActivationKey = StringUtil.randomId(len = KEY_LEN)

  override def generateMappingProps: List[DocField] = MPersonIdent.generateMappingProps

  /** Сборка static-полей маппинга. В этом маппинге должен быть ttl, чтобы старые записи автоматически выпиливались. */
  override def generateMappingStaticFields: List[Field] = {
    FieldTtl(enabled = true, default = TTL_DFLT) :: MPersonIdent.generateMappingStaticFieldsMin
  }

  /** Найти элементы по ключу. */
  def findByKey(key: String)(implicit ec: ExecutionContext, client: Client): Future[Seq[EmailActivation]] = {
    val keyQuery = QueryBuilders.termQuery(KEY_ESFN, key)
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(keyQuery)
      .execute()
      .map { searchResp2list }
  }

}


/**
 * Запись об активации почты.
 * @param email Почта.
 * @param key Ключ содержит какие-то данные, необходимые для активации. Например, id магазина.
 */
final case class EmailActivation(
  email     : String,
  key       : String = EmailActivation.randomActivationKey,
  id        : Option[String] = None
) extends MPersonIdent with MPersonLinks {
  override type T = EmailActivation

  override def personId = email
  override def companion = EmailActivation
  override def writeVerifyInfo = false
  override def idType = IdTypes.EMAIL_ACT
  override def isVerified = true
  override def value: Option[String] = None
  override def versionOpt = None
}


// JMX
trait EmailActivationJmxMBean extends EsModelJMXMBeanCommon
final class EmailActivationJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with EmailActivationJmxMBean
{
  override def companion = EmailActivation
}
