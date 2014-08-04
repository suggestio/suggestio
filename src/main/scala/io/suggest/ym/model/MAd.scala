package io.suggest.ym.model

import io.suggest.model._
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event._
import io.suggest.model.common._
import org.joda.time.DateTime
import io.suggest.util.{SioConstants, MacroLogsImpl}
import io.suggest.ym.model.ad._
import io.suggest.ym.model.common._
import scala.util.{Success, Failure}
import io.suggest.ym.model.common.EMReceivers.Receivers_t
import io.suggest.ym.ad.ShowLevelsUtil
import io.suggest.ym.model.common.EMImg.Imgs_t
import org.elasticsearch.search.aggregations.AggregationBuilders

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 15:00
 * Description: Рекламные карточки второго поколения, пригодные для использования в рекламной сети.
 * Рефакторинг в MMartAd был бы слишком глубок, поэтому лучше было начать с чистого листа.
 */
object MAd
  extends EsModelStaticEmpty
  with EMProducerIdStatic
  with EMAdOffersStatic
  with EMImgStatic
  with EMBlockMetaStatic
  with EMReceiversStatic
  with EMPrioOptStatic
  with EMUserCatIdStatic
  with EMDateCreatedStatic
  with EMText4SearchStatic
  with AdsSimpleSearchT
  with EMColorsStatic
  with MacroLogsImpl
  with EMDisableReasonStatic
  with EMRichDescrStatic
  with EMModerationStatic
  with EsModelStaticIgnore
{
  import LOGGER._

  override type T = MAd

  override val ES_TYPE_NAME = "ad"

  protected[model] val blockMetaDflt = BlockMeta(height = 140, blockId = 1)  // TODO Убрать после окончания миграции всех карточек на blocks.

  override protected[model] def dummy(id: Option[String], version: Option[Long]) = {
    MAd(
      producerId = null,
      blockMeta = blockMetaDflt,
      offers = Nil,
      imgs = Map.empty,
      id = id,
      versionOpt = version
    )
  }

  override def generateMappingStaticFields: List[Field] = List(
    FieldSource(enabled = true),
    FieldAll(enabled = true, index_analyzer = SioConstants.EDGE_NGRAM_AN_1, search_analyzer = DFLT_AN)
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
        // Удаляем картинки, привязанные к этой рекламе.
        ad.eraseResources
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
  override def findForProducerRt(producerId: String, maxResults: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[List[MAd]] = {
    super.findForProducerRt(producerId, maxResults)
      .map { sortByDateCreated }
  }


  /**
   * Собрать в кучу всех ресиверов у объяв, которые созданы указанным продьюсером
   * @param producerId id продьюсера.
   * @param maxResults Макс. кол-во результатов на выходе аггрегатора.
   * @return Карта rcvrId -> docCount.
   */
  def findReceiverIdsForProducer(producerId: String, maxResults: Int = 0)(implicit ec: ExecutionContext, client: Client): Future[Map[String, Long]] = {
    prepareSearch
      .setQuery( EMProducerId.producerIdQuery(producerId) )
      .addAggregation( EMReceivers.receiverIdsAgg )
      .setSize(maxResults)
      .execute()
      .map { searchResp =>
        EMReceivers.extractReceiverIdsAgg( searchResp.getAggregations )
      }
  }

  /**
   * Сбор всех продьюсеров, карточки которых имеют указанного ресивера в соотв.поле.
   * @param rcvrId id ресивера.
   * @param maxResults макс.кол-во возвращаемых результатов.
   * @return Карта (rcvrId -> docCount), длина не превышает maxResults.
   */
  def findProducerIdsForReceiver(rcvrId: String, maxResults: Int = 0)(implicit ec: ExecutionContext, client: Client): Future[Map[String, Long]] = {
    prepareSearch
      .setQuery( EMReceivers.receiverIdQuery(rcvrId) )
      .addAggregation(EMProducerId.producerIdAgg)
      .setSize(maxResults)
      .execute()
      .map { searchResp =>
        EMProducerId.extractProducerIdAgg(searchResp.getAggregations)
      }
  }

}


case class MAd(
  var producerId : String,
  var offers     : List[AOBlock],
  var imgs       : Imgs_t,
  var blockMeta  : BlockMeta,
  var receivers  : Receivers_t = Map.empty,
  var prio       : Option[Int] = None,
  var id         : Option[String] = None,
  var userCatId  : Option[String] = None,
  var texts4search : Texts4Search = Texts4Search(),
  var colors     : Map[String, String] = Map.empty,
  var disableReason : List[DisableReason] = Nil,
  var richDescrOpt  : Option[RichDescr] = None,
  var dateCreated   : DateTime = DateTime.now,
  var moderation    : ModerationInfo = ModerationInfo(),
  var versionOpt    : Option[Long] = None
)
  extends EsModelEmpty
  with MAdT
  with EMProducerIdMut
  with EMAdOffersMut
  with EMImgMut
  with EMBlockMetaMut
  with EMReceiversMut
  with EMPrioOptMut
  with EMUserCatIdMut
  with EMDateCreatedMut
  with EMTexts4Search
  with EMColorsMut
  with EMDisableReasonMut
  with EMRichDescrMut
  with EMModerationMut
{
  @JsonIgnore
  override type T = MAd

  @JsonIgnore
  override def companion = MAd

  /**
   * Сохранить экземпляр в хранилище ES. Если всё ок, то породить событие успешного сохранения.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val resultFut = super.save
    resultFut onSuccess { case adId =>
      if (this.id.isEmpty)
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
trait MAdJmxMBean extends EsModelJMXMBeanCommon {
  def searchForReceiverAtPubLevel(receiverId: String, level: String): String
}

/** JMX MBean реализация. */
case class MAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MAdJmxMBean {
  def companion = MAd

  override def searchForReceiverAtPubLevel(receiverId: String, level: String): String = {
    val searchArgs = new AdsSearchArgsT {
      override def levels = List(level.trim).filter(!_.isEmpty).flatMap(AdShowLevels.maybeWithName)
      override def receiverIds = List(receiverId.trim).filter(!_.isEmpty)
      override def maxResults = 100
      override def offset = 0
      override def catIds = Nil
      override def producerIds = Nil
      override def qOpt = None
      override def forceFirstIds = Nil
      override def generation = None
      override def withoutIds = Nil
    }
    MAd.searchAds(searchArgs).map {
      _.map(_.toJsonPretty)
        .mkString("\n,\n")
    }
  }
}
