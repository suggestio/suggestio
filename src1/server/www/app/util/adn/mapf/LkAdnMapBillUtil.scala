package util.adn.mapf

import java.time.{LocalDate, OffsetDateTime}

import com.google.inject.{Inject, Singleton}
import io.suggest.adn.mapf.MLamForm
import io.suggest.bill.price.dsl._
import io.suggest.bill.MGetPriceResp
import io.suggest.bin.BinaryUtil
import io.suggest.dt.YmdHelpersJvm
import io.suggest.geo.{CircleGs, MGeoCircle, PointGs}
import io.suggest.mbill2.m.dbg.{MDbgKeys, MDebug, MDebugs}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.util.effect.WT
import io.suggest.model.n2.node.MNode
import io.suggest.pick.PickleUtil
import io.suggest.util.logs.MacroLogsImpl
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
                                   mDebugs                    : MDebugs,
                                   advUtil                    : AdvUtil,
                                   tfDailyUtil                : TfDailyUtil,
                                   ymdHelpersJvm              : YmdHelpersJvm,
                                   protected val mCommonDi    : ICommonDi
                                 )
  extends MacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._
  import ymdHelpersJvm.Implicits.LocalDateYmdHelper

  /** id узла-источника тарифа для рассчёта всего остального. */
  def TF_NODE_ID: String = bill2Util.CBCA_NODE_ID


  def advBillCtx(isSuFree: Boolean, mnode: MNode, res: MLamForm): Future[MAdvBillCtx] = {
    // Подготовить интервал размещения...
    advUtil.rcvrBillCtx(
      rcvrIds = TF_NODE_ID :: Nil,
      ivl     = MDateStartEnd(res.datePeriod.info),
      bmc     = None
    )
  }


  /** Множитель стоимости размещения узла на карте. */
  private def PRICE_MULT = 5.0


  /**
    * Посчитать мультипликатор стоимости на основе даты и радиуса размещения.
    *
    * @param circle Гео-круг.
    * @return Double-мультипликатор цены.
    */
  private def getGeoPriceMult(circle: MGeoCircle): Double = {
    val radius = circle.radiusM / 50
    // Привести радиус на карте к множителю цены
    Math.max(0.1, radius * radius )
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
  def addToOrder(orderId: Gid_t, nodeId: String, formRes: MLamForm, status: MItemStatus, abc: IAdvBillCtx): DBIOAction[Seq[MItem], NoStream, WT] = {
    // Собираем экшен заливки item'ов. Один тег -- один item. А цена у всех одна.
    val priceDsl = getPriceDsl(formRes, abc)

    lazy val logPrefix = s"addToOrder($orderId)[${System.currentTimeMillis()}]:"

    if (priceDsl.isEmpty)
      throw new IllegalArgumentException(s"$logPrefix Cannot add to order empty form: $formRes")

    LOGGER.trace(s"$logPrefix ADN-node#$nodeId status=$status form=$formRes")

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

    val itemActions = priceDsl
      .splitOnSumTillItemLevel
      .toIterator
      .flatMap { priceTerm0 =>
        val priceTerm = advUtil.prepareForSave( priceTerm0 )
        lazy val logPrefix2 = s"$logPrefix (${priceTerm.getClass.getSimpleName}#${priceTerm.hashCode()}) "
        LOGGER.trace(s"$logPrefix2 term = $priceTerm")

        priceTerm
          .findWithReasonType( MReasonTypes.AdnMapAdv )
          .map { _ =>
            LOGGER.trace(s"$logPrefix2 on Advs map")
            MItem(
              orderId       = orderId,
              iType         = MItemTypes.AdnNodeMap,
              status        = status,
              price         = priceTerm.price,
              nodeId        = nodeId,
              dateStartOpt  = dtStartOpt,
              dateEndOpt    = dtEndOpt,
              // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
              rcvrIdOpt     = None,
              geoShape      = Some(
                PointGs( formRes.mapCursor.center )
              )
            )
          }
          .orElse {
            priceTerm
              .findWithReasonType( MReasonTypes.GeoLocCapture )
              .map { _ =>
                LOGGER.trace(s"$logPrefix2 Geo loc capture")
                MItem(
                  orderId       = orderId,
                  iType         = MItemTypes.GeoLocCaptureArea,
                  status        = status,
                  price         = priceTerm.price,
                  nodeId        = nodeId,
                  dateStartOpt  = dtStartOpt,
                  dateEndOpt    = dtEndOpt,
                  // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
                  rcvrIdOpt     = None,
                  geoShape      = Some(
                    CircleGs( formRes.mapCursor )
                  )
                )
              }
          }
          .map { _ -> priceTerm }
      }
      .map { case (itm0, priceTerm) =>
        for {
          mItem <- mItems.insertOne(itm0)
          mDbg  <- {
            val key = MDbgKeys.PriceDsl
            val mdbg0 = MDebug(
              objectId = mItem.id.get,
              key      = key,
              vsn      = key.V_CURRENT,
              data     = BinaryUtil.byteBufToByteArray(
                PickleUtil.pickle(priceTerm)
              )
            )
            mDebugs.insertOne( mdbg0 )
          }
        } yield {
          LOGGER.trace(s"Saved item $mItem and debug $mDbg")
          mItem
        }
      }
      .toSeq

    DBIO.sequence( itemActions )
      .transactionally
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
    val priceDsl = advUtil.prepareForRender( getPriceDsl(formRes, abc) )
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
  def getPriceDsl(formRes: MLamForm, abc: IAdvBillCtx): IPriceDslTerm = {
    val p0 = advUtil.calcDateAdvPriceOnTf(TF_NODE_ID, abc)

    var acc = List.empty[IPriceDslTerm]
    val priceMult = PRICE_MULT

    // Размещение на карте рекламодателей в ЛК
    if (formRes.opts.onAdvMap) {
      acc ::= Mapper(
        underlying    = p0,
        multiplifier  = Some( priceMult ),
        reason        = Some(
          MPriceReason(
            reasonType = MReasonTypes.AdnMapAdv
          )
        )
      )
    }

    // Размещение в геолокации выдачи.
    if (formRes.opts.onGeoLoc) {
      // Для "упрощения" цифр и просто по техническим причинам используется два вложенных маппера: один по площади, второй -- константа.
      val innerMapper = Mapper(
        underlying   = p0,
        // Маппер-константа.
        multiplifier = Some( priceMult ),
        reason       = Some( MPriceReason(
          reasonType = MReasonTypes.GeoLocCapture
        ))
      )

      // Маппер по площади на карте - снаружи, чтобы можно было внутрь него пихать иные мапперы.
      val geoMapper = Mapper(
        underlying    = innerMapper,
        multiplifier  = Some(
          getGeoPriceMult( formRes.mapCursor )
        ),
        reason        = Some(
          MPriceReason(
            reasonType = MReasonTypes.GeoArea,
            geoCircles = Seq( formRes.mapCursor )
          )
        )
      )

      acc ::= geoMapper
    }

    // Вернуть итоговую сумму.
    Sum( acc )
  }

}
