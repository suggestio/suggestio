package controllers

import models.{NodeGeoLevel, MAdnNodeCache, MAdnNodeGeo}
import play.api.data._, Forms._
import play.api.mvc.{Result, ActionBuilder}
import util.PlayLazyMacroLogsImpl
import util.FormUtil._
import util.SiowebEsUtil.client
import util.acl.{IsSuperuserAdnGeo, IsSuperuser, IsSuperuserAdnNode}
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.geo.osm.OsmElemTypes.OsmElemType
import util.geo.osm.{OsmClientStatusCodeInvalidException, OsmClient, OsmParsers}
import views.html.sys1.market.adn.geo._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 14:43
 * Description: Контроллер для работы с географическими данными узлов. Например, закачка из osm.
 */
object SysAdnGeo extends SioController with PlayLazyMacroLogsImpl {

  import LOGGER._

  /** Маппинг формы создания/редактирования фигуры на базе OSM-геообъекта. */
  private def osmNodeFormM = Form(tuple(
    "glevel"  -> nodeGeoLevelM,
    "url"     -> urlStrM
      .transform[Option[UrlParseResult]] (
        { UrlParseResult.fromUrl },
        { _.fold("")(_.url) }
      )
      .verifying("error.unsupported", _.isDefined)
      .transform[UrlParseResult](_.get, Some.apply)
  ))


  /** Выдать страницу с географиями по узлам. */
  def forNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    MAdnNodeGeo.findByNode(adnId) map { geos =>
      Ok(forNodeTpl(request.adnNode, geos))
    }
  }


  /** Страница с созданием геофигуры на базе произвольного osm-объекта. */
  def createForNodeOsm(adnId: String) = IsSuperuserAdnNode(adnId).apply { implicit request =>
    Ok(createAdnGeoOsmTpl(osmNodeFormM, request.adnNode))
  }

  /** Сабмит формы создания фигуры на базе osm-объекта. */
  def createForNodeOsmSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    lazy val logPrefix = s"createForNodeOsmSubmit($adnId): "
    osmNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(createAdnGeoOsmTpl(formWithErrors, request.adnNode))
      },
      {case (glevel, urlPr) =>
        // Запросить у osm.org инфу по элементу
        val resFut = OsmClient.fetchElement(urlPr.osmType, urlPr.id) flatMap { osmObj =>
          // Есть объект osm. Нужно его привести к шейпу, пригодному для модели и сохранить в ней же.
          val geo   = MAdnNodeGeo(
            adnId   = adnId,
            glevel  = glevel,
            shape   = osmObj.toGeoShape,
            url     = Some(urlPr.url)
          )
          geo.save map { geoId =>
            Redirect( routes.SysAdnGeo.forNode(adnId) )
              .flashing("success" -> "Создан geo-элемент. Обновите страницу, чтобы он появился в списке.")
          }
        }
        recoverOsm(resFut, glevel, urlPr)
      }
    )
  }

  /** Повесить recover на фьючерс фетч-парсинга osm.xml чтобы вернуть админу на экран нормальную ошибку. */
  private def recoverOsm(fut: Future[Result], glevel: NodeGeoLevel, urlPr: UrlParseResult): Future[Result] = {
    fut recover {
      case ex: OsmClientStatusCodeInvalidException =>
        NotFound(s"osm.org returned unexpected http status: ${ex.statusCode} for ${urlPr.osmType.xmlUrl(urlPr.id)}")
      case ex: Exception =>
        warn("Exception occured while fetch/parsing of " + urlPr.osmType.xmlUrl(urlPr.id), ex)
        NotFound(s"Failed to fetch/parse geo element: " + ex.getClass.getSimpleName + ": " + ex.getMessage)
    }
  }

  /** Сабмит запроса на удаление элемента. */
  def deleteSubmit(geoId: String) = IsSuperuserAdnGeo(geoId).async { implicit request =>
    // Надо прочитать geo-информацию, чтобы узнать adnId. Затем удалить его и отредиректить.
    request.adnGeo.delete map { isDel =>
      val flash: (String, String) = if (isDel) {
        "success" -> "География удалена."
      } else {
        "error" -> "Географический объект не найден."
      }
      Redirect( routes.SysAdnGeo.forNode(request.adnGeo.adnId) )
        .flashing(flash)
    }
  }


  /** Рендер страницы с формой редактирования osm-производной. */
  def editNodeOsm(geoId: String) = IsSuperuserAdnGeo(geoId).async { implicit request =>
    import request.adnGeo
    val nodeFut = MAdnNodeCache.getById(adnGeo.adnId)
    val urlPr = UrlParseResult.fromUrl( adnGeo.url.get ).get
    val formFilled = osmNodeFormM.fill((adnGeo.glevel, urlPr))
    nodeFut map { nodeOpt =>
      Ok(editAdnGeoOsmTpl(adnGeo, formFilled, nodeOpt.get))
    }
  }

  /** Сабмит формы редактирования osm-производной. */
  def editNodeOsmSubmit(geoId: String) = IsSuperuserAdnGeo(geoId).async { implicit request =>
    lazy val logPrefix = s"editNodeOsmSubmit($geoId): "
    osmNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val nodeFut = MAdnNodeCache.getById(request.adnGeo.adnId)
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        nodeFut map { nodeOpt =>
          NotAcceptable(editAdnGeoOsmTpl(request.adnGeo, formWithErrors, nodeOpt.get))
        }
      },
      {case (glevel2, urlPr2) =>
        val resFut = OsmClient.fetchElement(urlPr2.osmType, urlPr2.id) flatMap { osmObj =>
          val adnGeo2 = request.adnGeo.copy(
            shape = osmObj.toGeoShape,
            glevel = glevel2,
            url = Some(urlPr2.url)
          )
          adnGeo2.save map { _geoId =>
            Redirect( routes.SysAdnGeo.forNode(request.adnGeo.adnId) )
              .flashing("success" -> "Географическая фигура обновлена.")
          }
        }
        recoverOsm(resFut, glevel2, urlPr2)
      }
    )
  }


  object UrlParseResult {
    def fromUrl(url: String): Option[UrlParseResult] = {
      val parser = new OsmParsers
      val pr = parser.parse(parser.osmBrowserUrl2TypeIdP, url)
      if (pr.successful) {
        Some(UrlParseResult(
          url = url,
          osmType = pr.get._1,
          id = pr.get._2
        ))
      } else {
        None
      }
    }
  }
  case class UrlParseResult(url: String, osmType: OsmElemType, id: Long)
}
