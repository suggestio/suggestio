package util.adv.gtag

import com.google.inject.Inject
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import models.MPrice
import models.adv.gtag.IAdvGeoTagsInfo
import models.mproj.ICommonDi
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
  mCommonDi                           : ICommonDi
) {

  import mCommonDi._
  import dbConfig.driver.api._


  /** Посчитать стоимость размещения. */
  def computePrice(res: IAdvGeoTagsInfo): Future[MPrice] = {
    // TODO Запилить систему подсчета стоимости размещения.
    val p = bill2Util.zeroPrice
    Future.successful(p)
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
  def addToOrder(orderId: Gid_t, producerId: String, adId: String, price: MPrice, res: IAdvGeoTagsInfo): Future[Seq[MItem]] = {
    // Собираем экшен заливки item'ов. Один тег -- один item.
    val mitemsActs = for (tag <- res.tags.toSeq) yield {
      val itm0 = MItem(
        orderId       = orderId,
        iType         = MItemTypes.GeoTag,
        status        = MItemStatuses.Draft,
        price         = price,
        adId          = adId,
        prodId        = producerId,
        dtIntervalOpt = Some(res.interval),
        rcvrIdOpt     = tag.nodeId,
        tagFaceOpt    = Some(tag.face),
        geoShape      = Some(res.circle)
      )
      mItems.insertOne(itm0)
    }

    val dbioAction = DBIO.sequence(mitemsActs)

    // Запустить сохранение нового item'а.
    dbConfig.db.run(dbioAction.transactionally)
  }

}
