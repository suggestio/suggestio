package controllers

import java.io.FileInputStream

import _root_.util.acl._
import _root_.util.geo.umap._
import com.google.inject.Inject
import io.suggest.geo.{GsTypes, PointGs}
import io.suggest.model.n2.edge.{MEdgeGeoShape, MEdgeInfo, MNodeEdges}
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.maps.umap._
import models.mproj.ICommonDi
import models.req.{IReq, IReqHdr}
import play.api.http.HttpVerbs
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{Call, MultipartFormData, RequestHeader, Result}
import play.api.libs.json._
import views.html.helper.CSRF
import views.html.umap._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.09.14 12:09
 * Description: Контроллер для umap-backend'ов.
 */
class Umap @Inject() (
  umapUtil                        : UmapUtil,
  mNodes                          : MNodes,
  isSu                            : IsSu,
  isSuNode                        : IsSuNode,
  override val mCommonDi          : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi._

  /** Разрешено ли редактирование глобальной карты всех узлов? */
  val GLOBAL_MAP_EDIT_ALLOWED: Boolean = {
    configuration
      .getBoolean("umap.global.map.edit.allowed")
      .getOrElse(false)
  }


  /** Рендер статической карты для всех узлов, которая запросит и отобразит географию узлов. */
  def getAdnNodesMap = csrf.AddToken {
    isSu().apply { implicit request =>
      // TODO Нужно задействовать reverse-роутер
      val dlUrl = "/sys/umap/nodes/datalayer?ngl={pk}"
      val args = UmapTplArgs(
        dlUpdateUrl   = _csrfUrlPart(dlUrl),
        dlGetUrl      = dlUrl,
        editAllowed   = GLOBAL_MAP_EDIT_ALLOWED,
        title         = "Сводная карта всех узлов",
        ngls          = NodeGeoLevels.valuesNgl.toSeq.sortBy(_.id)
      )
      Ok( mapBaseTpl(args) )
    }
  }

  /** Кривая сборка шаблона ссылки сохранения с CSRF-токеном. */
  // TODO Нужно задействовать reverse-роутер тут вместо ручной сборки Сall с ручной ссылкой.
  private def _csrfUrlPart(url: String)(implicit request: RequestHeader): String = {
    val c = Call(HttpVerbs.POST, url)
    CSRF(c).url
  }


  /**
   * Редактирование карты в рамках одного узла.
   *
   * @param nodeId id узла.
   * @return 200 OK и страница с картой.
   */
  def getAdnNodeMap(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId) { implicit request =>
      // TODO Нужно задействовать reverse-роутер.
      val dlUrl = s"/sys/umap/node/$nodeId/datalayer?ngl={pk}"
      val args = UmapTplArgs(
        dlUpdateUrl   = _csrfUrlPart(dlUrl),
        dlGetUrl      = dlUrl,
        editAllowed   = true,
        title         = "Карта: " +
          request.mnode.meta.basic.name +
          request.mnode.meta.address.town.fold("")(" / " + _),
        ngls          = request.mnode
          .extras
          .adn
          .fold (List.empty[NodeGeoLevel]) { adn =>
            AdnShownTypes.adnInfo2val(adn).ngls
          }
      )
      Ok( mapBaseTpl(args) )
    }
  }


  /** Рендер одного слоя, перечисленного в карте слоёв. */
  def getDataLayerGeoJson(ngl: NodeGeoLevel) = isSu().async { implicit request =>
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[ICriteria] = {
        // Ищем только с node-location'ами на текущем уровне.
        val cr = Criteria(
          predicates  = Seq( MPredicates.NodeLocation ),
          gsIntersect = Some(GsCriteria(
            levels      = Seq(ngl),
            gjsonCompat = Some(true)
          ))
        )
        Seq(cr)
      }

      override def limit                = 600
    }
    for {
      mnodes <- mNodes.dynSearch(msearch)
    } yield {
      _getDataLayerGeoJson(None, ngl, mnodes)
    }
  }


  /** Общий код экшенов, занимающихся рендером слоёв в geojson-представление, пригодное для фронтенда. */
  private def _getDataLayerGeoJson(adnIdOpt: Option[String], ngl: NodeGeoLevel, nodes: Seq[MNode])
                                  (implicit request: IReqHdr): Result = {
    val msgs = implicitly[Messages]
    val centerMsg = msgs("Center")

    val features: Seq[Feature] = {

      val shapeFeaturesIter = for {
        mnode     <- umapUtil.prepareDataLayerGeos(nodes.iterator)
        locEdge   <- mnode.edges.withPredicateIter( MPredicates.NodeLocation)
        shape     <- locEdge.info.geoShapes
        if shape.shape.shapeType.isGeoJsonCompatible
      } yield {
        Feature(
          geometry = shape.shape,
          properties = Some(FeatureProperties(
            name        = mnode.guessDisplayNameOrId,
            description = Some( routes.SysMarket.showAdnNode(mnode.id.get).absoluteURL() )
          ))
        )
      }

      val centersFeaturesIter = nodes
        .iterator
        .flatMap { mnode =>
          mnode.geo.point.map { pt =>
            Feature(
              geometry = PointGs(pt),
              properties = Some(FeatureProperties(
                name        = Some( s"${mnode.meta.basic.name} ($centerMsg)" ),
                description = Some( routes.SysMarket.showAdnNode(mnode.id.get).absoluteURL() )
              ))
            )
          }
        }

      // Собрать итоговое значение features
      (centersFeaturesIter ++ shapeFeaturesIter)
        .toSeq
    }

    val fc = FeatureCollection(
      storage   = layerJson2(ngl),
      features  = features
    )

    Ok( Json.toJson(fc) )
  }


  /** Получение геослоя в рамках карты одного узла. */
  def getDataLayerNodeGeoJson(nodeId: String, ngl: NodeGeoLevel) = isSuNode(nodeId).async { implicit request =>
    val adnIdOpt = request.mnode.id
    val nodes = Seq(request.mnode)
    _getDataLayerGeoJson(adnIdOpt, ngl, nodes)
  }


  /** Обработка запроса сохранения сеттингов карты.
    * Само сохранение не реализовано, поэтому тут просто ответ 200 OK.
    */
  def saveMapSettingsSubmit = csrf.Check {
    isSu()(parse.multipartFormData) { implicit request =>
      val msgs = implicitly[Messages]
      val resp = MapSettingsSaved(
        url  = routes.Umap.getAdnNodesMap().url,
        info = msgs("Map.settings.save.unsupported"),
        id   = 16717    // цифра с потолка
      )
      Ok( Json.toJson(resp) )
    }
  }


  /** Сабмит одного слоя на глобальной карте. */
  def saveMapDataLayer(ngl: NodeGeoLevel) = csrf.Check {
    isSu().async(parse.multipartFormData) { implicit request =>
      // Банальная проверка на доступ к этому экшену.
      if (!GLOBAL_MAP_EDIT_ALLOWED)
        throw new IllegalAccessException("Global map editing is not allowed.")
      // Продолжаем веселье.
      _saveMapDataLayer(ngl, None) { feat =>
        feat.properties
          .flatMap(_.nodeId)
          .getOrElse("???")
      }
    }
  }


  /** Сабмит одного слоя на карте узла. */
  def saveNodeDataLayer(adnId: String, ngl: NodeGeoLevel) = csrf.Check {
    isSuNode(adnId).async(parse.multipartFormData) { implicit request =>
      _saveMapDataLayer(ngl, request.mnode.id){ _ => adnId }
    }
  }


  /** Общий код экшенов, занимающихся сохранением геослоёв. */
  private def _saveMapDataLayer(ngl: NodeGeoLevel, adnIdOpt: Option[String])(getAdnIdF: Feature => String)
                               (implicit request: IReq[MultipartFormData[TemporaryFile]]): Future[Result] = {
    // Готовимся к сохранению присланных данных.
    val logPrefix = s"saveMapDataLayer($ngl): "
    // Для обновления слоя нужно удалить все renderable-данные в этом слое, и затем залить в слой все засабмиченные через bulk request.
    request.body.file("geojson").fold[Future[Result]] {
      NotAcceptable("geojson not found in response")

    } { tempFile =>
      // Парсим json в потоке с помощью play.json:
      val is = new FileInputStream( tempFile.ref.file )
      val playJson = try {
        Json.parse(is)
      } finally {
        is.close()
        tempFile.ref.file.delete()
      }

      val maybeLayerData = playJson.validate[FeatureCollection]
      if (maybeLayerData.isError)
        LOGGER.error(s"$logPrefix Unable to parse request body: $maybeLayerData")
      val layerData = playJson.validate[FeatureCollection].get

      // Собираем запрос в карту, где ключ -- это nodeId.
      val nodeFeatures = layerData
        .features
        .groupBy(getAdnIdF)

      // Собираем карту узлов.
      val mnodesMapFut = mNodesCache.multiGetMap( nodeFeatures.keysIterator )

      val updAllFut = mnodesMapFut.flatMap { mnodesMap =>
        // Для каждого узла произвести персональное обновление.
        Future.traverse( nodeFeatures ) { case (adnId, features) =>

          // Собираем шейпы для узла
          val shapes = features
            .iterator
            .filter { _.geometry.shapeType == GsTypes.Polygon }
            .zipWithIndex
            .map { case (poly, i) =>
              MEdgeGeoShape(
                id      = i,
                shape   = poly.geometry,
                glevel  = ngl
              )
            }
            .toList

          // Собираем новый эдж сразу, старый будет удалён без суд и следствия.
          val locEdge = MEdge(
            predicate = MPredicates.NodeLocation,
            info = MEdgeInfo(
              geoShapes = shapes
            )
          )

          // Узнаём точку-центр, если есть.
          val centerOpt = features
            .iterator
            .flatMap { f =>
              f.geometry match {
                case p: PointGs => List(p.coord)
                case _          => Nil
              }
            }
            .toStream
            .headOption

          // Пытаемся сохранить новые геоданные в узел.
          mNodes.tryUpdate( mnodesMap(adnId) ) { mnode0 =>
            mnode0.copy(
              edges = mnode0.edges.copy(
                out = {
                  val keepIter = mnode0.edges.withoutPredicateIter( MPredicates.NodeLocation )
                  val newIter = Iterator( locEdge )
                  MNodeEdges.edgesToMap1( keepIter ++ newIter )
                }
              ),
              geo = mnode0.geo.copy(
                point   = centerOpt
              )
            )
          }
        }
      }

      implicit val msgs = implicitly[Messages]

      for (nodes <- updAllFut) yield {
        LOGGER.trace(s"$logPrefix Updated ${nodes.size} nodes.")
        val layer = layerJson2(ngl)(msgs)
        Ok( Json.toJson(layer) )
      }
    }
  }


  def createMapDataLayer = csrf.Check {
    isSu()(parse.multipartFormData) { implicit request =>
      Ok("asdasd")
    }
  }


  private def layerJson2(ngl: NodeGeoLevel)(implicit messages: Messages): Layer = {
    Layer(
      name          = messages("ngls." + ngl.esfn),
      id            = ngl.id,
      displayOnLoad = true
    )
  }

}
