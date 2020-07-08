package controllers

import java.time.OffsetDateTime

import io.suggest.es.model.EsModel
import io.suggest.geo._
import io.suggest.n2.edge._
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.util.logs.MacroLogsImplLazy
import javax.inject.Inject
import models.madn.AdnShownTypes
import models.mgeo.MGsPtr
import models.mproj.ICommonDi
import models.msys._
import models.req.INodeReq
import play.api.data.Forms._
import play.api.data._
import play.api.mvc.{RequestHeader, Result}
import util.FormUtil._
import util.acl._
import util.geo.osm.{OsmClient, OsmClientStatusCodeInvalidException}
import views.html.sys1.market.adn.geo._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.09.14 14:43
 * Description: Контроллер для работы с географическими данными узлов. Например, закачка из osm.
 * 2014.09.10: Расширение функционала через редактирование собственной геоинформации узла.
 * Расширение собственной геоинформации необходимо из-за [[https://github.com/elasticsearch/elasticsearch/issues/7663]].
 */
final class SysAdnGeo @Inject() (
                                  sioControllerApi                  : SioControllerApi,
                                  mCommonDi                         : ICommonDi,
                                )
  extends MacroLogsImplLazy
{

  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val isSuNode = injector.instanceOf[IsSuNode]
  private lazy val osmClient = injector.instanceOf[OsmClient]

  import sioControllerApi._
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
    isSuNode(adnId) { implicit request =>
      val geos = request.mnode
        .edges
        .withPredicateIter( MPredicates.NodeLocation )
        .flatMap(_.info.geoShapes)
        .toSeq

      val mapStateHash: Option[String] = {
        for (geo <- geos.headOption) yield {
          val point = geo.shape.firstPoint
          val scale = geos
            .headOption
            .fold(10)(_.glevel.osmMapScale)
          "#" + scale + "/" + point.lat + "/" + point.lon
        }
      }

      val rargs = MSysGeoForNodeTplArgs(
        mnode         = request.mnode,
        mapStateHash  = mapStateHash
      )
      Ok( forNodeTpl(rargs) )
    }
  }

  private def guessGeoLevel(implicit request: INodeReq[_]): Option[MNodeGeoLevel] = {
    AdnShownTypes.node2valOpt( request.mnode )
      .flatMap( _.ngls.headOption )
  }

  /** Страница с созданием геофигуры на базе произвольного osm-объекта. */
  def createForNodeOsm(adnId: String) = csrf.AddToken {
    isSuNode(adnId) { implicit request =>
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
          LOGGER.debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(createAdnGeoOsmTpl(formWithErrors, request.mnode))
        },
        {case (glevel, urlPr) =>
          val resFut = for {
          // Запросить у osm.org инфу по элементу
            osmObj <- osmClient.fetchElement(urlPr.osmType, urlPr.id)

            // Попытаться сохранить новый шейп в документ узла N2.
            _  <- {
              import esModel.api._

              // Есть объект osm. Нужно залить его в шейпы узла.
              val p = MPredicates.NodeLocation
              mNodes.tryUpdate(request.mnode) { mnode0 =>
                // Найти текущий эдж, если есть.
                val locEdgeOpt = mnode0.edges
                  .out
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
                  MEdge.info
                    .composeLens( MEdgeInfo.geoShapes )
                    .set( shapes1 )(medge0)
                }

                // Залить новый эдж в карту эджей узла
                MNode.edges
                  .composeLens( MNodeEdges.out )
                  .set {
                    val iter = mnode0.edges.withoutPredicateIter(p) ++ Iterator.single(locEdge1)
                    MNodeEdges.edgesToMap1(iter)
                  }(mnode0)
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
  private def recoverOsm(fut: Future[Result], glevel: MNodeGeoLevel, urlPrOpt: Option[OsmUrlParseResult])(implicit rh: RequestHeader): Future[Result] = {
    fut.recoverWith { case ex: Exception =>
      val rest = urlPrOpt.fold("-") { urlPr =>
        urlPr.osmType.xmlUrl(urlPr.id)
      }
      val respBody = ex match {
        case ocex: OsmClientStatusCodeInvalidException =>
          s"osm.org returned unexpected http status: ${ocex.statusCode} for $rest"
        case _ =>
          LOGGER.warn("Exception occured while fetch/parsing of " + rest, ex)
          s"Failed to fetch/parse geo element: " + ex.getClass.getSimpleName + ": " + ex.getMessage
      }
      NotFound(respBody)
    }
  }


  /** Сабмит запроса на удаление элемента. */
  def deleteSubmit(g: MGsPtr) = csrf.Check {
    isSuNode(g.nodeId).async { implicit request =>
      import esModel.api._

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
            LOGGER.debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
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

            import esModel.api._

            // Сохранить и сгенерить результат
            val resFut = for {
              // Дождаться готовности эджа.
              mgs2 <- adnGeo2Fut

              // Обновляем ноду...
              _ <- mNodes.tryUpdate( request.mnode ) {
                _nodeUpdateGeoShape(mgs2)
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
  private def _nodeUpdateGeoShape(gs: MEdgeGeoShape): MNode => MNode = {
    MNode.edges.modify { edges0 =>
      val pf = { e: MEdgeGeoShape =>
        e.id == gs.id
      }
      val p = MPredicates.NodeLocation
      edges0.updateAll { e =>
        e.predicate == p  &&  e.info.geoShapes.exists(pf)
      } { e0 =>
        Some(e0.copy(
          info = e0.info.copy(
            geoShapes = gs :: e0.info.geoShapes.filterNot(pf)
          )
        ))
      }
    }
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
      val circleOpt = CircleGsJvm.maybeFromGs(geo.shape)
      if (circleOpt.isEmpty)
        LOGGER.warn(s"circleFormM(): Unable to unbind geo shape of class ${geo.shape.getClass.getSimpleName} into circle.")
      for (circle <- circleOpt) yield {
        (geo.glevel, circle)
      }
    })
  }


  /** Рендер страницы с формой создания круга. */
  def createCircle(nodeId: String) = csrf.AddToken {
    isSuNode(nodeId).apply { implicit request =>
      val ngl = guessGeoLevel getOrElse MNodeGeoLevels.default
      // Нередко в узле указана geo point, характеризующая её. Надо попытаться забиндить её в круг.
      val gpStub = request.mnode.edges
        .withPredicateIter( MPredicates.NodeLocation )
        .flatMap(_.info.geoPoints)
        .nextOption()
        .getOrElse( MGeoPoint(lat = 0, lon = 0) )
      val stub = MEdgeGeoShape(
        id      = -1,
        glevel  = ngl,
        shape   = CircleGs(gpStub, radiusM = 0d)
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
          LOGGER.debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
          NotAcceptable(createCircleTpl(formWithErrors, request.mnode))
        },
        {circle0 =>
          import esModel.api._

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
                val lens = MEdge.info
                  .composeLens( MEdgeInfo.geoShapes )

                val shapes0 = lens.get(e0)
                val circle1 = (MEdgeGeoShape.id set MEdgeGeoShape.nextShapeId(shapes0))(circle0)

                lens.modify { circle1 :: _ }(e0)
              }

            MNode.edges
              .composeLens( MNodeEdges.out )
              .set {
                val iter = mnode0.edges.withoutPredicateIter(p) ++ Iterator.single(edge0)
                MNodeEdges.edgesToMap1(iter)
              }(mnode0)
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
            LOGGER.debug(logPrefix + "Failed to bind form:\n" + formatFormErrors(formWithErrors))
            _editCircleResp(g, formWithErrors, mgs, NotAcceptable)
          },

          {geoStub =>
            import esModel.api._

            // Собрать обновленный шейп...
            val mgs2 = mgs.copy(
              glevel      = geoStub.glevel,
              shape       = geoStub.shape,
              dateEdited  = Some( OffsetDateTime.now() )
            )

            // Обновить шейпы узла
            val updFut = mNodes.tryUpdate( request.mnode ) {
              _nodeUpdateGeoShape(mgs2)
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
      // Сейчас тут рендер GeoJSON-compatible. Раньше был просто рендер, почему-то.
      import IGeoShape.JsonFormats.geoJsonFormat
      val json = geoJsonFormat.writes( mgs.shape )
      Ok( json )
    }
  }

}
