package controllers

import akka.util.ByteString
import javax.inject.{Inject, Singleton}

import akka.stream.scaladsl.{Keep, Sink}
import io.suggest.adv.geo._
import io.suggest.dt.YmdHelpersJvm
import io.suggest.dt.interval.MRangeYmdOpt
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.MItems
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.pick.PickleUtil
import io.suggest.streams.StreamsUtil
import io.suggest.util.logs.MacroLogsImpl
import models.adv.geo.cur.{MAdvGeoBasicInfo, MAdvGeoShapeInfo}
import models.mctx.Context
import models.mproj.MCommonDi
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc.Result
import util.acl.CanAccessItem
import util.adv.geo.{AdvGeoBillUtil, AdvGeoFormUtil}
import util.billing.Bill2Util

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.05.17 14:20
  * Description: Контроллерная утиль
  */
@Singleton
protected class LkGeoCtlUtil @Inject() (
                                         streamsUtil             : StreamsUtil,
                                         bill2Util               : Bill2Util,
                                         mItems                  : MItems,
                                         canAccessItem           : CanAccessItem,
                                         advGeoFormUtil          : AdvGeoFormUtil,
                                         ymdHelpersJvm           : YmdHelpersJvm,
                                         advGeoBillUtil          : AdvGeoBillUtil,
                                         override val mCommonDi  : MCommonDi
                                       )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._
  import streamsUtil.Implicits._
  import ymdHelpersJvm.Implicits._


  /** Макс.кол-во item'ов ресиверов, возвращаемых в одном rcvr-попапе. */
  private def RCVR_ITEMS_PER_POPUP_LIMIT = 50

  private def CACHE_10 = CACHE_CONTROL -> "private, max-age=10"


  /** Тело экшена форматирования инфы о гео-шейпах.
    *
    * @param nodeId id узла. Значение колонки node_id в MItems.
    * @param request Исходный HTTP-реквест.
    * @return HTTP-ответ.
    */
  def currentNodeItemsGsToGeoJson(nodeId: String, itemTypes: TraversableOnce[MItemType])(implicit request: IReq[_]): Future[Result] = {
    // Собрать данные о текущих гео-размещениях карточки, чтобы их отобразить юзеру на карте.
    val jsonsSrc = slick.db
      .stream {
        val query = mItems.findCurrentForNode( nodeId, itemTypes )
        bill2Util.onlyGeoShapesInfo(query)
      }
      .toSource
      // Причесать сырой выхлоп базы, состоящий из пачки Option'ов.
      .mapConcat( MAdvGeoShapeInfo.applyOpt(_).toList )
      // Каждый элемент нужно скомпилить в пригодную для сериализации модель.
      // Сконвертить в GeoJSON и сериализовать в промежуточное JSON-представление.
      .map { si =>
        val gjFeature = advGeoFormUtil.shapeInfo2geoJson(si)
        Json.toJson( gjFeature )
      }
      .maybeTraceCount(this) { totalCount =>
        s"currentNodeItemsGsToGeoJson($nodeId): streamed $totalCount GeoJSON features"
      }
      // Превратить поток JSON-значений в "поточную строку", направленную в сторону юзера.
      .jsValuesToJsonArray

    Ok.chunked( jsonsSrc )
      .as( withCharset(JSON) )
      .withHeaders( CACHE_10 )
  }


  /** Экшен рендера попапа для существующего и текущего item'а.
    *
    * @param itemId id item'а.
    * @param itemMapperF Функция-рендерер основной инфы с item'а в payload, пригодный для отправки клиенту.
    * @return Action.
    */
  def currentItemPopup(itemId: Gid_t, itemTypes: Seq[MItemType])(itemMapperF: MAdvGeoBasicInfo => Option[MGeoItemInfoPayload]) = {
    canAccessItem(itemId, edit = false).async { implicit request =>
      def logPrefix = s"existGeoAdvsShapePopup($itemId):"

      // Доп. проверка прав доступа: вдруг юзер захочет пропихнуть тут какой-то свой item, но не относящийся к форме.
      // TODO Вынести суть ассерта на уровень отдельного ActionBuilder'а, проверяющего права доступа по аналогии с CanAccessItemPost.
      if ( itemTypes.nonEmpty && !itemTypes.contains( request.mitem.iType ) )
        throw new IllegalArgumentException(s"$logPrefix Item itype==${request.mitem.iType}, but here we need GeoPlace: ${MItemTypes.GeoPlace}")

      // Наврядли можно отрендерить в попапе даже это количество...
      val itemsMax = RCVR_ITEMS_PER_POPUP_LIMIT

      // Запросить у базы инфы по размещениям в текущем месте...
      val itemsSrc0 = slick.db
        .stream {
          advGeoBillUtil.itemsWithSameGeoShapeAs(
            query0  = mItems.findCurrentForNode( request.mitem.nodeId, itemTypes ),
            itemId  = itemId,
            limit   = itemsMax
          )
        }
        .toSource

      // Сразу создаём ответвление от потока, которое будет считать полученные результаты, материализуя общее кол-во на выходе:
      val itemsSrc = itemsSrc0.alsoToMat( streamsUtil.Sinks.count )(Keep.right)

      implicit val ctx = implicitly[Context]

      val (itemsCountFut, rowsMsFut) = itemsSrc
        // Причесать кортежи в нормальные инстансы
        .map( MAdvGeoBasicInfo.apply )
        // Сгруппировать и объеденить по периодам размещения.
        .groupBy(itemsMax, { m => (m.dtStartOpt, m.dtEndOpt) })
        .fold( List.empty[MAdvGeoBasicInfo] ) { (acc, e) => e :: acc }
        .map { infos =>
          // Нужно отсортировать item'ы по алфавиту или id, завернув их в итоге в Row
          val info0 = infos.head
          val row = MGeoAdvExistRow(
            // Диапазон дат, если есть.
            dateRange = MRangeYmdOpt.applyFrom(
              dateStartOpt = advGeoBillUtil.offDate2localDateOpt(info0.dtStartOpt)(ctx),
              dateEndOpt   = advGeoBillUtil.offDate2localDateOpt(info0.dtEndOpt)(ctx)
            ),
            // Инфа по item'ам.
            items = infos
              .sortBy(m => (m.tagFaceOpt, m.id) )
              .flatMap { m =>
                val mgiPlOpt = itemMapperF(m)
                /* Option[MGeoItemInfoPayload] = m.iType match {
                  case MItemTypes.GeoTag =>
                    m.tagFaceOpt
                      .map { InGeoTag.apply }
                  case MItemTypes.GeoPlace =>
                    Some( OnMainScreen )
                  case otherType =>
                    LOGGER.error(s"$logPrefix Unexpected iType=$otherType for #${m.id}, Dropping adv data.")
                    None
                  //throw new IllegalArgumentException("Unexpected iType = " + otherType)
                }*/

                if (mgiPlOpt.isEmpty)
                  LOGGER.warn(s"$logPrefix Dropped adv data: $m")

                for (mgiPl <- mgiPlOpt) yield {
                  MGeoItemInfo(
                    itemId        = m.id,
                    isOnlineNow   = m.status == MItemStatuses.Online,
                    payload       = mgiPl
                  )
                }
              }
          )
          val startMs = info0.dtStartOpt.map(_.toInstant.toEpochMilli)
          startMs -> row
        }
        // Вернуться на уровень основного потока...
        .mergeSubstreams
        // Собрать все имеющиеся результаты в единую коллекцию:
        .toMat( Sink.seq )(Keep.both)
        .run()

      // Сборка непоточного бинарного ответа.
      for {
        rowsMs      <- rowsMsFut
        itemsCount  <- itemsCountFut
      } yield {
        // Отсортировать ряды по датам, собрать итоговую модель ответа...
        val mresp = MGeoAdvExistPopupResp(
          rows = rowsMs
            .sortBy(_._1)
            .map(_._2),
          haveMore = itemsCount >= itemsMax
        )
        LOGGER.trace(s"$logPrefix count=$itemsCount/$itemsMax haveMore=${mresp.haveMore} rows=${mresp.rows}")

        // Сериализовать и вернуть результат:
        val pickled = PickleUtil.pickle(mresp)
        Ok( ByteString(pickled) )
          .withHeaders( CACHE_10 )
      }
    }
  }

}
