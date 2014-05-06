package io.suggest.ym.model

import io.suggest.model._
import org.joda.time.DateTime
import org.codehaus.jackson.annotate.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model.common._
import io.suggest.ym.model.ad.MAdT
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
  extends EsModelStaticEmpty
  with EMProducerIdStatic
  with EMImgStatic
  with EMDateCreatedStatic
  with MacroLogsImplLazy
  with EsModelStaticIgnore
{

  val ES_TYPE_NAME = "wcAd"

  override type T = MWelcomeAd

  protected def dummy(id: String) = MWelcomeAd(
    producerId = null,
    imgs = null,
    id = Some(id)
  )

  def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val adOptFut = getById(id)
    val imgDelFut = adOptFut flatMap {
      case Some(ad) => eraseImgs(ad)
      case None     => Future successful ()
    }
    imgDelFut flatMap { _ =>
      super.deleteById(id)
    }
  }
}


case class MWelcomeAd(
  var producerId  : String,
  var imgs        : Imgs_t,
  var dateCreated : DateTime = null,
  var id          : Option[String] = None
)
  extends EsModelEmpty
  with MAdT
  with EMProducerIdMut
  with EMImgMut
  with EMDateCreatedMut
{
  override type T = MWelcomeAd

  @JsonIgnore override def companion = MWelcomeAd
  @JsonIgnore override def offers = Nil
  @JsonIgnore override def prio = None
  @JsonIgnore override def userCatId = None
  @JsonIgnore override def receivers = Map.empty
  @JsonIgnore override def blockMeta = MAd.blockMetaDflt
  @JsonIgnore override def colors = Map.empty
}


/** JMX MBean интерфейс */
trait MWelcomeAdJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MWelcomeAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MWelcomeAdJmxMBean {
  def companion = MWelcomeAd
}
