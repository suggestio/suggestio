package controllers

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
    Ok(mapBaseTpl())
  }

  def getDatalayerGeojson = IsSuperuser { implicit request =>
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

  def saveMapSettingsSubmit = IsSuperuser { implicit request =>
    Ok("STUB")
  }

  def saveMapDataLayers = IsSuperuser { implicit request =>
    Ok("STUB")
  }

}
