package util.adv.geo

import java.time.{LocalDate, OffsetDateTime}

import com.google.inject.Inject
import io.suggest.adv.geo.MFormS
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.bill._
import io.suggest.bill.price.dsl._
import io.suggest.dt.YmdHelpersJvm
import io.suggest.geo.CircleGs
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
    * @param radiusKm Радиус гео-круга.
    * @return Double-мультипликатор цены.
    */
  private def getPriceMult(radiusKm: Double): Double = {
    // Привести радиус на карте к множителю цены
    Math.max(0.001, radiusKm * radiusKm / 1.5)
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
  def addToOrder(adId: String, orderId: Gid_t, status: MItemStatus, abc: MGeoAdvBillCtx): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    lazy val logPrefix = s"addToOrder($orderId)[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix status=$status, ${abc.rcvrsMap.size} rcvrs, ${abc.mcalsCtx.calsMap.size} calendars")

    // Собираем экшен заливки item'ов. Один тег -- один item. А цена у всех одна.
    val ymdPeriod = abc.res.datePeriod.info
    val dateStart = ymdPeriod.dateStart[LocalDate]
    val dateEnd   = ymdPeriod.dateEnd[LocalDate]

    // Инновация: берём временную зону прямо из браузера!
    val tzOffset = DateTimeUtil.minutesOffset2TzOff( abc.res.tzOffsetMinutes )

    def __dt(localDate: LocalDate): Option[OffsetDateTime] = {
      Some( localDate.atStartOfDay().atOffset(tzOffset) )
    }

    val dtStartOpt  = __dt( dateStart )
    val dtEndOpt    = __dt( dateEnd )

    LOGGER.trace(s"$logPrefix period=$ymdPeriod tzOff=$tzOffset => ${dtStartOpt.orNull}..${dtEndOpt.orNull}")

    val itemActsIter = calcAdvGeoPrice(abc)
      .splitOnSumUntil {
        // Маппер содержит причину трансформации. Резать можно только до уровня item'а.
        case m: Mapper =>
          m.reason.exists { r =>
            // Если достигаем itemType, то дальше дробить уже нельзя.
            !r.reasonType.isItemLevel
          }
        // Остальные элементы -- просто дробить по сумматорам.
        case _ => true
      }
      .toIterator
      .flatMap { term =>
        val term2 = advUtil.prepareForSave(term)
        lazy val logPrefix2 = s"$logPrefix (${term2.getClass.getSimpleName}#${term2.hashCode()}) "
        LOGGER.trace(s"$logPrefix2 term = $term2")
        term2
          .findWithReasonType( MReasonTypes.GeoArea )
          .flatMap { geoSubTerm =>
            val gsOpt = abc.res
              .radCircle
              .map { CircleGs.apply }

            LOGGER.trace(s"$logPrefix2 It is Geo term, circle = ${abc.res.radCircle.orNull}, gs => ${gsOpt.orNull}")
            geoSubTerm
              // Проверить на geo + тег:
              .findWithReasonType( MReasonTypes.Tag )
              .map { tagSubTerm =>
                val tagFace = tagSubTerm.asInstanceOf[Mapper].reason.get.strings.head
                LOGGER.trace(s"$logPrefix2 It is a GeoTag: #$tagFace")
                // Это размещение в гео-теге.
                MItem(
                  orderId       = orderId,
                  iType         = MItemTypes.GeoTag,
                  status        = status,
                  price         = term2.price,
                  nodeId        = adId,
                  dateStartOpt  = dtStartOpt,
                  dateEndOpt    = dtEndOpt,
                  // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
                  rcvrIdOpt     = None,
                  tagFaceOpt    = Some( tagFace ),
                  geoShape      = gsOpt
                )
              }
              // Проверить на geo + onMainScreen
              .orElse {
                geoSubTerm
                  .findWithReasonType(MReasonTypes.OnMainScreen)
                  .map { _ =>
                    LOGGER.trace(s"$logPrefix2 It is Geo+OnMainScreen")
                    // Это onMainScreen-гео-размещение.
                    MItem(
                      orderId       = orderId,
                      iType         = MItemTypes.GeoPlace,
                      status        = status,
                      price         = term2.price,
                      nodeId        = adId,
                      dateStartOpt  = dtStartOpt,
                      dateEndOpt    = dtEndOpt,
                      rcvrIdOpt     = None,
                      geoShape      = gsOpt
                    )
                  }
              }
          }
          // Проверить на rcvr-размещение.
          .orElse {
            term2
              .findWithReasonType( MReasonTypes.Rcvr )
              .flatMap { rcvrSubTerm =>
                // Это прямое размещение на каком-то ресивере. У него обязан быть выставленный id.
                val rcvrId = rcvrSubTerm.asInstanceOf[Mapper].reason.get.nameIds.head.id.get
                LOGGER.trace(s"$logPrefix2 It is Rcvr term on rcvrId=$rcvrId")

                // Это может быть главный экран или тег.
                rcvrSubTerm
                  .findWithReasonType( MReasonTypes.Tag )
                  .map { tagSubTerm =>
                    val tagFace = tagSubTerm.asInstanceOf[Mapper].reason.get.strings.head
                    LOGGER.trace(s"$logPrefix2 It is direct tag #$tagFace on Rcvr#$rcvrId")
                    // Это размещение в теге на ресивере.
                    MItem(
                      orderId       = orderId,
                      iType         = MItemTypes.TagDirect,
                      status        = status,
                      price         = term2.price,
                      nodeId        = adId,
                      dateStartOpt  = dtStartOpt,
                      dateEndOpt    = dtEndOpt,
                      // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
                      rcvrIdOpt     = Some(rcvrId),
                      tagFaceOpt    = Some( tagFace ),
                      geoShape      = None
                    )
                  }
                  // Проверить на rcvr + OMS
                  .orElse {
                    rcvrSubTerm
                      .findWithReasonType( MReasonTypes.OnMainScreen )
                      .map { _ =>
                        LOGGER.trace(s"$logPrefix2 It is OnMainScreen on rcvr#$rcvrId")
                        // Это размещение на главном экране ресивера.
                        MItem(
                          orderId       = orderId,
                          iType         = MItemTypes.AdvDirect,
                          status        = status,
                          price         = term2.price,
                          nodeId        = adId,
                          dateStartOpt  = dtStartOpt,
                          dateEndOpt    = dtEndOpt,
                          rcvrIdOpt     = Some(rcvrId),
                          geoShape      = None
                        )
                      }
                  }
              }
          }
          .orElse {
            // Какая-то логическая ошибка в коде: этот метод не понимает выхлоп из calcAdvGeoPrice().
            throw new UnsupportedOperationException(s"Not supported price term: $term2 Please check current billing class code.")
            // Давим ошибку. Лишнего с юзера не спишется, а оплата в целом пройдёт.
            //None
          }
      }
      .map { mItems.insertOne }
      .toSeq

    DBIO.sequence(itemActsIter)
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
    // Сборка считалки цены:
    val priceDsl0 = calcAdvGeoPrice(abc)

    val gpResp = if (priceDsl0.isEmpty) {
      bill2Util.zeroPricing

    } else {
      val priceDsl2 = advUtil.prepareForRender(priceDsl0)

      // Собрать итоговый ответ с подробными ценами для формы.
      val resp = MGetPriceResp(
        prices   = priceDsl2.price :: Nil,
        priceDsl = Some(priceDsl2)
      )

      resp
    }

    Future.successful( gpResp )
  }


  /** Сборка PriceDSL, на основе которой можно вычислить результат. */
  def calcAdvGeoPrice(abc: MGeoAdvBillCtx): Sum = {
    import abc.res

    lazy val logPrefix = s"calcAdvGeoPrice()[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix $res")

    var accRev: List[IPriceDslTerm] = Nil

    val tagsWithReasons = res.tagsEdit.tagsExists
      .toSeq
      .sorted
      .map { tagFace =>
        tagFace -> Some( MPriceReason(MReasonTypes.Tag, strings = tagFace :: Nil) )
      }

    // Посчитать стоимость размещения указанных элементов (oms, теги) в гео-круге.
    for {
      radCircle <- res.radCircle
      if res.onMainScreen || res.tagsEdit.tagsExists.nonEmpty
    } {
      // Посчитать стоимость данного гео-круга:
      val radiusKm = radCircle.radiusKm
      var accGeoRev: List[IPriceDslTerm] = Nil

      val allDaysPrice = advUtil.calcDateAdvPriceOnTf(GEO_TF_SRC_NODE_ID, abc)

      // Накинуть за гео-круг + главный экран:
      if (res.onMainScreen) {
        accGeoRev ::= Mapper(
          underlying    = allDaysPrice,
          multiplifier  = Some( ON_MAIN_SCREEN_MULT ),
          reason        = Some( MPriceReason( MReasonTypes.OnMainScreen ) )
        )
      }

      // Накинуть за гео-круг + теги
      for {
        (_, reasonOpt) <- tagsWithReasons
      } {
        accGeoRev ::= Mapper(
          underlying = allDaysPrice,
          reason     = reasonOpt
        )
      }

      val geoAllDaysPrice = Mapper(
        underlying    = Sum( accGeoRev.reverse ),
        multiplifier  = Some( getPriceMult(radiusKm) ),
        reason        = Some( MPriceReason(
          MReasonTypes.GeoArea,
          geoCircles  = radCircle :: Nil
        ) )
      )

      // Закинуть гео-итог в общий акк.
      accRev ::= geoAllDaysPrice
    }   // END geo

    // Сборка причины наценки за ресивер.
    def __rcvrPriceReason(rcvrId: String): Option[MPriceReason] = {
      // TODO Opt Инстансы причин наценки на ресивер могут повторяться на последующих шагах, желательно возвращать одни и те же инстансы.
      Some(
        MPriceReason(
          MReasonTypes.Rcvr,
          nameIds = MNameId(
            id   = Some(rcvrId),
            // Определить какое-нибудь название ресивера. Ресивера может не быть в abc.rcvrsMap при isSu==true или какой-то проблеме.
            name = abc.rcvrsMap
              .get(rcvrId)
              .fold("")(_.guessDisplayNameOrIdOrQuestions)
          ) :: Nil
        )
      )
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
        val rcvrPrice = advUtil.calcDateAdvPriceOnTf(rcvrId, abc)

        val omsMultOpt: Option[Double] = if (rcvrKey.tail.nonEmpty) {
          // Для суб-ресиверов: без доп.наценок за главный экран.
          None
        } else {
          // Домножаем за главный экран только для top-ресиверов.
          Some(ON_MAIN_SCREEN_MULT)
        }
        // Маппер OMS нужен ВСЕГДА, иначе addToOrder() не поймёт, что от него хотят.
        val rcvrOmsPrice = Mapper(
          underlying    = rcvrPrice,
          multiplifier  = omsMultOpt,
          reason        = Some( MPriceReason( MReasonTypes.OnMainScreen ) )
        )

        // Закинуть в аккамулятор результатов.
        accRev ::= Mapper(
          underlying = rcvrOmsPrice,
          reason     = __rcvrPriceReason(rcvrId)
        )
      }

      // Отработать теги на ресиверах: теги размещаются только на верхних узлах.
      if (res.tagsEdit.tagsExists.nonEmpty) {
        val topRcvrIds = res.rcvrsMap
          // TODO Отработать rcvrProps. Возможна отмена размещения вместо создания.
          .keysIterator
          .flatMap(_.headOption)
          .toSet

        for (rcvrId <- topRcvrIds) {
          val rcvrTagPrice = advUtil.calcDateAdvPriceOnTf(rcvrId, abc)

          // Вычислить все цены на тегах.
          val tagsOnRcvrPrices = for {
            (_, reasonOpt) <- tagsWithReasons
          } yield {
            Mapper(
              underlying  = rcvrTagPrice,
              reason      = reasonOpt
            )
          }

          accRev ::= Mapper(
            underlying  = Sum(tagsOnRcvrPrices),
            reason      = __rcvrPriceReason(rcvrId)
          )
        }
      }
    }

    Sum( accRev.reverse )
    // Для рендера юзеру: надо не забыть .mapAllPrices( .normalizeByExponent + TplDataFormatUtil.setPriceAmountStr )
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
