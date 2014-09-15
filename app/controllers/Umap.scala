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
  def getDataLayerGeoJson(ngl: NodeGeoLevel) = IsSuperuser { implicit request =>
    val feature1 = JsObject(Seq(
      "type" -> JsString("Feature"),
      "geometry" -> JsObject(Seq(
        "type" -> JsString("Polygon"),
        "coordinates" -> JsArray(Seq(JsArray(Seq(
          JsArray(Seq(JsNumber( -4.6142578125), JsNumber(69.9679672584945))),
          JsArray(Seq(JsNumber(2.6806640625), JsNumber(70.1552878601035))),
          JsArray(Seq(JsNumber(1.2744140625), JsNumber(67.9581478610158))),
          JsArray(Seq(JsNumber(-4.2626953125), JsNumber(68.00757101804))),
          JsArray(Seq(JsNumber(-4.6142578125), JsNumber(69.9679672584945)))
        ))))
      )),
      "properties" -> JsObject(Seq.empty)
    ))
    val features = JsArray(Seq(feature1))
    val storage = JsObject(Seq(
      "name" -> JsString("Узлы ADN"),
      "displayOnLoad" -> JsBoolean(true),
      "id" -> JsNumber(666)
    ))
    val resp = JsObject(Seq(
      "type"      -> JsString("FeatureCollection"),
      "_storage"  -> storage,
      "features"  -> features
    ))
    Ok(resp)
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
