package util.adv.geo

import java.time.{LocalDate, OffsetDateTime}

import com.google.inject.Inject
import io.suggest.adv.geo.MFormS
import io.suggest.bill.{MGetPriceResp, MItemInfo, MNameId, MPrice}
import io.suggest.dt.YmdHelpersJvm
import io.suggest.geo.{CircleGs, MGeoCircle}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
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
import util.adv.AdvUtil
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
    * @param circle Гео-круг, заданный юзером в форме георазмещения.
    * @return Double-мультипликатор цены.
    */
  def getPriceMult(circle: MGeoCircle): Double = {
    // Привести радиус на карте к множителю цены
    val radKm = circle.radiusM / 1000d   // метры -> км
    radKm * radKm / 1.5
  }


  /** Сборка контекста для direct-биллинга поверх географии.
    *
    * @param mad рекламная карточка.
    * @param res Содержимое формы.
    * @param request реквест. Биллинг зависит от юзера и продьсера, которые лежат в реквесте.
    * @return Фьючерс с контекстом биллинга.
    */
  def advBillCtx(isSuFree: Boolean, mad: MNode, res: MFormS)(implicit request: IAdProdReq[_]): Future[MGeoAdvBillCtx] = {
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

        abc <- advUtil.rcvrBillCtx(mad, rcvrIdsSet, ivl)
      } yield {
        abc
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
        .map { i =>
          MItemInfo(
            iType = i.iType,
            price = TplDataFormatUtil.setPriceAmountStr( i.price ),
            rcvr = for (rcvrId <- i.rcvrIdOpt) yield {
              MNameId(
                id = i.rcvrIdOpt,
                name = abc.rcvrsMap
                  .get(rcvrId)
                  .flatMap(_.guessDisplayNameOrId)
                  .getOrElse(rcvrId)
              )
            },
            gsInfo = i.geoShape
              .map(TplDataFormatUtil.formatGeoShape)
          )
        }
        // Отсортировать, чтобы одни и теже элементы не плясали.
        .sortBy { ii =>
          ii.iType.strId + " " + ii.rcvr.map(_.name).getOrElse("")
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


    /** Запуск этого калькулятора результата. */
    def execute(): List[T] = {
      LOGGER.trace(s"$logPrefix $res")

      // Посчитать стоимость размещения указанных элементов (oms, теги) в гео-круге.
      for {
        radCircle <- res.radCircle
        if res.onMainScreen || res.tagsEdit.tagsExists.nonEmpty
      } {
        // Посчитать стоимость данного гео-круга:
        val circleGeoMult = getPriceMult(radCircle)
        val gs = mkCircleGs(radCircle)
        val geoAllDaysPrice = advUtil.calculateAdvPriceOnRcvr(GEO_TF_SRC_NODE_ID, abc)

        // Накинуть за гео-круг + главный экран:
        if (res.onMainScreen) {
          val omsMult = circleGeoMult * ON_MAIN_SCREEN_MULT
          val priceOms = geoAllDaysPrice.multiplifiedBy(omsMult)
            .normalizeAmountByExponent
          val geoOmsRes = geoOms(gs, priceOms)
          LOGGER.trace(s"$logPrefix geo + onMainScreen => multAcc ::= $circleGeoMult * $ON_MAIN_SCREEN_MULT = $omsMult => $geoAllDaysPrice * $priceOms => $geoOmsRes")
          _acc ::= geoOmsRes
        }

        // Накинуть за гео-круг + теги
        if (tagsCount > 0) {
          val oneTagPrice = geoAllDaysPrice.multiplifiedBy( circleGeoMult )
            .normalizeAmountByExponent
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
          // Накинуть за ресивер (главный экран ресивера)
          val rcvrId = rcvrKey.last
          val rcvrPrice = advUtil.calculateAdvPriceOnRcvr(rcvrId, abc)
          val rcvrPriceOms = rcvrPrice.multiplifiedBy( ON_MAIN_SCREEN_MULT )
            .normalizeAmountByExponent
          val rcvrOmsRes = rcvrOms(rcvrId, rcvrPriceOms)
          LOGGER.trace(s"$logPrefix Rcvr ${rcvrKey.mkString(".")}: price $rcvrPrice, oms => $rcvrPriceOms,\n $rcvrOmsRes")
          _acc ::= rcvrOmsRes
        }

        // Отработать теги на ресиверах: теги размещаются только на верхних узлах.
        val topRcvrIds = res.rcvrsMap
          // TODO Отработать rcvrProps. Возможна отмена размещения вместо создания.
          .keysIterator
          .flatMap(_.headOption)
          .toSet
        for (rcvrId <- topRcvrIds) {
          val rcvrTagPrice = advUtil.calculateAdvPriceOnRcvr(rcvrId, abc)
            .normalizeAmountByExponent
          LOGGER.trace(s"$logPrefix Top-rcvr $rcvrId + $tagsCount tags, $rcvrTagPrice per tag")
          addRcvrTags(rcvrId, rcvrTagPrice)
        }
      }

      // И вернуть наверх финальный аккамулятор.
      _acc
    }

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
