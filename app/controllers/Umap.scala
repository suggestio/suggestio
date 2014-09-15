package controllers

import models._
import play.api.i18n.Messages
import util.PlayMacroLogsImpl
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import util.acl.IsSuperuser
import views.html.umap._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 15.09.14 12:09
 * Description: Контроллер для umap-backend'ов.
 */
object Umap extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /** Название поля в форме карты, которое содержит id узла. */
  val ADN_ID_SHAPE_FORM_FN = "name"

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
    MAdnNodeGeo.findAllRenderable(ngl, maxResults = 600).map { geos =>
      val features: Seq[JsObject] = geos.map { geo =>
        JsObject(Seq(
          "type" -> JsString("Feature"),
          "geometry" -> geo.shape.toPlayJson(geoJsonCompatible = true),
          "properties" -> JsObject(Seq(
            "name" -> JsString(geo.adnId)
          ))
        ))
      }
      val lang = request2lang
      val storage = JsObject(Seq(
        "name"            -> JsString( Messages("ngls." + ngl.esfn)(lang) ),
        "displayOnLoad"   -> JsBoolean(true),
        "id"              -> JsNumber(ngl.id)
      ))
      val resp = JsObject(Seq(
        "type"      -> JsString("FeatureCollection"),
        "_storage"  -> storage,
        "features"  -> JsArray(features)
      ))
      Ok(resp)
    }

  }

  /** Сохранение сеттингов карты. */
  def saveMapSettingsSubmit = IsSuperuser { implicit request =>
    // TODO Stub
    val resp = JsObject(Seq(
      "url"  -> JsString( routes.Umap.getAdnNodesMap().url ),
      "info" -> JsString( "Settings ignored" ),
      "id"   -> JsNumber( -1 )
    ))
    Ok(resp)
  }

  /** Сабмит одного слоя на карте. */
  def saveMapDataLayer(ngl: NodeGeoLevel) = IsSuperuser { implicit request =>
    val resp = JsObject(Seq(
      "name"          -> JsString("Sloy 1"),
      "id"            -> JsNumber(1),
      "displayOnLoad" -> JsBoolean(true)
    ))
    Ok(resp)
  }

}
