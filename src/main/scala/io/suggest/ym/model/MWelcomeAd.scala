package io.suggest.ym.model

import io.suggest.model._
import org.joda.time.DateTime
import org.codehaus.jackson.annotate.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model.common._
import io.suggest.ym.model.ad.{MAdT, AdOfferT}
import io.suggest.model.common._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 16:46
 * Description: Модель для приветственной рекламы. Поиск тут не требуется, только изолированное хранение.
 */
object MWelcomeAd
  extends EsModelStaticEmpty[MWelcomeAd]
  with EMProducerIdStatic[MWelcomeAd]
  with EMImgStatic[MWelcomeAd]
  with EMDateCreatedStatic[MWelcomeAd]
{

  val ES_TYPE_NAME = "wcAd"

  protected def dummy(id: String) = MWelcomeAd(
    producerId = null,
    img = null,
    id = Some(id)
  )

  def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )

}


case class MWelcomeAd(
  var producerId  : String,
  var img         : MImgInfo,
  var dateCreated : DateTime = null,
  var id          : Option[String] = None
)
  extends EsModelEmpty[MWelcomeAd]
  with MAdT[MWelcomeAd]
  with EMProducerIdMut[MWelcomeAd]
  with EMImgMut[MWelcomeAd]
  with EMDateCreatedMut[MWelcomeAd]
{

  @JsonIgnore def companion = MWelcomeAd

  @JsonIgnore override def offers: List[AdOfferT] = Nil
  @JsonIgnore override def textAlign: Option[TextAlign] = None
  @JsonIgnore override def panel: Option[AdPanelSettings] = None
  @JsonIgnore override def prio: Option[Int] = None
  @JsonIgnore override def showLevels: Set[AdShowLevel] = Set.empty
  @JsonIgnore override def userCatId: Option[String] = None
  @JsonIgnore override def logoImgOpt: Option[MImgInfo] = None
  @JsonIgnore override def receivers: Set[AdReceiverInfo] = Set.empty
}


/** JMX MBean интерфейс */
trait MWelcomeAdJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MWelcomeAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MWelcomeAdJmxMBean {
  def companion = MWelcomeAd
}
