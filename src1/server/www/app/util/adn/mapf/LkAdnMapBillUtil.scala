package util.adn.mapf

import java.time.{LocalDate, OffsetDateTime}

import com.google.inject.{Inject, Singleton}
import io.suggest.adn.mapf.MLamForm
import io.suggest.bill.price.dsl.{IPriceDslTerm, MPriceReason, MReasonTypes, Mapper}
import io.suggest.bill.MGetPriceResp
import io.suggest.dt.YmdHelpersJvm
import io.suggest.geo.PointGs
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.model.n2.node.MNode
import io.suggest.www.util.dt.DateTimeUtil
import models.adv.{IAdvBillCtx, MAdvBillCtx}
import models.mctx.Context
import models.mdt.MDateStartEnd
import models.mproj.ICommonDi
import util.adv.AdvUtil
import util.billing.{Bill2Util, TfDailyUtil}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.16 22:12
  * Description: Биллинг размещения узлов просто на карте.
  */
@Singleton
class LkAdnMapBillUtil @Inject() (
                                   bill2Util                  : Bill2Util,
                                   mItems                     : MItems,
                                   advUtil                    : AdvUtil,
                                   tfDailyUtil                : TfDailyUtil,
                                   ymdHelpersJvm              : YmdHelpersJvm,
                                   protected val mCommonDi    : ICommonDi
                                 ) {

  import mCommonDi._
  import slick.profile.api._
  import ymdHelpersJvm.Implicits.LocalDateYmdHelper

  /** id узла-источника тарифа для рассчёта всего остального. */
  def TF_NODE_ID: String = bill2Util.CBCA_NODE_ID


  def advBillCtx(isSuFree: Boolean, mnode: MNode, res: MLamForm): Future[MAdvBillCtx] = {
    // Подготовить интервал размещения...
    advUtil.rcvrBillCtx(
      mad     = mnode,
      rcvrIds = TF_NODE_ID :: Nil,
      ivl     = MDateStartEnd(res.datePeriod.info),
      bmc     = None
    )
  }


  /** Множитель стоимости размещения узла на карте. */
  def PRICE_MULT = 5.0


  /**
    * Добавление item'а размещения ADN-узла на карте в ордер (корзину).
    *
    * @param orderId id ордера-корзины.
    * @param nodeId id ADN-узла, размещаемого на карте.
    * @param formRes Результат биндинга формы размещения.
    * @param status Статус новых item'ов.
    * @return DB-экшен добавления заказа в ордер.
    */
  def addToOrder(orderId: Gid_t, nodeId: String, formRes: MLamForm, status: MItemStatus, abc: IAdvBillCtx): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    // Собираем экшен заливки item'ов. Один тег -- один item. А цена у всех одна.
    val priceDsl = advUtil.prepareForSave( getPriceDsl(abc) )

    val ymdPeriod = formRes.datePeriod.info
    val dateStart = ymdPeriod.dateStart[LocalDate]
    val dateEnd   = ymdPeriod.dateEnd[LocalDate]

    // Инновация: берём временную зону прямо из браузера!
    val tzOffset = DateTimeUtil.minutesOffset2TzOff( formRes.tzOffsetMinutes )

    def __dt(localDate: LocalDate): Option[OffsetDateTime] = {
      Some( localDate.atStartOfDay().atOffset(tzOffset) )
    }

    val dtStartOpt  = __dt( dateStart )
    val dtEndOpt    = __dt( dateEnd )

    val mitem = MItem(
      orderId       = orderId,
      iType         = MItemTypes.AdnNodeMap,
      status        = status,
      price         = priceDsl.price,
      nodeId        = nodeId,
      dateStartOpt  = dtStartOpt,
      dateEndOpt    = dtEndOpt,
      rcvrIdOpt     = None,
      geoShape      = Some( PointGs(formRes.coord) )
    )
    for {
      mitem2 <- mItems.insertOne(mitem)
    } yield {
      mitem2 :: Nil
    }
  }

  // TODO Подумать на тему максимум одной покупки и отката других adn-map размещений ПОСЛЕ оплаты.

  /** Рассчёт ценника размещения. */
  def getPricing(formRes: MLamForm, isSuFree: Boolean, abc: IAdvBillCtx)(implicit ctx: Context): Future[MGetPriceResp] = {
    if (isSuFree) {
      bill2Util.zeroPricingFut
    } else {
      getPricing(formRes, abc)
    }
  }
  def getPricing(formRes: MLamForm, abc: IAdvBillCtx)(implicit ctx: Context): Future[MGetPriceResp] = {
    val priceDsl = advUtil.prepareForRender( getPriceDsl(abc) )
    // Собрать итоговый ответ с подробными ценами для формы.
    val resp = MGetPriceResp(
      prices   = priceDsl.price :: Nil,
      priceDsl = Some(priceDsl)
    )
    Future.successful(resp)
  }


  /** Рассчёт посуточной стоимости через PriceDSL.
    *
    * @param abc Контекст подсчёта.
    * @return Терм PriceDSL, готовый к рассчётам цены.
    */
  def getPriceDsl(abc: IAdvBillCtx): IPriceDslTerm = {
    val p0 = advUtil.calcDateAdvPriceOnTf(TF_NODE_ID, abc)
    Mapper(
      underlying    = p0,
      multiplifier  = Some( PRICE_MULT ),
      reason        = Some(
        MPriceReason(
          reasonType = MReasonTypes.AdnMapAdv
        )
      )
    )
  }

}
