package controllers

import io.suggest.model.geo.{GeoShapeQuerable, Distance, CircleGs}
import io.suggest.ym.model.common.AdnNodeGeodata
import org.elasticsearch.common.unit.DistanceUnit
import play.api.data._, Forms._
import play.api.mvc.Result
import util.PlayLazyMacroLogsImpl
import util.FormUtil._
import util.SiowebEsUtil.client
import util.acl.{IsSuperuserAdnGeo, IsSuperuser, IsSuperuserAdnNode}
import util.event.SiowebNotifier.Implicts.sn
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.geo.osm.OsmElemTypes.OsmElemType
import util.geo.osm.{OsmClientStatusCodeInvalidException, OsmClient, OsmParsers}
import views.html.sys1.market.adn.geo._
import models._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 14:43
 * Description: Контроллер для работы с географическими данными узлов. Например, закачка из osm.
 * 2014.09.10: Расширение функционала через редактирование собственной геоинформации узла.
 * Расширение собственной геоинформации необходимо из-за [[https://github.com/elasticsearch/elasticsearch/issues/7663]].
 */
object SysAdnGeo extends SioController with PlayLazyMacroLogsImpl {

  import LOGGER._

  private def glevelKM = "glevel" -> nodeGeoLevelM

  /** Маппинг формы создания/редактирования фигуры на базе OSM-геообъекта. */
  private def osmNodeFormM = Form(tuple(
    glevelKM,
    "url"     -> urlStrM
      .transform[Option[UrlParseResult]] (
        { UrlParseResult.fromUrl },
        { _.fold("")(_.url) }
      )
      .verifying("error.url.unsupported", _.isDefined)
      .transform[UrlParseResult](_.get, Some.apply)
  ))


  /** Выдать страницу с географиями по узлам. */
  def forNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    val parentsMapFut = adnIds2NodesMap(request.adnNode.geo.allParentIds)
    for {
      geos        <- MAdnNodeGeo.findByNode(adnId, withVersions = true)
      parentsMap  <- parentsMapFut
    } yield {
      Ok(forNodeTpl(request.adnNode, geos, parentsMap))
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
  def deleteSubmit(geoId: String, adnId: String) = IsSuperuserAdnGeo(geoId, adnId).async { implicit request =>
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
  def editNodeOsm(geoId: String, adnId: String) = IsSuperuserAdnGeo(geoId, adnId).async { implicit request =>
    import request.adnGeo
    val nodeFut = MAdnNodeCache.getById(adnGeo.adnId)
    val formFilled = adnGeo.url match {
      case Some(url) =>
        val urlPr = UrlParseResult.fromUrl( url ).get
        osmNodeFormM.fill((adnGeo.glevel, urlPr))
      case None =>
        osmNodeFormM
    }
    nodeFut map { nodeOpt =>
      Ok(editAdnGeoOsmTpl(adnGeo, formFilled, nodeOpt.get))
    }
  }

  /** Сабмит формы редактирования osm-производной. */
  def editNodeOsmSubmit(geoId: String, adnId: String) = IsSuperuserAdnGeo(geoId, adnId).async { implicit request =>
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


  /** Маппинг формы биндинга geo-объекта в виде круга. */
  private def circleFormM = {
    Form(mapping(
      glevelKM,
      "circle" -> circleM
    )
    {(glevel, circle) =>
      MAdnNodeGeo(
        shape = circle,
        adnId = null,
        glevel = glevel
      )
    }
    {geo =>
      geo.shape match {
        case circle: CircleGs =>
          Some((geo.glevel, circle))
        case other =>
          warn(s"circleFormM(): Unable to unbind geo shape of class ${other.getClass.getSimpleName} into circle.")
          None
      }
    })
  }


  /** Рендер страницы с формой создания круга. */
  def createCircle(adnId: String) = IsSuperuserAdnNode(adnId).apply { implicit request =>
    val form0 = circleFormM
    // Нередко в узле указана geo point, характеризующая её. Надо попытаться забиндить её в круг.
    val form1 = request.adnNode.geo.point.fold(form0) { loc =>
      val geoStub = MAdnNodeGeo(
        adnId = adnId,
        glevel = NodeGeoLevels.default,
        shape = CircleGs(loc, Distance(0.0, DistanceUnit.METERS))
      )
      form0 fill geoStub
    }
    Ok(createCircleTpl(form1, request.adnNode))
  }

  /** Сабмит формы создания круга. */
  def createCircleSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    lazy val logPrefix = s"createCircleSubmit($adnId): "
    circleFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(createCircleTpl(formWithErrors, request.adnNode))
      },
      {geoStub =>
        val geo = geoStub.copy(adnId = adnId)
        geo.save.map { geoId =>
          Redirect( routes.SysAdnGeo.forNode(adnId) )
            .flashing("success" -> "Создан круг.")
        }
      }
    )
  }


  /** Рендер страницы с формой редактирования geo-круга. */
  def editCircle(geoId: String, adnId: String) = IsSuperuserAdnGeo(geoId, adnId).async { implicit request =>
    import request.adnGeo
    val nodeOptFut = MAdnNodeCache.getById(adnGeo.adnId)
    val formBinded = circleFormM.fill(adnGeo)
    nodeOptFut map { nodeOpt =>
      Ok(editCircleTpl(adnGeo, formBinded, nodeOpt.get))
    }
  }

  /** Сабмит формы редактирования круга. */
  def editCircleSubmit(geoId: String, adnId: String) = IsSuperuserAdnGeo(geoId, adnId).async { implicit request =>
    lazy val logPrefix = s"editCircleSubmit($geoId): "
    circleFormM.bindFromRequest().fold(
      {formWithErrors =>
        val nodeOptFut = MAdnNodeCache.getById(request.adnGeo.adnId)
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        nodeOptFut.map { nodeOpt =>
          NotAcceptable(editCircleTpl(request.adnGeo, formWithErrors, nodeOpt.get))
        }
      },
      {geoStub =>
        val geo2 = request.adnGeo.copy(
          shape = geoStub.shape,
          glevel = geoStub.glevel
        )
        geo2.save map { _geoId =>
          Redirect( routes.SysAdnGeo.forNode(geo2.adnId) )
            .flashing("success" -> "Изменения сохранены.")
        }
      }
    )
  }

  /** Отрендерить geojson для валидации через geojsonlint. */
  def showGeoJson(geoId: String, adnId: String) = IsSuperuserAdnGeo(geoId, adnId).apply { implicit request =>
    Ok(request.adnGeo.shape.toPlayJson())
  }


  /** Маппинг для формы, которая описывает поле geo в модели MAdnNode. */
  private def nodeGeoFormM: Form[AdnNodeGeodata] = {
    Form(
      mapping(
        "point"         -> latLng2geopointOptM,
        "parentAdnId"   -> optional(esIdM)
      )
      {(pointOpt, parentIdOpt) => AdnNodeGeodata(pointOpt, directParentIds = parentIdOpt.toSet) }
      { angd => Some((angd.point, angd.directParentIds.headOption)) }
    )
  }

  /** Собрать карту узлов на основе списка. */
  private def adnIds2NodesMap(parentIds: TraversableOnce[String]): Future[Map[String, MAdnNode]] = {
    MAdnNodeCache.multiGet(parentIds)
      .map { nodes2nodesMap }
  }

  /** Список узлов в карту (adnId -> adnNode). */
  private def nodes2nodesMap(nodes: Iterable[MAdnNode]): Map[String, MAdnNode] = {
    nodes.iterator.map { parent => parent.id.get -> parent }.toMap
  }

  /** Сбор узлов, находящихся на указанных уровнях. TODO: Нужен радиус обнаружения. */
  private def collectNodesOnLevels(glevels: Seq[NodeGeoLevel]): Future[Seq[MAdnNode]] = {
    MAdnNodeGeo.findAdnIdsWithLevels(glevels)
      .map { _.toSet }
      .flatMap { MAdnNodeCache.multiGet }
  }

  /**
   * Рендер страницы с предложением по заполнению геоданными geo-поле узла.
   * @param adnId id узла.
   * @return 200 ок + страница с формой редактирования geo-поля узла.
   */
  def editAdnNodeGeodataPropose(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    // Запускаем поиск всех шейпов текущего узла.
    val nodeShapesFut = MAdnNodeGeo.findByNode(adnId)
    // Для parentAdnId: берем шейп на текущем уровне, затем ищем пересечение с ним на уровне (уровнях) выше.
    val parentAdnIdsFut: Future[Set[String]] = nodeShapesFut.flatMap { shapes =>
      shapes
        .filter(_.glevel.upper.isDefined)
        .find(_.shape.isInstanceOf[GeoShapeQuerable])
        .fold(Future successful Set.empty[String]) { geo =>
          val upperGlevel = geo.glevel.upper.get
          val shapeq = geo.shape.asInstanceOf[GeoShapeQuerable]
          MAdnNodeGeo.geoFindAdnIdsOnLevel(upperGlevel, shapeq, maxResults = 1)
            .map { _.toSet }
        }
    }
    val nodesMapFut = nodeShapesFut flatMap { geos =>
      geos.headOption
        .map(_.glevel.allUpperLevels)
        .filter(_.nonEmpty)
        .fold
          { Future successful Map.empty[String, MAdnNode] }
          { upperLevels => collectNodesOnLevels(upperLevels) map nodes2nodesMap }
    }
    // Предлагаем центр имеющегося круга за точку центра.
    val pointOptFut: Future[Option[GeoPoint]] = nodeShapesFut.map { geos =>
      geos.find(_.shape.isInstanceOf[CircleGs])
        .map(_.shape.asInstanceOf[CircleGs].center)
    }
    // Когда всё готово, рендерим шаблон.
    for {
      pointOpt      <- pointOptFut
      parentAdnIds  <- parentAdnIdsFut
      nodesMap      <- nodesMapFut
    } yield {
      val geo = AdnNodeGeodata(pointOpt, parentAdnIds)
      val formBinded = nodeGeoFormM.fill(geo)
      Ok(editNodeGeodataTpl(request.adnNode, formBinded, nodesMap, isProposed = true))
    }
  }

  /** Сбор возможных родительских узлов. */
  private def adnId2possibleParentsMap(adnId: String): Future[Map[String, MAdnNode]] = {
    MAdnNodeGeo.findIndexedPtrsForNode(adnId).flatMap { geoPtrs =>
      val glevels0 = geoPtrs
        .map(_.glevel)
        .headOption
        .fold[List[NodeGeoLevel]] (Nil) { _.allUpperLevels }
      // Бывает, что нет результатов.
      val glevels = if (glevels0.nonEmpty) {
        glevels0
      } else {
        List(NodeGeoLevels.NGL_TOWN, NodeGeoLevels.NGL_TOWN_DISTRICT)
      }
      collectNodesOnLevels(glevels) map nodes2nodesMap
    }
  }

  /**
   * Рендер страницы с формой редактирования geo-части adn-узла.
   * Тут по сути расширение формы обычного редактирования узла.
   * @param adnId id редактируемого узла.
   * @return 200 Ok + страница с формой редактирования узла.
   */
  def editAdnNodeGeodata(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    val nodesMapFut = adnId2possibleParentsMap(adnId)
    val formBinded = nodeGeoFormM fill request.adnNode.geo
    nodesMapFut map { nodesMap =>
      Ok(editNodeGeodataTpl(request.adnNode, formBinded, nodesMap, isProposed = false))
    }
  }

  /**
   * Сабмит формы редактирования гео-части узла.
   * @param adnId id редактируемого узла.
   * @return редирект || 406 NotAcceptable.
   */
  def editAdnNodeGeodataSubmit(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    lazy val logPrefix = s"editAdnNodeGeodataSubmit($adnId): "
    nodeGeoFormM.bindFromRequest().fold(
      {formWithErrors =>
        val nodesMapFut = adnId2possibleParentsMap(adnId)
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        nodesMapFut map { nodesMap =>
          NotAcceptable(editNodeGeodataTpl(request.adnNode, formWithErrors, nodesMap, isProposed = false))
        }
      },
      {geo2 =>
        // Нужно собрать значение для поля allParentIds, пройдясь по все родительским узлам.
        Future.traverse( geo2.directParentIds ) { parentAdnId =>
          MAdnNodeCache.getById(parentAdnId).map { parentNodeOpt =>
            parentNodeOpt.get.geo.allParentIds
          }
        }.map {
          _.reduce(_ ++ _)
        }.flatMap { allParentIds0 =>
          val allParentIds = geo2.directParentIds ++ allParentIds0
          MAdnNode.tryUpdate(request.adnNode) { adnNode =>
            val geo3 = adnNode.geo.copy(
              point           = geo2.point,
              directParentIds = geo2.directParentIds,
              allParentIds    = allParentIds
            )
            adnNode.copy(
              geo = geo3
            )
          }.map { _adnId =>
            Redirect( routes.SysAdnGeo.forNode(_adnId) )
              .flashing("success" -> "Геоданные узла обновлены.")
          }
        }
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
