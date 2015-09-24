package models.usr

import io.suggest.model.es.BulkRespCounted
import io.suggest.model.n2.node.meta.MNodeMeta
import io.suggest.model.n2.node.{MNodeTypes, MNode}
import io.suggest.model.n2.node.common.MNodeCommon
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
 * Description: Юзер, зареганный на сайте.
 *
 * 2015.sep.24: В связи с переездом на MNode эта модель осталась для совместимости.
 * Она пока останется, но будет отрезана от ES, и будет содержать только кое-какие вещи, упрощающие жизнь.
 */

// Статическая часть модели.
object MPerson extends EsModelStaticT with PlayMacroLogsImpl with EsmV2Deserializer {

  import LOGGER._

  override type T = MPerson

  override val ES_TYPE_NAME = "person"

  def LANG_ESFN   = "lang"


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

  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    (__ \ LANG_ESFN).read[String]
      .map { lang =>
        MPerson(lang = lang, id = meta.id)
      }
  }

  /**
   * Экспорт содержимого этой модели в MNode.
   * @return (счетчик результатов, BulkResponse).
   */
  def exportAllIntoToMNode()(implicit client: Client, ec: ExecutionContext): Future[BulkRespCounted] = {
    val bulk = client.prepareBulk()
    val countSuccessFut = foreach() { mperson =>
      val mnode = mperson.toNode
      bulk.add( mnode.indexRequestBuilder )
    }
    for {
      total <- countSuccessFut
      bresp <- bulk.execute()
    } yield {
      BulkRespCounted(total, bresp)
    }
  }

}


import models.usr.MPerson._


/**
 * Экземпляр модели MPerson.
 * @param lang Язык интерфейса для указанного пользователя.
 *             Формат четко неопределён, и соответствует коду выхлопа Controller.lang().
 */
@deprecated("Use MNode instead", "2015.sep.24")
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

  /** Сконвертить в инстанс узла N2. */
  def toNode = MNode.applyPerson(lang, id)

}


/** Трайт ссылок с юзера для других моделей. */
trait MPersonLinks {

  def personId: String

  def isSuperuser: Boolean = {
    SuperUsers.isSuperuserId(personId)
  }

}


trait MPersonJmxMBean extends EsModelJMXMBeanI {
  def exportAllIntoToMNode(): String
}
final class MPersonJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MPersonJmxMBean
{
  override def companion = MPerson

  override def exportAllIntoToMNode(): String = {
    val fut = MPerson.exportAllIntoToMNode()
      .map { res =>
        s"Done.\nTotal counted=${res.total}\nSave took=${res.bresp.getTookInMillis}ms\n---------\nFailures: \n${res.bresp.buildFailureMessage()}"
      }
    awaitString(fut)
  }

}

