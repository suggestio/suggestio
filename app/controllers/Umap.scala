package controllers

import models._
import play.api.i18n.{Lang, Messages}
import play.api.mvc.RequestHeader
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl.IsSuperuser
import views.html.umap._
import play.api.libs.json._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.09.14 12:09
 * Description: Контроллер для umap-backend'ов.
 */
object Umap extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Название поля в форме карты, которое содержит id узла. */
  val ADN_ID_SHAPE_FORM_FN = "description"

  /** Рендер статической карты, которая запросит и отобразит географию узлов. */
  def getAdnNodesMap = IsSuperuser { implicit request =>
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
    Ok( mapBaseTpl(nglsJson)(ctx) )
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
  def saveMapDataLayer(ngl: NodeGeoLevel) = IsSuperuser { implicit request =>
    // Для обновления слоя нужно удалить все renderable-данные в этом слое, и затем залить в слой все засабмиченные через bulk request.
    val resp = layerJson(ngl, request2lang)
    Ok(resp)
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
