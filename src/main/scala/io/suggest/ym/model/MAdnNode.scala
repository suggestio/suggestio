package io.suggest.ym.model

import io.suggest.model.{EsModelJMXBase, EsModelJMXMBeanCommon, EsModelEmpty, EsModelStaticEmpty}
import io.suggest.util.SioEsUtil._
import io.suggest.model.common._
import com.fasterxml.jackson.annotation.JsonIgnore
import common._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.event.SioNotifier.{Subscriber, Classifier}
import io.suggest.event.subscriber.SnFunSubscriber

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.14 19:16
 * Description: Узел рекламной сети. Он приходит на смену всем MMart, MShop и т.д.
 * Модель узла конструируется компилятором из кучи кусков, вроде бы не связанных между собой.
 */
object MAdnNode
  extends EsModelStaticEmpty
  with EMCompanyIdStatic
  with EMPersonIdsStatic
  with EMAdNetMemberStatic
  with EMLogoImgStatic
  with EMAdnMMetadataStatic
{
  val ES_TYPE_NAME: String = "adnNode"

  override type T = MAdnNode

  protected def dummy(id: String) = MAdnNode(
    companyId = null,
    personIds = Set.empty,
    adn = null,
    meta = null,
    id = Option(id)
  )

  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val delFut = super.deleteById(id)
    delFut onSuccess { case isDeleted =>
      sn publish AdnNodeDeletedEvent(id, isDeleted)
    }
    delFut
  }

  // TODO Удалить это после публичного запуска. Тарифы были перенесены в SQL-модель MBillTariff.
  override def applyKeyValue(acc: MAdnNode): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (EMTariff.TARIFF_ESFN, _) => // Do nothing
    }
  }

}


case class MAdnNode(
  var companyId     : CompanyId_t,
  var personIds     : Set[String],
  var adn           : AdNetMemberInfo,
  var meta          : AdnMMetadata,
  var logoImgOpt    : Option[MImgInfo] = None,
  var id            : Option[String] = None
)
  extends EsModelEmpty
  with EMCompanyId
  with EMPersonIds
  with EMAdNetMember
  with EMLogoImgMut
  with EMAdnMMetadata
{
  override type T = MAdnNode

  @JsonIgnore
  def companion = MAdnNode


  /** Перед сохранением можно проверять состояние экземпляра (полей экземпляра). */
  @JsonIgnore
  override def isFieldsValid: Boolean = {
    super.isFieldsValid &&
      companyId != null && personIds != null && adn != null && meta != null &&
      meta != null && meta != null
  }


  /**
   * Сохранить экземпляр в хранилище модели.
   * При успехе будет отправлено событие [[io.suggest.event.AdnNodeSavedEvent]] в шину событий.
   * @return Фьючерс с новым/текущим id.
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val saveFut = super.save
    saveFut onSuccess { case adnId =>
      sn publish AdnNodeSavedEvent(adnId, this, isCreated = id.isEmpty)
    }
    saveFut
  }

  /**
   * У текущего узла появился под-узел.
   * @param childNode Дочерний узел.
   * @return true, если в полях текущего узла произошли какие-то изменения, требующие сохранения.
   */
  def handleChildNodeAddedToMe(childNode: MAdnNode): Boolean = {
    adn.memberType.updateParentForChild(this, child = childNode)
  }

  /**
   * Текущий узел в процессе создания в дочернего узла по отношению к указанному узлу.
   * @param parentNode Существующий родительский узел
   * @return true, если текущая сущность изменялась.
   */
  def handleMeAddedAsChildFor(parentNode: MAdnNode): Boolean = {
    adn.memberType.prepareChildForParent(parentNode, child = this)
  }
}


/** JMX MBean интерфейс */
trait MAdnNodeJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MAdnNodeJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdnNodeJmxMBean {
  def companion = MAdnNode
}
