package io.suggest.ym.model

import io.suggest.model._
import org.joda.time.DateTime
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model.common._
import io.suggest.ym.model.ad.{ModerationInfo, MAdT}
import io.suggest.model.common._
import io.suggest.util.MacroLogsImplLazy
import io.suggest.ym.model.common.EMImg.Imgs_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 16:46
 * Description: Модель для приветственной рекламы. Поиск тут не требуется, только изолированное хранение.
 */
object MWelcomeAd
  extends EsModelStaticMutAkvEmptyT
  with EsModelStaticT
  with EMProducerIdStatic
  with EMImgStatic
  with EMDateCreatedStatic
  with MacroLogsImplLazy
  with EsModelStaticMutAkvIgnoreT
{

  override val ES_TYPE_NAME = "wcAd"

  override type T = MWelcomeAd

  override protected def dummy(id: Option[String], version: Option[Long]) = {
    MWelcomeAd(
      producerId = null,
      imgs = Map.empty,
      id = id,
      versionOpt = version
    )
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )

}


final case class MWelcomeAd(
  var producerId  : String,
  var imgs        : Imgs_t,
  var dateCreated : DateTime = null,
  var id          : Option[String] = None,
  versionOpt      : Option[Long] = None
)
  extends EsModelEmpty
  with EsModelT
  with MAdT
  with EMProducerIdMut
  with EMImgMut
  with EMDateCreatedMut
{
  override type T = MWelcomeAd
  override def companion = MWelcomeAd

  override def offers = Nil
  override def prio = None
  override def userCatId = Set.empty
  override def receivers = Map.empty
  override def blockMeta = BlockMeta.DEFAULT    // TODO Блоки вообще не относятся к карточкам приветствия. Нужно это спилить.
  override def colors = Map.empty
  override def disableReason = Nil
  override def richDescrOpt = None
  override def moderation = ModerationInfo.EMPTY
}


/** JMX MBean интерфейс */
trait MWelcomeAdJmxMBean extends EsModelJMXMBeanI {
  /** Найти все id'шники, которые НЕ используются ни одним рекламным узлом. Некая read-only сборка мусора. */
  def printAllUnusedByAdnNodes(): String

  /** Найти и удалить все id'шники. */
  def deleteAllUnusedByAdnNodes(): String
}

/** JMX MBean реализация. */
final class MWelcomeAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MWelcomeAdJmxMBean {
  def companion = MWelcomeAd

  def findUnusedByAdnNodes(): Future[Set[String]] = {
    val usedIdsFut = MAdnNode.findAllWelcomeAdIds()
    val allIdsFut  = MWelcomeAd.getAllIds(maxResults = -1)
      .map { _.toSet }
    for {
      usedIds <- usedIdsFut
      allIds  <- allIdsFut
    } yield {
      allIds -- usedIds
    }
  }

  override def printAllUnusedByAdnNodes(): String = {
    findUnusedByAdnNodes()
      .mkString("\n")
  }

  override def deleteAllUnusedByAdnNodes(): String = {
    findUnusedByAdnNodes().flatMap { ids =>
      Future.traverse(ids) { wadId =>
        MWelcomeAd.deleteById(wadId)
      } map { _ =>
        ids.mkString("\n")
      }
    }
  }

}
