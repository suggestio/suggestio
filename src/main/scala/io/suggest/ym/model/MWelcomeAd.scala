package io.suggest.ym.model

import io.suggest.model._
import org.joda.time.DateTime
import org.codehaus.jackson.annotate.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model.common._
import io.suggest.ym.model.ad.{RichDescr, MAdT}
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
      imgs = null,
      id = id
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
  var id          : Option[String] = None
)
  extends EsModelEmpty
  with EsModelT
  with MAdT
  with EMProducerIdMut
  with EMImgMut
  with EMDateCreatedMut
{
  override type T = MWelcomeAd
  @JsonIgnore override def versionOpt = None
  @JsonIgnore override def companion = MWelcomeAd

  @JsonIgnore override def offers = Nil
  @JsonIgnore override def prio = None
  @JsonIgnore override def userCatId = None
  @JsonIgnore override def receivers = Map.empty
  @JsonIgnore override def blockMeta = MAd.blockMetaDflt
  @JsonIgnore override def colors = Map.empty
  @JsonIgnore override def disableReason = Nil
  @JsonIgnore override def richDescrOpt = None
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
