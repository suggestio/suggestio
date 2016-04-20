package controllers.sc

import io.suggest.model.n2.node.IMNodes
import models.msc.map.{IMMapNodesDi, MMapAreaInfo, MNodesSource, MNodesSources}
import play.api.libs.json.Json
import util.acl.MaybeAuth

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 15:44
  * Description: Аддон для [[controllers.MarketShowcase]], добавляющий экшены для
  * получения данных географической карты.
  */
trait ScMap
  extends ScController
    with MaybeAuth
    with IMNodes
    with IMMapNodesDi
{

  import mCommonDi._

  /**
    * Запуск GeoJSON-рендера слоя узлов для указанной конфигурации карты.
    * В зависимости от масштаба может произойти server-side агрегация или возврат реальных точек.
    *
    * @param mapInfo Состояние карты.
    * @return 200 OK + GeoJSON.
    */
  def renderMapNodesLayer(mapInfo: MMapAreaInfo) = MaybeAuth().async { implicit request =>

    // На основе текущего зума карты надо выбрать, что делать: аггрегация или просто точки.
    val needCluster = mMapNodes.isClusteredZoom(mapInfo.zoom)

    // Начать собирать запрос поиска отображаемых на карте узлов
    val msearch = mMapNodes.mapNodesQuery(needCluster, Some(mapInfo.envelope))

    // Дальше в зависимости от needCluster логика сильно разделяется.
    val sourcesFut: Future[ISeq[MNodesSource]] = {
      if (needCluster) {
        mMapNodes.findClusteredSource(mapInfo.zoom, msearch)
      } else {
        for (ptsFc <- mMapNodes.getPoints(msearch)) yield {
          mMapNodes.points2NodeSources(ptsFc)
            .toList
        }
      }
    }

    // Собираем итоговый ответ сервера.
    for {
      sources <- sourcesFut
    } yield {
      val fc = MNodesSources(
        sources = sources
      )
      Ok( Json.toJson(fc) )
    }
  }


  /**
    * Рендер вообще всех точек узлов без кластеризации, которые необходимо отобразить на карте узлов.
    * Рендер идёт максимально примитивно, чтобы mapbox мог самостоятельно получить и обработать данные
    * в фоновом service-воркере, который произведёт послойную кластеризацию.
    *
    * Желательно, чтобы запрос этот проходил через CDN.
    *
    * @return GeoJSON.
    */
  def renderMapNodesAll = MaybeAuth().async { implicit request =>
    // Начать собирать запрос поиска отображаемых на карте узлов
    // Кешируем кратковременно всё, т.к. экшен тяжеловат по RAM и CPU.
    cacheApiUtil.getOrElseFut("sc.map.nodes.all", expiration = 10.seconds) {
      val msearch = mMapNodes.mapNodesQuery(isClustered = false)
      for (ptsFc <- mMapNodes.getPoints(msearch)) yield {
        Ok( Json.toJson(ptsFc) )
          .withHeaders(CACHE_CONTROL -> "public, max-age=30")
      }
    }
  }

}
