package controllers

import java.time.OffsetDateTime

import com.google.inject.Inject
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.edge.{MEdgeGeoShape, MEdgeInfo, MNodeEdges}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.mgeo.MGsPtr
import models.mproj.ICommonDi
import models.msys._
import models.req.INodeReq
import org.elasticsearch.common.unit.DistanceUnit
import play.api.data._
import Forms._
import io.suggest.geo.{CircleGs, Distance, GeoShapeQuerable, MGeoPoint}
import io.suggest.model.n2.node.MNodes
import io.suggest.primo.id.OptId
import io.suggest.util.logs.MacroLogsImplLazy
import play.api.mvc.Result
import util.FormUtil._
import util.acl._
import util.geo.osm.{OsmClient, OsmClientStatusCodeInvalidException}
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
class SysAdnGeo @Inject() (
  implicit private val osmClient    : OsmClient,
  mNodes                            : MNodes,
  isSuNode                          : IsSuNode,
  override val mCommonDi            : ICommonDi
)
  extends SioControllerImpl
  with MacroLogsImplLazy
{

  // TODO Выпилить отсюда MAdnNodeGeo, использовать MNode.geo.shape

  import LOGGER._
  import mCommonDi._

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
  def forNode(adnId: String) = csrf.AddToken {
    isSuNode(adnId).async { implicit request =>
      // Сборка карты данных по родительским узлам.
      val parentsMapFut = {
        val parentIdsIter = request.mnode
          .edges
          .withPredicateIterIds( MPredicates.GeoParent )
        mNodesCache.multiGetMap(parentIdsIter)
      }

      val geos = request.mnode
        .edges
        .withPredicateIter( MPredicates.NodeLocation )
        .flatMap(_.info.geoShapes)
        .toSeq

      val mapStateHash: Option[String] = {
        request.mnode
          .geo
          .point
          .orElse {
            geos
              .headOption
              .map(_.shape.firstPoint)
          }
          .map { point =>
            val scale = geos
              .headOption
              .fold(10)(_.glevel.osmMapScale)
            "#" + scale + "/" + point.lat + "/" + point.lon
          }
      }

      for {
        parentsMap  <- parentsMapFut
      } yield {
        val rargs = MSysGeoForNodeTplArgs(
          mnode         = request.mnode,
          parentsMap    = parentsMap,
          mapStateHash  = mapStateHash
        )
        Ok( forNodeTpl(rargs) )
      }
    }
  }

  private def guessGeoLevel(implicit request: INodeReq[_]): Option[NodeGeoLevel] = {
    AdnShownTypes.node2valOpt( request.mnode )
      .flatMap( _.ngls.headOption )
  }

  /** Страница с созданием геофигуры на базе произвольного osm-объекта. */
  def createForNodeOsm(adnId: String) = csrf.AddToken {
    isSuNode(adnId).apply { implicit request =>
      val form = guessGeoLevel.fold(createOsmNodeFormM) { ngl =>
        val pr = OsmUrlParseResult("", null, -1)
        val formRes = (ngl, pr)
        createOsmNodeFormM.fill(formRes)
      }
      Ok(createAdnGeoOsmTpl(form, request.mnode))
    }
  }

  /** Сабмит формы создания фигуры на базе osm-объекта. */
  def createForNodeOsmSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      lazy val logPrefix = s"createForNodeOsmSubmit($adnId): "
      createOsmNodeFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(createAdnGeoOsmTpl(formWithErrors, request.mnode))
        },
        {case (glevel, urlPr) =>
          val resFut = for {
          // Запросить у osm.org инфу по элементу
            osmObj <- osmClient.fetchElement(urlPr.osmType, urlPr.id)

            // Попытаться сохранить новый шейп в документ узла N2.
            _  <- {
              // Есть объект osm. Нужно залить его в шейпы узла.
              val p = MPredicates.NodeLocation
              mNodes.tryUpdate(request.mnode) { mnode0 =>
                // Найти текущий эдж, если есть.
                val locEdgeOpt = mnode0.edges
                  .iterator
                  .find(_.predicate == p)

                // Найти текущие шейпы.
                val shapes0 = locEdgeOpt
                  .iterator
                  .flatMap(_.info.geoShapes)
                  .toList

                // Собрать добавляемый шейп.
                val shape1 = MEdgeGeoShape(
                  id      = MEdgeGeoShape.nextShapeId(shapes0),
                  glevel  = glevel,
                  shape   = osmObj.toGeoShape,
                  fromUrl = Some(urlPr.url)
                )

                // Закинуть шейп в кучу шейпов.
                val shapes1 = shape1 :: shapes0

                // Собрать обновлённый эдж.
                val locEdge1 = locEdgeOpt.fold [MEdge] {
                  MEdge(
                    predicate = p,
                    info = MEdgeInfo(
                      geoShapes = shapes1
                    )
                  )
                } { medge0 =>
                  medge0.copy(
                    info = medge0.info.copy(
                      geoShapes = shapes1
                    )
                  )
                }

                // Залить новый эдж в карту эджей узла
                mnode0.copy(
                  edges = mnode0.edges.copy(
                    out = {
                      val iter = mnode0.edges.withoutPredicateIter(p) ++ Iterator(locEdge1)
                      MNodeEdges.edgesToMap1(iter)
                    }
                  )
                )
              }
            }

          } yield {
            Redirect( routes.SysAdnGeo.forNode(adnId) )
              .flashing(FLASH.SUCCESS -> "Создан geo-элемент. Обновите страницу, чтобы он появился в списке.")
          }

          // Аккуратно отработать возможные ошибки
          recoverOsm(resFut, glevel, Some(urlPr))
        }
      )
    }
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
  def deleteSubmit(g: MGsPtr) = csrf.Check {
    isSuNode(g.nodeId).async { implicit request =>
      // Запустить обновление узла.
      val updFut = mNodes.tryUpdate(request.mnode) { mnode0 =>
        val p = MPredicates.NodeLocation

        val hasGs = mnode0.edges
          .withPredicateIter(p)
          .flatMap(_.info.geoShapes)
          .exists(_.id == g.gsId)
        if ( !hasGs )
          throw new NoSuchElementException("GS not found")

        val edgesLocIter = mnode0
          .edges
          .withPredicateIter(p)
          .flatMap { e =>
            val shapes1 = e.info.geoShapes.filterNot(_.id == g.gsId)
            if (shapes1.isEmpty) {
              Nil
            } else {
              Seq(
                e.copy(
                  info = e.info.copy(
                    geoShapes = shapes1
                  )
                )
              )
            }
          }

        val edgesKeepIter = mnode0
          .edges
          .withoutPredicateIter(p)

        mnode0.copy(
          edges = mnode0.edges.copy(
            out = MNodeEdges.edgesToMap1( edgesLocIter ++ edgesKeepIter )
          )
        )
      }

      // Понять, удалён шейп или нет
      val isDeletedFut = updFut
        .map(_ => true)
        .recover {
          case _: NoSuchElementException => false
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
  }

  private def _withNodeShape(gsId: Int)
                            (notFoundF: => Future[Result] = NotFound("No such geo shape"))
                            (foundF: MEdgeGeoShape => Future[Result])
                            (implicit request: INodeReq[_]): Future[Result] = {
    request.mnode
      .edges
      .withPredicateIter(MPredicates.NodeLocation)
      .flatMap(_.info.geoShapes)
      .find(_.id == gsId)
      .fold(notFoundF)(foundF)
  }

  /** Рендер страницы с формой редактирования osm-производной. */
  def editNodeOsm(g: MGsPtr) = csrf.AddToken {
    isSuNode(g.nodeId).async { implicit request =>
      _withNodeShape(g.gsId)() { mgs =>
        val urlPrOpt = mgs.fromUrl
          .flatMap { OsmUrlParseResult.fromUrl }
        val form = editOsmNodeFormM.fill( (mgs.glevel, urlPrOpt) )
        val rargs = MSysNodeGeoOsmEditTplArgs(mgs, form, request.mnode, g)
        Ok( editAdnGeoOsmTpl(rargs) )
      }
    }
  }

  /** Сабмит формы редактирования osm-производной. */
  def editNodeOsmSubmit(g: MGsPtr) = csrf.Check {
    isSuNode(g.nodeId).async { implicit request =>
      _withNodeShape(g.gsId)() { mgs =>
        lazy val logPrefix = s"editNodeOsmSubmit(${g.nodeId}#${g.gsId}): "
        editOsmNodeFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
            val rargs = MSysNodeGeoOsmEditTplArgs(mgs, formWithErrors, request.mnode, g)
            NotAcceptable( editAdnGeoOsmTpl(rargs) )
          },

          {case (glevel2, urlPrOpt) =>
            val someNow = Some( OffsetDateTime.now() )

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
                    dateEdited  = someNow
                  )
                }

              // Без ссылки - нужно немного обновить то, что уже имеется.
              case None =>
                val r = mgs.copy(
                  glevel      = glevel2,
                  dateEdited  = someNow
                )
                Future.successful(r)
            }

            // Сохранить и сгенерить результат
            val resFut = for {
            // Дождаться готовности эджа.
              mgs2 <- adnGeo2Fut

              // Обновляем ноду...
              _ <- mNodes.tryUpdate( request.mnode ) { mnode0 =>
                _nodeUpdateGeoShape(mnode0, mgs2)
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
  }


  /** Обновления ноды новым одного гео-шейпом. */
  private def _nodeUpdateGeoShape(mnode0: MNode, gs: MEdgeGeoShape): MNode = {
    mnode0.copy(
      edges = {
        val pf = { e: MEdgeGeoShape =>
          e.id == gs.id
        }
        val p = MPredicates.NodeLocation
        mnode0.edges.updateFirst { e =>
          e.predicate == p  &&  e.info.geoShapes.exists(pf)
        } { e0 =>
          Some(e0.copy(
            info = e0.info.copy(
              geoShapes = gs :: e0.info.geoShapes.filterNot(pf)
            )
          ))
        }
      }
    )
  }


  /** Маппинг формы биндинга geo-объекта в виде круга. */
  private def circleFormM: Form[MEdgeGeoShape] = {
    Form(mapping(
      glevelKM,
      "circle" -> circleM
    )
    {(glevel, circle) =>
      MEdgeGeoShape(
        id      = 1,
        glevel  = glevel,
        shape   = circle
      )
    }
    {geo =>
      val circleOpt = CircleGs.maybeFromGs(geo.shape)
      if (circleOpt.isEmpty)
        warn(s"circleFormM(): Unable to unbind geo shape of class ${geo.shape.getClass.getSimpleName} into circle.")
      for (circle <- circleOpt) yield {
        (geo.glevel, circle)
      }
    })
  }


  /** Рендер страницы с формой создания круга. */
  def createCircle(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).apply { implicit request =>
      val ngl = guessGeoLevel getOrElse NodeGeoLevels.default
      // Нередко в узле указана geo point, характеризующая её. Надо попытаться забиндить её в круг.
      val gpStub = request.mnode.geo.point
        .getOrElse( MGeoPoint(lat = 0, lon = 0) )
      val stub = MEdgeGeoShape(
        id      = -1,
        glevel  = ngl,
        shape   = CircleGs(gpStub, Distance(0.0, DistanceUnit.METERS))
      )
      val form1 = circleFormM.fill( stub )
      Ok( createCircleTpl(form1, request.mnode) )
    }
  }

  /** Сабмит формы создания круга. */
  def createCircleSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      lazy val logPrefix = s"createCircleSubmit($adnId): "
      circleFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(createCircleTpl(formWithErrors, request.mnode))
        },
        {circle0 =>
          val saveFut = mNodes.tryUpdate(request.mnode) { mnode0 =>
            val p = MPredicates.NodeLocation
            // Собрать новый эдж на базе возможно существующего.
            val edge0 = mnode0.edges
              .withPredicateIter(p)
              .toSeq
              .headOption
              .fold [MEdge] {
              MEdge(
                predicate = p,
                info = MEdgeInfo(
                  geoShapes = List(circle0)
                )
              )
            } { e0 =>
              val shapes0 = e0.info.geoShapes
              val circle1 = circle0.copy(
                id = MEdgeGeoShape.nextShapeId(shapes0)
              )
              e0.copy(
                info = e0.info.copy(
                  geoShapes = circle1 :: shapes0
                )
              )
            }

            mnode0.copy(
              edges = mnode0.edges.copy(
                out = {
                  val iter = mnode0.edges.withoutPredicateIter(p) ++ Iterator(edge0)
                  MNodeEdges.edgesToMap1(iter)
                }
              )
            )
          }
          for (_ <- saveFut) yield {
            Redirect( routes.SysAdnGeo.forNode(adnId) )
              .flashing(FLASH.SUCCESS -> "Создан круг.")
          }
        }
      )
    }
  }



  /** Рендер страницы с формой редактирования geo-круга. */
  def editCircle(g: MGsPtr) = csrf.AddToken {
    isSuNode(g.nodeId).async { implicit request =>
      _withNodeShape(g.gsId)() { mgs =>
        val formBinded = circleFormM.fill(mgs)
        _editCircleResp(g, formBinded, mgs, Ok)
      }
    }
  }

  /** Рендер формы редактирования круга. */
  private def _editCircleResp(g: MGsPtr, form: Form[MEdgeGeoShape], mgs: MEdgeGeoShape, rs: Status)
                             (implicit req: INodeReq[_]): Future[Result] = {
    val rargs = MSysNodeGeoCircleEditTplArgs(mgs, form, req.mnode, g)
    rs( editCircleTpl(rargs) )
  }

  /** Сабмит формы редактирования круга. */
  def editCircleSubmit(g: MGsPtr) = csrf.Check {
    isSuNode(g.nodeId).async { implicit request =>
      _withNodeShape(g.gsId)() { mgs =>
        lazy val logPrefix = s"editCircleSubmit(${g.nodeId}#${g.gsId}): "
        circleFormM.bindFromRequest().fold(
          {formWithErrors =>
            debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
            _editCircleResp(g, formWithErrors, mgs, NotAcceptable)
          },

          {geoStub =>
            // Собрать обновленный шейп...
            val mgs2 = mgs.copy(
              glevel      = geoStub.glevel,
              shape       = geoStub.shape,
              dateEdited  = Some( OffsetDateTime.now() )
            )

            // Обновить шейпы узла
            val updFut = mNodes.tryUpdate( request.mnode ) { mnode0 =>
              _nodeUpdateGeoShape(mnode0, mgs2)
            }

            for (_ <- updFut) yield {
              Redirect( routes.SysAdnGeo.forNode(g.nodeId) )
                .flashing(FLASH.SUCCESS -> "Changes.saved")
            }
          }
        )
      }
    }
  }


  /** Отрендерить geojson для валидации через geojsonlint. */
  def showGeoJson(g: MGsPtr) = isSuNode(g.nodeId).async { implicit request =>
    _withNodeShape(g.gsId)() { mgs =>
      Ok( mgs.shape.toPlayJson() )
    }
  }


  /** Маппинг для формы, которая описывает поле geo в модели MAdnNode. */
  private def nodeGeoFormM = {
    Form(
      tuple(
        "point"         -> geoPointOptM,
        "parentAdnId"   -> optional(esIdM)
      )
    )
  }

  /** Список узлов в карту (adnId -> adnNode). */
  private def nodes2nodesMap(nodes: Iterable[MNode]): Map[String, MNode] = {
    OptId.els2idMap[String, MNode](nodes)
  }

  /** Сбор узлов, находящихся на указанных уровнях. TODO: Нужен радиус обнаружения или сортировка по близости к какой-то точке. */
  private def collectNodesOnLevels(glevels: Seq[NodeGeoLevel]): Future[Seq[MNode]] = {
    // Собрать настройки поиска узлов:
    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          predicates = Seq( MPredicates.NodeLocation ),
          gsIntersect = Some(GsCriteria(
            levels = glevels
          ))
        )
        Seq(cr)
      }
      override def limit    = 100
    }

    for {
      nodeIds <- mNodes.dynSearchIds(msearch)
      nodes   <- mNodesCache.multiGet( nodeIds.iterator.toSet )
    } yield {
      nodes
    }
  }


  /**
    * Рендер страницы с предложением по заполнению геоданными geo-поле узла.
    *
    * @param adnId id узла.
    * @return 200 ок + страница с формой редактирования geo-поля узла.
    */
  def editAdnNodeGeodataPropose(adnId: String) = csrf.AddToken {
    isSuNode(adnId).async { implicit request =>
      // Запускаем поиск всех шейпов текущего узла.
      val shapes = request.mnode
        .edges
        .withPredicateIter(MPredicates.NodeLocation)
        .flatMap(_.info.geoShapes)
        .toSeq

      // Для parentAdnId: берем шейп на текущем уровне, затем ищем пересечение с ним на уровне (уровнях) выше.
      val parentAdnIdsFut: Future[Seq[String]] = {
        shapes
          .iterator
          .filter(_.glevel.upper.isDefined)
          .find(_.shape.isInstanceOf[GeoShapeQuerable])
          .fold [Future[Seq[String]]] (Future.successful(Nil)) { geo =>
          val shapeq = geo.shape.asInstanceOf[GeoShapeQuerable]
          val msearch = new MNodeSearchDfltImpl {
            override def limit = 1

            override def outEdges: Seq[ICriteria] = {
              val gsCr = GsCriteria(
                levels = geo.glevel.upper.toSeq,
                shapes = Seq(shapeq)
              )
              val cr = Criteria(
                predicates  = Seq( MPredicates.NodeLocation ),
                gsIntersect = Some(gsCr)
              )
              Seq(cr)
            }
          }
          for (res <- mNodes.dynSearchIds(msearch)) yield {
            res.iterator.toSeq
          }
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
      val pointOpt: Option[MGeoPoint] = {
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
        val rargs = MSysNodeGeoEditDataTplArgs(request.mnode, formBinded, nodesMap, isProposed = true)
        Ok( editNodeGeodataTpl(rargs) )
      }
    }
  }

  /** Сбор возможных родительских узлов. */
  private def adnId2possibleParentsMap(mnode: MNode): Future[Map[String, MNode]] = {
    val glevels0 = mnode
      .edges.withPredicateIter( MPredicates.NodeLocation )
      .flatMap(_.info.geoShapes)
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
    *
    * @param adnId id редактируемого узла.
   * @return 200 Ok + страница с формой редактирования узла.
   */
  def editAdnNodeGeodata(adnId: String) = csrf.AddToken {
    isSuNode(adnId).async { implicit request =>
      val directParentId: Option[String] = {
        request.mnode
          .edges
          .withPredicateIterIds( MPredicates.GeoParent.Direct )
          .toStream
          .headOption
      }
      val formBinded = nodeGeoFormM fill (request.mnode.geo.point, directParentId)
      _editAdnNodeGeodata(formBinded, Ok)
    }
  }

  private def _editAdnNodeGeodata(form: Form[_], respStatus: Status)
                                 (implicit request: INodeReq[_]): Future[Result] = {
    val nodesMapFut = adnId2possibleParentsMap(request.mnode)
    nodesMapFut map { nodesMap =>
      val rargs = MSysNodeGeoEditDataTplArgs(request.mnode, form, nodesMap, isProposed = false)
      respStatus( editNodeGeodataTpl(rargs) )
    }
  }

  /**
   * Сабмит формы редактирования гео-части узла.
    *
    * @param adnId id редактируемого узла.
   * @return редирект || 406 NotAcceptable.
   */
  def editAdnNodeGeodataSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      lazy val logPrefix = s"editAdnNodeGeodataSubmit($adnId): "
      nodeGeoFormM.bindFromRequest().fold(
        {formWithErrors =>
          debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          _editAdnNodeGeodata(formWithErrors, NotAcceptable)
        },
        {case (pointOpt, parentNodeIdOpt) =>
          for {
          // Нужно собрать значение для поля allParentIds, пройдясь по все родительским узлам.
            parentParentIds0 <- {
              parentNodeIdOpt.fold {
                Future.successful( Set.empty[String] )
              } { parentNodeId =>
                for {
                  Some(parentNode) <- mNodesCache.getById(parentNodeId)
                } yield {
                  parentNode.edges
                    .withPredicateIterIds( MPredicates.GeoParent )
                    .toSet
                }

              }
            }
            // Подготовить данные, обновить узел.
            _ <- {
              val allParentIds = parentParentIds0 ++ parentNodeIdOpt
              val parentEdges = {
                val p = MPredicates.GeoParent
                allParentIds.iterator
                  .map { parentId =>
                    MEdge(
                      predicate = p,
                      nodeIds   = Set(parentId)
                    )
                  }
                  .toStream
              }
              // Запуск апдейта новыми геоданными
              mNodes.tryUpdate(request.mnode) { mnode =>
                mnode.copy(
                  geo = mnode.geo.copy(
                    point = pointOpt
                  ),
                  edges = mnode.edges.copy(
                    out = {
                      val parenNodeEdgesIter = parentNodeIdOpt
                        .iterator
                        .map { parentNodeId =>
                          MEdge(
                            predicate = MPredicates.GeoParent.Direct,
                            nodeIds   = parentNodeIdOpt.toSet
                          )
                        }
                      val iter = mnode.edges.withoutPredicateIter( MPredicates.GeoParent ) ++
                        parentEdges.iterator ++
                        parenNodeEdgesIter
                      MNodeEdges.edgesToMap1( iter )
                    }
                  )
                )
              }
            }

          } yield {
            Redirect( routes.SysAdnGeo.forNode(adnId) )
              .flashing(FLASH.SUCCESS -> "Геоданные узла обновлены.")
          }
        }
      )
    }
  }

}
