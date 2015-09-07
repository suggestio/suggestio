package models.usr

import util.PlayMacroLogsImpl
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.EsModel._
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import org.elasticsearch.client.Client
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json._

import scala.collection.Map
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.04.13 18:19
 * Description: Юзер, зареганный на сайте. Как правило, админ одного или нескольких сайтов.
 * Люди различаются по email'ам и/или по номерам телефона.
 * 01.03.2014 Вместо hbase использовать EsModel. Добавлен phone. id вместо email теперь
 * генерится силами ES.
 */

// Статическая часть модели.
object MPerson extends EsModelStaticT with PlayMacroLogsImpl with CurriedPlayJsonEsDocDeserializer {

  import LOGGER._

  override type T = MPerson

  override val ES_TYPE_NAME = "person"

  def LANG_ESFN   = "lang"

  /** PersonId суперпользователей sio. */
  private var SU_IDS: Set[String] = Set.empty

  /** Выставление personId для суперпользователей. Вызывается из Global при старте. */
  def setSuIds(suIds: Set[String]) {
    SU_IDS = suIds
  }

  /**
   * Принадлежит ли указанный id суперюзеру suggest.io?
   * @param personId Реальный id юзера.
   * @return true, если это админ. Иначе false.
   */
  def isSuperuserId(personId: String): Boolean = {
    SU_IDS.contains(personId)
  }


  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )

  override def generateMappingProps: List[DocField] = List(
    FieldString(LANG_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false)
  )

  @deprecated("Use deserializeOne2() instead", "2015.sep.05")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    MPerson(
      id = id,
      lang = m.get(LANG_ESFN).fold("ru")(stringParser)
    )
  }

  /** Асинхронно найти подходящее имя юзера в хранилищах и подмоделях. */
  def findUsername(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    MPersonIdent.findAllEmails(personId)
      .map(_.headOption)
  }

  /** Ключ в кеше для юзернейма. */
  private def personCacheKey(personId: String) = personId + ".pu"

  /** Сколько секунд кешировать найденный юзернейм в кеше play? */
  val USERNAME_CACHE_EXPIRATION_SEC: Int = current.configuration.getInt("mperson.username.cache.seconds") getOrElse 100

  /** Асинхронно найти подходящее имя для юзера используя кеш. */
  def findUsernameCached(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    val cacheKey = personCacheKey(personId)
    Cache.getAs[String](cacheKey) match {
      case Some(result) =>
        Future.successful(Some(result))
      case None =>
        val resultFut = findUsername(personId)
        resultFut onSuccess {
          case Some(result) =>
            Cache.set(cacheKey, result, USERNAME_CACHE_EXPIRATION_SEC)
          case None =>
            warn(s"findUsernameCached($personId): Username not found for user. Invalid session?")
        }
        resultFut
    }
  }

  override def esDocReads: Reads[Reads_t] = {
    (__ \ LANG_ESFN).read[String]
      .map { lang =>
        {(idOpt, vsnOpt) =>
          MPerson(lang = lang, id = idOpt)
        }
      }
  }

}

import models.usr.MPerson._

/**
 * Экземпляр модели MPerson.
 * @param lang Язык интерфейса для указанного пользователя.
 *             Формат четко неопределён, и соответствует коду выхлопа Controller.lang().
 */
final case class MPerson(
  lang  : String,
  id    : Option[String] = None
) extends EsModelPlayJsonT with EsModelT with MPersonLinks {

  override type T = MPerson
  override def versionOpt = None
  override def companion = MPerson
  override def personId = id.get

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    LANG_ESFN -> JsString(lang) :: acc
  }

}


/** Трайт ссылок с юзера для других моделей. */
trait MPersonLinks {
  def personId: String

  def isSuperuser: Boolean = {
    MPerson.isSuperuserId(personId)
  }
}


trait MPersonJmxMBean extends EsModelJMXMBeanI
final class MPersonJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MPersonJmxMBean
{
  override def companion = MPerson
}

