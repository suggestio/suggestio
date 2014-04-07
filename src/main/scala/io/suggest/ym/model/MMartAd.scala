package io.suggest.ym.model

import io.suggest.model._
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.util.SioEsUtil._
import EsModel._
import io.suggest.util.{MacroLogsImpl, JacksonWrapper}
import MShop.ShopId_t, MMart.MartId_t
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import io.suggest.event.{AdDeletedEvent, AdSavedEvent, SioNotifierStaticClientI}
import scala.collection.JavaConversions._
import scala.util.{Failure, Success}
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonIgnore}
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.search.sort.SortOrder
import org.joda.time.DateTime
import com.github.nscala_time.time.OrderingImplicits._
import common._
import ad._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.14 18:30
 * Description: Рекламные "плакаты" в торговом центре.
 * prio используется для задания приоритета в отображении в рамках магазина. На текущий момент там всё просто:
 * если null, то приоритета нет, если 1 то он есть.
 */
@deprecated("mart+shop arch deprecated. Use MAd instead.", "2014.apr.07")
object MMartAd extends EsModelStaticT[MMartAd] with MacroLogsImpl {

  import LOGGER._

  val ES_TYPE_NAME      = "martAd"

  val IMG_ESFN          = "img"
  val OFFERS_ESFN       = "offers"
  val OFFER_BODY_ESFN   = "offerBody"
  val VENDOR_ESFN       = "vendor"
  val MODEL_ESFN        = "model"
  val PRICE_ESFN        = "price"
  val OLD_PRICE_ESFN    = "oldPrice"
  val PANEL_ESFN        = "panel"
  // Категория по дефолту задана через id. Но при индексации заполняется ещё str, который include in all и помогает в поиске.
  val USER_CAT_ID_ESFN  = "userCat.id"
  val OFFER_TYPE_ESFN   = "offerType"

  val FONT_ESFN         = "font"
  val SIZE_ESFN         = "size"
  val COLOR_ESFN        = "color"
  val TEXT_ALIGN_ESFN   = "textAlign"
  val ALIGN_ESFN        = "align"

  val TEXT_ESFN         = "text"
  val TEXT1_ESFN        = "text1"
  val TEXT2_ESFN        = "text2"
  val DISCOUNT_ESFN     = "discount"
  val DISCOUNT_TPL_ESFN = "discoTpl"


  /** Перманентные уровни отображения для рекламных карточек магазина. Если магазин включен, то эти уровни всегда доступны. */
  val SHOP_ALWAYS_SHOW_LEVELS: Set[AdShowLevel] = Set(AdShowLevels.LVL_PRODUCER, AdShowLevels.LVL_PRODUCERS_CATALOG)

  /** Список уровней, которые могут быть активны только у одной карточки в рамках магазина. */
  val SHOP_LEVELS_SINGLETON: Set[AdShowLevel] = Set(AdShowLevels.LVL_RECEIVER_TOP, AdShowLevels.LVL_PRODUCERS_CATALOG)

  /** Перманентные уровни отображения для рекламных карточек ТЦ. */
  val MART_ALWAYS_SHOW_LEVELS: Set[AdShowLevel] = Set(AdShowLevels.LVL_RECEIVER_TOP)

  def dummy(id: String) = {
    MMartAd(
      id = Option(id),
      offers = Nil,
      img = null,
      receivers = null,
      companyId = null,
      producerId = null,
      textAlign = null,
      producerType = null
    )
  }

  /**
   * Найти все рекламные карточки магазина.
   * @param shopId id магазина
   * @return Список результатов.
   */
  def findForShop(shopId: ShopId_t)(implicit ec: ExecutionContext, client: Client): Future[Seq[MMartAd]] = {
    client.prepareSearch(ES_INDEX_NAME)
      .setTypes(ES_TYPE_NAME)
      .setQuery(shopSearchQuery(shopId))
      .addSort(DATE_CREATED_ESFN, SortOrder.DESC)
      .execute()
      .map { searchResp2list }
  }


  def generateMappingStaticFields: List[Field] = ???

  /**
   * Найти все рекламные карточки магазина с поправкой на реалтаймовое обновление индекса.
   * @param shopId id магазина
   * @return Список результатов.
   */
  def findForShopRt(shopId: ShopId_t)(implicit ec: ExecutionContext, client: Client): Future[List[MMartAd]] = {
    findRt(shopSearchQuery(shopId))
  }

  private def sortResults(mads: List[MMartAd]) = mads.sortBy(_.dateCreated).reverse
  private def martQuery(martId: MartId_t) = QueryBuilders.termQuery(MART_ID_ESFN, martId)
  private def martSearchQuery(martId: MartId_t, shopMustMiss: Boolean, withLevels: Option[Seq[AdShowLevel]]) = {
    var query: QueryBuilder = martQuery(martId)
    if (shopMustMiss) {
      val shopMissingFilter = FilterBuilders.missingFilter(SHOP_ID_ESFN)
      query = QueryBuilders.filteredQuery(query, shopMissingFilter)
    }
    searchQuery(query, withLevels)
  }
  private def searchQuery(query0: QueryBuilder, withLevels: Option[Seq[AdShowLevel]]): QueryBuilder = {
    if (withLevels.isDefined) {
      val lvlSet = withLevels.get
      // Нужен фильтр по уровням.
      val lvlFilter = if (lvlSet.isEmpty) {
        // нужна реклама без уровней вообще
        FilterBuilders.missingFilter(EMReceivers.SLS_WANT_ESFN)
      } else {
        FilterBuilders.termsFilter(EMReceivers.SLS_WANT_ESFN, lvlSet : _*)
      }
      val nestedLvlFilter = FilterBuilders.nestedFilter(EMReceivers.RECEIVERS_ESFN, lvlFilter)
      QueryBuilders.filteredQuery(query0, nestedLvlFilter)
    } else {
      query0
    }
  }
  private[model] def shopSearchQuery(shopId: ShopId_t, withLevels: Option[Seq[AdShowLevel]] = None) = {
    val query = QueryBuilders.termQuery(SHOP_ID_ESFN, shopId)
    searchQuery(query, withLevels)
  }

  /**
   * Реалтаймовый поиск карточек в рамках ТЦ для отображения в ЛК ТЦ.
   * @param martId id ТЦ
   * @param shopMustMiss true, если нужно найти карточки, не относящиеся к магазинам. Т.е. собственные
   *                     карточки ТЦ.
   *                     false - в выдачу также попадут карточки магазинов.
   * @return Список карточек, относящихся к ТЦ.
   */
  def findForMartRt(martId: MartId_t, shopMustMiss: Boolean)(implicit ec: ExecutionContext, client: Client): Future[List[MMartAd]] = {
    val query = martSearchQuery(martId, shopMustMiss, withLevels = None)
    findRt(query)
  }

  /** common-функция для запросов в реальном времени. */
  private def findRt(query: QueryBuilder)(implicit ec: ExecutionContext, client: Client): Future[List[MMartAd]] = {
    findQueryRt(query)
      .map { sortResults }
  }


  override def applyKeyValue(acc: MMartAd): PartialFunction[(String, AnyRef), Unit] = {
      case (PRIO_ESFN, value)         => acc.prio = Option(intParser(value))
      case (USER_CAT_ID_ESFN, value)  => acc.userCatId = Option(stringParser(value))
      case (OFFERS_ESFN, value: java.lang.Iterable[_]) =>
        acc.offers = value.toList.map {
          case jsObject: java.util.Map[_, _] =>
            jsObject.get(OFFER_TYPE_ESFN) match {
              case ots: String =>
                val ot = AdOfferTypes.withName(ots)
                val offerBody = jsObject.get(OFFER_BODY_ESFN)
                import AdOfferTypes._
                ot match {
                  case PRODUCT  => AOProduct.deserialize(offerBody)
                  case DISCOUNT => AODiscount.deserialize(offerBody)
                  case TEXT     => AOText.deserialize(offerBody)
                }
            }
        }
      case (PANEL_ESFN, value)        => acc.panel = Option(JacksonWrapper.convert[AdPanelSettings](value))
      case (TEXT_ALIGN_ESFN, value)   => acc.textAlign = Option(JacksonWrapper.convert[TextAlign](value))
      case (IMG_ESFN, value)          => acc.img = JacksonWrapper.convert[MImgInfo](value)
  }

  /** Генератор пропертисов для маппигов индексов этой модели. */
  override def generateMappingProps: List[DocField] = {
    val fontField = FieldObject(FONT_ESFN, enabled = false, properties = Nil)
    def stringValueField(boost: Float = 1.0F) = FieldString(
      VALUE_ESFN,
      index = FieldIndexingVariants.no,
      include_in_all = true,
      boost = Some(boost)
    )
    def floatValueField(iia: Boolean) = {
      FieldNumber(VALUE_ESFN,  fieldType = DocFieldTypes.float,  index = FieldIndexingVariants.no,  include_in_all = iia)
    }
    def priceFields(iia: Boolean) = Seq(
      floatValueField(iia),
      fontField,
      FieldString("currencyCode", include_in_all = false, index = FieldIndexingVariants.no),
      FieldString("orig", include_in_all = false, index = FieldIndexingVariants.no)
    )
    // Поле приоритета. На первом этапе null или число.
    val offerBodyProps = Seq(
      // product-поля
      FieldObject(VENDOR_ESFN, properties = Seq(stringValueField(1.5F), fontField)),
      FieldObject(MODEL_ESFN, properties = Seq(stringValueField(), fontField)),
      // TODO нужно как-то проанализировать цифры эти, округлять например.
      FieldObject(PRICE_ESFN,  properties = priceFields(iia = true)),
      FieldObject(OLD_PRICE_ESFN,  properties = priceFields(iia = false)),
      // discount-поля
      FieldObject(TEXT1_ESFN, properties = Seq(stringValueField(1.1F), fontField)),
      FieldObject(DISCOUNT_ESFN, properties = Seq(floatValueField(iia = true), fontField)),
      FieldObject(DISCOUNT_TPL_ESFN, enabled = false, properties = Nil),
      FieldObject(TEXT2_ESFN, properties = Seq(stringValueField(0.9F), fontField)),
      // text-поля
      FieldObject(TEXT_ESFN, properties = Seq(
        // HTML будет пострипан тут автоматом.
        FieldString(VALUE_ESFN, index = FieldIndexingVariants.no, include_in_all = true),
        stringValueField(),
        fontField
      ))
    )
    val offersField = FieldNestedObject(OFFERS_ESFN, enabled = true, properties = Seq(
      FieldString(OFFER_TYPE_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldObject(OFFER_BODY_ESFN, enabled = true, properties = offerBodyProps)
    ))
    List(
      FieldObject(IMG_ESFN, enabled = false, properties = Nil),
      FieldObject(TEXT_ALIGN_ESFN,  enabled = false,  properties = Nil),
      FieldString(USER_CAT_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed),
      FieldObject(PANEL_ESFN,  enabled = false,  properties = Nil),
      FieldNumber(PRIO_ESFN,  fieldType = DocFieldTypes.integer,  index = FieldIndexingVariants.not_analyzed,  include_in_all = false),
      offersField
    )
  }


  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  override def deleteById(id: String)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    // удалить связанную картинку из хранилища
    val adOptFut = getById(id)
    adOptFut flatMap {
      case Some(ad) =>
        val imgId = ad.img.id
        MPict.deleteFully(imgId) onComplete {
          case Success(_)  => trace("Successfuly erased picture: " + imgId)
          case Failure(ex) => error("Failed to delete associated picture: " + imgId, ex)
        }
        // TODO Нужно одновременно удалять вторичный логотип в logoImg!
        val resultFut = super.deleteById(id)
        /*resultFut onSuccess { case _ =>
          sn publish AdDeletedEvent(ad)
        }*/
        resultFut

      case None => Future successful false
    }
  }


  /** Для апдейта уровней необходимо использовать json, описывающий изменённые поля документа. Тут идёт сборка такого JSON. */
  private def mkLevelsUpdateDoc(newLevels: Iterable[AdShowLevel]): XContentBuilder = {
    val newDocFieldsXCB = XContentFactory.jsonBuilder()
      .startObject()
      .startArray(EMReceivers.RECEIVERS_ESFN + "." + EMReceivers.SLS_WANT_ESFN)
    newLevels.foreach { sl =>
      newDocFieldsXCB.value(sl.toString)
    }
    newDocFieldsXCB.endArray().endObject()
  }


  /**
   * Обновить допустимые уровни отображения рекламы на указанное значение.
   * private - потому что пока за пределами класса-компаньона не используется.
   * @return Фьючерс для синхронизации.
   */
  private def setShowLevels(thisAd: MMartAd)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    // Если в новый уровнях нет уровней, относящихся к singleton-уровням, то обновляем по-быстрому.
    if (thisAd.showLevels.isEmpty) {
      updateShowLevels(thisAd)
    } else {
      if (thisAd.isShopAd)
        setShowLevelsShop(thisAd)
      else
        setShowLevelsMart(thisAd)
    }
  }


  /** Обновление непустых уровней отображения для магазинной карточки. */
  private def setShowLevelsShop(thisAd: MMartAd)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    val shopId = thisAd.producerId
    val adId = thisAd.id.get
    // Считаем кол-во реклам на нижнем уровне. Магазин может отображать только ограниченное кол-во рекламы на своём уровне.
    import AdShowLevels.{LVL_PRODUCER  => l3}
    val has3rdLvl = thisAd.showLevels contains l3
    val thisCanAdd3rdLvlFut = if (has3rdLvl) {
      val query1 = shopSearchQuery(shopId, withLevels = Some(Seq(l3)))
      // Нужно НЕ считать текущую рекламу
      val noIdFilter = FilterBuilders.notFilter(
        FilterBuilders.termFilter("_id", adId)
      )
      val countNoThisQuery = QueryBuilders.filteredQuery(query1, noIdFilter)
      val countFut = count(countNoThisQuery)
      MShop.getById(shopId).map(_.get.settings.supLShopMaxAdsShown) flatMap { max =>
        countFut map { currCount =>
          currCount.toInt < max
        }
      }
    } else {
      Future successful false
    }

    // посчитать signleton-уровни и запустить обновление карточек
    val singletonLevels = thisAd.showLevels intersect SHOP_LEVELS_SINGLETON
    if (!singletonLevels.isEmpty) {
      // В текущем adId есть [новые] уровни, которые затрагивают singleton-ограничение. Нужно сделать апдейт во всех рекламах магазина, убрав их оттуда.
      lazy val logPrefix = s"setShowLevelsShop(${thisAd.id.get}): "
      trace(logPrefix + "Singleton level(s) enabled: " + singletonLevels.mkString(", "))
      findForShopRt(shopId) flatMap { allShopsAds =>
        thisCanAdd3rdLvlFut flatMap { thisCanAdd3rdLvl =>
          val (brb, mads) = allShopsAds.iterator.map { mad =>
            val lvls1 = if (mad.idOrNull == adId) {
              val lvls2 = thisAd.showLevels
              // Отфильтровываем 3й уровень, если предел кол-ва уровней пробит.
              if (has3rdLvl && !thisCanAdd3rdLvl) {
                debug(logPrefix + l3 + " level requested, but limit reached. Dropping it.")
                lvls2.filter(_ != l3)
              } else {
                lvls2
              }
            } else {
              mad.showLevels -- singletonLevels
            }
            mad.showLevels = lvls1
            val urb = client.prepareUpdate(mad.esIndexName, mad.esTypeName, mad.id.get)
              .setDoc(mkLevelsUpdateDoc(lvls1))
            mad -> urb
          }.foldLeft (client.prepareBulk() -> List.empty[MMartAd]) {
            case ((bulk1, madsAcc), (mad, urb))  =>  bulk1.add(urb) -> (mad :: madsAcc)
          }
          val resultFut = laFuture2sFuture(brb.execute())
          /*resultFut onSuccess { case bulkResp =>
            // Сообщить всем, что имело место обновления записей.
            mads foreach { mad =>
              sn publish AdSavedEvent(mad)
            }
          }*/
          resultFut
        }
      }

    } else {
      // singleton-уровней нет, но фильтровать по третьему уровню надо. Делаем это
      if (has3rdLvl) {
        thisCanAdd3rdLvlFut.flatMap { thisCanAdd3rdLvl =>
          if (!thisCanAdd3rdLvl) {
            thisAd.showLevels = thisAd.showLevels.filter(_ != l3)
          }
          updateShowLevels(thisAd)
        }
      } else {
        updateShowLevels(thisAd)
      }
    }
  }


  /** Проверить кол-во рекламы для ТЦ, и если всё ок, то выполнить выставление уровней. */
  private def setShowLevelsMart(thisAd: MMartAd)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    // Эта функция исходит из того, что ВКЛючается уровень. Т.к. уровень всего 1, то его отключение отрабаыватеся в setShowLevels().
    import thisAd.receivers
    import AdShowLevels.{LVL_RECEIVER_TOP => l1}
    val maxAdsFut: Future[Int] = ??? // MMart.getById(thisAd.receivers).map(_.get.settings.supL1MaxAdsShown)
    val countQuery: QueryBuilder = ??? // martSearchQuery(receivers, shopMustMiss = true, withLevels = Some(Seq(l1)))
    // Нужно отсеять из подсчёта текущий id, если он там есть.
    val idFilter = FilterBuilders.notFilter( FilterBuilders.termFilter(MART_ID_ESFN, thisAd.id.get) )
    val count2query = QueryBuilders.filteredQuery(countQuery, idFilter)
    count(count2query) flatMap { currAdsCount =>
      maxAdsFut flatMap { maxAds =>
        if (currAdsCount.toInt >= maxAds) {
          debug(s"setShowLevelsMart(${thisAd.id.get}): $l1 level requested, but limit($maxAds) reached")
          thisAd.showLevels = thisAd.showLevels.filter(_ != l1)
        }
        updateShowLevels(thisAd)
      }
    }
  }

  /** Просто отправить обновление рекламы в хранилище модели. */
  private def updateShowLevels(thisAd: MMartAd)(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    // Это немагазинная реклама, либо singleton-уровней нет.
    val resultFut: Future[UpdateResponse] = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, thisAd.id.get)
      .setDoc(mkLevelsUpdateDoc(thisAd.showLevels))
      .execute()
    // Уведомить всех о том, что в текущей рекламе были изменения.
    //resultFut onSuccess { case updateResp =>
    //  sn publish AdSavedEvent(thisAd)
    //}
    resultFut
  }

}

import MMartAd._

/**
 * Экземпляр модели.
 * @param offers Список рекламных офферов (как правило из одного элемента). Используется прямое кодирование в json
 *               без промежуточных YmOfferDatum'ов. Поля оффера также хранят в себе данные о своём дизайне.
 * @param img Данные по используемой картинке.
 * @param prio Приоритет. На первом этапе null или минимальное значение для обозначения главного и вторичных плакатов.
 * @param userCatId Индексируемые данные по категории рекламируемого товара.
 * @param companyId id компании-владельца в рамках модели MCompany.
 * @param showLevels Список уровней, на которых должна отображаться эта реклама.
 * @param id id товара.
 */
@deprecated("mart+shop arch deprecated. Use MAd instead.", "2014.apr.07")
case class MMartAd(
  var producerId   : ShopId_t,
  var producerType : AdNetMemberType,
  var receivers    : Set[AdReceiverInfo],
  var offers       : List[AdOfferT],
  var img          : MImgInfo,
  var textAlign    : Option[TextAlign],
  var companyId    : MCompany.CompanyId_t,
  var logoImgOpt   : Option[MImgInfo] = None,
  var panel        : Option[AdPanelSettings] = None,
  var prio         : Option[Int] = None,
  var showLevels   : Set[AdShowLevel] = Set.empty,
  var userCatId    : Option[String] = None,
  var id           : Option[String] = None,
  var dateCreated  : DateTime = DateTime.now
) extends MMartAdT[MMartAd] {

  def companion = MMartAd

  def getOwnerId = producerId

  /** Перед сохранением можно проверять состояние экземпляра. */
  @JsonIgnore override def isFieldsValid: Boolean = {
    super.isFieldsValid &&
      img != null && producerId != null && companyId != null && receivers != null
  }


  /** Можно делать какие-то действия после десериализации. Например, можно исправлять значения после эволюции схемы. */
  override def postDeserialize(): Unit = {
    super.postDeserialize()
    if (img == null) {
      img = MImgInfo("BROKEN_DATA")
    }
  }

  /**
   * Сохранить экземпляр в хранилище ES и сгенерить уведомление, если всё ок.
   * @return Фьючерс с новым/текущим id
   */
  override def save(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[String] = {
    val resultFut = super.save
    /*resultFut onSuccess { case adId =>
      this.id = Option(adId)
      sn publish AdSavedEvent(this)
    }*/
    resultFut
  }

  /** Короткий враппер над статическим [[MMartAd.setShowLevels]]. */
  def saveShowLevels(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    MMartAd.setShowLevels(this)
  }
}

/** Интерфейс экземпляра модели для возможности создания классов-врапперов. */
@deprecated("mart+shop arch deprecated. Use MAd instead.", "2014.apr.07")
trait MMartAdT[T <: MMartAdT[T]] extends EsModelT[T] {
  def producerId   : ShopId_t
  def companyId    : MCompany.CompanyId_t
  def receivers    : Set[AdReceiverInfo]
  def producerType : AdNetMemberType
  def offers       : List[AdOfferT]
  def textAlign    : Option[TextAlign]
  def panel        : Option[AdPanelSettings]
  def prio         : Option[Int]
  def showLevels   : Set[AdShowLevel]
  def userCatId    : Option[String]
  def img          : MImgInfo
  def logoImgOpt   : Option[MImgInfo]
  def dateCreated  : DateTime

  @JsonIgnore def isShopAd: Boolean = producerType == AdNetMemberTypes.SHOP

  override def writeJsonFields(acc: XContentBuilder) {
    if (userCatId.isDefined)
      acc.field(USER_CAT_ID_ESFN, userCatId.get)
    if (prio.isDefined)
      acc.field(PRIO_ESFN, prio.get)
    if (panel.isDefined)
      panel.get.render(acc)
    // Загружаем офферы
    if (!offers.isEmpty) {
      acc.startArray(OFFERS_ESFN)
        offers foreach { _ renderJson acc }
      acc.endArray()
    }
    if (!showLevels.isEmpty) {
      acc.startArray(EMReceivers.SLS_WANT_ESFN)
      showLevels.foreach { sl =>
        acc.value(sl.toString)
      }
      acc.endArray()
    }
    acc.rawField(IMG_ESFN, JacksonWrapper.serialize(img).getBytes)
    // TextAlign. Reflections из-за проблем с XCB.
    if (textAlign.isDefined)
      acc.rawField(TEXT_ALIGN_ESFN, JacksonWrapper.serialize(textAlign.get).getBytes)
  }
}


/** Враппер для моделей [[MMartAdT]]. Позволяет легко и быстро написать wrap-модель над уже готовым
  * экземпляром [[MMartAdT]]. Полезно на экспорт-моделях, которые занимаются сохранением расширенных экземпляров
  * [[MMartAdT]] в другие ES-индексы. */
@deprecated("mart+shop arch deprecated. Use MAd instead.", "2014.apr.07")
trait MMartAdWrapperT[T <: MMartAdT[T]] extends MMartAdT[T] {
  def mmartAd: MMartAdT[T]

  def userCatId = mmartAd.userCatId
  def showLevels = mmartAd.showLevels
  def prio = mmartAd.prio
  def panel = mmartAd.panel
  def companyId = mmartAd.companyId
  def producerId = mmartAd.producerId
  def textAlign = mmartAd.textAlign
  def offers = mmartAd.offers
  def receivers = mmartAd.receivers
  def id = mmartAd.id
  def dateCreated = mmartAd.dateCreated
  def img = mmartAd.img
  def producerType: AdNetMemberType = mmartAd.producerType
  def logoImgOpt = mmartAd.logoImgOpt

  @JsonIgnore def companion: EsModelMinimalStaticT[T] = mmartAd.companion
  @JsonIgnore override def isFieldsValid: Boolean = super.isFieldsValid && mmartAd.isFieldsValid
}




/** JMX MBean интерфейс */
@deprecated("mart+shop arch deprecated. Use MAdJmx instead.", "2014.apr.07")
trait MMartAdJmxMBean extends EsModelJMXMBeanCommon

/** JMX MBean реализация. */
@deprecated("mart+shop arch deprecated. Use MAdJmx instead.", "2014.apr.07")
case class MMartAdJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase with MMartAdJmxMBean {
  def companion = MMartAd
}

