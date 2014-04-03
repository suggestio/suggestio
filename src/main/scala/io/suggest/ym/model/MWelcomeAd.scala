package io.suggest.ym.model

import io.suggest.model._
import io.suggest.ym.model.MMart.MartId_t
import org.joda.time.DateTime
import org.codehaus.jackson.annotate.JsonIgnore
import io.suggest.util.SioEsUtil._
import EsModel._
import io.suggest.util.JacksonWrapper
import MMartAd.IMG_ESFN
import scala.concurrent.ExecutionContext
import org.elasticsearch.client.Client
import io.suggest.event.SioNotifierStaticClientI

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 16:46
 * Description: Модель для приветственной рекламы. Поиск тут не требуется, только изолированное хранение.
 */
object MWelcomeAd extends EsModelStaticT[MWelcomeAd] {

  val ES_TYPE_NAME = "welcomeAd"

  protected def dummy(id: String) = MWelcomeAd(martId = null, img = null, companyId = null)

  def generateMappingProps: List[DocField] = {
    List(
      FieldString(COMPANY_ID_ESFN,  index = FieldIndexingVariants.no,  include_in_all = false),
      FieldString(MART_ID_ESFN, index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      FieldObject(IMG_ESFN, enabled = false, properties = Nil),
      FieldDate(DATE_CREATED_ESFN, include_in_all = false, index = FieldIndexingVariants.no)
    )
  }

  def generateMappingStaticFields: List[Field] = List(
    FieldAll(enabled = false),
    FieldSource(enabled = true)
  )

  def applyKeyValue(acc: MWelcomeAd): PartialFunction[(String, AnyRef), Unit] = {
    case (MART_ID_ESFN, value)      => acc.martId = martIdParser(value)
    case (COMPANY_ID_ESFN, value)   => acc.companyId = companyIdParser(value)
    case (DATE_CREATED_ESFN, value) => acc.dateCreated = dateCreatedParser(value)
    case (IMG_ESFN, value)          => acc.img = JacksonWrapper.convert[MImgInfo](value)
  }
}

case class MWelcomeAd(
  var martId      : MartId_t,
  var img         : MImgInfo,
  var companyId   : MCompany.CompanyId_t,
  var dateCreated : DateTime = null,
  var id          : Option[String] = None
) extends EsModelT[MWelcomeAd] with MMartAdT[MWelcomeAd] {

  @JsonIgnore def companion: EsModelMinimalStaticT[MWelcomeAd] = MWelcomeAd

  @JsonIgnore def offers: List[MMartAdOfferT] = Nil
  @JsonIgnore def textAlign: Option[MMartAdTextAlign] = None
  @JsonIgnore def panel: Option[MMartAdPanelSettings] = None
  @JsonIgnore def prio: Option[Int] = None
  @JsonIgnore def showLevels: Set[AdShowLevel] = Set.empty
  @JsonIgnore def userCatId: Option[String] = None
  @JsonIgnore def logoImg: Option[MImgInfo] = None
  @JsonIgnore def shopId: Option[MShop.ShopId_t] = None
}



/** JMX MBean интерфейс */
trait MWelcomeAdJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MWelcomeAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MWelcomeAdJmxMBean {
  def companion = MWelcomeAd
}
