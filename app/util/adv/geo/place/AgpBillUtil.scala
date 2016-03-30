package util.adv.geo.place

import com.google.inject.{Inject, Singleton}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItems, MItem}
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import models.MPrice
import models.adv.geo.place.{IAgpFormResult, MAgpFormResult}
import models.adv.price.MAdvPricing
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl
import util.adv.geo.AdvGeoBillUtil
import util.billing.Bill2Util

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 14:11
  * Description: Утиль для биллинга размещений в произвольных местах на карте.
  */
@Singleton
class AgpBillUtil @Inject() (
  bill2Util       : Bill2Util,
  advGeoBillUtil  : AdvGeoBillUtil,
  mItems          : MItems,
  val mCommonDi   : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.driver.api._


  /** Базовый множитель цены. */
  def BASE_PRICE_MULT = 3.0


  def getPricing(res: MAgpFormResult, forceFree: Boolean): Future[MAdvPricing] = {
    if (forceFree)
      bill2Util.zeroPricingFut
    else
      getPricing(res)
  }
  def getPricing(resOpt: Option[MAgpFormResult], forceFree: Boolean): Future[MAdvPricing] = {
    resOpt.fold {
      bill2Util.zeroPricingFut
    } { res =>
      getPricing(res, forceFree)
    }
  }

  /**
    * Рассчет общей стоимости для результата маппинга формы.
    *
    * @param res Запрашиваемое юзером размещение.
    * @return
    */
  def getPricing(res: IAgpFormResult): Future[MAdvPricing] = {
    val p1 = getPrice(res)

    // Посчитать цены размещения для каждого тега.
    val prices1 = Seq(p1)

    LOGGER.trace(s"computePricing(): $res => $prices1")

    val result = bill2Util.getAdvPricing( prices1 )
    Future.successful(result)
  }


  private def getPrice(res: IAgpFormResult): MPrice = {
    val geoMult = advGeoBillUtil.getPriceMult(res)
    bill2Util.zeroPrice.copy(
      amount = geoMult * BASE_PRICE_MULT
    )
  }


  def addToOrder(orderId: Gid_t, producerId: String, adId: String, res: IAgpFormResult, status: MItemStatus): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    val itm0 = MItem(
      orderId       = orderId,
      iType         = MItemTypes.GeoPlace,
      status        = status,
      price         = getPrice(res),
      adId          = adId,
      dtIntervalOpt = Some(res.period.interval),
      rcvrIdOpt     = None,
      geoShape      = Some(res.radMapVal.circle)
    )
    for (itm1 <- mItems.insertOne(itm0)) yield {
      Seq(itm1)
    }
  }

}
