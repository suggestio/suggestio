package controllers

import java.nio.file.Files

import _root_.util.acl._
import _root_.util.geo.umap._
import com.google.inject.Inject
import io.suggest.model.geo.{GsTypes, PointGs}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models._
import models.mproj.MCommonDi
import play.api.i18n.Messages
import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, RequestHeader, Result}
import util.PlayMacroLogsImpl
import play.api.libs.json._
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
  override val mCommonDi          : MCommonDi
)
  extends SioControllerImpl
  with PlayMacroLogsImpl
  with IsSuperuserAdnNode
  with IsSuperuser
{

  import mCommonDi._

  /** Разрешено ли редактирование глобальной карты всех узлов? */
  val GLOBAL_MAP_EDIT_ALLOWED: Boolean = configuration.getBoolean("umap.global.map.edit.allowed") getOrElse false


  /** Рендер статической карты для всех узлов, которая запросит и отобразит географию узлов. */
  def getAdnNodesMap = IsSuperuserGet.async { implicit request =>
    // Скачиваем все узлы из базы. TODO Закачать через кэш?
    val allNodesMapFut: Future[Map[AdnShownType, Seq[MNode]]] = {
      val msearch = new MNodeSearchDfltImpl {
        override def withAdnRights  = Seq(AdnRights.RECEIVER)
        override def limit          = 500
        override def nodeTypes      = Seq( MNodeTypes.AdnNode )
      }
      MNode.dynSearch( msearch )
    } map { allNodes =>
      allNodes
        .groupBy { node =>
          node.extras.adn
            .flatMap( _.shownTypeIdOpt )
            .flatMap( AdnShownTypes.maybeWithName )
            .getOrElse( AdnShownTypes.default )
        }
        .mapValues { _.sortBy(_.meta.basic.name) }
    }
    allNodesMapFut map { nodesMap =>
      val dlUrl = "/sys/umap/nodes/datalayer?ngl={pk}"
      val args = UmapTplArgs(
        dlUpdateUrl   = dlUrl, // TODO Нужно задействовать reverse-роутер.
        dlGetUrl      = dlUrl,
        nodesMap      = nodesMap,
        editAllowed   = GLOBAL_MAP_EDIT_ALLOWED,
        title         = "Сводная карта всех узлов",
        ngls          = NodeGeoLevels.valuesNgl.toSeq.sortBy(_.id)
      )
      Ok( mapBaseTpl(args) )
    }
  }


  /**
   * Редактирование карты в рамках одного узла.
   * @param adnId id узла.
   * @return 200 OK и страница с картой.
   */
  def getAdnNodeMap(adnId: String) = IsSuperuserAdnNodeGet(adnId) { implicit request =>
    val dlUrl = s"/sys/umap/node/$adnId/datalayer?ngl={pk}"
    val args = UmapTplArgs(
      dlUpdateUrl   = dlUrl, // TODO Нужно задействовать reverse-роутер.
      dlGetUrl      = dlUrl,
      nodesMap      = Map.empty,
      editAllowed   = true,
      title         = "Карта: " +
        request.adnNode.meta.basic.name +
        request.adnNode.meta.address.town.fold("")(" / " + _),
      ngls          = request.adnNode
        .extras
        .adn
        .fold (List.empty[NodeGeoLevel]) { adn =>
          AdnShownTypes.adnInfo2val(adn).ngls
        }
    )
    Ok( mapBaseTpl(args) )
  }


  /** Рендер одного слоя, перечисленного в карте слоёв. */
  def getDataLayerGeoJson(ngl: NodeGeoLevel) = IsSuperuser.async { implicit request =>
    val msearch = new MNodeSearchDfltImpl {
      override def gsGeoJsonCompatible  = Some(true)
      override def gsLevels             = Seq(ngl)
      override def limit                = 600
    }
    for {
      mnodes <- MNode.dynSearch(msearch)
    } yield {
      _getDataLayerGeoJson(None, ngl, mnodes)
    }
  }

  /** Общий код экшенов, занимающихся рендером слоёв в geojson-представление, пригодное для фронтенда. */
  private def _getDataLayerGeoJson(adnIdOpt: Option[String], ngl: NodeGeoLevel, nodes: Seq[MNode])
                                  (implicit request: RequestHeader): Result = {
    // TODO Вынести json в отдельную модель.
    val features: Seq[JsObject] = {
      val shapeFeaturesIter = for {
        mnode <- umapUtil.prepareDataLayerGeos(nodes.iterator)
        shape <- mnode.geo.shapes if shape.shape.shapeType.isGeoJsonCompatible
      } yield {
        JsObject(Seq(
          "type"        -> JsString("Feature"),
          "geometry"    -> shape.shape.toPlayJson(geoJsonCompatible = true),
          "properties"  -> JsObject(Seq(
            "name" -> JsString( mnode.guessDisplayNameOrId.get ),
            "description" -> JsString(
              routes.SysMarket.showAdnNode(mnode.id.get).absoluteURL()
            )
          ))
        ))
      }
      val centersFeaturesIter = nodes
        .iterator
        .flatMap { mnode =>
          mnode.geo.point.map { pt =>
            JsObject(Seq(
              "type" -> JsString("Feature"),
              "geometry"   -> PointGs(pt).toPlayJson(geoJsonCompatible = true),
              "properties" -> JsObject(Seq(
                "name"        -> JsString( mnode.meta.basic.name + " (центр)" ),
                "description" -> JsString( routes.SysMarket.showAdnNode(mnode.id.get).absoluteURL() )
              ))
            ))
          }
        }
      (centersFeaturesIter ++ shapeFeaturesIter)
        .toSeq
    }
    val resp = JsObject(Seq(
      "type"      -> JsString("FeatureCollection"),
      "_storage"  -> layerJson(ngl),
      "features"  -> JsArray(features)
    ))
    Ok(resp)
  }

  /** Получение геослоя в рамках карты одного узла. */
  def getDataLayerNodeGeoJson(adnId: String, ngl: NodeGeoLevel) = IsSuperuserAdnNode(adnId).async { implicit request =>
    val adnIdOpt = request.adnNode.id
    val nodes = Seq(request.adnNode)
    _getDataLayerGeoJson(adnIdOpt, ngl, nodes)
  }


  /** Сохранение сеттингов карты. */
  def saveMapSettingsSubmit = IsSuperuser(parse.multipartFormData) { implicit request =>
    // TODO Stub
    val resp = JsObject(Seq(
      "url"  -> JsString( routes.Umap.getAdnNodesMap().url ),
      "info" -> JsString( "Settings ignored" ),
      "id"   -> JsNumber( 16717 )
    ))
    Ok(resp)
  }

  /** Сабмит одного слоя на глобальной карте. */
  def saveMapDataLayer(ngl: NodeGeoLevel) = IsSuperuserPost.async(parse.multipartFormData) { implicit request =>
    // Банальная проверка на доступ к этому экшену.
    if (!GLOBAL_MAP_EDIT_ALLOWED)
      throw new IllegalAccessException("Global map editing is not allowed.")
    // Продолжаем веселье.
    _saveMapDataLayer(ngl, None)(_.adnIdOpt.get)
  }

  /** Сабмит одного слоя на карте узла. */
  def saveNodeDataLayer(adnId: String, ngl: NodeGeoLevel) = {
    IsSuperuserAdnNodePost(adnId).async(parse.multipartFormData) { implicit request =>
      _saveMapDataLayer(ngl, request.adnNode.id){ _ => adnId }
    }
  }

  /** Общий код экшенов, занимающихся сохранением геослоёв. */
  private def _saveMapDataLayer(ngl: NodeGeoLevel, adnIdOpt: Option[String])(getAdnIdF: UmapFeature => String)
                               (implicit request: AbstractRequestWithPwOpt[MultipartFormData[TemporaryFile]]): Future[Result] = {
    // Готовимся к сохранению присланных данных.
    val logPrefix = s"saveMapDataLayer($ngl): "
    // Для обновления слоя нужно удалить все renderable-данные в этом слое, и затем залить в слой все засабмиченные через bulk request.
    request.body.file("geojson").fold[Future[Result]] {
      NotAcceptable("geojson not found in response")

    } { tempFile =>
      // TODO Надо бы задействовать InputStream или что-то ещё для парсинга.
      val jsonBytes = try {
        Files.readAllBytes(tempFile.ref.file.toPath)
      } finally {
        tempFile.ref.file.delete()
      }
      val layerData = umapUtil.deserializeFromBytes(jsonBytes).get

      // Собираем запрос в карту, где ключ -- это nodeId.
      val nodeFeatures = layerData
        .features
        .iterator
        .toSeq
        .groupBy(getAdnIdF)

      // Собираем карту узлов.
      val mnodesMapFut = mNodeCache.multiGetMap( nodeFeatures.keysIterator )

      val updAllFut = mnodesMapFut.flatMap { mnodesMap =>
        // Для каждого узла произвести персональное обновление.
        Future.traverse( nodeFeatures ) { case (adnId, features) =>
          // Собираем шейпы для узла
          val shapes = features
            .iterator
            .filter { _.geometry.shapeType == GsTypes.polygon }
            .zipWithIndex
            .map { case (poly, i) =>
              MGeoShape(
                id      = i,
                shape   = poly.geometry,
                glevel  = ngl
              )
            }
            .toSeq
          // Узнаём центр.
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
          MNode.tryUpdate( mnodesMap(adnId) ) { mnode0 =>
            mnode0.copy(
              geo = mnode0.geo.copy(
                shapes  = shapes,
                point   = centerOpt
              )
            )
          }
        }
      }

      for (nodeIds <- updAllFut) yield {
        LOGGER.trace(s"$logPrefix Updated ${nodeIds.size} nodes.")
        val resp = layerJson(ngl)
        Ok(resp)
      }
    }
  }

  def createMapDataLayer = IsSuperuser(parse.multipartFormData) { implicit request =>
    Ok("asdasd")
  }


  /** Рендер json'а, описывающего геослой. */
  private def layerJson(ngl: NodeGeoLevel)(implicit messages: Messages): JsObject = {
    // TODO Нужна отдельная модель с play.json
    JsObject(Seq(
      "name"          -> JsString( messages("ngls." + ngl.esfn) ),
      "id"            -> JsNumber(ngl.id),
      "displayOnLoad" -> JsBoolean(true)
    ))
  }

}
