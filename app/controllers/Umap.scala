package controllers

import java.nio.file.Files

import play.api.mvc.Result
import util.geo.umap.UmapUtil, UmapUtil.ADN_ID_SHAPE_FORM_FN
import models._
import play.api.i18n.{Lang, Messages}
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl.IsSuperuser
import views.html.umap._
import play.api.libs.json._
import io.suggest.util.SioEsUtil.laFuture2sFuture
import AdnShownTypes._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.09.14 12:09
 * Description: Контроллер для umap-backend'ов.
 */
object Umap extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Рендер статической карты, которая запросит и отобразит географию узлов. */
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
    val ctx = implicitly[Context]
    val nglsJson = JsArray(
      NodeGeoLevels.values.toSeq.sortBy(_.id).map { ngl =>
        JsObject(Seq(
          "displayOnLoad" -> JsBoolean(true),
          "name"          -> JsString( Messages("ngls." + ngl.esfn)(ctx.lang) ),
          "id"            -> JsNumber(ngl.id)
        ))
      }
    )
    allNodesMapFut map { nodesMap =>
      Ok(mapBaseTpl(nglsJson, nodesMap)(ctx))
    }
  }


  /** Рендер одного слоя, перечисленного в карте слоёв. */
  def getDataLayerGeoJson(ngl: NodeGeoLevel) = IsSuperuser.async { implicit request =>
    MAdnNodeGeo.findAllRenderable(ngl, maxResults = 600).flatMap { geos =>
      val nodesMapFut: Future[Map[String, MAdnNode]] = {
        MAdnNodeCache.multiGet(geos.map(_.adnId).toSet)
          .map { nodes => nodes.map(node => node.id.get -> node).toMap}
      }
      nodesMapFut map { nodesMap =>
        val features: Seq[JsObject] = geos.map { geo =>
          JsObject(Seq(
            "type" -> JsString("Feature"),
            "geometry" -> geo.shape.toPlayJson(geoJsonCompatible = true),
            "properties" -> JsObject(Seq(
              "name" -> JsString( nodesMap.get(geo.adnId).fold(geo.adnId)(_.meta.name) ),
              ADN_ID_SHAPE_FORM_FN -> JsString(geo.adnId)
            ))
          ))
        }
        val resp = JsObject(Seq(
          "type"      -> JsString("FeatureCollection"),
          "_storage"  -> layerJson(ngl, request2lang),
          "features"  -> JsArray(features)
        ))
        Ok(resp)
      }
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

  /** Сабмит одного слоя на карте. */
  def saveMapDataLayer(ngl: NodeGeoLevel) = IsSuperuser.async(parse.multipartFormData) { implicit request =>
    val logPrefix = s"saveMapDataLayer($ngl): "
    // Для обновления слоя нужно удалить все renderable-данные в этом слое, и затем залить в слой все засабмиченные через bulk request.
    request.body.file("geojson").fold[Future[Result]] {
      NotAcceptable("geojson not found in response")
    } { tempFile =>
      val allRenderableFut = MAdnNodeGeo.findAllRenderable(ngl)
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
        .filter(_.adnIdOpt.isDefined)
        .foreach { feature =>
          val geo = MAdnNodeGeo(
            adnId = feature.adnIdOpt.get,
            glevel = ngl,
            shape = feature.geometry
          )
          bulk.add(geo.indexRequestBuilder)
        }
      // TODO Надо защиту на случай проблем.
      // Слой распарсился и готов к сохранению. Запускаем удаление исходных данных слоя.
      allRenderableFut.flatMap { all =>
        val bulkDel = client.prepareBulk()
        all.foreach { geo =>
          bulkDel.add(geo.prepareDelete)
        }
        trace(logPrefix + "Deleting " + bulkDel.numberOfActions() + " shapes on layer...")
        bulkDel.execute()
      } flatMap { bulkDelResult =>
        trace(s"${logPrefix}Layer $ngl wiped: $bulkDelResult ;; Starting to save new shapes...")
        bulk.execute()
      } map { br =>
        trace(logPrefix + "Layer saved: " + br.buildFailureMessage())
        val resp = layerJson(ngl, request2lang)
        Ok(resp)
      } recoverWith {
        case ex: Throwable =>
          error("Failed to update layer " + ngl, ex)
          allRenderableFut flatMap { all =>
            val bulkReSave = client.prepareBulk()
            all.foreach { geo =>
              bulkReSave.add(geo.indexRequestBuilder)
            }
            bulkReSave.execute()
          } map { bsr =>
            debug("Rollbacked deleted data")
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
