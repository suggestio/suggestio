package controllers.sc

import io.suggest.adn.MAdnRights
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.coord.{CoordOps, GeoCoord_t}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.es.model._
import io.suggest.es.search.MSubSearch
import io.suggest.geo._
import io.suggest.i18n.MsgCodes
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.media.{MMediaInfo, MMediaTypes}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.{Criteria, GsCriteria}
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeTypes, NodeNotFoundException}
import io.suggest.sc.index.{MSc3IndexResp, MScIndexArgs, MWelcomeInfo}
import io.suggest.sc.sc3.{MSc3RespAction, MScCommonQs, MScQs, MScRespActionTypes}
import io.suggest.sc.{MScApiVsns, ScConstants}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.im.{MImgT, MImgWithWhInfo}
import models.mwc.MWelcomeRenderArgs
import models.req.IReq
import org.locationtech.spatial4j.shape.SpatialRelation

import javax.inject.Inject
import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.14 12:12
  * Description: Кусок Sc-контроллера для поддержки раздачи индекса выдачи, т.е. определения дизайна,
  * "обёртки", текущего узла, геолокации и т.д.
  */
final class ScIndex @Inject()(
                               val scCtlUtil: ScCtlUtil,
                             )
  extends MacroLogsImpl
{

  import scCtlUtil._
  import scCtlUtil.sioControllerApi.ec
  import esModel.api._


  /** Унифицированная логика выдачи в фазе index. */
  abstract class ScIndexLogic extends scCtlUtil.LogicCommonT with IRespActionFut { logic =>

    /** qs-аргументы реквеста. */
    def _qs: MScQs
    def _geoIpInfo: ScCtlUtil#GeoIpInfo

    /** Быстрый доступ к MScIndexArgs. По идее, это безопасно, т.к. запрос должен быть вместе с index args. */
    final def _scIndexArgs = _qs.index.get

    /** Тип sc-responce экшена. */
    def respActionType = MScRespActionTypes.Index

    lazy val logPrefix = s"scIndex[${ctx.timestamp}]"


    /** #00: поиск узла по id ресивера, указанного в qs.
      * Future[NSEE], когда нет необходимого узла. */
    def l00_rcvrByIdFut: Future[Seq[MIndexNodeInfo]] = {
      val adnIdOpt = _scIndexArgs.nodeId

      val rFut = for {
        mnodeOpt <- mNodes.maybeGetByIdCached( adnIdOpt )
      } yield {
        // Если узел-ресивер запрошен, но не найден, прервать работу над index: это какая-то нештатная ситуация.
        if (mnodeOpt.isEmpty && adnIdOpt.nonEmpty)
          throw new NodeNotFoundException(adnIdOpt.orNull)
        // Может быть узел найден?
        val mnode = mnodeOpt.get
        // Узел есть, вернуть положительный результат работы.
        LOGGER.trace(s"$logPrefix Found rcvr node[${mnode.idOrNull}]: ${mnode.guessDisplayNameOrIdOrEmpty}")
        MIndexNodeInfo(mnode, isRcvr = true) :: Nil
      }

      // Залоггировать возможное неизвестное исключение
      rFut.failed.foreach {
        case _: NodeNotFoundException =>
          LOGGER.warn(s"$logPrefix Rcvr node missing: $adnIdOpt! Somebody scanning our nodes? I'll try to geolocate rcvr node.")
        case _: NoSuchElementException =>
          // do nothing
        case ex =>
          LOGGER.warn(s"$logPrefix Unknown exception while getting rcvr node $adnIdOpt", ex)
      }
      rFut
    }


    /** #10: Определение текущего узла выдачи по ближним маячкам. */
    def l10_detectUsingNearBeacons: Future[Seq[MIndexNodeInfo]] = {
      // Ищем активные узлы-ресиверы, относящиеся к видимым маячкам.
      val searchOpt = bleUtil.scoredByDistanceBeaconSearch(
        maxBoost    = 100000F,
        predicates  = MPredicates.PlacedIn :: Nil,
        bcns        = _qs.common.locEnv.beacons
      )
      searchOpt.fold [Future[Seq[MIndexNodeInfo]]] {
        Future.successful( Nil )
      } { bcnSearch =>
        val subSearch = MSubSearch(
          search = bcnSearch,
          must   = IMust.MUST
        )
        val msearch = new MNodeSearch {
          override def subSearches  = subSearch :: Nil
          override def isEnabled    = Some(true)
          override def nodeTypes    = MNodeTypes.AdnNode :: Nil
          override def limit        = ScConstants.Index.MAX_NODES_DETECT
        }
        val nearBeaconHolderOptFut = mNodes.dynSearch(msearch)
        for (mnodes <- nearBeaconHolderOptFut) yield {
          LOGGER.trace(s"$logPrefix detectUsingNearBeacons: => [${mnodes.length}] ${mnodes.iterator.flatMap(_.id).mkString(", ")} ${mnodes.iterator.flatMap(_.guessDisplayNameOrId).mkString(", ")}")
          mnodes
            .iterator
            .map { mnode =>
              MIndexNodeInfo(
                mnode  = mnode,
                isRcvr = true
              )
            }
            // Не-ленивый список, чтобы запустить подготовку логотипов и т.д.
            .toList
        }
      }
    }

    private def _nodeLocPredicates = MPredicates.NodeLocation :: Nil

    private def _geoLocToEsShape(geoLoc: MGeoLoc): GeoShapeToEsQuery = {
      val circle = CircleGs( geoLoc.point, radiusM = 1 )
      GeoShapeToEsQuery( circle )
    }


    /** поискать покрывающий ТЦ/город/район. */
    def l50_detectUsingCoords: Future[Seq[MIndexNodeInfo]] = {
      for {
        // TODO To append detected geoIp into requested qs geoLocs? Need this?
        geoLocs <- _geoIpInfo.reqGeoLocsFut

        // Пусть будет сразу NSEE, если нет данных геолокации.
        if {
          val r = geoLocs.nonEmpty
          if (!r) LOGGER.trace(s"$logPrefix No geolocation, nothing to search")
          r
        }

        geoLocQueryShapes = geoLocs
          .map { geoLoc => geoLoc -> _geoLocToEsShape( geoLoc ) }
        // For buildings geo-level: search using any geolocations. For semi-epheral stuff: use only via real geolocation.
        qShapesBuilding = geoLocQueryShapes
          .map( _._2 )
        qShapesNonBuilding = if (
          _scIndexArgs.returnEphemeral &&
          geoLocQueryShapes.exists(_._1.source contains[MGeoLocSource] MGeoLocSources.NativeGeoLocApi)
        ) {
          // TODO Use more complex check: if qs.index.retEph && ... && *isDemandLocTesting* ?
          // Non-building (district, town, etc) - contains semi-ephemeral nodes just for design uses.
          // But, when user presses "location button" in shocase, ephemeral node to return must as exact as possible to current GPS location.
          geoLocQueryShapes
            .iterator
            .filter( _._1.source contains[MGeoLocSource] MGeoLocSources.NativeGeoLocApi )
            .map( _._2 )
            .toSeq
        } else {
          // Return any ephemeral node, without any forcing "my location node".
          qShapesBuilding
        }
        nodeLocPreds = _nodeLocPredicates

        // Если запрещено погружение в реальные узлы-ресиверы (геолокация), то запрещаем получать узлы-ресиверы от elasticsearch:
        (withAdnRights1, adnRightsMustOrNot1) = if (!_scIndexArgs.geoIntoRcvr) {
          // Запрещено погружаться в ресиверы. Значит, ищем просто узел-обёртку для выдачи, а не ресивер.
          (MAdnRights.RECEIVER :: Nil, false)
        } else {
          (Nil, true)
        }
        someTrue = {
          LOGGER.trace(s"$logPrefix geoIntoRcvr=${_scIndexArgs.geoIntoRcvr} => adnRights=[${withAdnRights1.mkString(",")}] adnRightsMustOrNot=${adnRightsMustOrNot1}\n qShapes.building = [${qShapesBuilding.mkString(", ")}]")
          OptionUtil.SomeBool.someTrue
        }

        // Получить первый успешный результат или вернуть NSEE.
        rs <- Future.traverse(MNodeGeoLevels.values: Iterable[MNodeGeoLevel]) { ngl =>
          for {
            mnodes <- mNodes.dynSearch(
              new MNodeSearch {
                // Неактивные узлы сразу вылетают из выдачи.
                override def isEnabled = someTrue
                override val outEdges: MEsNestedSearch[Criteria] = {
                  // Возможно, надо сортировать на предмет близости к точке.
                  val cr = Criteria(
                    predicates  = nodeLocPreds,
                    gsIntersect = Some {
                      GsCriteria(
                        levels = ngl :: Nil,
                        shapes = {
                          if (ngl ==* MNodeGeoLevels.NGL_BUILDING)
                            qShapesBuilding
                          else
                            qShapesNonBuilding
                        },
                      )
                    },
                  )
                  MEsNestedSearch.plain( cr )
                }
                override def withAdnRights = withAdnRights1
                override def adnRightsMustOrNot = adnRightsMustOrNot1
                override def limit = ScConstants.Index.MAX_NODES_DETECT
              }
            )
          } yield {
            LOGGER.trace(s"$logPrefix ${qShapesBuilding.length} geoLocs(${qShapesBuilding.iterator.map(_.gs).mkString("|")}) on level $ngl => [${mnodes.length}]: [${mnodes.iterator.flatMap(_.id).mkString(", ")}]")
            mnodes
              .iterator
              .map { mnode =>
                MIndexNodeInfo(
                  mnode  = mnode,
                  // 2018-03-23 Проверка упрощена. TODO Можно попытаться вынести её на уровень поиска в индексе по adnRights. Сортировать по RCVR и ngl (а как по дважды-nested сортировать???), и сразу получить нужный элемент.
                  isRcvr = mnode.extras.isRcvr,
                )
              }
              // Явный запуск сборки коллекции, чтобы инициализацировать все val'ы внутри MIndexNodeInfo.
              .toList
          }
        }

      } yield {
        val r = rs.iterator
          .flatten
          .foldLeft( (false: Boolean, List.empty[MIndexNodeInfo]) ) {
            case (acc0 @ (haveNonRcvrNode, nodeInfoAcc0), nodeInfo) =>
              if (!nodeInfo.isRcvr) {
                if (haveNonRcvrNode) {
                  LOGGER.trace(s"$logPrefix Dropped non-rcvr node - ${nodeInfo.mnode.guessDisplayNameOrIdOrEmpty}")
                  acc0
                } else {
                  LOGGER.trace(s"$logPrefix !haveNonRcvrNode, and found !rcvr node#${nodeInfo.currNodeIdOpt.orNull}: ${nodeInfo.mnode.guessDisplayName getOrElse ""} ##${nodeInfo.mnode.idOrNull}")
                  (true, nodeInfo :: nodeInfoAcc0)
                }
              } else {
                LOGGER.trace(s"$logPrefix Found rcvr node#${nodeInfo.currNodeIdOpt.orNull}: ${nodeInfo.mnode.guessDisplayName getOrElse ""}")
                (haveNonRcvrNode, nodeInfo :: nodeInfoAcc0)
              }
          }
          ._2
          .reverse

        //LOGGER.trace(s"$logPrefix First-detected node#${resNode.mnode.idOrNull} isRcvr?${resNode.isRcvr}")
        LOGGER.trace(s"$logPrefix For ${geoLocs.length} geoLocs (${geoLocs.mkString(", ")}) geolocated ${r.length} receivers: [${r.iterator.flatMap(_.mnode.id).mkString(", ")}]")

        r
      }
    }


    /** Найти в пуле или придумать какой-то рандомный узел, желательно без id даже. */
    def l95_ephemeralNodesFromPool: Future[Seq[MIndexNodeInfo]] = {
      val ephNodeId = nodesUtil.noAdsFound404RcvrId( ctx )
      val _mnodeOptFut = mNodes.getByIdCache( ephNodeId )
      LOGGER.trace(s"$logPrefix Index node NOT geolocated. Trying to get ephemeral covering node[$ephNodeId] for lang=${ctx.messages.lang.code}.")

      for {
        mnodeOpt <- _mnodeOptFut
      } yield {
        val mnode = mnodeOpt.get
        val ephNodeName = ctx.messages( MsgCodes.`Current.location` )
        LOGGER.trace(s"$logPrefix Choosen ephemeral node[$ephNodeId]: ${mnode.guessDisplayNameOrIdOrEmpty}: ")
        MIndexNodeInfo(
          // Change eph.node title to localized "My current location", if demand index/location testing. When eph.node listed as-is, it looks too unexpected.
          // TODO Change geo.coords of eph.node here? Currenly, it is done client-side inside IndexAh().handle( IndexSwitchNodeClick() )
          mnode = MNode.meta
            .composeLens( MMeta.basic )
            .composeLens( MBasicMeta.nameOpt )
            .set( Some(ephNodeName) ) {
              mNodes.withDocMeta( mnode, EsDocMeta(None, EsDocVersion.notSaveable) )
            },
          isRcvr = false
        ) :: Nil
      }
    }


    /** Придумывание текущего узла из головы. */
    def l99_ephemeralNode: MIndexNodeInfo = {
      MIndexNodeInfo(
        mnode = nodesUtil.userNodeInstance(
          nameOpt     = Some( MsgCodes.`Current.location` ),
          personIdOpt = None
        ),
        isRcvr = false
      )
    }


    /** Узлы, которые необходимо вернуть в качестве index'а.
      * Хоть один какой-то узел обязательно, но не обязательно реальный существующий узел-ресивер.
      */
    def indexNodesFut: Future[Seq[MIndexNodeInfo]] = {
      l00_rcvrByIdFut.recoverWith { case _: NoSuchElementException =>
        // Если с ресивером по id не фартует, но есть данные геолокации, то заодно запускаем поиск узла-ресивера по геолокации.
        // Нет смысла выносить этот асинхронный код за пределы recoverWith(), т.к. он или не нужен, или же выполнится сразу синхронно.
        val beaconsNodesFut = l10_detectUsingNearBeacons
        val coordsNodesFut  = for {
          coordNodes <- l50_detectUsingCoords
        } yield {
          // Надо выкинуть !isRcvr, если ЕСТЬ isRcvr.
          if (
            coordNodes.exists(m => !m.isRcvr) &&
            ( _scIndexArgs.returnEphemeral || coordNodes.exists(_.isRcvr) )
          ) {
            val r = coordNodes.filter(_.isRcvr)
            LOGGER.trace(s"$logPrefix Dropping non-rcvr nodes: \n was ${coordNodes.length} nodes = ${coordNodes.iterator.map(_.currNodeIdOpt getOrElse "?").mkString(", ")}\n become ${r.length} nodes = ${r.iterator.map(_.currNodeIdOpt getOrElse "?").mkString(", ")}")
            r
          } else {
            coordNodes
          }
        }

        // Запуск параллельной сборки узлов по маячкам, по gps и просто по заглушке.
        val futs1 =
          beaconsNodesFut ::
          coordsNodesFut ::
          Nil

        // Не надо собирать доп.узлы, если по координатам уже есть узел с !isRcvr
        val ephemeralNodesFut = for {
          coordNodes <- coordsNodesFut
          isAlsoRenderEphemeral = {
            if (
              coordNodes.isEmpty ||
              (_scIndexArgs.returnEphemeral && !coordNodes.exists(!_.isRcvr))
            ) {
              LOGGER.trace(s"$logPrefix Will ad ephemeral node to ${coordNodes.length} coord-detected nodes, qs.retEph?${_scIndexArgs.returnEphemeral}")
              true

            } else if (_scIndexArgs.returnEphemeral) {
              val qsGeoLocsAll = _qs.common.locEnv.geoLoc
              val qGpsShapes = qsGeoLocsAll
                .filter(_.source contains[MGeoLocSource] MGeoLocSources.NativeGeoLocApi)
              LOGGER.trace(s"$logPrefix Prepared ${qGpsShapes.length} GPS-geo-shapes for user geo.locations from qs.geoLocsAll[${qsGeoLocsAll.length}]=${qsGeoLocsAll}")

              val nodesHaveNonRcvrIntersection = qGpsShapes.isEmpty || {
                coordNodes.exists { m =>
                  !m.isRcvr &&
                  m .intersectionsWith( qGpsShapes )
                    .map(_ => true)
                    .nextOption()
                    .getOrElseFalse
                }
              }
              LOGGER.trace(s"$logPrefix Has non-rcrv semi-ephemeral nodes intersections? $nodesHaveNonRcvrIntersection shapes[${qGpsShapes.length}]")
              !nodesHaveNonRcvrIntersection

            } else {
              false
            }
          }
          r <- if (isAlsoRenderEphemeral) {
            l95_ephemeralNodesFromPool
              .recover { case _ => l99_ephemeralNode :: Nil }
          } else {
            Future.successful(Nil)
          }
        } yield {
          r
        }

        for {
          nodeLists <- Future.sequence {
            futs1 appended ephemeralNodesFut
          }
        } yield {
          val nodes = nodeLists.flatten
          LOGGER.trace(s"$logPrefix LocEnv-nodes total = ${nodes.length}")
          nodes
        }
      }
    }

    lazy val indexNodeFutVal = indexNodesFut


    /** Контейнер данных о выбранном index-узле. Изначально, содержал только N2-узел.
      *
      * @param mnode Узел, на котором будет базироваться выдача.
      * @param isRcvr Это узел-ресивер для карточек грядущей выдачи?
      *               false значит, что узел есть, которой не является нормальным ресивером, а просто узел-вывеска.
      */
    case class MIndexNodeInfo(
                               mnode       : MNode,
                               isRcvr      : Boolean
                             ) {

      /** Получение графического логотипа узла, если возможно. */
      val logoImgOptFut: Future[Option[MImgT]] = {
        val logoOptRaw = logoUtil.getLogoOfNode( mnode )
        logoUtil.getLogoOpt4scr(logoOptRaw, _qs.common.screen)
      }


      /** Получение карточки приветствия. */
      val welcomeOptFut: Future[Option[MWelcomeRenderArgs]] =
        welcomeUtil.getWelcomeRenderArgs(mnode, ctx.deviceScreenOpt)(ctx)

      /** Определение заголовка выдачи. */
      def title: String = {
        mnode
          .meta
          .basic
          .nameOpt
          .getOrElse {
            ctx.messages
              .translate("isuggest", Nil)
              .getOrElse( MsgCodes.`iSuggest` )
          }
      }

      val currNodeIdOpt: Option[String] = {
        mnode.id
          .filter(_ => isRcvr)
      }

      /** Подготовка данных по логотипу узла. */
      def logoMediaInfoOptFut: Future[Option[MMediaInfo]] = {
        logoImgOptFut.flatMap { logoImgOpt =>
          FutureUtil.optFut2futOpt(logoImgOpt) { logoImg =>
            // Пока без wh, т.к. у логотипа была константная высота, заданная в css-пикселях ScConstants.Logo.HEIGHT_CSSPX
            _img2mediaInfo( logoImg, whPxOpt = None )
          }
        }
      }

      private def _imgWithWhOpt2mediaInfo(iwwiOpt: Option[MImgWithWhInfo]): Future[Option[MMediaInfo]] = {
        FutureUtil.optFut2futOpt(iwwiOpt) { ii =>
          _img2mediaInfo( ii.mimg, whPxOpt = Some(MSize2di.applyOrThis(ii.meta)) )
        }
      }

      /** Завернуть данные для рендера welcome в выходной формат. */
      def welcomeInfoOptFut: Future[Option[MWelcomeInfo]] = {
        welcomeOptFut.flatMap { wcOpt =>
          FutureUtil.optFut2futOpt(wcOpt) { wc =>
            val bgImageFut = _imgWithWhOpt2mediaInfo( wc.bg.toOption )
            for {
              fgImage <- _imgWithWhOpt2mediaInfo( wc.fgImage )
              bgImage <- bgImageFut
            } yield {
              val mWcInfo = MWelcomeInfo(
                bgColor = for (hexCode <- wc.bg.left.toOption) yield {
                  MColorData(
                    code = hexCode
                  )
                },
                bgImage = bgImage,
                fgImage = fgImage
              )
              Some( mWcInfo )
            }
          }
        }
      }

      /** Проверка на то, принадлежит ли текущий узел текущему юзеру. */
      def isMyNodeFut: Future[Boolean] = {
        _request.user.personIdOpt.fold {
          //LOGGER.trace(s"$logPrefix not logged in")
          Future.successful( false )
        } { _ =>
          currNodeIdOpt.fold ( Future.successful(false) ) { nodeId =>
            isNodeAdmin
              .isAdnNodeAdmin(adnId = nodeId, user = _request.user)
              .map(_.nonEmpty)
          }
        }
      }

      // Если нет географии, то поискать центр для найденного узла.
      // Для нормальных узлов (не районов) следует возвращать клиенту их координату.
      lazy val nodeLocEdges = mnode.edges
        .withPredicateIter( _nodeLocPredicates: _* )
        .to( LazyList )

      lazy val nodeLocCenters = {
        // Ленивая коллекция гео-точек для NodeLocation-эджей
        nodeLocEdges
          .flatMap { medge =>
            val ei = medge.info
            if (ei.geoPoints.nonEmpty) {
              ei.geoPoints
            } else {
              ei.geoShapes
                .flatMap { gs =>
                  gs.shape
                    .centerPoint
                    .orElse {
                      // Try to detect center using spatial4j:
                      for {
                        edgeShape <- Some( gs.shape )
                          .collect { case gsQ: IGeoShapeQuerable => gsQ }
                        pointS4J <- Option {
                          GeoShapeToEsQuery( edgeShape )
                            .esShapeBuilder
                            .buildS4J()
                            .getCenter
                        }
                      } yield {
                        GeoPoint( pointS4J )
                      }
                    }
                }
            }
          }
      }

      /** Поиск отображаемой координаты узла.
        * Карта на клиенте будет отцентрована по этой точке. */
      def nodeGeoPointOptFut: Future[Option[MGeoPoint]] = {
        // Если география уже активна на уровне index-запроса, то тут ничего делать не требуется.
        val mgpOpt: Option[MGeoPoint] = if (isRcvr) {
          val edgesPoints = nodeLocCenters

          // Надо вернуть
          // - Ближайшую к текущей локации точку, чтобы избежать ситуации.
          // - либо первую центральную точку,
          // - либо первую попавшующся точку вообще.
          val qsGeoLocs = _qs.common.locEnv.geoLoc
          def __nearestPointOf(gpIter: Iterator[MGeoPoint]): Option[MGeoPoint] = {
            import Ordering.Double.TotalOrdering
            // Detect nearest point to current request geolocation
            val nearestPoint = gpIter
              .minByOption { centerPoint =>
                qsGeoLocs
                  .iterator
                  .map { geoLoc =>
                    CoordOps
                      .distanceXY[MGeoPoint, GeoCoord_t]( centerPoint, geoLoc.point )
                      .abs
                      .doubleValue
                  }
                  .minOption
                  .getOrElse( Double.PositiveInfinity )
              }
            LOGGER.trace(s"$logPrefix Node#${mnode.idOrNull}: nearest to ${qsGeoLocs.length} qs geopoints => $nearestPoint\n Choosen from ${edgesPoints.length} points: ${edgesPoints.mkString(" | ")}")
            nearestPoint
          }

          val r = if (qsGeoLocs.nonEmpty && edgesPoints.lengthIs > 1) {
            __nearestPointOf( edgesPoints.iterator )

          } else if (edgesPoints.isEmpty) {
            // Нет центральных точек - взять первую попавшуюся из любого шейпа.
            __nearestPointOf {
              for {
                nlEdge <- nodeLocEdges.iterator
                gs <- nlEdge.info.geoShapes
              } yield {
                val first = gs.shape.firstPoint
                LOGGER.warn(s"$logPrefix Node#${mnode.idOrNull} choose any shape firstPoint $first, because no geoPoints or centerPoints.")
                first
              }
            }

          } else {
            val hd = edgesPoints.headOption
            LOGGER.trace(s"$logPrefix Node#${mnode.idOrNull}: Choosen the only possible geopoint ${hd.orNull}")
            hd
          }

          LOGGER.trace(s"$logPrefix Node#${mnode.id} geo pt. => $r")
          r

        } else {
          // Non-receiver or ephemeral node. Center point depends on NodeLocation shape intersection with qs geoLocs, if any.
          intersectionsWith( _qs.common.locEnv.geoLoc )
            .map( _._1.point )
            .nextOption()
            .orElse {
              nodeLocCenters.headOption
            }
        }

        Future.successful(mgpOpt)
      }


      /** Collect intersection information against these geolocations. */
      def intersectionsWith(geoLocs: Iterable[MGeoLoc]) = {
        val geoLocsS4J = geoLocs
          .map { geoLoc =>
            val s4j = _geoLocToEsShape( geoLoc )
              .esShapeBuilder
              .buildS4J()
            geoLoc -> s4j
          }
        for {
          nodeLocEdge <- mnode.edges.withPredicateIter( _nodeLocPredicates: _* )
          edgeGs <- nodeLocEdge.info.geoShapes.iterator
          edgeShape <- Some( edgeGs.shape )
            .collect { case gsQ: IGeoShapeQuerable => gsQ }
            .iterator
          edgeShapeS4J = GeoShapeToEsQuery( edgeShape )
            .esShapeBuilder
            .buildS4J()
          (geoLoc, qShapeS4J) <- geoLocsS4J.iterator
          relation = edgeShapeS4J relate qShapeS4J
          isShapesIntersects = (relation != SpatialRelation.DISJOINT)
          if isShapesIntersects
        } yield {
          LOGGER.trace(s"$logPrefix Detected shape intersection with node#${mnode.idOrNull} rcvr?${isRcvr} gs#${edgeGs.id}#${edgeGs.shape.shapeType} :: intersect-relation => ${relation}")
          (geoLoc, nodeLocEdge, edgeGs, edgeShape)
        }
      }


      /** RespAction для index-ответа. */
      val indexRespActionFut: Future[Option[MSc3IndexResp]] = {
        val _logoMediaInfoOptFut = logoMediaInfoOptFut
        val _welcomeInfoOptFut   = welcomeInfoOptFut
        val _nodeGeoPointOptFut  = nodeGeoPointOptFut
        val _isMyNodeFut         = isMyNodeFut
        // Возвращать геолокацию юзера только если затребовано в исходном запросе.
        val _reqGeoLocOptFut =
          if (_scIndexArgs.retUserLoc) _geoIpInfo.geoIpLocOptFut
          else Future.successful(None)

        for {
          logoOpt         <- _logoMediaInfoOptFut
          welcomeOpt      <- _welcomeInfoOptFut
          nodeGeoPointOpt <- _nodeGeoPointOptFut
          isMyNode        <- _isMyNodeFut
          reqGeoLocOpt    <- _reqGeoLocOptFut
        } yield {
          val inxRa = MSc3IndexResp(
            nodeId        = currNodeIdOpt,
            ntype         = Some( mnode.common.ntype ),
            name          = Option( title ),
            colors        = mnode.meta.colors,
            logoOpt       = logoOpt,
            welcome       = welcomeOpt,
            geoPoint      = nodeGeoPointOpt,
            isMyNode      = Some(isMyNode),
            userGeoLoc    = reqGeoLocOpt,
            isLoggedIn    = Some( _request.user.isAuth ),
          )
          Some(inxRa)
        }
      }

    }


    override def scStat: Future[Stat2] = {
      // Запуск асинхронных задач в фоне.
      val _userSaOptFut     = statUtil.userSaOptFutFromRequest()
      val _indexNodeFut     = indexNodeFutVal
      val _geoIpResOptFut   = _geoIpInfo.geoIpResOptFut

      // Исполнение синхронных задач.
      val _remoteIp         = _geoIpInfo.remoteIp

      // Сборка асинхронного результата.
      for {
        _userSaOpt          <- _userSaOptFut
        _indexNode          <- _indexNodeFut
        _geoIpResOpt        <- _geoIpResOptFut
      } yield {
        // Сбилдить статистику по данному index'у.
        new Stat2 {
          override def userSaOpt: Option[MAction] = _userSaOpt
          override def statActions: List[MAction] = {
            val actType = if ( _indexNode.exists(_.isRcvr) ) {
              MActionTypes.ScIndexRcvr
            } else {
              MActionTypes.ScIndexCovering
            }
            val inxSa = MAction(
              actions   = actType :: Nil,
              nodeId    = _indexNode.iterator.flatMap(_.mnode.id).toSeq,
              nodeName  = _indexNode.iterator.flatMap(_.mnode.guessDisplayName).toSeq
            )
            inxSa :: Nil
          }
          override def components = MComponents.Index :: super.components
          override def remoteAddr = _remoteIp
          override def devScreenOpt = _qs.common.screen
          override def locEnvOpt = Some( _qs.common.locEnv )
          override def geoIpLoc = _geoIpResOpt
        }
      }
    }



    /** true, если вызов идёт из [[ScIndex]]AdOpen. */
    def isFocusedAdOpen: Boolean = false

    /** Поддержка dist nodes: собрать картинки для index'а узла. */
    lazy val distMediaHostsMapFut: Future[Map[String, Seq[MHostInfo]]] = {
      // В ScIndex входят logo, wc.fg, wc.bg. Собрать их в кучу и запросить для них media-узлы:
      for {
        nodeInfos <- indexNodeFutVal

        logosFuts = Future
          .sequence( nodeInfos.map(_.logoImgOptFut) )
          .map(_.flatten)
        welcomes <- Future
          .sequence( nodeInfos.map(_.welcomeOptFut) )
          .map(_.flatten)
        logos <- logosFuts

        // Узнать узлы, на которых хранятся связанные картинки.
        mediaHostsMap <- nodesUtil.nodeMediaHostsMap(
          logoImgOpt = logos,
          welcomeOpt = welcomes,
        )

      } yield {
        LOGGER.trace(s"distNodesMap: ${mediaHostsMap.valuesIterator.flatten.map(_.namePublic).toSet.mkString(", ")}")
        mediaHostsMap
      }
    }


    private def _img2mediaInfo(mimg: MImgT, whPxOpt: Option[MSize2di]): Future[Option[MMediaInfo]] = {
      for {
        mediaHostsMap <- distMediaHostsMapFut
      } yield {
        val mMediaInfo = MMediaInfo(
          giType = MMediaTypes.Image,
          url    = cdnUtil.maybeAbsUrl( _qs.common.apiVsn.forceAbsUrls ) {
            dynImgUtil.distCdnImgCall(mimg, mediaHostsMap)
          }(ctx),
          whPx   = whPxOpt,
          contentType = mimg.dynImgId.imgFormat.get.mime,
        )
        Some( mMediaInfo )
      }
    }

    /** index-ответ сервера с данными по узлу. */
    override def respActionFut: Future[MSc3RespAction] = {
      for {
        nodeInfos      <- indexNodeFutVal
        respActionOpts <- Future.sequence(
          nodeInfos.map(_.indexRespActionFut)
        )
        respActions = respActionOpts.flatten
      } yield {
        LOGGER.trace(s"$logPrefix => ${respActions.length} actions:\n ${respActions.iterator.map(m => m.nodeId.getOrElse("") + " " + m.name.getOrElse("")).mkString("\n ")}")
        MSc3RespAction(
          acType = respActionType,
          search = Some {
            MGeoNodesResp(
              nodes = respActions.map( MGeoNodePropsShapes(_, Nil) )
            )
          }
        )
      }
    }

  }

  object ScIndexLogic {
    def apply(qs: MScQs, geoIpInfo: ScCtlUtil#GeoIpInfo)(implicit request: IReq[_]): ScIndexLogic = {
      val scApiVsn = qs.common.apiVsn
      if (scApiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        ScIndexLogicV3(qs, geoIpInfo)(request)
      } else {
        throw new UnsupportedOperationException("Unknown API vsn: " + scApiVsn)
      }
    }
  }


  /** Раздача индекса выдачи v3.
    *
    * По сравнению с v2 здесь существенное упрощение серверной части,
    * т.к. вся генерация html идёт на клиенте.
    *
    * Сервер отвечает только параметрами для рендера, без html.
    */
  case class ScIndexLogicV3(override val _qs: MScQs,
                            override val _geoIpInfo: ScCtlUtil#GeoIpInfo)
                           (override implicit val _request: IReq[_]) extends ScIndexLogic



  /** ScIndex-логика перехода на с focused-карточки в индекс выдачи продьюсера фокусируемой карточки.
    *
    * @param producer Продьюсер, в который требуется перескочить.
    * @param focQs Исходные qs-аргументы запроса фокусировки.
    * @param _request Исходный HTTP-реквест.
    */
  case class ScFocToIndexLogicV3(producer: MNode, focQs: MScQs, override val _geoIpInfo: ScCtlUtil#GeoIpInfo)
                                (override implicit val _request: IReq[_]) extends ScIndexLogic {

    /** Подстановка qs-аргументы реквеста. */
    // TODO Заменить lazy val на val.
    override lazy val _qs: MScQs = {
      // v3 выдача. Собрать аргументы для вызова index-логики:
      var qsCommon2 = focQs.common

      // Если выставлен отказ от bluetooth-маячков в ответе, то убрать маячки из locEnv.
      if (
        focQs.foc
          .exists(_.indexAdOpen
            .exists(!_.withBleBeaconAds)) &&
          qsCommon2.locEnv.beacons.nonEmpty
      ) {
        LOGGER.trace(s"$logPrefix _qs: Forget ${qsCommon2.locEnv.beacons.length} BLE Beacons info from QS locEnv, because foc.indexAdOpen.withBleBeaconsAds = false")
        qsCommon2 = MScCommonQs.locEnv
          .composeLens( MLocEnv.beacons )
          .set( Nil )( qsCommon2 )
      }

      MScQs(
        common = qsCommon2,
        index = Some(
          MScIndexArgs(
          )
        ),
      )
    }

    override def isFocusedAdOpen = {
      true
    }

    override lazy val indexNodesFut: Future[Seq[MIndexNodeInfo]] = {
      val nodeInfo = MIndexNodeInfo(
        mnode   = producer,
        isRcvr  = true
      )
      Future.successful( nodeInfo :: Nil )
    }

  }


}
