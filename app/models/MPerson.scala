package models

import io.suggest.model.{EsModelStaticT, EsModelT}
import com.fasterxml.jackson.annotation.JsonIgnore
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel._
import scala.concurrent.{Future, ExecutionContext, future}
import org.elasticsearch.client.Client
import play.api.Play.current
import play.api.cache.Cache
import util.PlayMacroLogsImpl
import play.api.libs.json.JsString

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
object MPerson extends EsModelStaticT[MPerson] with PlayMacroLogsImpl {

  import LOGGER._

  val ES_TYPE_NAME = "person"

  val LANG_ESFN   = "lang"
  val IDENTS_ESFN = "idents"

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
  def isSuperuserId(personId: String) = SU_IDS contains personId


  def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )

  def generateMappingProps: List[DocField] = List(
    FieldString(LANG_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false)
  )


  def applyKeyValue(acc: MPerson): PartialFunction[(String, AnyRef), Unit] = {
    case (LANG_ESFN, value)     => acc.lang = stringParser(value)
  }

  protected def dummy(id: String) = MPerson(id = Some(id), lang = null)


  /** Асинхронно найти подходящее имя юзера в хранилищах и подмоделях. */
  def findUsername(personId: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    MPersonIdent.findAllEmails(personId).map(_.headOption)
  }

  /** Ключ в кеше для юзернейма. */
  private def personCacheKey(personId: String) = personId + ".pu"

  val USERNAME_CACHE_EXPIRATION_SEC: Int = current.configuration.getInt("mperson.username.cache.seconds") getOrElse 600

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

}

import MPerson._

/**
 * Экземпляр модели MPerson.
 * @param lang Язык интерфейса для указанного пользователя.
 *             Формат четко неопределён, и соответствует коду выхлопа Controller.lang().
 */
case class MPerson(
  var lang  : String,
  var id    : Option[String] = None
) extends EsModelT[MPerson] with MPersonLinks {

  def personId = id.get

  override def companion = MPerson

  def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    LANG_ESFN -> JsString(lang) :: acc
  }

}


/** Трайт ссылок с юзера для других моделей. */
trait MPersonLinks {
  def personId: String

  @JsonIgnore def person(implicit ec:ExecutionContext, client: Client) = {
    MPerson getById personId
  }
  @JsonIgnore def isSuperuser = MPerson isSuperuserId personId
  def authzForDomain(dkey: String) = MPersonDomainAuthz.getForPersonDkey(dkey, personId)
  @JsonIgnore def allDomainsAuthz = MPersonDomainAuthz.getForPerson(personId)
}


