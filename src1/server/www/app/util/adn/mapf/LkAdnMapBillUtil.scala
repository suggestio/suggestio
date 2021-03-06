package util.adn.mapf

import java.time.{LocalDate, OffsetDateTime}
import javax.inject.Inject
import io.suggest.adn.mapf.MLamForm
import io.suggest.bill.price.dsl._
import io.suggest.bill.MGetPriceResp
import io.suggest.common.empty.OptionUtil
import io.suggest.dt.CommonDateTimeUtil
import io.suggest.geo.CircleGs
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItem
import io.suggest.mbill2.m.item.status.MItemStatus
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.util.effect.WT
import io.suggest.model.SlickHolder
import io.suggest.n2.node.MNode
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.util.logs.MacroLogsImpl
import models.adv.{IAdvBillCtx, MAdvBillCtx}
import models.mctx.Context
import models.mdt.MDateStartEnd
import play.api.inject.Injector
import scalaz.{EphemeralStream, Tree}
import util.adv.AdvUtil
import util.billing.{Bill2Conf, BillDebugUtil}

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.11.16 22:12
  * Description: Биллинг размещения узлов просто на карте.
  */
final class LkAdnMapBillUtil @Inject() (
                                         injector: Injector,
                                       )
  extends MacroLogsImpl
{

  private lazy val bill2Conf = injector.instanceOf[Bill2Conf]
  private lazy val billDebugUtil = injector.instanceOf[BillDebugUtil]
  private lazy val advUtil = injector.instanceOf[AdvUtil]
  protected[this] lazy val slickHolder = injector.instanceOf[SlickHolder]

  import slickHolder.slick
  import slick.profile.api._


  /** id узла-источника тарифа для рассчёта всего остального. */
  def TF_NODE_ID: String = bill2Conf.CBCA_NODE_ID


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
  private def getGeoPriceMult(circle: CircleGs): Double = {
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
    val tzOffset = CommonDateTimeUtil.minutesOffset2TzOff( formRes.tzOffsetMinutes )

    def __dt(localDate: LocalDate): Option[OffsetDateTime] = {
      Some( localDate.atStartOfDay().atOffset(tzOffset) )
    }

    val dtStartOpt  = __dt( dateStart )
    val dtEndOpt    = __dt( dateEnd )

    val isFreeAdv   = status.isAdvBusyApproved

    val itemActions = priceDsl
      .splitOnSumTillItemLevel
      .iterator
      .flatMap { priceTerm0 =>
        val priceTerm = advUtil.prepareForSave( priceTerm0 )
        lazy val logPrefix2 = s"$logPrefix (${priceTerm.getClass.getSimpleName}#${priceTerm.hashCode()}) "
        LOGGER.trace(s"$logPrefix2 term = $priceTerm")

        // Если status соответствует уже одобренному размещению, то значит цену записывать не требуется.
        val itemPrice = if (isFreeAdv)
          bill2Conf.zeroPrice
        else
          priceTerm.price

        priceTerm
          .findWithReasonType( MReasonTypes.GeoLocCapture )
          .map { _ =>
            LOGGER.trace(s"$logPrefix2 Geo loc capture")
            MItem(
              orderId       = orderId,
              iType         = MItemTypes.GeoLocCaptureArea,
              status        = status,
              price         = itemPrice,
              nodeId        = nodeId,
              dateStartOpt  = dtStartOpt,
              dateEndOpt    = dtEndOpt,
              // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
              rcvrIdOpt     = None,
              geoShape      = Some( formRes.mapCursor ),
              //priceDsl      = Some( priceTerm ), // Disabled, need compression for long date ranges.
            )
          }
          .map { itm =>
            itm -> OptionUtil.maybe(!isFreeAdv)(priceTerm)
          }
      }
      .map { billDebugUtil.insertItemWithPriceDebug1 }
      .toSeq

    DBIO.sequence( itemActions )
      .transactionally
  }


  // TODO Подумать на тему максимум одной покупки и отката других adn-map размещений ПОСЛЕ оплаты.

  /** Рассчёт ценника размещения. */
  def getPricing(formRes: MLamForm, isSuFree: Boolean, abc: IAdvBillCtx)
                (implicit ctx: Context): Future[MGetPriceResp] = {
    if (isSuFree) {
      bill2Conf.zeroPricingFut
    } else {
      getPricing(formRes, abc)
    }
  }
  def getPricing(formRes: MLamForm, abc: IAdvBillCtx)(implicit ctx: Context): Future[MGetPriceResp] = {
    val priceDsl = advUtil.prepareForRender(
      getPriceDsl(formRes, abc) )
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
  def getPriceDsl(formRes: MLamForm, abc: IAdvBillCtx): Tree[PriceDsl] = {
    // Размещение в геолокации выдачи.
    lazy val p0 = advUtil.calcDateAdvPriceOnTf(TF_NODE_ID, abc)

    // Для "упрощения" цифр и просто по техническим причинам используется два вложенных маппера: один по площади, второй -- константа.
    val innerMapper = Tree.Node(
      PriceDsl.mapper(
        // Маппер-константа.
        multiplifier = Some( PRICE_MULT ),
        reason       = Some( MPriceReason(
          reasonType = MReasonTypes.GeoLocCapture
        ))
      ),
      // Рассчитать исходную финансовую нагрузку на основе посуточного тарифа.
      p0 ##:: EphemeralStream[Tree[PriceDsl]],
    )

    // Маппер по площади на карте - снаружи, чтобы можно было внутрь него пихать иные мапперы.
    val geoMapper = Tree.Node(
      PriceDsl.mapper(
        multiplifier  = Some(
          getGeoPriceMult( formRes.mapCursor )
        ),
        reason        = Some(
          MPriceReason(
            reasonType = MReasonTypes.GeoArea,
            geoCircles = Seq( formRes.mapCursor )
          )
        )
      ),
      EphemeralStream( innerMapper ),
    )

    geoMapper
  }

}
