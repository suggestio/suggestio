package controllers

import com.google.inject.{Inject, Singleton}
import io.suggest.async.StreamsUtil
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.util.logs.MacroLogsImpl
import models.adv.geo.cur.MAdvGeoShapeInfo
import models.mproj.MCommonDi
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc.Result
import util.adv.geo.AdvGeoFormUtil
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
                                         advGeoFormUtil          : AdvGeoFormUtil,
                                         override val mCommonDi  : MCommonDi
                                       )
  extends SioController
  with MacroLogsImpl
{

  import mCommonDi._
  import streamsUtil.Implicits._


  /** Тело экшена форматирования инфы о гео-шейпах.
    *
    * @param nodeId id узла. Значение колонки node_id в MItems.
    * @param request Исходный HTTP-реквест.
    * @return HTTP-ответ.
    */
  def currentNodeItemsGsToGeoJson(nodeId: String, itemTypes: TraversableOnce[MItemType])(implicit request: IReq[_]): Future[Result] = {
    // Собрать данные о текущих гео-размещениях карточки, чтобы их отобразить юзеру на карте.
    val currAdvsSrc = slick.db
      .stream {
        val query = bill2Util.findCurrentForNodeQuery( nodeId, itemTypes )
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

    // Превратить поток JSON-значений в "поточную строку", направленную в сторону юзера.
    val jsonStrSrc = streamsUtil.jsonSrcToJsonArrayNullEnded(currAdvsSrc)

    streamsUtil.maybeTraceCount(currAdvsSrc, this) { totalCount =>
      s"currentNodeItemsGsToGeoJson($nodeId): streamed $totalCount GeoJSON features"
    }

    Ok.chunked( jsonStrSrc )
      .as( withCharset(JSON) )
      .withHeaders( CACHE_CONTROL -> "private, max-age=10" )
  }

}
