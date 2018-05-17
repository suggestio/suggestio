package models.usr

import io.suggest.es.model.EsModelUtil._
import io.suggest.es.util.SioEsUtil._
import io.suggest.util.JacksonParsing
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable
import javax.inject.{Inject, Singleton}
import io.suggest.es.model.{EsModelJMXBaseImpl, EsModelJMXMBeanI, EsmV2Deserializer, IEsDocMeta}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.primo.id.OptStrId
import io.suggest.text.StringUtil
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.08.14 15:08
 * Description: Модель для хранения email-активаций.
 */

// TODO Потеряли поле TTL, оно тут было до наступления es-5.x.
// Не ясно, надо ли оно: email-регистрацию собирались дропать ведь, заменить всё на OAuth,
// как требуют новые законы РФ.

/** Статическая часть модели [[EmailActivation]].
  * Модель нужна для хранения ключей для проверки/активации почтовых ящиков. */
@Singleton
class EmailActivations @Inject() (
  override val mCommonDi: ICommonDi
)
  extends EsModelStaticIdentT
    with MacroLogsImpl
    with EsmV2Deserializer
{
  import mCommonDi._

  override type T = EmailActivation

  override def ES_TYPE_NAME: String = "mpiEmailAct"


  @deprecated("Delete id, replaced by deserializeOne2()", "2015.sep.07")
  override def deserializeOne(id: Option[String], m: scala.collection.Map[String, AnyRef], version: Option[Long]): T = {
    EmailActivation(
      id    = id,
      key   = JacksonParsing.stringParser(m(KEY_ESFN)),
      email = JacksonParsing.stringParser(m(PERSON_ID_ESFN))
    )
  }


  // Кешируем промежуточный недособранный неизменяемый Reads-десериализатор.
  private val _reads0 = {
    (__ \ KEY_ESFN).read[String] and
    (__ \ PERSON_ID_ESFN).read[String]
  }
  override protected def esDocReads(meta: IEsDocMeta): Reads[EmailActivation] = {
    _reads0 {
      (key, personId) =>
        EmailActivation.apply(email = personId, key = key, id = meta.id)
    }
  }


  /** Найти элементы по ключу. */
  def findByKey(key: String): Future[Seq[EmailActivation]] = {
    val keyQuery = QueryBuilders.termQuery(KEY_ESFN, key)
    esClient.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(keyQuery)
      .execute()
      .map { searchResp2list }
  }

}

/** Интерфейс для поля с DI-инстансом [[EmailActivations]]. */
trait IEmailActivationsDi {
  def emailActivations: EmailActivations
}


object EmailActivation {

  /** Длина генерируемых ключей для активации. */
  def KEY_LEN = 16  // current.configuration.getInt("ident.email.act.key.len") getOrElse 16

  /** Сгенерить новый рандомный ключ активации.
    *
    * @return Строка из символов [a-zA-Z0-9].
    */
  def randomActivationKey = StringUtil.randomId(len = KEY_LEN)

}

/**
 * Запись об активации почты.
  *
  * @param email Почта.
 * @param key Ключ содержит какие-то данные, необходимые для активации. Например, id магазина.
 */
final case class EmailActivation(
  email     : String,
  key       : String = EmailActivation.randomActivationKey,
  id        : Option[String] = None
) extends MPersonIdent with IEaEmailId {

  override def personId = email
  override def writeVerifyInfo = false
  override def idType = IdTypes.EMAIL_ACT
  override def isVerified = true
  override def value: Option[String] = None
  override def versionOpt = None
}


// JMX
trait EmailActivationsJmxMBean extends EsModelJMXMBeanI
final class EmailActivationsJmx @Inject() (
  override val companion  : EmailActivations,
  override val ec         : ExecutionContext
)
  extends EsModelJMXBaseImpl
    with EmailActivationsJmxMBean
{
  override type X = EmailActivation
}


// 2015.jan.28: Добавлена поддержка qsb для email+id qs.
/** Интерфейс для QSB. Он совместим с [[EmailActivation]], но id всё-таки обязательный. */
trait IEaEmailId extends OptStrId {
  def email: String
}


/** Статическая поддержка модели [[IEaEmailId]]. */
object IEaEmailId {

  def ID_FN    = "k"
  def EMAIL_FN = "e"

  implicit def eaEmailIdQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[IEaEmailId] = {
    new QueryStringBindableImpl[IEaEmailId] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, IEaEmailId]] = {
        val k = key1F(key)
        for {
          maybeEmail  <- strB.bind(k(EMAIL_FN), params)
          maybeId     <- strB.bind(k(ID_FN), params)
        } yield {
          for {
            email <- maybeEmail.right
            id    <- maybeId.right
          } yield {
            EaEmailId(
              email = email,
              id    = Some(id)
            )
          }
        }
      }

      override def unbind(key: String, value: IEaEmailId): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          strB.unbind(k(EMAIL_FN),  value.email),
          strB.unbind(k(ID_FN),     value.id.get)
        )
      }
    }
  }

}

/** Дефолтовая реализация [[IEaEmailId]] */
case class EaEmailId(
  email : String,
  id    : Option[String]
)
  extends IEaEmailId
