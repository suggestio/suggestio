package util.adv.geo

import java.time.{LocalDate, OffsetDateTime}

import com.google.inject.Inject
import io.suggest.adv.geo.MFormS
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill._
import io.suggest.cal.m.{MCalType, MCalTypes}
import io.suggest.dt.{MYmd, YmdHelpersJvm}
import io.suggest.geo.{CircleGs, MGeoCircle}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.bill.tariff.daily.{MDayClause, MTfDaily}
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.www.util.dt.DateTimeUtil
import models.adv.geo.MGeoAdvBillCtx
import models.adv.geo.cur.{AdvGeoBasicInfo_t, AdvGeoShapeInfo_t}
import models.mctx.Context
import models.mdt.MDateStartEnd
import models.mproj.ICommonDi
import models.req.IAdProdReq
import util.TplDataFormatUtil
import util.adn.NodesUtil
import util.adv.{AdvUtil, IAdvPriceDaysCalcListener}
import util.billing.Bill2Util

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.15 13:43
  * Description: Утиль для биллинга размещений прямо на гео-карте.
  *
  * Через год сюда приехал биллинг ресиверов в попапах.
  */
class AdvGeoBillUtil @Inject() (
  bill2Util                           : Bill2Util,
  ymdHelpersJvm                       : YmdHelpersJvm,
  advUtil                             : AdvUtil,
  nodesUtil                           : NodesUtil,
  mNodes                              : MNodes,
  protected val mItems                : MItems,
  protected val mCommonDi             : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._
  import ymdHelpersJvm.Implicits._


  /**
    * id узла, с которого надо брать посуточный тариф для размещения на карте.
    * По идее, тут всегда узел CBCA.
    */
  private def GEO_TF_SRC_NODE_ID = bill2Util.CBCA_NODE_ID

  /**
    * Посчитать мультипликатор стоимости на основе даты и радиуса размещения.
    *
    * @param radiusKm Радиус гео-круга.
    * @return Double-мультипликатор цены.
    */
  private def getPriceMult(radiusKm: Double): Double = {
    // Привести радиус на карте к множителю цены
    radiusKm * radiusKm / 1.5
  }


  /** Сборка контекста для direct-биллинга поверх географии.
    *
    * @param mad рекламная карточка.
    * @param res Содержимое формы.
    * @param request реквест. Биллинг зависит от юзера и продьсера, которые лежат в реквесте.
    * @param addFreeRcvrs Если требуется безопасно дописать в контекст бесплатных ресиверов (без тарифов), то true.
    *                        В норме - false.
    * @return Фьючерс с контекстом биллинга.
    */
  def advBillCtx(isSuFree: Boolean, mad: MNode, res: MFormS, addFreeRcvrs: Boolean = false)(implicit request: IAdProdReq[_]): Future[MGeoAdvBillCtx] = {
    // Подготовить интервал размещения...
    val ivl = MDateStartEnd(res.datePeriod.info)

    val abcFut = if (isSuFree) {
      val freeAbc = advUtil.freeRcvrBillCtx(mad, ivl)
      Future.successful( freeAbc )

    } else {

      for {
        freeNodeIds <- freeAdvNodeIds(
          personIdOpt   = request.user.personIdOpt,
          producerIdOpt = request.producer.id
        )

        // Собираем id всех интересующих узлов. Из этих узлов затем будут получены тарифы...
        rcvrIdsSet = {
          val b = Set.newBuilder[String]

          // Закинуть все id узлов прямого размещения.
          if (res.rcvrsMap.nonEmpty) {
            b ++= res.rcvrsMap
              .keysIterator
              // Выкинуть ресиверов, которые упоминаются среди бесплатных.
              // Тогда, тарифы на них не будут получены, и система выставит им нулевые цены. TODO Что с валютой? Откуда взять нормальное значение оной?
              .filter { rk =>
                !rk.exists(freeNodeIds.contains)
              }
              // Собрать в кучу все id всех упомянутых узлов, дедублицировав их...
              .flatten
          }

          // Если активно георазмещение просто на карте, то надо добавить узел-источник геоценника:
          if (res.radCircle.nonEmpty)
            b += GEO_TF_SRC_NODE_ID

          // Собрать итоговое множество id узлов для сборки карты тарифов.
          b.result()
        }

        abcFut = advUtil.rcvrBillCtx(mad, rcvrIdsSet, ivl)

        // Часть узлов вылетает из карты узлов-ресиверов. Поэтому надо недостающие элементы вычислить и дописать:
        missNodesMapFut = {
          if (addFreeRcvrs) {
            val missNodeIds = res.rcvrsMap
              .keysIterator
              .flatMap(_.lastOption)
              .toSet
              .--(rcvrIdsSet)
            mNodesCache.multiGetMap(missNodeIds)
          } else {
            Future.successful( Map.empty )
          }
        }

        abc <- abcFut
        missNodesMap <- missNodesMapFut

      } yield {
        // Залить недостающих ресиверов в списочек ресиверов bill-контекста.
        abc.withRcvrsMap(
          abc.rcvrsMap ++ missNodesMap
        )
      }
    }

    // Передать краткие итоги работы в сборку.
    for (abc <- abcFut) yield {
      MGeoAdvBillCtx(abc, res)
    }
  }


  /**
    * Закинуть в корзину bill-v2.
    *
    * @param orderId id-ордера-корзины, т.е. текущего заказа. Туда надо добавить возможную покупку.
    *                Например, выхлоп [[util.billing.Bill2Util.ensureCartOrder()]].
    * @return Фьючерс c результатом.
    */
  def addToOrder(orderId: Gid_t, status: MItemStatus, abc: MGeoAdvBillCtx): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    val calc = new ItemsCalc(abc) {
      override val _orderId     = orderId
      override val _itemStatus  = status
    }

    val itemsAcc = calc.execute()
    val itemActs = itemsAcc.map { mItems.insertOne }

    DBIO.sequence(itemActs)
  }


  /**
    * Рассчет общей стоимости для результата маппинга формы.
    * Для суперюзеров с бесплатным размещением этот метод НЕ должен вызываться вообще:
    * метод игнорит состояние флага бесплатного размещения.
    *
    * @param abc Контекст гео-биллинга для рассчёта ценника. См. [[advBillCtx()]].
    * @return Фьючерс с данными прайсинга, пригодными для сериализации и отправки на клиент.
    */
  def getPricing(abc: MGeoAdvBillCtx)(implicit ctx: Context): Future[MGetPriceResp] = {
    // 2017.04.04: Надо бы выводить в форму более подробную инфу по рассчёту стоимости.
    // Поэтому, рассчёт на основе тех же item'ов, что и при добавлении в корзину:
    // item'ы генерятся, их цена суммируется, некоторые данные item'ов сериализуются прямо в ответ.
    val itemsCalc = new ItemsCalc(abc) {
      // Все эти обязательные значения не важны и будут проигнорены:
      override val _orderId     = -1L
      override val _itemStatus  = MItemStatuses.Draft
    }

    val items = itemsCalc.execute()

    if (items.isEmpty) {
      // Можно не продолжать, всё и так понятно.
      bill2Util.zeroPricingFut

    } else {
      // Просуммировать ценники в один итоговый ценник:
      val prices2 = {
        val pricesAcc = items.map(_.price)
        MPrice.sumPricesByCurrency(pricesAcc)
      }

      // Извлечь полезную инфу из items списком
      val infos = items
        .iterator
        .zipWithIndex
        .map { case (mitem, index) =>
          MItemInfo(
            index = index,
            iType = mitem.iType,
            price = TplDataFormatUtil.setPriceAmountStr( mitem.price ),
            rcvr = for (rcvrId <- mitem.rcvrIdOpt) yield {
              MNameId(
                id = mitem.rcvrIdOpt,
                name = abc.rcvrsMap
                  .get(rcvrId)
                  .flatMap(_.guessDisplayNameOrId)
                  .getOrElse(rcvrId)
              )
            },
            gsInfo = mitem.geoShape
              .map( TplDataFormatUtil.formatGeoShape )
          )
        }
        .toSeq
        // Отсортировать, чтобы одни и теже элементы не плясали.
        .sortBy { ii =>
          ii.iType.strId + ii.rcvr.fold("")(rcvr => " " + rcvr.name)
        }

      // Собрать итоговый ответ с подробными ценами для формы.
      val resp = MGetPriceResp(
        prices = prices2.values,
        items  = infos
      )
      Future.successful(resp)
    }
  }


  /**
    * Получение детализованных данных по рассчёту стоимости.
    * Здесь есть доля ненужных действий, но это нормально.
    *
    * @param itemIndex порядковый номер item'а, для которого надо детализацию получить.
    * @param abc Контекст гео-биллинга.
    * @param ctx Контекст рендера
    * @return Фьючерс с детальными данными по рассчёту стоимости указанного item'а.
    */
  def getDetalizedPricing(itemIndex: Int, abc: MGeoAdvBillCtx)(implicit ctx: Context): Future[MDetailedPriceResp] = {
    val itemsCalc = new ItemsCalc(abc) {
      // Все эти обязательные значения не важны и будут проигнорены:
      override val _orderId     = -1L
      override val _itemStatus  = MItemStatuses.Draft


      // Listener для сбора промежуточных данных-рассчётов.
      // Используя промежуточные данные, он так же считает итоговые цены за каждый день.
      // Спроектирован исходя из того, что юзера будет интересовать только один конкретный item.
      class Listener extends IAdvPriceDaysCalcListener with ICalcExecListener {

        /** Счётчик пройденных item'ов. Используется для определения item'а, который интересует юзера. */
        var _currItemIndex = 0

        /** Кол-во item'ов на текущем шаге.
          * Инициализируется через willCalcDaysAdvPriceForNItems() перед каждым вызовом. */
        var _itemsIndexNextStep = 0

        /** Является ли обрабатываемый item релевантным запросу юзера? */
        def _isCollectingData: Boolean = {
          itemIndex == _currItemIndex || {
            // При пачке тегов возможна ситуация, когда "текущий" item будет перепрыгнут. Решаем проблему диапазоном текущих индексов:
            (_currItemIndex < itemIndex) && (itemIndex + _itemsIndexNextStep) > itemIndex
          }
        }

        /** Инициализировать items step. */
        override def willCalcDaysAdvPriceForNItems(itemsCount: Int): Unit = {
          _itemsIndexNextStep = itemsCount
        }

        var _tfCurrency: Option[MCurrency] = None
        // Поля MDetailedPriceResp
        var _daysAccRev         : List[MDayPriceInfo] = Nil
        var _onMainScreenMult   : Option[Double] = None
        var _geoInfo            : Option[MGeoInfo] = None

        def _setDaysAccRevMult(mult: Double): Unit = {
          _daysAccRev = for (d <- _daysAccRev) yield {
            d.withPrice(
              TplDataFormatUtil.setPriceAmountStr(
                d.baseDayPrice
                  .multiplifiedBy(mult * abc.blockModulesCount)
                  .normalizeAmountByExponent
              )
            )
          }
        }

        /** Сдвинуть счётчик текущего item'а на новое положение. Обнулить step. */
        def _itemsProcessed(): Unit = {
          if (_itemsIndexNextStep < 1)
            throw new IllegalStateException("willCalcDaysAdvPriceForNItems() must be called before _itemsProcessed()")
          _currItemIndex += _itemsIndexNextStep
          _itemsIndexNextStep = 0
        }

        override def handleTfDaily(tf: MTfDaily): Unit = {
          if (_isCollectingData)
            _tfCurrency = Some( tf.currency )
        }

        override def handleOneDayData(day: LocalDate, dow17: Int, mcalTypeOpt: Option[MCalType], mdc: MDayClause, dayAmount: Amount_t): Unit = {
          if (_isCollectingData) {
            val basePrice = TplDataFormatUtil.setPriceAmountStr(
              MPrice(dayAmount, _tfCurrency.get)
            )
            _daysAccRev ::= MDayPriceInfo(
              ymd           = MYmd.from(day),
              calType       = mcalTypeOpt.getOrElse(MCalTypes.WeekDay),
              baseDayPrice  = basePrice,
              price         = null // Нельзя определить финальную цену за день без инфы по мультипликатору.
            )
          }
        }


        override def handleGeoOms(radiusMOpt: Option[Int], allDaysPriceBase: MPrice, omsMult: Amount_t, geoMult: Amount_t, geoPrice: MPrice): Unit = {
          if (_isCollectingData) {
            _geoInfo = Some(MGeoInfo(
              radiusM   = radiusMOpt,
              priceMult = geoMult
            ))
            _onMainScreenMult = Some(omsMult)
            _setDaysAccRevMult( omsMult * geoMult )
          }
          _itemsProcessed()
        }

        override def handleGeoTags(radiusMOpt: Option[Int], allDaysPriceBase: MPrice, geoMult: Amount_t, oneTagPrice: MPrice, tagsCount: Int): Unit = {
          if (_isCollectingData) {
            _geoInfo = Some(MGeoInfo(
              radiusM   = radiusMOpt,
              priceMult = geoMult
            ))
            _setDaysAccRevMult( geoMult )
          }
          _itemsProcessed()
        }

        override def handleRcvrOms(rcvrKey: RcvrKey, rcvrId: String, allDaysPriceBase: MPrice, omsMult: Amount_t, totalPrice: MPrice): Unit = {
          if (_isCollectingData) {
            _onMainScreenMult = Some( omsMult )
            _setDaysAccRevMult( omsMult )
          }
          _itemsProcessed()
        }

        override def handleRcvrTags(rcvrId: String, priceMult: Double, oneTagPrice: MPrice, tagsCount: Int): Unit = {
          if (_isCollectingData) {
            _setDaysAccRevMult( priceMult )
          }
          _itemsProcessed()
        }

        def getDetailedResult: MDetailedPriceResp = {
          MDetailedPriceResp(
            blockModulesCount   = abc.blockModulesCount,
            onMainScreenMult    = _onMainScreenMult,
            geoInfo             = _geoInfo,
            days                = _daysAccRev.reverse
          )
        }

      }

      val LISTENER = new Listener
      val someListener = Some( LISTENER )

      override def _advPriceListener = someListener
      override def _executionListener = someListener

    }

    itemsCalc.execute()

    val resp = itemsCalc.LISTENER.getDetailedResult
    Future.successful(resp)
  }


  /**
    * Вычислитель результата абстрактной биллинговой операции в контексте формы георазмещения.
    * Не секрет, что getPrice и submit имеют общую для обоих фазу рассчёта стоимости размещения.
    * Однако, результат рассчёта в одном случае цена, в другом -- items'ы в базе.
    * Этот класс содержит общую логику рассчёта стоимости в отвязке от типа конкретного результата.
    *
    * @tparam T Тип накапливаемых результатов.
    * @param abc Контекст гео-биллинга.
    */
  private abstract class Calc[T]( val abc: MGeoAdvBillCtx ) {

    import abc.res

    val tagsCount = res.tagsEdit.tagsExists.size

    // Бывает, что нужно передавать в geo-методы круг в модифицированном виде.
    type CircleGs_t
    def mkCircleGs(circle: MGeoCircle): CircleGs_t

    var _acc: List[T] = Nil

    /** Префикс для сообщений логгера, вызывается лишь один раз для сборки полного log-префикса. */
    def logPrefixPrefix: String
    lazy val logPrefix = s"$logPrefixPrefix#Calc[${System.currentTimeMillis()}]:"

    /** Сборка item'а для geo + onMainScreen. */
    def geoOms(circleGs: CircleGs_t, price: MPrice): T

    /** Добавить в _acc информацию по геотегам. */
    def addGeoTags(circleGs: CircleGs_t, oneTagPrice: MPrice): Unit

    /** Собрать item'а для размещения на одном ресивере на его главном экране. */
    def rcvrOms(rcvrId: String, price: MPrice): T

    /** Добавить в _acc данные по размещениям всех тегов на ресивере. */
    def addRcvrTags(rcvrId: String, oneTagPrice: MPrice): Unit


    /** listener для вызова advUtil.calculateAdvPriceOnRcvr(). */
    def _advPriceListener: Option[IAdvPriceDaysCalcListener] = None

    /** listener вызова execute. */
    def _executionListener: Option[ICalcExecListener] = None

    /** Укороченно дёргаем exec-listener. */
    @inline
    private def __listen[U](f: ICalcExecListener => U): Unit = {
      _executionListener.foreach(f)
    }

    /** Запуск высчитывания результата. */
    def execute(): List[T] = {
      LOGGER.trace(s"$logPrefix $res")

      // Посчитать стоимость размещения указанных элементов (oms, теги) в гео-круге.
      for {
        radCircle <- res.radCircle
        if res.onMainScreen || res.tagsEdit.tagsExists.nonEmpty
      } {
        // Посчитать стоимость данного гео-круга:
        val radiusKm = radCircle.radiusKm
        val someRadiusM = Some( radCircle.radiusM.toInt )

        val circleGeoMult = getPriceMult( radiusKm )

        val gs = mkCircleGs(radCircle)
        __listen { l =>
          l.willCalcDaysAdvPriceForNItems(
            tagsCount + (if (res.onMainScreen) 1 else 0)
          )
        }
        val geoAllDaysPrice = advUtil.calculateAdvPriceOnRcvr(GEO_TF_SRC_NODE_ID, abc, _advPriceListener)

        // Накинуть за гео-круг + главный экран:
        if (res.onMainScreen) {
          val omsMult = ON_MAIN_SCREEN_MULT
          val geoOmsMult = circleGeoMult * omsMult
          val geoOmsPrice = geoAllDaysPrice.multiplifiedBy(geoOmsMult)
            .normalizeAmountByExponent
          __listen( _.handleGeoOms(someRadiusM, geoAllDaysPrice, geoOmsMult, circleGeoMult, geoOmsPrice) )
          val geoOmsRes = geoOms(gs, geoOmsPrice)
          LOGGER.trace(s"$logPrefix geo + onMainScreen => multAcc ::= $circleGeoMult * $ON_MAIN_SCREEN_MULT = $geoOmsMult => $geoAllDaysPrice * $geoOmsPrice => $geoOmsRes")
          _acc ::= geoOmsRes
        }

        // Накинуть за гео-круг + теги
        if (tagsCount > 0) {
          val oneTagPrice = geoAllDaysPrice.multiplifiedBy( circleGeoMult )
            .normalizeAmountByExponent
          __listen( _.handleGeoTags(someRadiusM, geoAllDaysPrice, circleGeoMult, oneTagPrice, tagsCount) )
          LOGGER.trace(s"$logPrefix geo + $tagsCount tags, geo=$circleGeoMult * $geoAllDaysPrice = $oneTagPrice per each tag" )
          addGeoTags(gs, oneTagPrice)
        }

      }

      // Отработать ресиверы, если заданы.
      if (res.rcvrsMap.nonEmpty) {
        // Отработать прямые размещения на ресиверах
        for {
          (rcvrKey, rcvrProps) <- res.rcvrsMap
          // TODO Отработать rcvrProps. Возможно, отмена размещения вместо создания.
        } {
          __listen( _.willCalcDaysAdvPriceForNItems(1) )
          // Накинуть за ресивер (главный экран ресивера)
          val rcvrId = rcvrKey.last
          val rcvrPrice = advUtil.calculateAdvPriceOnRcvr(rcvrId, abc, _advPriceListener)

          val omsMult = ON_MAIN_SCREEN_MULT
          val rcvrPriceOms = rcvrPrice.multiplifiedBy( omsMult )
            .normalizeAmountByExponent
          __listen( _.handleRcvrOms(rcvrKey, rcvrId, rcvrPrice, omsMult, rcvrPriceOms) )
          val rcvrOmsRes = rcvrOms(rcvrId, rcvrPriceOms)
          LOGGER.trace(s"$logPrefix Rcvr ${rcvrKey.mkString(".")}: price $rcvrPrice, oms => $rcvrPriceOms,\n $rcvrOmsRes")
          _acc ::= rcvrOmsRes
        }

        // Отработать теги на ресиверах: теги размещаются только на верхних узлах.
        if (tagsCount > 0) {
          val topRcvrIds = res.rcvrsMap
            // TODO Отработать rcvrProps. Возможна отмена размещения вместо создания.
            .keysIterator
            .flatMap(_.headOption)
            .toSet
          for (rcvrId <- topRcvrIds) {
            __listen( _.willCalcDaysAdvPriceForNItems(tagsCount) )
            val rcvrTagPrice = advUtil.calculateAdvPriceOnRcvr(rcvrId, abc, _advPriceListener)
              .normalizeAmountByExponent
            val priceMult = 1.0
            __listen( _.handleRcvrTags(rcvrId, priceMult, rcvrTagPrice, tagsCount) )
            LOGGER.trace(s"$logPrefix Top-rcvr $rcvrId + $tagsCount tags, $rcvrTagPrice per tag")
            addRcvrTags(rcvrId, rcvrTagPrice)
          }
        }
      }

      __listen( _.handleAllDone() )

      // И вернуть наверх финальный аккамулятор.
      _acc
    }

  }


  /** Листенер промежуточных шагов и данных в Calc. */
  private trait ICalcExecListener {
    /** Listener предупреждается, что сейчас будет n item'ов с одинаковой базовой стоимостью. */
    def willCalcDaysAdvPriceForNItems(itemsCount: Int): Unit = {}
    def handleGeoOms(radiusMOpt: Option[Int], allDaysPriceBase: MPrice, omsMult: Double, geoMult: Double, geoPrice: MPrice): Unit = {}
    def handleGeoTags(radiusMOpt: Option[Int], allDaysPriceBase: MPrice, geoMult: Double, oneTagPrice: MPrice, tagsCount: Int): Unit = {}

    def handleRcvrOms(rcvrKey: RcvrKey, rcvrId: String, allDaysPriceBase: MPrice, omsMult: Double, totalPrice: MPrice): Unit = {}
    def handleRcvrTags(rcvrId: String, priceMult: Double, oneTagPrice: MPrice, tagsCount: Int): Unit = {}

    def handleAllDone(): Unit = {}
  }


  /** Items-генератор на основе калькулятора. */
  private abstract class ItemsCalc(abc: MGeoAdvBillCtx) extends Calc[MItem](abc) {

    val _orderId: Gid_t

    // Собираем экшен заливки item'ов. Один тег -- один item. А цена у всех одна.
    val _ymdPeriod = abc.res.datePeriod.info
    val _dateStart = _ymdPeriod.dateStart[LocalDate]
    val _dateEnd   = _ymdPeriod.dateEnd[LocalDate]

    // Инновация: берём временную зону прямо из браузера!
    val _tzOffset = DateTimeUtil.minutesOffset2TzOff( abc.res.tzOffsetMinutes )

    private def __dt(localDate: LocalDate): Option[OffsetDateTime] = {
      Some( localDate.atStartOfDay().atOffset(_tzOffset) )
    }

    val _dtStartOpt  = __dt( _dateStart )
    val _dtEndOpt    = __dt( _dateEnd )

    val _itemStatus: MItemStatus
    val _adId = abc.adId

    override def logPrefixPrefix: String = s"addToOrder(ord=${_orderId},ad=${_adId},st=${_itemStatus})"

    override type CircleGs_t = Option[CircleGs]
    override def mkCircleGs(circle: MGeoCircle): CircleGs_t = {
      Some( CircleGs( circle ) )
    }

    /** Сборка item'а для geo + onMainScreen. */
    override def geoOms(circleGs: CircleGs_t, price: MPrice): MItem = {
      MItem(
        orderId       = _orderId,
        iType         = MItemTypes.GeoPlace,
        status        = _itemStatus,
        price         = price,
        nodeId        = _adId,
        dateStartOpt  = _dtStartOpt,
        dateEndOpt    = _dtEndOpt,
        rcvrIdOpt     = None,
        geoShape      = circleGs
      )
    }

    /** Добавить в _acc информацию по геотегам. */
    override def addGeoTags(circleGs: CircleGs_t, oneTagPrice: MPrice): Unit = {
      for (tagFace <- abc.res.tagsEdit.tagsExists) {
        _acc ::= MItem(
          orderId       = _orderId,
          iType         = MItemTypes.GeoTag,
          status        = _itemStatus,
          price         = oneTagPrice,
          nodeId        = _adId,
          dateStartOpt  = _dtStartOpt,
          dateEndOpt    = _dtEndOpt,
          // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
          rcvrIdOpt     = None,
          tagFaceOpt    = Some(tagFace),
          geoShape      = circleGs
        )
      }
    }

    /** Собрать item'а для размещения на одном ресивере на его главном экране. */
    override def rcvrOms(rcvrId: String, price: MPrice): MItem = {
      MItem(
        orderId       = _orderId,
        iType         = MItemTypes.AdvDirect,
        status        = _itemStatus,
        price         = price,
        nodeId        = _adId,
        dateStartOpt  = _dtStartOpt,
        dateEndOpt    = _dtEndOpt,
        rcvrIdOpt     = Some(rcvrId),
        geoShape      = None
      )
    }

    /** Добавить в _acc данные по размещениям всех тегов на ресивере. */
    override def addRcvrTags(rcvrId: String, oneTagPrice: MPrice): Unit = {
      for (tagFace <- abc.res.tagsEdit.tagsExists) {
        _acc ::= MItem(
          orderId       = _orderId,
          iType         = MItemTypes.TagDirect,
          status        = _itemStatus,
          price         = oneTagPrice,
          nodeId        = _adId,
          dateStartOpt  = _dtStartOpt,
          dateEndOpt    = _dtEndOpt,
          // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
          rcvrIdOpt     = None,
          tagFaceOpt    = Some(tagFace),
          geoShape      = None
        )
      }
    }

  }


  /** Базовый множитель цены для размещения на главном экране (ресивера, карты). */
  private def ON_MAIN_SCREEN_MULT = 3.0


  def freeAdvNodeIds(personIdOpt: Option[String], producerIdOpt: Option[String]): Future[Set[String]] = {
    lazy val logPrefix = s"_freeAdvNodeIds(u=${personIdOpt.orNull},prod=${producerIdOpt.orNull}):"
    personIdOpt.fold {
      LOGGER.warn(s"$logPrefix called on unauthorized user")
      Future.successful( Set.empty[String] )

    } { personId =>
      for {
        ownedNodeIdsSeq <- mNodes.dynSearchIds {
          nodesUtil.personNodesSearch(personId)
        }
      } yield {
        val b = Set.newBuilder[String]

        b ++= ownedNodeIdsSeq
        b ++= personIdOpt
        b += personId

        val res = b.result()
        LOGGER.trace(s"$logPrefix => ${res.mkString(", ")}")
        res
      }
    }
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


  /** Собрать минимальную и достаточную геоинфу для рендера разноцветных кружочков на карте размещений.
    *
    * @param query Исходный запрос item'ов. Например, выхлоп от findCurrentForAdQ().
    *
    * @return Пачка из Option'ов, т.к. все затрагиваемые столбцы базы заявлены как NULLable,
    *         и slick не может это проигнорить:
    *         (geo_shape, id, isAwaitingMdr).
    */
  def onlyGeoShapesInfo(query: Query[mItems.MItemsTable, MItem, Seq], limit: Int = 500): DBIOAction[Seq[AdvGeoShapeInfo_t], Streaming[AdvGeoShapeInfo_t], Effect.Read] = {
    query
      // WHERE не пустой geo_shape
      .filter(_.geoShapeStrOpt.isDefined)
      // GROUP BY geo_shape
      .groupBy(_.geoShapeStrOpt)
      .map { case (geoShapeStrOpt, group) =>
        // Делаем правильный кортеж: ключ -- строка шейпа, id - любой, status -- только максимальный
        (geoShapeStrOpt,
          group.map(_.id).max,
          group.map(_.statusStr).max =!= MItemStatuses.AwaitingMdr.strId
          )
      }
      // LIMIT 200
      .take(limit)
      .result
    // TODO Нужно завернуть кортежи в MAdvGeoShapeInfo. .map() не котируем, т.к. ломает streaming.
  }


  /**
    * Найти item'ы с таким же гео-шейпом, как у указанного item'а.
    * @param query Исходный запрос item'ов. Например, выхлоп от findCurrentForAdQ().
    * @param itemId id item'а, содержащего необходимый шейп.
    * @param limit Макс.кол-во результатов.
    * @return Streamable-результаты.
    */
  def withSameGeoShapeAs(query: Query[mItems.MItemsTable, MItem, Seq], itemId: Gid_t, limit: Int = 500)
  : DBIOAction[Seq[AdvGeoBasicInfo_t], Streaming[AdvGeoBasicInfo_t], Effect.Read] = {
    query
      .filter { i =>
        val itemShapeQ = mItems.query
          .filter(_.id === itemId)
          .map(_.geoShapeStrOpt)
          .filter(_.isDefined)
        i.geoShapeStrOpt in itemShapeQ
      }
      .map { i =>
        (i.id, i.iType, i.status, i.dateStartOpt, i.dateEndOpt, i.tagFaceOpt)
      }
      .take(limit)
      // Без сортировки, т.к. будет последующая группировка на стороне клиента.
      .result
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

}
