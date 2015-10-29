package controllers

import com.google.inject.Inject
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.geo.{GeoShapeQuerable, Distance, CircleGs}
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.geo.MGeoShape
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.mgeo.MGsPtr
import models.msys._
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.DistanceUnit
import org.joda.time.DateTime
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
  implicit val osmClient          : OsmClient,
  override val mNodeCache         : MAdnNodeCache,
  override val _contextFactory    : Context2Factory,
  override val errorHandler       : ErrorHandler,
  override val messagesApi        : MessagesApi,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends SioControllerImpl
  with PlayLazyMacroLogsImpl
  with IsSuperuserAdnNode
{

  // TODO Выпилить отсюда MAdnNodeGeo, использовать MNode.geo.shape

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
    // Сборка карты данных по родительским узлам.
    val parentsMapFut = {
      val parentIdsIter = request.adnNode
        .edges
        .withPredicateIterIds( MPredicates.GeoParent )
      mNodeCache.multiGetMap(parentIdsIter)
    }

    val geos = request.adnNode
      .geo
      .shapes

    val mapStateHash: Option[String] = {
      request.adnNode
        .geo
        .point
        .orElse {
          geos.headOption
            .map(_.shape.firstPoint)
        }
        .map { point =>
          val scale = geos.headOption
            .map { _.glevel.osmMapScale }
            .getOrElse(10)
          "#" + scale + "/" + point.lat + "/" + point.lon
        }
    }

    for {
      parentsMap  <- parentsMapFut
    } yield {
      val rargs = MSysGeoForNodeTplArgs(
        mnode         = request.adnNode,
        parentsMap    = parentsMap,
        mapStateHash  = mapStateHash
      )
      Ok( forNodeTpl(rargs) )
    }
  }

  private def guessGeoLevel(implicit request: AbstractRequestForAdnNode[_]): Option[NodeGeoLevel] = {
    AdnShownTypes.node2valOpt( request.adnNode )
      .flatMap( _.ngls.headOption )
  }

  /** Страница с созданием геофигуры на базе произвольного osm-объекта. */
  def createForNodeOsm(adnId: String) = IsSuperuserAdnNodeGet(adnId).apply { implicit request =>
    val form = guessGeoLevel.fold(createOsmNodeFormM) { ngl =>
      val pr = OsmUrlParseResult("", null, -1)
      val formRes = (ngl, pr)
      createOsmNodeFormM.fill(formRes)
    }
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
        val resFut = for {
          // Запросить у osm.org инфу по элементу
          osmObj <- osmClient.fetchElement(urlPr.osmType, urlPr.id)
          // Попытаться сохранить новый шейп в документ узла N2.
          _  <- {
            // Есть объект osm. Нужно залить его в шейпы узла.
            MNode.tryUpdate(request.adnNode) { mnode0 =>
              mnode0.copy(
                geo = mnode0.geo.copy(
                  shapes = {
                    val shapes0 = mnode0.geo.shapes
                    val mshape = MGeoShape(
                      id      = mnode0.geo.nextShapeId,
                      glevel  = glevel,
                      shape   = osmObj.toGeoShape,
                      fromUrl = Some(urlPr.url)
                    )
                    shapes0 ++ Seq(mshape)
                  }
                )
              )
            }
          }
        } yield {
          Redirect( routes.SysAdnGeo.forNode(adnId) )
            .flashing(FLASH.SUCCESS -> "Создан geo-элемент. Обновите страницу, чтобы он появился в списке.")
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
      val respBody = ex match {
        case ocex: OsmClientStatusCodeInvalidException =>
          s"osm.org returned unexpected http status: ${ocex.statusCode} for $rest"
        case _ =>
          warn("Exception occured while fetch/parsing of " + rest, ex)
          s"Failed to fetch/parse geo element: " + ex.getClass.getSimpleName + ": " + ex.getMessage
      }
      NotFound(respBody)
    }
  }


  /** Сабмит запроса на удаление элемента. */
  def deleteSubmit(g: MGsPtr) = IsSuperuserAdnNodePost(g.nodeId).async { implicit request =>
    // Запустить обновление узла.
    val updFut = MNode.tryUpdate(request.adnNode) { mnode0 =>
      val shapes0 = mnode0.geo.shapes
      val (found, shapes1) = shapes0.partition(_.id == g.gsId)
      // Если ничего не удалено, то вернуть исключение.
      if (found.isEmpty) {
        throw new NoSuchElementException(s"Shape ${g.gsId} not exists on node ${g.nodeId}")
      } else {
        mnode0.copy(
          geo = mnode0.geo.copy(
            shapes = shapes1
          )
        )
      }
    }

    // Понять, удалён шейп или нет
    val isDeletedFut = updFut
      .map(_ => true)
      .recover {
        case ex: NoSuchElementException => false
      }

    // Отредиректить юзера на страницу со списком узлов, когда всё будет готово.
    for {
      isDeleted  <- isDeletedFut
    } yield {
      val flash: (String, String) = if (isDeleted) {
        FLASH.SUCCESS -> "География удалена."
      } else {
        FLASH.ERROR   -> "Географический объект не найден."
      }
      Redirect( routes.SysAdnGeo.forNode( g.nodeId ) )
        .flashing(flash)
    }
  }

  private def _withNodeShape(gsId: Int)
                            (notFoundF: => Future[Result] = NotFound("No such geo shape"))
                            (foundF: MGeoShape => Future[Result])
                            (implicit request: RequestForAdnNodeAdm[_]): Future[Result] = {
    request.adnNode
      .geo
      .findShape(gsId)
      .fold(notFoundF)(foundF)
  }

  /** Рендер страницы с формой редактирования osm-производной. */
  def editNodeOsm(g: MGsPtr) = IsSuperuserAdnNodeGet(g.nodeId).async { implicit request =>
    _withNodeShape(g.gsId)() { mgs =>
      val urlPrOpt = mgs.fromUrl
        .flatMap { OsmUrlParseResult.fromUrl }
      val form = editOsmNodeFormM.fill( (mgs.glevel, urlPrOpt) )
      val rargs = MSysNodeGeoOsmEditTplArgs(mgs, form, request.adnNode, g)
      Ok( editAdnGeoOsmTpl(rargs) )
    }
  }

  /** Сабмит формы редактирования osm-производной. */
  def editNodeOsmSubmit(g: MGsPtr) = IsSuperuserAdnNodePost(g.nodeId).async { implicit request =>
    _withNodeShape(g.gsId)() { mgs =>
      lazy val logPrefix = s"editNodeOsmSubmit(${g.nodeId}#${g.gsId}): "
      editOsmNodeFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          val rargs = MSysNodeGeoOsmEditTplArgs(mgs, formWithErrors, request.adnNode, g)
          NotAcceptable( editAdnGeoOsmTpl(rargs) )
        },
        {case (glevel2, urlPrOpt) =>
          val now = DateTime.now()
          val adnGeo2Fut = urlPrOpt match {
            // Админ задал новую ссылку для скачивания контура.
            case Some(urlPr2) =>
              for {
                osmObj <- osmClient.fetchElement(urlPr2.osmType, urlPr2.id)
              } yield {
                mgs.copy(
                  shape       = osmObj.toGeoShape,
                  glevel      = glevel2,
                  fromUrl     = Some(urlPr2.url),
                  dateEdited  = now
                )
              }
            // Без ссылки - нужно немного обновить то, что уже имеется.
            case None =>
              val r = mgs.copy(
                glevel      = glevel2,
                dateEdited  = now
              )
              Future successful r
          }
          // Сохранить и сгенерить результат
          val resFut = for {
            mgs2 <- adnGeo2Fut
            _    <- {
              MNode.tryUpdate( request.adnNode ) { mnode0 =>
                mnode0.copy(
                  geo = mnode0.geo.updateShape(mgs2)
                )
              }
            }
          } yield {
            Redirect( routes.SysAdnGeo.forNode(g.nodeId) )
              .flashing(FLASH.SUCCESS -> "Географическая фигура обновлена.")
          }
          // Повесить recover() для перехвата ошибок
          recoverOsm(resFut, glevel2, urlPrOpt)
        }
      )
    }
  }


  /** Маппинг формы биндинга geo-объекта в виде круга. */
  private def circleFormM = {
    Form(mapping(
      glevelKM,
      "circle" -> circleM
    )
    {(glevel, circle) =>
      MGeoShape(
        id = -1,
        glevel = glevel,
        shape = circle
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
  def createCircle(nodeId: String) = IsSuperuserAdnNodeGet(nodeId).apply { implicit request =>
    val ngl = guessGeoLevel getOrElse NodeGeoLevels.default
    // Нередко в узле указана geo point, характеризующая её. Надо попытаться забиндить её в круг.
    val gpStub = request.adnNode.geo.point getOrElse GeoPoint(0, 0)
    val stub = MGeoShape(
      id      = -1,
      glevel  = ngl,
      shape   = CircleGs(gpStub, Distance(0.0, DistanceUnit.METERS))
    )
    val form1 = circleFormM.fill( stub )
    Ok( createCircleTpl(form1, request.adnNode) )
  }

  /** Сабмит формы создания круга. */
  def createCircleSubmit(adnId: String) = IsSuperuserAdnNodePost(adnId).async { implicit request =>
    lazy val logPrefix = s"createCircleSubmit($adnId): "
    circleFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        NotAcceptable(createCircleTpl(formWithErrors, request.adnNode))
      },
      {circle0 =>
        val saveFut = MNode.tryUpdate(request.adnNode) { mnode0 =>
          mnode0.copy(
            geo = mnode0.geo.copy(
              shapes = {
                val circle1 = circle0.copy(
                  id = mnode0.geo.nextShapeId
                )
                mnode0.geo.shapes ++ Seq(circle1)
              }
            )
          )
        }
        for {
          _ <- saveFut
        } yield {
          Redirect( routes.SysAdnGeo.forNode(adnId) )
            .flashing(FLASH.SUCCESS -> "Создан круг.")
        }
      }
    )
  }


  /** Рендер страницы с формой редактирования geo-круга. */
  def editCircle(g: MGsPtr) = IsSuperuserAdnNodeGet(g.nodeId).async { implicit request =>
    _withNodeShape(g.gsId)() { mgs =>
      val formBinded = circleFormM.fill(mgs)
      val rargs = MSysNodeGeoCircleEditTplArgs(mgs, formBinded, request.adnNode, g)
      Ok( editCircleTpl(rargs) )
    }
  }

  /** Сабмит формы редактирования круга. */
  def editCircleSubmit(g: MGsPtr) = IsSuperuserAdnNodePost(g.nodeId).async { implicit request =>
    _withNodeShape(g.gsId)() { mgs =>
      lazy val logPrefix = s"editCircleSubmit(${g.nodeId}#${g.gsId}): "
      circleFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          val rargs = MSysNodeGeoCircleEditTplArgs(mgs, formWithErrors, request.adnNode, g)
          NotAcceptable( editCircleTpl(rargs) )
        },
        {geoStub =>
          val mgs2 = mgs.copy(
            glevel      = geoStub.glevel,
            shape       = geoStub.shape,
            dateEdited  = DateTime.now()
          )
          val updFut = MNode.tryUpdate( request.adnNode ) { mnode0 =>
            mnode0.copy(
              geo = mnode0.geo.updateShape(mgs2)
            )
          }
          for {
            _ <- updFut
          } yield {
            Redirect( routes.SysAdnGeo.forNode(g.nodeId) )
              .flashing(FLASH.SUCCESS -> "Changes.saved")
          }
        }
      )
    }
  }


  /** Отрендерить geojson для валидации через geojsonlint. */
  def showGeoJson(g: MGsPtr) = IsSuperuserAdnNode(g.nodeId).async { implicit request =>
    _withNodeShape(g.gsId)() { mgs =>
      Ok( mgs.shape.toPlayJson() )
    }
  }


  /** Маппинг для формы, которая описывает поле geo в модели MAdnNode. */
  private def nodeGeoFormM = {
    Form(
      tuple(
        "point"         -> latLng2geopointOptM,
        "parentAdnId"   -> optional(esIdM)
      )
    )
  }

  /** Список узлов в карту (adnId -> adnNode). */
  private def nodes2nodesMap(nodes: Iterable[MNode]): Map[String, MNode] = {
    nodes.iterator.map { parent => parent.id.get -> parent }.toMap
  }

  /** Сбор узлов, находящихся на указанных уровнях. TODO: Нужен радиус обнаружения или сортировка по близости к какой-то точке. */
  private def collectNodesOnLevels(glevels: Seq[NodeGeoLevel]): Future[Seq[MNode]] = {
    val msearch = new MNodeSearchDfltImpl {
      override def gsLevels = glevels
      override def limit    = 100
    }
    for {
      nodeIds <- MNode.dynSearchIds(msearch)
      nodes   <- mNodeCache.multiGet( nodeIds.toSet )
    } yield {
      nodes
    }
  }

  /**
   * Рендер страницы с предложением по заполнению геоданными geo-поле узла.
   * @param adnId id узла.
   * @return 200 ок + страница с формой редактирования geo-поля узла.
   */
  def editAdnNodeGeodataPropose(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
    // Запускаем поиск всех шейпов текущего узла.
    val shapes = request.adnNode.geo.shapes

    // Для parentAdnId: берем шейп на текущем уровне, затем ищем пересечение с ним на уровне (уровнях) выше.
    val parentAdnIdsFut: Future[Seq[String]] = {
      shapes
        .iterator
        .filter(_.glevel.upper.isDefined)
        .find(_.shape.isInstanceOf[GeoShapeQuerable])
        .fold [Future[Seq[String]]] (Future successful Nil) { geo =>
          val shapeq = geo.shape.asInstanceOf[GeoShapeQuerable]
          val msearch = new MNodeSearchDfltImpl {
            override def limit = 1
            override def gsShapes = Seq(shapeq)
            override def gsLevels = geo.glevel.upper.toSeq
          }
          MNode.dynSearchIds(msearch)
        }
    }

    val nodesMapFut = {
      shapes.headOption
        .map(_.glevel.allUpperLevels)
        .filter(_.nonEmpty)
        .fold
          { Future successful Map.empty[String, MNode] }
          { upperLevels => collectNodesOnLevels(upperLevels) map nodes2nodesMap }
    }

    // Предлагаем центр имеющегося круга за точку центра.
    val pointOpt: Option[GeoPoint] = {
      shapes
        .find(_.shape.isInstanceOf[CircleGs])
        .map(_.shape.asInstanceOf[CircleGs].center)
    }

    // Когда всё готово, рендерим шаблон.
    for {
      parentAdnIds  <- parentAdnIdsFut
      nodesMap      <- nodesMapFut
    } yield {
      val formValue = (pointOpt, parentAdnIds.headOption)
      val formBinded = nodeGeoFormM.fill( formValue )
      val rargs = MSysNodeGeoEditDataTplArgs(request.adnNode, formBinded, nodesMap, isProposed = true)
      Ok( editNodeGeodataTpl(rargs) )
    }
  }

  /** Сбор возможных родительских узлов. */
  private def adnId2possibleParentsMap(mnode: MNode): Future[Map[String, MNode]] = {
    val glevels0 = mnode
      .geo.shapes
      .iterator
      .map(_.glevel)
      .toSeq
      .headOption
      .fold (List.empty[NodeGeoLevel]) { _.allUpperLevels }
    // Бывает, что нет результатов.
    val glevels = if (glevels0.nonEmpty) {
      glevels0
    } else {
      List(NodeGeoLevels.NGL_TOWN, NodeGeoLevels.NGL_TOWN_DISTRICT)
    }
    collectNodesOnLevels(glevels) map nodes2nodesMap
  }

  /**
   * Рендер страницы с формой редактирования geo-части adn-узла.
   * Тут по сути расширение формы обычного редактирования узла.
   * @param adnId id редактируемого узла.
   * @return 200 Ok + страница с формой редактирования узла.
   */
  def editAdnNodeGeodata(adnId: String) = IsSuperuserAdnNodeGet(adnId).async { implicit request =>
    val directParentId: Option[String] = {
      request.adnNode
        .edges
        .withPredicateIterIds( MPredicates.GeoParent.Direct )
        .toStream
        .headOption
    }
    val formBinded = nodeGeoFormM fill (request.adnNode.geo.point, directParentId)
    _editAdnNodeGeodata(formBinded, Ok)
  }

  private def _editAdnNodeGeodata(form: Form[_], respStatus: Status)
                                 (implicit request: RequestForAdnNodeAdm[_]): Future[Result] = {
    val nodesMapFut = adnId2possibleParentsMap(request.adnNode)
    nodesMapFut map { nodesMap =>
      val rargs = MSysNodeGeoEditDataTplArgs(request.adnNode, form, nodesMap, isProposed = false)
      respStatus( editNodeGeodataTpl(rargs) )
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
        debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
        _editAdnNodeGeodata(formWithErrors, NotAcceptable)
      },
      {case (pointOpt, parentNodeIdOpt) =>
        // Нужно собрать значение для поля allParentIds, пройдясь по все родительским узлам.
        parentNodeIdOpt.fold {
          Future successful Set.empty[String]
        } { parentNodeId =>
          for {
            Some(parentNode) <- mNodeCache.getById(parentNodeId)
          } yield {
            parentNode.edges
              .withPredicateIterIds( MPredicates.GeoParent )
              .toSet
          }

        }.flatMap { parentParentIds0 =>
          val allParentIds = parentParentIds0 ++ parentNodeIdOpt
          val parentEdges = {
            val p = MPredicates.GeoParent
            allParentIds.iterator
              .map { parentId => MEdge(p, parentId) }
              .toStream
          }
          MNode.tryUpdate(request.adnNode) { mnode =>
            mnode.copy(
              geo = mnode.geo.copy(
                point = pointOpt
              ),
              edges = mnode.edges.copy(
                out = {
                  val iter = mnode.edges.withoutPredicateIter( MPredicates.GeoParent ) ++
                    parentEdges.iterator ++
                    parentNodeIdOpt.iterator.map( MEdge(MPredicates.GeoParent.Direct, _) )
                  MNodeEdges.edgesToMap1( iter )
                }
              )
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
