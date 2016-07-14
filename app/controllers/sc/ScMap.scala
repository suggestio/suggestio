package controllers.sc

import io.suggest.model.n2.node.IMNodes
import models.msc.map.IMMapNodesDi
import play.api.libs.json.Json
import util.acl.MaybeAuth

import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.04.16 15:44
  * Description: Аддон для [[controllers.Sc]], добавляющий экшены для
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
