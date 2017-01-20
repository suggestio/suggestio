package util.adn.mapf

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.model.geo.PointGs
import models.madn.mapf.MAdnMapFormRes
import models.mproj.ICommonDi
import util.billing.Bill2Util

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.16 22:12
  * Description: Биллинг размещения узлов просто на карте.
  */
@Singleton
class LkAdnMapBillUtil @Inject() (
  bill2Util                 : Bill2Util,
  mItems                    : MItems,
  protected val mCommonDi   : ICommonDi
) {

  import mCommonDi._
  import slick.driver.api._


  /** Цена размещения за одни сутки. */
  def ONE_DAY_PRICE = bill2Util.zeroPrice.withAmount( 2.0 )


  /** Рассчёт стоимости размещения. */
  def getPrice(formRes: MAdnMapFormRes): MPrice = {
    // +1 потому что кол-во дней как-то неправильно считается.
    val daysCount = bill2Util.getDaysCount( formRes.period )
    val oneDayPrice = ONE_DAY_PRICE
    oneDayPrice.withAmount(
      daysCount * oneDayPrice.amount
    )
  }


  /**
    * Добавление item'а размещения ADN-узла на карте в ордер (корзину).
    *
    * @param orderId id ордера-корзины.
    * @param nodeId id ADN-узла, размещаемого на карте.
    * @param formRes Результат биндинга формы размещения.
    * @param status Статус новых item'ов.
    * @return DB-экшен добавления заказа в ордер.
    */
  def addToOrder(orderId: Gid_t, nodeId: String, formRes: MAdnMapFormRes, status: MItemStatus): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    val ivl = formRes.period.interval
    val mitem = MItem(
      orderId       = orderId,
      iType         = MItemTypes.AdnNodeMap,
      status        = status,
      price         = getPrice(formRes),
      nodeId        = nodeId,
      dateStartOpt  = Some( ivl.getStart ),
      dateEndOpt    = Some( ivl.getEnd ),
      rcvrIdOpt     = None,
      geoShape      = Some( PointGs(formRes.point) )
    )
    for {
      mitem2 <- mItems.insertOne(mitem)
    } yield {
      mitem2 :: Nil
    }
  }

  // TODO Подумать на тему максимум одной покупки и отката других adn-map размещений ПОСЛЕ оплаты.

  /** Рассчёт ценника размещения. */
  def getPricing(formRes: MAdnMapFormRes, isSuFree: Boolean): Future[MGetPriceResp] = {
    if (isSuFree) {
      bill2Util.zeroPricingFut
    } else {
      getPricing(formRes)
    }
  }
  def getPricing(formRes: MAdnMapFormRes): Future[MGetPriceResp] = {
    val price = getPrice(formRes)
    val pricing = bill2Util.getAdvPricing(Seq(price))
    Future.successful(pricing)
  }

}
