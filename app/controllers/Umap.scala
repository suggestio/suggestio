package controllers

import java.nio.file.Files

import play.api.libs.Files.TemporaryFile
import play.api.mvc.{MultipartFormData, RequestHeader, Result}
import _root_.util.geo.umap._
import models._
import play.api.i18n.{Lang, Messages}
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import _root_.util.acl.{AbstractRequestWithPwOpt, IsSuperuserAdnNode, IsSuperuser}
import views.html.umap._
import play.api.libs.json._
import io.suggest.util.SioEsUtil.laFuture2sFuture
import AdnShownTypes._
import play.api.Play.{current, configuration}
import AdnShownTypes.adnInfo2val

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.09.14 12:09
 * Description: Контроллер для umap-backend'ов.
 */
object Umap extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Разрешено ли редактирование глобальной карты всех узлов? */
  val GLOBAL_MAP_EDIT_ALLOWED: Boolean = configuration.getBoolean("umap.global.map.edit.allowed") getOrElse false


  /** Рендер статической карты для всех узлов, которая запросит и отобразит географию узлов. */
  def getAdnNodesMap = IsSuperuser.async { implicit request =>
    // Скачиваем все узлы из базы. TODO Закачать через кэш?
    val allNodesMapFut: Future[Map[AdnShownType, Seq[MAdnNode]]] = {
      val sargs = new AdnNodesSearchArgs {
        override def withAdnRights = Seq(AdnRights.RECEIVER)
        override def maxResults = 500
      }
      MAdnNode.dynSearch(sargs)
    } map { allNodes =>
      allNodes
        .groupBy { node => AdnShownTypes.withName(node.adn.shownTypeId) : AdnShownType }
        .mapValues { _.sortBy(_.meta.name) }
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
      Ok(mapBaseTpl(args))
    }
  }


  /**
   * Редактирование карты в рамках одного узла.
   * @param adnId id узла.
   * @return 200 OK и страница с картой.
   */
  def getAdnNodeMap(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    val dlUrl = s"/sys/umap/node/$adnId/datalayer?ngl={pk}"
    val args = UmapTplArgs(
      dlUpdateUrl   = dlUrl, // TODO Нужно задействовать reverse-роутер.
      dlGetUrl      = dlUrl,
      nodesMap      = Map.empty,
      editAllowed   = true,
      title         = "Карта: " + request.adnNode.meta.name + request.adnNode.meta.town.fold("")(" / " + _),
      ngls          = request.adnNode.adn.ngls
    )
    Ok(mapBaseTpl(args))
  }


  /** Рендер одного слоя, перечисленного в карте слоёв. */
  def getDataLayerGeoJson(ngl: NodeGeoLevel) = IsSuperuser.async { implicit request =>
    MAdnNodeGeo.findAllRenderable(ngl, maxResults = 600).flatMap { geos =>
      val nodesMapFut: Future[Map[String, MAdnNode]] = {
        MAdnNodeCache.multiGet(geos.map(_.adnId).toSet)
          .map { nodes => nodes.map(node => node.id.get -> node).toMap}
      }
      nodesMapFut map { nodesMap =>
        _getDataLayerGeoJson(None, ngl, nodesMap, geos)
      }
    }
  }

  /** Общий код экшенов, занимающихся рендером слоёв в geojson-представление, пригодное для фронтенда. */
  private def _getDataLayerGeoJson(adnIdOpt: Option[String], ngl: NodeGeoLevel, nodesMap: Map[String, MAdnNode],
                                   geos: Seq[MAdnNodeGeo])(implicit request: RequestHeader): Result = {
    val features: Seq[JsObject] = {
      UmapUtil.prepareDataLayerGeos(geos.iterator)
        .map { geo =>
          JsObject(Seq(
            "type" -> JsString("Feature"),
            "geometry" -> geo.shape.toPlayJson(geoJsonCompatible = true),
            "properties" -> JsObject(Seq(
              "name"        -> JsString( nodesMap.get(geo.adnId).fold(geo.adnId)(_.meta.name) ),
              "description" -> JsString( routes.SysMarket.showAdnNode(geo.adnId).absoluteURL() )
            ))
          ))
        }
        .toSeq
    }
    val resp = JsObject(Seq(
      "type"      -> JsString("FeatureCollection"),
      "_storage"  -> layerJson(ngl, request2lang),
      "features"  -> JsArray(features)
    ))
    Ok(resp)
  }

  /** Получение геослоя в рамках карты одного узла. */
  def getDataLayerNodeGeoJson(adnId: String, ngl: NodeGeoLevel) = IsSuperuserAdnNode(adnId).async { implicit request =>
    // Наноэкономия памяти:
    val adnIdOpt = request.adnNode.id
    MAdnNodeGeo.findAllRenderable(ngl, adnIdOpt) map { geos =>
      val nodesMap = Map(adnId -> request.adnNode)
      _getDataLayerGeoJson(adnIdOpt, ngl, nodesMap, geos)
    }
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
  def saveMapDataLayer(ngl: NodeGeoLevel) = IsSuperuser.async(parse.multipartFormData) { implicit request =>
    // Банальная проверка на доступ к этому экшену.
    if (!GLOBAL_MAP_EDIT_ALLOWED)
      throw new IllegalAccessException("Global map editing is not allowed.")
    // Продолжаем веселье.
    _saveMapDataLayer(ngl, None)(_.adnIdOpt.get)
  }

  /** Сабмит одного слоя на карте узла. */
  def saveNodeDataLayer(adnId: String, ngl: NodeGeoLevel) = IsSuperuserAdnNode(adnId).async(parse.multipartFormData) {
    implicit request =>
      _saveMapDataLayer(ngl, request.adnNode.id){ _ => adnId }
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
      val allRenderableFut = MAdnNodeGeo.findAllRenderable(ngl, adnIdOpt)
      // TODO Надо бы задействовать InputStream или что-то ещё для парсинга.
      val jsonBytes = try {
        Files.readAllBytes(tempFile.ref.file.toPath)
      } finally {
        tempFile.ref.file.delete()
      }
      val layerData = UmapUtil.deserializeFromBytes(jsonBytes).get
      // Собираем BulkRequest для сохранения данных.
      val bulk = client.prepareBulk()
      layerData.features
        .foreach { feature =>
          val geo = MAdnNodeGeo(
            adnId = getAdnIdF(feature),
            glevel = ngl,
            shape = feature.geometry
          )
          bulk.add(geo.indexRequestBuilder)
        }
      // TODO Надо защиту на случай проблем.
      // Слой распарсился и готов к сохранению. Запускаем удаление исходных данных слоя.
      allRenderableFut.flatMap { all =>
        if (all.nonEmpty) {
          val bulkDel = client.prepareBulk()
          all.foreach { geo =>
            bulkDel.add(geo.prepareDelete)
          }
          trace(logPrefix + "Deleting " + bulkDel.numberOfActions() + " shapes on layer...")
          bulkDel.execute() map { Some.apply }
        } else {
          Future successful None
        }
      } flatMap { bulkDelResultOpt =>
        trace(s"${logPrefix}Layer $ngl wiped: $bulkDelResultOpt ;; Starting to save new shapes...")
        bulk.execute()
      } map { br =>
        if (br.hasFailures) {
          warn("Layer saved with problems: " + br.buildFailureMessage())
        } else {
          trace(logPrefix + "Layer saved without problems.")
        }
        val resp = layerJson(ngl, request2lang)
        Ok(resp)
      } recoverWith {
        case ex: Throwable =>
          error("Failed to update layer " + ngl, ex)
          allRenderableFut flatMap { all =>
            if (all.nonEmpty) {
              val bulkReSave = client.prepareBulk()
              all.foreach { geo =>
                bulkReSave.add(geo.indexRequestBuilder)
              }
              bulkReSave.execute().map(Some.apply)
            } else {
              Future successful None
            }
          } map { bsrOpt =>
            debug("Rollbacked deleted data: " + bsrOpt)
            InternalServerError("Failed to save. See logs.")
          }
      }
    }
  }

  def createMapDataLayer = IsSuperuser(parse.multipartFormData) { implicit request =>
    Ok("asdasd")
  }


  /** Рендер json'а, описывающего геослой. */
  private def layerJson(ngl: NodeGeoLevel, lang: Lang): JsObject = {
    JsObject(Seq(
      "name"          -> JsString( Messages("ngls." + ngl.esfn)(lang) ),
      "id"            -> JsNumber(ngl.id),
      "displayOnLoad" -> JsBoolean(true)
    ))
  }

}
