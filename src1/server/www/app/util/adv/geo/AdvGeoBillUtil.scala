package util.adv.geo

import akka.stream.scaladsl.Source
import com.google.inject.Inject
import io.suggest.adv.geo.MFormS
import io.suggest.bill.{MGetPriceResp, MPrice}
import io.suggest.dt.YmdHelpersJvm
import io.suggest.geo.MGeoCircle
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.{MItemStatus, MItemStatuses}
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.geo.CircleGs
import models.adv.geo.cur.{AdvGeoBasicInfo_t, AdvGeoShapeInfo_t}
import models.mproj.ICommonDi
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import util.billing.Bill2Util
import util.ble.BeaconsBilling

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.12.15 13:43
  * Description: Утиль для биллинга размещений прямо на гео-карте.
  *
  *
  */
class AdvGeoBillUtil @Inject() (
  bill2Util                           : Bill2Util,
  beaconsBilling                      : BeaconsBilling,
  ymdHelpersJvm                       : YmdHelpersJvm,
  protected val mItems                : MItems,
  protected val mCommonDi             : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._
  import slick.profile.api._
  import ymdHelpersJvm.Implicits._

  private def _oneTag1dayPrice: MPrice = {
    bill2Util.zeroPrice.withAmount(1.0)
  }

  private def _oneTagPrice(geoMult: Double): MPrice = {
    val oneTag1dPrice = _oneTag1dayPrice
    oneTag1dPrice.withAmount(
      oneTag1dPrice.amount * geoMult
    )
  }

  /**
    * Посчитать мультипликатор стоимости на основе даты и радиуса размещения.
    *
    * @param circle Гео-круг, заданный юзером в форме георазмещения.
    * @return Double-мультипликатор цены.
    */
  def getPriceMult(circle: MGeoCircle): Double = {
    // Привести радиус на карте к множителю цены
    val radKm = circle.radiusM / 1000   // метры -> км
    radKm * radKm / 1.5
  }

  /**
    * Закинуть в корзину bill-v2.
    *
    * @param orderId id-ордера-корзины, т.е. текущего заказа. Туда надо добавить возможную покупку.
    *                Например, выхлоп [[util.billing.Bill2Util.ensureCartOrder()]].
    * @param adId    id узла-цели размещения тегов, обычно рекламная карточка.
    * @param res     Данные по размещаемым тегам.
    * @return Фьючерс c результатом.
    */
  def addToOrder(orderId: Gid_t, producerId: String, adId: String, res: MFormS, status: MItemStatus): DBIOAction[Seq[MItem], NoStream, Effect.Write] = {
    // Собираем экшен заливки item'ов. Один тег -- один item. А цена у всех одна.
    val ymdPeriod = res.datePeriod.info
    val dtStartOpt = Some( ymdPeriod.dateStart[DateTime] )
    val dtEndOpt   = Some( ymdPeriod.dateEnd[DateTime] )

    val daysCount = bill2Util.getDaysCount( res.datePeriod.info )

    val geoActsIter = res.radCircle
      .iterator
      .flatMap { radCircle =>
        val geoMult = getPriceMult(radCircle) * daysCount
        val p = _oneTagPrice(geoMult)
        val someGs = Some( CircleGs( radCircle ) )

        // Пройтись по тегам
        val mitemsTagActsIter = for {
          tagFace <- res.tagsEdit.tagsExists.iterator
        } yield {
          MItem(
            orderId       = orderId,
            iType         = MItemTypes.GeoTag,
            status        = status,
            price         = p,
            nodeId        = adId,
            dateStartOpt  = dtStartOpt,
            dateEndOpt    = dtEndOpt,
            // Было раньше tag.nodeId, но вроде от этого отказались: rcvrId вроде выставляется на этапе install().
            rcvrIdOpt     = None,
            tagFaceOpt    = Some(tagFace),
            geoShape      = someGs
          )
        }

        // Если галочка главного экрана выставлена, то ещё разместить и так просто, в месте на карте.
        val mitemsActs = if (res.onMainScreen) {
          val itmP = MItem(
            orderId       = orderId,
            iType         = MItemTypes.GeoPlace,
            status        = status,
            price         = getPricePlace(geoMult),
            nodeId        = adId,
            dateStartOpt  = dtStartOpt,
            dateEndOpt    = dtEndOpt,
            rcvrIdOpt     = None,
            geoShape      = someGs
          )
          itmP :: Nil
        } else {
          Nil
        }

        (mitemsTagActsIter ++ mitemsActs)
          .map { mItems.insertOne }
      }

    // TODO Нужно пройтись по карте ресиверов, накатить необходимые изменения.

    val itemActs = geoActsIter.toSeq

    DBIO.sequence(itemActs)
  }


  /**
    * Рассчет общей стоимости для результата маппинга формы.
    *
    * @param res Запрашиваемое юзером размещение.
    * @return
    */
  def getPricing(res: MFormS): Future[MGetPriceResp] = {
    val daysCount = bill2Util.getDaysCount( res.datePeriod.info )

    val geoPricesIter = res.radCircle
      .iterator
      .flatMap { rc =>
        val geoMult = getPriceMult(rc) * daysCount
        val p1 = _oneTagPrice(geoMult)

        // Посчитать цены размещения для каждого тега.
        var prices1 = List(
          p1.withAmount(
            p1.amount * res.tagsEdit.tagsExists.size
          )
        )

        if (res.onMainScreen)
          prices1 ::= getPricePlace(geoMult)

        LOGGER.trace(s"computePricing(): $res => $prices1")
        prices1
      }

    val prices1 = geoPricesIter.toSeq
    val prices2 = MPrice.sumPricesByCurrency(prices1).toSeq

    val result = bill2Util.getAdvPricing( prices2 )
    Future.successful(result)
  }


  def getPricing(res: MFormS, forceFree: Boolean): Future[MGetPriceResp] = {
    if (forceFree)
      bill2Util.zeroPricingFut
    else
      getPricing(res)
  }

  def getPricing(resOpt: Option[MFormS], forceFree: Boolean): Future[MGetPriceResp] = {
    resOpt.fold {
      bill2Util.zeroPricingFut
    } { res =>
      getPricing(res, forceFree)
    }
  }



  /** Базовый множитель цены. */
  def PLACE_PRICE_MULT = 3.0


  private def getPricePlace(geoMult: Double): MPrice = {
    bill2Util.zeroPrice.withAmount(
      geoMult * PLACE_PRICE_MULT
    )
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


  /** Поиск всех id'шников под-узлов для указанного узла-ресивера.
    * Это включает в себя поиск активированных BLE-маячков узла и возможно какие-то иные вещи.
    *
    * @param nodeId id узла-ресивера.
    * @return Фьючерс с множеством id'шников всех как-то подчиненных узлов.
    */
  def findActiveSubNodeIdsOfRcvr(nodeId: String): Future[Set[String]] = {
    // Запустить сбор маячков, активированных на узле.
    Source.fromPublisher {
      slick.db.stream {
        beaconsBilling.findActiveBeaconIdsOfRcvr(nodeId)
      }
    }
      .runFold( Set.newBuilder[String] )(_ += _)
      .map( _.result() )
  }

}
