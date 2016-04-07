package util.adv.geo

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.MPrice
import models.adv.geo.IAdvGeoFormResult
import models.adv.geo.tag.IAgtFormResult
import models.adv.price.MAdvPricing
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.billing.Bill2Util

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:43
 * Description: Утиль для биллинга размещений в тегах.
 */
class AdvGeoBillUtil @Inject()(
  bill2Util                           : Bill2Util,
  mItems                              : MItems,
  val mCommonDi                       : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.driver.api._

  private def _oneTag1dayPrice: MPrice = {
    bill2Util.zeroPrice.copy(amount = 1.0)
  }

  private def _oneTagPrice(geoMult: Double): MPrice = {
    val oneTag1dPrice = _oneTag1dayPrice
    oneTag1dPrice.copy(
      amount = oneTag1dPrice.amount * geoMult
    )
  }

  /**
    * Посчитать мультипликатор стоимости на основе даты и радиуса размещения.
    *
    * @param res Результат маппинга формы.
    * @return Double-мультипликатор цены.
    */
  def getPriceMult(res: IAdvGeoFormResult): Double = {
    val daysCount = Math.max(1, res.period.interval.toDuration.getStandardDays) + 1

    // Привести радиус на карте к множителю цены
    val radKm = res.radMapVal.circle.radius.kiloMeters
    val radMult = radKm / 1.5

    radMult * daysCount
  }

  /**
    * Закинуть в корзину bill-v2.
    *
    * @param orderId id-ордера-корзины, т.е. текущего заказа. Туда надо добавить возможную покупку.
    *                Например, выхлоп [[util.billing.Bill2Util.ensureCart()]].
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
        adId          = adId,
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
        adId          = adId,
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
      p1.copy(
        amount = p1.amount * res.tags.size
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
    bill2Util.zeroPrice.copy(
      amount = geoMult * PLACE_PRICE_MULT
    )
  }


  /**
    * Поиск текущих размещений для указанной карточки.
    *
    * @param adId id рекламной карточки.
    * @return DBIO-экшен, возвращающий MItem'ы.
    */
  def getCurrentForAd(adId: String, limit: Int = 200): DBIOAction[Seq[MItem], Streaming[MItem], Effect.Read] = {
    mItems.query
      .filter { i =>
        (i.adId === adId) &&
          (i.statusStr inSet MItemStatuses.advActualIds.toSeq) &&
          (i.iTypeStr inSet MItemTypes.onlyAdvGeoTypeIds.toSeq)
      }
      .take(limit)
      // Сортировка пока не требуется, но возможно потребуется.
      .result
  }

}
