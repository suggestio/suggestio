package util.adv.geo.tag

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.MPrice
import models.adv.geo.tag.IAdvGeoTagsInfo
import models.adv.price.MAdvPricing
import models.mproj.ICommonDi
import models.mtag.MTagBinded
import util.PlayMacroLogsImpl
import util.billing.Bill2Util

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.12.15 13:43
 * Description: Утиль для биллинга размещений в тегах.
 */
class GeoTagAdvBillUtil @Inject() (
  bill2Util                           : Bill2Util,
  mItems                              : MItems,
  val mCommonDi                       : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.driver.api._

  private def _oneTag1dayPrice: MPrice = bill2Util.zeroPrice.copy(amount = 1.0)

  private def _oneTagPrice(res: IAdvGeoTagsInfo): MPrice = {
    val priceMult = _getPriceMult(res)

    val oneTag1dPrice = _oneTag1dayPrice
    oneTag1dPrice.copy(
      amount = oneTag1dPrice.amount * priceMult
    )
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
  def addToOrder(orderId: Gid_t, producerId: String, adId: String, res: IAdvGeoTagsInfo): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    // Собираем экшен заливки item'ов. Один тег -- один item.
    val p = _oneTagPrice(res)

    val mitemsActs = for (tag <- res.tags.toSeq) yield {
      val itm0 = MItem(
        orderId       = orderId,
        iType         = MItemTypes.GeoTag,
        status        = MItemStatuses.Draft,
        price         = p,
        adId          = adId,
        dtIntervalOpt = Some(res.dates.interval),
        rcvrIdOpt     = tag.nodeId,
        tagFaceOpt    = Some(tag.face),
        geoShape      = Some(res.circle)
      )
      mItems.insertOne(itm0)
    }

    DBIO.sequence(mitemsActs)
  }

  /** Рассчет общего мультипликатора цены для каждого из тегов. */
  private def _getPriceMult(res: IAdvGeoTagsInfo): Double = {
    val daysCount = Math.max(1, res.dates.interval.toDuration.getStandardDays) + 1

    // Привести радиус на карте к множителю цены
    val radKm = res.circle.radius.kiloMeters
    val radMult = radKm / 1.5

    radMult * daysCount
  }

  /**
    * Рассчет общей стоимости для результата маппинга формы.
    *
    * @param res Запрашиваемое юзером размещение.
    * @return
    */
  def computePricing(res: IAdvGeoTagsInfo): Future[MAdvPricing] = {
    val p1 = _oneTagPrice(res)

    // Посчитать цены размещения для каждого тега.
    val prices1 = Seq( p1.copy(
      amount = p1.amount * res.tags.size
    ))

    LOGGER.trace(s"computePricing(): $res => $prices1")

    val result = bill2Util.getAdvPricing( prices1 )
    Future.successful(result)
  }

}
