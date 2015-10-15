package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.geo.{GeoShapeQuerable, Distance, CircleGs}
import io.suggest.ym.model.common.AdnNodeGeodata
import models.msys.OsmUrlParseResult
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.DistanceUnit
import play.api.data._, Forms._
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import util.PlayLazyMacroLogsImpl
import util.FormUtil._
import util.acl._
import util.geo.osm.{OsmClientStatusCodeInvalidException, OsmClient}
import views.html.sys1.market.adn.geo._
import models._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 14:43
 * Description: Контроллер для работы с географическими данными узлов. Например, закачка из osm.
 * 2014.09.10: Расширение функционала через редактирование собственной геоинформации узла.
 * Расширение собственной геоинформации необходимо из-за [[https://github.com/elasticsearch/elasticsearch/issues/7663]].
 */
class SysAdnGeo @Inject() (
  override val messagesApi        : MessagesApi,
  implicit val osmClient          : OsmClient,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayLazyMacroLogsImpl
  with IsSuperuserAdnGeo
  with IsSuperuserAdnNode
{

  import LOGGER._

  private def glevelKM = "glevel" -> nodeGeoLevelM

  private def osmUrlOptM: Mapping[Option[OsmUrlParseResult]] = {
    urlStrOptM
      .transform[Option[OsmUrlParseResult]] (
        { _.flatMap(OsmUrlParseResult.fromUrl) },
        { _.map(_.url) }
      )
  }

  /** Маппинг формы создания/редактирования фигуры на базе OSM-геообъекта. */
  private def createOsmNodeFormM = Form(tuple(
    glevelKM,
    "url"     -> osmUrlOptM
      .verifying("error.url.unsupported", _.isDefined)
      .transform[OsmUrlParseResult](_.get, Some.apply)
  ))

  private def editOsmNodeFormM = Form(tuple(
    glevelKM,
    "url" -> osmUrlOptM
  ))


  /** Выдать страницу с географиями по узлам. */
  def forNode(adnId: String) = IsSuperuserAdnNode(adnId).async { implicit request =>
    val parentsMapFut = adnIds2NodesMap(request.adnNode.geo.allParentIds)
    for {
      geos        <- MAdnNodeGeo.findByNode(adnId, withVersions = true)
      parentsMap  <- parentsMapFut
    } yield {
      val mapStateHash: Option[String] = {
        request.adnNode.geo.point.orElse(geos.headOption.map(_.shape.firstPoint)).map { point =>
          val scale = geos.headOption.map { _.glevel.osmMapScale }.getOrElse(10)
          "#" + scale + "/" + point.lat + "/" + point.lon
        }
      }
      Ok(forNodeTpl(request.adnNode, geos, parentsMap, mapStateHash))
    }
  }

  private def guessGeoLevel(implicit request: AbstractRequestForAdnNode[_]): Option[NodeGeoLevel] = {
    AdnShownTypes.maybeWithName( request.adnNode.adn.shownTypeId )
      .flatMap( _.ngls.headOption )
  }

  /** Страница с созданием геофигуры на базе произвольного osm-объекта. */
  def createForNodeOsm(adnId: String) = IsSuperuserAdnNodeGet(adnId).apply { implicit request =>
    val form = guessGeoLevel
      .fold(createOsmNodeFormM) { ngl => createOsmNodeFormM.fill((ngl, OsmUrlParseResult("", null, -1))) }
    Ok(createAdnGeoOsmTpl(form, request.adnNode))
  }

  /** Сабмит формы создания фигуры на базе osm-объекта. */
  def createForNodeOsmSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
    lazy val logPrefix = s"createForNodeOsmSubmit($adnId): "
    createOsmNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(createAdnGeoOsmTpl(formWithErrors, request.adnNode))
      },
      {case (glevel, urlPr) =>
        // Запросить у osm.org инфу по элементу
        val resFut = osmClient.fetchElement(urlPr.osmType, urlPr.id) flatMap { osmObj =>
          // Есть объект osm. Нужно его привести к шейпу, пригодному для модели и сохранить в ней же.
          val geo   = MAdnNodeGeo(
            adnId   = adnId,
            glevel  = glevel,
            shape   = osmObj.toGeoShape,
            url     = Some(urlPr.url)
          )
          geo.save map { geoId =>
            Redirect( routes.SysAdnGeo.forNode(adnId) )
              .flashing(FLASH.SUCCESS -> "Создан geo-элемент. Обновите страницу, чтобы он появился в списке.")
          }
        }
        recoverOsm(resFut, glevel, Some(urlPr))
      }
    )
  }

  /** Повесить recover на фьючерс фетч-парсинга osm.xml чтобы вернуть админу на экран нормальную ошибку. */
  private def recoverOsm(fut: Future[Result], glevel: NodeGeoLevel, urlPrOpt: Option[OsmUrlParseResult]): Future[Result] = {
    fut recover { case ex: Exception =>
      val rest = urlPrOpt.fold("-") { urlPr =>
        urlPr.osmType.xmlUrl(urlPr.id)
      }
      ex match {
        case ocex: OsmClientStatusCodeInvalidException =>
          NotFound(s"osm.org returned unexpected http status: ${ocex.statusCode} for $rest")
        case _ =>
          warn("Exception occured while fetch/parsing of " + rest, ex)
          NotFound(s"Failed to fetch/parse geo element: " + ex.getClass.getSimpleName + ": " + ex.getMessage)
      }
    }
  }

  /** Сабмит запроса на удаление элемента. */
  def deleteSubmit(geoId: String, adnId: String) = IsSuperuserAdnGeoPost(geoId, adnId).async { implicit request =>
    // Надо прочитать geo-информацию, чтобы узнать adnId. Затем удалить его и отредиректить.
    request.adnGeo.delete map { isDel =>
      val flash: (String, String) = if (isDel) {
        FLASH.SUCCESS -> "География удалена."
      } else {
        FLASH.ERROR   -> "Географический объект не найден."
      }
      Redirect( routes.SysAdnGeo.forNode(request.adnGeo.adnId) )
        .flashing(flash)
    }
  }


  /** Рендер страницы с формой редактирования osm-производной. */
  def editNodeOsm(geoId: String, adnId: String) = IsSuperuserAdnGeoGet(geoId, adnId).async { implicit request =>
    import request.adnGeo
    val nodeFut = MAdnNodeCache.getById(adnGeo.adnId)
    val form = {
      val urlPrOpt = adnGeo.url
        .flatMap { OsmUrlParseResult.fromUrl }
      editOsmNodeFormM.fill( (adnGeo.glevel, urlPrOpt) )
    }
    nodeFut map { nodeOpt =>
      Ok(editAdnGeoOsmTpl(adnGeo, form, nodeOpt.get))
    }
  }

  /** Сабмит формы редактирования osm-производной. */
  def editNodeOsmSubmit(geoId: String, adnId: String) = IsSuperuserAdnGeoPost(geoId, adnId).async { implicit request =>
    lazy val logPrefix = s"editNodeOsmSubmit($geoId): "
    editOsmNodeFormM.bindFromRequest().fold(
      {formWithErrors =>
        val nodeFut = MAdnNodeCache.getById(request.adnGeo.adnId)
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        nodeFut map { nodeOpt =>
          NotAcceptable(editAdnGeoOsmTpl(request.adnGeo, formWithErrors, nodeOpt.get))
        }
      },
      {case (glevel2, urlPrOpt) =>
        val adnGeo2Fut = urlPrOpt match {
          // Админ задал новую ссылку для скачивания контура.
          case Some(urlPr2) =>
            osmClient.fetchElement(urlPr2.osmType, urlPr2.id) map { osmObj =>
              request.adnGeo.copy(
                shape   = osmObj.toGeoShape,
                glevel  = glevel2,
                url     = Some(urlPr2.url)
              )
            }
          // Без ссылки - нужно немного обновить то, что уже имеется.
          case None =>
            val r = request.adnGeo.copy(glevel = glevel2)
            Future successful r
        }
        // Сохранить и сгенерить результат
        val resFut = for {
          adnGeo2 <- adnGeo2Fut
          _geoId  <- adnGeo2.save
        } yield {
          Redirect( routes.SysAdnGeo.forNode(request.adnGeo.adnId) )
            .flashing(FLASH.SUCCESS -> "Географическая фигура обновлена.")
        }
        // Повесить recover() для перехвата ошибок
        recoverOsm(resFut, glevel2, urlPrOpt)
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
  def createCircle(adnId: String) = IsSuperuserAdnNodeGet(adnId).apply { implicit request =>
    val ngl = guessGeoLevel getOrElse NodeGeoLevels.default
    // Нередко в узле указана geo point, характеризующая её. Надо попытаться забиндить её в круг.
    val gpStub = request.adnNode.geo.point getOrElse GeoPoint(0, 0)
    val geoStub = MAdnNodeGeo(
      adnId = adnId,
      glevel = ngl,
      shape = CircleGs(gpStub, Distance(0.0, DistanceUnit.METERS))
    )
    val form1 = circleFormM fill geoStub
    Ok(createCircleTpl(form1, request.adnNode))
  }

  /** Сабмит формы создания круга. */
  def createCircleSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
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
            .flashing(FLASH.SUCCESS -> "Создан круг.")
        }
      }
    )
  }


  /** Рендер страницы с формой редактирования geo-круга. */
  def editCircle(geoId: String, adnId: String) = IsSuperuserAdnGeoGet(geoId, adnId).async { implicit request =>
    import request.adnGeo
    val nodeOptFut = MAdnNodeCache.getById(adnGeo.adnId)
    val formBinded = circleFormM.fill(adnGeo)
    nodeOptFut map { nodeOpt =>
      Ok(editCircleTpl(adnGeo, formBinded, nodeOpt.get))
    }
  }

  /** Сабмит формы редактирования круга. */
  def editCircleSubmit(geoId: String, adnId: String) = IsSuperuserAdnGeoPost(geoId, adnId).async { implicit request =>
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
            .flashing(FLASH.SUCCESS -> "Изменения сохранены.")
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
      .flatMap { MAdnNodeCache.multiGet(_) }
  }

  /**
   * Рендер страницы с предложением по заполнению геоданными geo-поле узла.
   * @param adnId id узла.
   * @return 200 ок + страница с формой редактирования geo-поля узла.
   */
  def editAdnNodeGeodataPropose(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
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
  def editAdnNodeGeodata(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
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
  def editAdnNodeGeodataSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
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
        }.map { idsSets =>
          if (idsSets.nonEmpty) {
            idsSets.reduce(_ ++ _)
          } else {
            Set.empty
          }
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
              .flashing(FLASH.SUCCESS -> "Геоданные узла обновлены.")
          }
        }
      }
    )
  }

}
