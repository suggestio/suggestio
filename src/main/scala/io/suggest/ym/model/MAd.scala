package io.suggest.ym.model

import io.suggest.model._
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.model.common._
import org.joda.time.DateTime
import io.suggest.util.MacroLogsImpl
import io.suggest.ym.model.ad._
import io.suggest.ym.model.common._
import scala.util.{Success, Failure}
import org.elasticsearch.index.query.QueryBuilder

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 15:00
 * Description: Рекламные карточки второго поколения, пригодные для использования в рекламной сети.
 * Рефакторинг в MMartAd был бы слишком глубок, поэтому лучше было начать с чистого листа.
 */
object MAd
  extends EsModelStaticEmpty[MAd]
  with EMProducerIdStatic[MAd]
  with EMAdOffersStatic[MAd]
  with EMImgStatic[MAd]
  with EMReceiversStatic[MAd]
  with EMLogoImgStatic[MAd]
  with EMTextAlignStatic[MAd]
  with EMAdPanelSettingsStatic[MAd]
  with EMPrioOptStatic[MAd]
  with EMUserCatIdStatic[MAd]
  with EMDateCreatedStatic[MAd]
  with EMText4SearchStatic[MAd]
  with MacroLogsImpl
{

  import LOGGER._

  val ES_TYPE_NAME: String = "ad"

  protected[model] def dummy(id: String) = MAd(
    producerId = null,
    offers = Nil,
    img = null,
    id = Some(id)
  )

  def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true)
  )


  /**
   * Реалтаймовый поиск по создателю.
   * @param producerId id продьюсера.
   * @return Список MAd.
   */
  def findForProducerRt(producerId: String, maxResults: Int = 100): Future[Seq[MAd]] = {
    findQueryRt(producerIdQuery(producerId), maxResults)
  }

  /**
   * Реалтаймовый поиск по получателю.
   * @param receiverId id получателя.
   * @param maxResults Макс. кол-во результатов.
   * @return Последовательность MAd.
   */
  def findForReceiverRt(receiverId: String, maxResults: Int = 100): Future[Seq[MAd]] = {
    findQueryRt(receiverIdQuery(receiverId), maxResults)
  }

  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    val adOptFut = getById(id)
    adOptFut flatMap {
      case Some(ad) =>
        lazy val logPrefix = s"deleteById($id): "
        // Удаляем картинку рекламы в фоне
        val imgId = ad.img.id
        MPict.deleteFully(imgId) onComplete {
          case Success(_)  => trace(logPrefix + "Successfuly erased main picture: " + imgId)
          case Failure(ex) => error(logPrefix + "Failed to delete associated picture: " + imgId, ex)
        }
        // Одновременно удаляем логотип.
        ad.logoImgOpt.foreach { logoImg =>
          val logoImgId = logoImg.id
          MPict.deleteFully(logoImgId) onComplete {
            case Success(_)  => trace(logPrefix + "Successfuly erased 2nd-logo picture: " + logoImgId)
            case Failure(ex) => error(logPrefix + "Failed to delete 2nd-logo picture: " + logoImgId, ex)
          }
        }
        // Одновременно удаляем саму рекламную карточку из хранилища
        val resultFut = super.deleteById(id)
        // Когда всё будет удалено ок, то надо породить событие.
        resultFut onSuccess {
          case true  => sn publish AdDeletedEvent(ad)
          case false => warn(logPrefix + "Failed to delete ad: id not found, but it was! Already deleted concurrently?")
        }
        resultFut

      case None => Future successful false
    }
  }

}


case class MAd(
  var producerId : String,
  var offers     : List[AdOfferT],
  var img        : MImgInfo,
  var receivers  : Set[AdReceiverInfo] = Set.empty,
  var logoImgOpt : Option[MImgInfo] = None,
  var textAlign  : Option[TextAlign] = None,
  var panel      : Option[AdPanelSettings] = None,
  var prio       : Option[Int] = None,
  var id         : Option[String] = None,
  var userCatId  : Option[String] = None,
  var texts4search : Texts4Search = Texts4Search(),
  var dateCreated : DateTime = DateTime.now
)
  extends EsModelEmpty[MAd]
  with MAdT
  with EMProducerIdMut[MAd]
  with EMAdOffersMut[MAd]
  with EMImgMut[MAd]
  with EMReceiversMut[MAd]
  with EMLogoImgMut[MAd]
  with EMTextAlignMut[MAd]
  with EMAdPanelSettingsMut[MAd]
  with EMPrioOptMut[MAd]
  with EMUserCatIdMut[MAd]
  with EMDateCreatedMut[MAd]
  with EMTexts4Search[MAd]
{
  @JsonIgnore
  def companion = MAd

  /**
   * Сохранить экземпляр в хранилище ES. Если всё ок, то породить событие успешного сохранения.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val resultFut = super.save
    resultFut onSuccess { case adId =>
      this.id = Option(adId)
      sn publish AdSavedEvent(this)
    }
    resultFut
  }

}


/** JMX MBean интерфейс */
trait MAdJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdJmxMBean {
  def companion = MAd
}
