package util.adv.geo

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.MPrice
import models.adv.geo.IAdvGeoFormResult
import models.adv.geo.mapf.AdvGeoShapeInfo_t
import models.adv.geo.tag.IAgtFormResult
import models.adv.price.MAdvPricing
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.billing.Bill2Util
import util.ble.BeaconsBilling

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:43
 * Description: Утиль для биллинга размещений в тегах.
 */
class AdvGeoBillUtil @Inject() (
  bill2Util                           : Bill2Util,
  beaconsBilling                      : BeaconsBilling,
  protected val mItems                : MItems,
  protected val mCommonDi             : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.driver.api._

  private def _oneTag1dayPrice: MPrice = {
    bill2Util.zeroPrice.withAmount(1.0)
  }

  private def _oneTagPrice(geoMult: Double): MPrice = {
    val oneTag1dPrice = _oneTag1dayPrice
    oneTag1dPrice.withAmount(
      oneTag1dPrice.amount * geoMult
    )
  }

  /**
    * Посчитать мультипликатор стоимости на основе даты и радиуса размещения.
    *
    * @param res Результат маппинга формы.
    * @return Double-мультипликатор цены.
    */
  def getPriceMult(res: IAdvGeoFormResult): Double = {
    val daysCount = bill2Util.getDaysCount(res.period)

    // Привести радиус на карте к множителю цены
    val radKm = res.radMapVal.circle.radius.kiloMeters
    val radMult = radKm / 1.5

    radMult * daysCount
  }

  /**
    * Закинуть в корзину bill-v2.
    *
    * @param orderId id-ордера-корзины, т.е. текущего заказа. Туда надо добавить возможную покупку.
    *                Например, выхлоп [[util.billing.Bill2Util.ensureCartOrder()]].
    * @param adId    id узла-цели размещения тегов, обычно рекламная карточка.
    * @param res     Данные по размещаемым тегам.
    * @return Фьючерс c результатом.
    */
  def addToOrder(orderId: Gid_t, producerId: String, adId: String, res: IAgtFormResult, status: MItemStatus): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    // Собираем экшен заливки item'ов. Один тег -- один item. А цена у всех одна.
    val geoMult = getPriceMult(res)
    val p = _oneTagPrice(geoMult)

    // Пройтись по тегам
    val mitemsTagActs = for (tag <- res.tags) yield {
      val itm0 = MItem(
        orderId       = orderId,
        iType         = MItemTypes.GeoTag,
        status        = status,
        price         = p,
        nodeId        = adId,
        dtIntervalOpt = Some(res.period.interval),
        rcvrIdOpt     = tag.nodeId,
        tagFaceOpt    = Some(tag.face),
        geoShape      = Some(res.radMapVal.circle)
      )
      mItems.insertOne(itm0)
    }

    // Если галочка главного экрана выставлена, то ещё разместить и так просто, в месте на карте.
    val mitemsActs = if (res.onMainScreen) {
      val itmP = MItem(
        orderId       = orderId,
        iType         = MItemTypes.GeoPlace,
        status        = status,
        price         = getPricePlace(geoMult),
        nodeId        = adId,
        dtIntervalOpt = Some(res.period.interval),
        rcvrIdOpt     = None,
        geoShape      = Some(res.radMapVal.circle)
      )
      mItems.insertOne(itmP) :: mitemsTagActs

    } else {
      mitemsTagActs
    }

    DBIO.sequence(mitemsActs)
  }


  /**
    * Рассчет общей стоимости для результата маппинга формы.
    *
    * @param res Запрашиваемое юзером размещение.
    * @return
    */
  def getPricing(res: IAgtFormResult): Future[MAdvPricing] = {
    val geoMult = getPriceMult(res)
    val p1 = _oneTagPrice(geoMult)

    // Посчитать цены размещения для каждого тега.
    var prices1 = List(
      p1.withAmount(
        p1.amount * res.tags.size
      )
    )

    if (res.onMainScreen)
      prices1 ::= getPricePlace(geoMult)

    LOGGER.trace(s"computePricing(): $res => $prices1")

    val prices2 = MPrice.sumPricesByCurrency(prices1).toSeq

    val result = bill2Util.getAdvPricing( prices2 )
    Future.successful(result)
  }


  def getPricing(res: IAgtFormResult, forceFree: Boolean): Future[MAdvPricing] = {
    if (forceFree)
      bill2Util.zeroPricingFut
    else
      getPricing(res)
  }

  def getPricing(resOpt: Option[IAgtFormResult], forceFree: Boolean): Future[MAdvPricing] = {
    resOpt.fold {
      bill2Util.zeroPricingFut
    } { res =>
      getPricing(res, forceFree)
    }
  }



  /** Базовый множитель цены. */
  def PLACE_PRICE_MULT = 3.0


  private def getPricePlace(geoMult: Double): MPrice = {
    bill2Util.zeroPrice.withAmount(
      geoMult * PLACE_PRICE_MULT
    )
  }

  /** Сборка query для поиска текущих item'ов карточки. */
  def findCurrentForAdQ(adId: String): Query[mItems.MItemsTable, MItem, Seq] = {
    mItems.query
      .filter { i =>
        i.withNodeId(adId) &&
          i.withTypes( MItemTypes.advGeoTypes ) &&
          i.withStatuses( MItemStatuses.advBusy )
      }
  }

  /**
    * Поиск уже текущих размещений для указанной карточки.
    *
    * @param adId id рекламной карточки.
    * @return DBIO-экшен, возвращающий MItem'ы.
    */
  def findCurrentForAd(adId: String, limit: Int = 200): DBIOAction[Seq[MItem], Streaming[MItem], Effect.Read] = {
    findCurrentForAdQ(adId)
      .take(limit)
      // Сортировка пока не требуется, но возможно потребуется.
      .result
  }


  /** Собрать минимальную и достаточную геоинфу для рендера разноцветных кружочков на карте размещений. */
  def onlyGeoShapesInfo(query: Query[mItems.MItemsTable, MItem, Seq], limit: Int = 200): DBIOAction[Seq[AdvGeoShapeInfo_t], Streaming[AdvGeoShapeInfo_t], Effect.Read] = {
    query
      // WHERE не пустой geo_shape
      .filter(_.geoShapeOpt.isDefined)
      // GROUP BY geo_shape
      .groupBy(_.geoShapeStrOpt)
      .map { case (geoShapeStrOpt, group) =>
        // Делаем правильный кортеж: ключ -- строка шейпа, id - любой, status -- только максимальный
        (geoShapeStrOpt.get,
          group.map(_.id).max.get,
          (group.map(_.statusStr).max =!= MItemStatuses.AwaitingMdr.strId).get
          )
      }
      // LIMIT 200
      .take(limit)
      .result
    // TODO Нужно зашейпить эти кортежи в MAdvGeoShapeInfo. .map() не котируем, т.к. это ломает streaming.
  }

  /**
    * Поиск ПРЯМЫХ размещений для рекламной карточки на указанных ресиверах.
    *
    * @param adId id рекламной карточки.
    * @param rcvrIds id узлов-ресиверов.
    * @param limitOpt Предел кол-ва результатов.
    * @return DB-экшен, возвращающий список item'ом в неопределенном порядке.
    */
  def findCurrForAdToRcvrs(adId: String, rcvrIds: Traversable[String], statuses: TraversableOnce[MItemStatus], limitOpt: Option[Int] = None): DBIOAction[Seq[MItem], Streaming[MItem], Effect.Read] = {
    val q = mItems.query
      .filter { i =>
        // Интересует только указанная карточка...
        i.withNodeId(adId) &&
          // Размещаяемая на указанных узлах-ресиверах
          i.withRcvrs( rcvrIds ) &&
          // и только прямые размещения на узлах.
          i.withTypes( MItemTypes.advDirectTypes ) &&
          // и только текущие размещения (по статусам)
          i.withStatuses( statuses )
      }
    // Без limit, если подразумевается стриминг всех результатов.
    limitOpt
      .fold(q)(q.take)
      .result
  }


  /**
    * Поиск черновых размещений для указанной карточки.
    *
    * @param adId id рекламной карточки.
    * @param limit макс. кол-во результатов.
    * @return DB-Экшен
    */
  def findDraftsForAd(adId: String, limit: Int = 100): DBIOAction[Seq[MItem], Streaming[MItem], Effect.Read] = {
    mItems.query
      .filter { i =>
        i.withNodeId(adId) &&
          i.withTypes( MItemTypes.advGeoTypes ) &&
          i.withStatus( MItemStatuses.Draft )
      }
      .take(limit)
      .result
  }


  /** Поиск всех id'шников под-узлов для указанного узла-ресивера.
    * Это включает в себя поиск активированных BLE-маячков узла и возможно какие-то иные вещи.
    *
    * @param nodeId id узла-ресивера.
    * @return Фьючерс с множеством id'шников всех как-то подчиненных узлов.
    */
  def findActiveSubNodeIdsOfRcvr(nodeId: String): Future[Set[String]] = {
    // Запустить сбор маячков, активированных на узле.
    Source.fromPublisher {
      slick.db.stream {
        beaconsBilling.findActiveBeaconIdsOfRcvr(nodeId)
      }
    }
      .runFold( Set.newBuilder[String] )(_ += _)
      .map( _.result() )
  }

}
