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
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import io.suggest.ym.ad.ShowLevelsUtil
import io.suggest.ym.model.common.EMImg.Imgs_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 15:00
 * Description: Рекламные карточки второго поколения, пригодные для использования в рекламной сети.
 * Рефакторинг в MMartAd был бы слишком глубок, поэтому лучше было начать с чистого листа.
 */
object MAd
  extends EsModelStaticIgnore
  with EMProducerIdStatic
  with EMAdOffersStatic
  with EMImgStatic
  with EMBlockMetaStatic
  with EMReceiversStatic
  with EMLogoImgStatic
  with EMPrioOptStatic
  with EMUserCatIdStatic
  with EMDateCreatedStatic
  with EMText4SearchStatic
  with AdsSimpleSearchT
  with MacroLogsImpl
{
  import LOGGER._

  override type T = MAd

  val ES_TYPE_NAME: String = "ad"

  protected[model] val blockMetaDflt = BlockMeta(height = 140, blockId = 1)  // TODO Убрать после окончания миграции всех карточек на blocks.

  protected[model] def dummy(id: String) = MAd(
    producerId = null,
    blockMeta = blockMetaDflt,
    offers = Nil,
    imgs = Map.empty,
    id = Some(id)
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
    val adOptFut = getById(id)
    adOptFut flatMap {
      case Some(ad) =>
        lazy val logPrefix = s"deleteById($id): "
        // Удаляем картинку рекламы в фоне
        eraseImgs(ad)
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

  /**
   * Реалтаймовый поиск по создателю. Нужно сортировать в порядке создания карточек.
   * @param producerId id продьюсера.
   * @return Список MAd.
   */
  override def findForProducerRt(producerId: String, maxResults: Int)(implicit ec: ExecutionContext, client: Client): Future[List[MAd]] = {
    super.findForProducerRt(producerId, maxResults)
      .map { sortByDateCreated }
  }
}


case class MAd(
  var producerId : String,
  var offers     : List[AOBlock],
  var imgs       : Imgs_t,
  var blockMeta  : BlockMeta,
  var receivers  : Receivers_t = Map.empty,
  var logoImgOpt : Option[MImgInfo] = None,
  var prio       : Option[Int] = None,
  var id         : Option[String] = None,
  var userCatId  : Option[String] = None,
  var texts4search : Texts4Search = Texts4Search(),
  var dateCreated : DateTime = DateTime.now
)
  extends EsModelEmpty
  with MAdT
  with EMProducerIdMut
  with EMAdOffersMut
  with EMImgMut
  with EMBlockMetaMut
  with EMReceiversMut
  with EMLogoImgMut
  with EMPrioOptMut
  with EMUserCatIdMut
  with EMDateCreatedMut
  with EMTexts4Search
{
  @JsonIgnore
  override type T = MAd

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
      emitSavedEvent
    }
    resultFut
  }

  /** Сохранить новые ресиверы через update. */
  override def saveReceivers(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val resultFut = super.saveReceivers
    resultFut onSuccess {
      case _ => emitSavedEvent
    }
    resultFut
  }

  /** Отправить в шину SN событие успешного сохранения этого экземпляра. */
  def emitSavedEvent(implicit sn: SioNotifierStaticClientI) = sn publish AdSavedEvent(this)

  /** Подготовить изменения в БД для изменения в отображении.
    * @param producer Экземпляр продьюсера, который обычно уже доступен при вызове или закеширован.
    * @return На выходе будет список рекламных карточек, ещё не сохраненных.
    */
  def applyOutputConstraintsFor(producer: MAdnNode)(implicit ec: ExecutionContext, client: Client) = {
    if (producerId != producer.id.get)
      throw new IllegalArgumentException(s"My producer is $producerId, but producer with id=${producer.id.get} passed.")
    ShowLevelsUtil.applyOutputConstraints(this, producer)
  }
}


/** JMX MBean интерфейс */
trait MAdJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
case class MAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdJmxMBean {
  def companion = MAd
}
