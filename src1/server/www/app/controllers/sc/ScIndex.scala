package controllers.sc

import _root_.util.di._
import io.suggest.adn.MAdnRights
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.coord.{CoordOps, GeoCoord_t}
import io.suggest.common.geom.d2.MSize2di
import io.suggest.es.model.{EsModelDi, IMust, MEsNestedSearch}
import io.suggest.es.search.MSubSearch
import io.suggest.geo.{MGeoLoc, _}
import io.suggest.i18n.MsgCodes
import io.suggest.maps.nodes.{MGeoNodePropsShapes, MGeoNodesResp}
import io.suggest.media.{IMediaInfo, MMediaInfo, MMediaTypes}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.edge.search.{Criteria, GsCriteria}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{IMNodes, MNode, MNodeTypes, NodeNotFoundException}
import io.suggest.sc.MScApiVsns
import io.suggest.sc.index.{MSc3IndexResp, MWelcomeInfo}
import io.suggest.sc.sc3.{MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.IMacroLogs
import models.im.{MImgT, MImgWithWhInfo}
import models.mwc.MWelcomeRenderArgs
import models.req.IReq
import util.acl._
import util.adn.INodesUtil
import util.ble.IBleUtilDi
import util.geo.IGeoIpUtilDi
import util.img.IDynImgUtil
import util.stat.{IStatCookiesUtilDi, IStatUtil}
import util.showcase.IScUtil
import japgolly.univeq._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.14 12:12
  * Description: Кусок Sc-контроллера для поддержки раздачи индекса выдачи, т.е. определения дизайна,
  * "обёртки", текущего узла, геолокации и т.д.
  */
trait ScIndex
  extends ScController
  with IMacroLogs
  with IStatCookiesUtilDi
  with IMNodes
  with INodesUtil
  with IWelcomeUtil
  with IScUtil
  with IGeoIpUtilDi
  with IStatUtil
  with IBleUtilDi
  with IDynImgUtil
  with IIsNodeAdmin
  with EsModelDi
{

  import mCommonDi._
  import esModel.api._


  /** Унифицированная логика выдачи в фазе index. */
  abstract class ScIndexLogic extends LogicCommonT with IRespActionFut { logic =>

    /** qs-аргументы реквеста. */
    def _qs: MScQs

    /** Быстрый доступ к MScIndexArgs. По идее, это безопасно, т.к. запрос должен быть вместе с index args. */
    final def _scIndexArgs = _qs.index.get

    /** Тип sc-responce экшена. */
    def respActionType = MScRespActionTypes.Index

    lazy val logPrefix = s"scIndex[${ctx.timestamp}]"

    /** Подчищенные нормализованные данные о remote-адресе. */
    lazy val _remoteIp = geoIpUtil.fixedRemoteAddrFromRequest

    /** Пошаренный результат ip-geo-локации. */
    lazy val geoIpResOptFut: Future[Option[IGeoFindIpResult]] = {
      val remoteIp = _remoteIp
      val findFut = geoIpUtil.findIpCached(remoteIp.remoteAddr)
      if (LOGGER.underlying.isTraceEnabled()) {
        findFut.onComplete { res =>
          LOGGER.trace(s"$logPrefix geoIpResOptFut[$remoteIp]:: tried to geolocate by ip => $res")
        }
      }
      findFut
    }

    /** Результат ip-геолокации. приведённый к MGeoLoc. */
    lazy val geoIpLocOptFut = geoIpUtil.geoIpRes2geoLocOptFut( geoIpResOptFut )

    /** ip-геолокация, когда гео-координаты или иные полезные данные клиента отсутствуют. */
    lazy val reqGeoLocFut: Future[Option[MGeoLoc]] = {
      geoIpUtil.geoLocOrFromIp( _qs.common.locEnv.geoLocOpt )( geoIpLocOptFut )
    }


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
        bcns        = _qs.common.locEnv.bleBeacons
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
          override def limit        = 5
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


    /** поискать покрывающий ТЦ/город/район. */
    def l50_detectUsingCoords: Future[Seq[MIndexNodeInfo]] = {
      // Если с ресивером по id не фартует, но есть данные геолокации, то заодно запускаем поиск узла-ресивера по геолокации.
      // В понятиях старой выдачи, это поиск активного узла-здания.
      // Нет смысла выносить этот асинхронный код за пределы recoverWith(), т.к. он или не нужен, или же выполнится сразу синхронно.
      val _reqGeoLocFut = reqGeoLocFut

      _reqGeoLocFut.flatMap { geoLocOpt =>
        // Пусть будет сразу NSEE, если нет данных геолокации.
        val geoLoc = geoLocOpt.get
        LOGGER.trace(s"$logPrefix Detect node using geo-loc: $geoLoc")

        // Пройтись по всем геоуровням, запустить везде параллельные поиски узлов в точке, закинув в recover'ы.
        val circle = CircleGs(geoLoc.point, radiusM = 1)
        val qShape = CircleGsJvm.toEsQueryMaker( circle )
        val nodeLocPred = MPredicates.NodeLocation

        // Если запрещено погружение в реальные узлы-ресиверы (геолокация), то запрещаем получать узлы-ресиверы от elasticsearch:
        val (withAdnRights1, adnRightsMustOrNot1) = if (!_scIndexArgs.geoIntoRcvr) {
          // Запрещено погружаться в ресиверы. Значит, ищем просто узел-обёртку для выдачи, а не ресивер.
          (MAdnRights.RECEIVER :: Nil, false)
        } else {
          (Nil, true)
        }

        val nglsResultsFut = Future.traverse(MNodeGeoLevels.values: Iterable[MNodeGeoLevel]) { ngl =>
          val msearch = new MNodeSearch {
            // Неактивные узлы сразу вылетают из выдачи.
            override val isEnabled = Some(true)
            override val outEdges: MEsNestedSearch[Criteria] = {
              // Возможно, надо сортировать на предмет близости к точке.
              val gsCr = GsCriteria(
                levels = ngl :: Nil,
                shapes = qShape :: Nil
              )
              val cr = Criteria(
                predicates  = nodeLocPred :: Nil,
                gsIntersect = Some(gsCr)
              )
              MEsNestedSearch(
                clauses = cr :: Nil,
              )
            }
            override def withAdnRights = withAdnRights1
            override def adnRightsMustOrNot = adnRightsMustOrNot1
            override def limit = 5
          }
          // Запустить поиск по запрошенным адресам.
          for {
            mnodes <- mNodes.dynSearch(msearch)
          } yield {
            LOGGER.trace(s"$logPrefix $geoLoc on level $ngl => [${mnodes.length}] - ${mnodes.iterator.flatMap(_.id).mkString(", ")}")
            mnodes
              .iterator
              .map { mnode =>
                MIndexNodeInfo(
                  mnode  = mnode,
                  // 2018-03-23 Проверка упрощена. TODO Можно попытаться вынести её на уровень поиска в индексе по adnRights. Сортировать по RCVR и ngl (а как по дважды-nested сортировать???), и сразу получить нужный элемент.
                  isRcvr = mnode.extras.isRcvr
                )
              }
              // Явный запуск сборки коллекции, чтобы инициализацировать все val'ы внутри MIndexNodeInfo.
              .toList
          }
        }

        // Получить первый успешный результат или вернуть NSEE.
        val fut1 = for (rs <- nglsResultsFut) yield {
          rs.iterator
            .flatten
            .foldLeft( (false: Boolean, List.empty[MIndexNodeInfo]) ) {
              case (acc0 @ (haveNonRcvrNode, nodeInfoAcc0), nodeInfo) =>
                if (!nodeInfo.isRcvr) {
                  if (haveNonRcvrNode) {
                    LOGGER.trace(s"$logPrefix Dropped non-rcvr node - ${nodeInfo.mnode.guessDisplayNameOrIdOrEmpty}")
                    acc0
                  } else {
                    (true, nodeInfo :: nodeInfoAcc0)
                  }
                } else {
                  (haveNonRcvrNode, nodeInfo :: nodeInfoAcc0)
                }
            }
            ._2
            .reverse
          //LOGGER.trace(s"$logPrefix First-detected node#${resNode.mnode.idOrNull} isRcvr?${resNode.isRcvr}")
        }

        // Записываем в логи промежуточные итоги геолокации.
        fut1.onComplete {
          case Success(info) =>
            LOGGER.trace(s"$logPrefix For $geoLoc geolocated ${info.length} receivers: [${info.iterator.flatMap(_.mnode.id).mkString(", ")}]")
          case Failure(ex2) =>
            if (ex2.isInstanceOf[NoSuchElementException])
              LOGGER.trace(s"$logPrefix No receivers found via geolocation: $geoLoc")
            else
              LOGGER.warn(s"$logPrefix Failed to geolocate for receiver node using $geoLoc", ex2)
        }

        fut1
      }
    }


    /** Найти в пуле или придумать какой-то рандомный узел, желательно без id даже. */
    def l95_ephemeralNodesFromPool: Future[Seq[MIndexNodeInfo]] = {
      val ephNodeId = nodesUtil.noAdsFound404RcvrId( ctx )
      val _mnodeOptFut = mNodes.getByIdCache( ephNodeId )
      LOGGER.trace(s"$logPrefix Index node not geolocated. Trying to get ephemeral covering node[$ephNodeId] for lang=${ctx.messages.lang.code}.")

      for (mnodeOpt <- _mnodeOptFut) yield {
        val mnode = mnodeOpt.get
        LOGGER.trace(s"$logPrefix Choosen ephemeral node[$ephNodeId]: ${mnode.guessDisplayNameOrIdOrEmpty}")
        MIndexNodeInfo(
          mnode = mnode.copy(
            id = None,
            versionOpt = None
          ),
          isRcvr = false
        ) :: Nil
      }
    }


    /** Придумывание текущего узла из головы. */
    def l99_ephemeralNode: MIndexNodeInfo = {
      MIndexNodeInfo(
        mnode = nodesUtil.userNodeInstance(
          nameOpt     = Some( MsgCodes.`iSuggest` ),
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
        val beaconsNodesFut = l10_detectUsingNearBeacons
        val coordsNodesFut  = for (coordNodes <- l50_detectUsingCoords) yield {
          // Надо выкинуть !isRcvr, если ЕСТЬ isRcvr.
          if ( coordNodes.exists(m => !m.isRcvr) && coordNodes.exists(_.isRcvr) ) {
            val r = coordNodes.filter(_.isRcvr)
            LOGGER.trace(s"$logPrefix Dropping non-rcvr nodes: \n was ${coordNodes.length} nodes = ${coordNodes.iterator.map(_.currNodeIdOpt.getOrElse("?")).mkString(", ")}\n become ${r.length} nodes = ${r.iterator.map(_.currNodeIdOpt.getOrElse("?")).mkString(", ")}")
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
        val ephemeralNodesFut = coordsNodesFut.flatMap { coordNodes =>
          if (coordNodes.nonEmpty) {
            Future.successful(Nil)
          } else {
            l95_ephemeralNodesFromPool
              .recover { case _ => l99_ephemeralNode :: Nil }
          }
        }

        for {
          nodeLists <- Future.sequence {
            futs1 :+ ephemeralNodesFut
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
      def logoMediaInfoOptFut: Future[Option[IMediaInfo]] = {
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


      /** Поиск отображаемой координаты узла.
        * Карта на клиенте будет отцентрована по этой точке. */
      def nodeGeoPointOptFut: Future[Option[MGeoPoint]] = {
        // Если география уже активна на уровне index-запроса, то тут ничего делать не требуется.
        val mgpOpt = OptionUtil.maybeOpt( isRcvr ) {
          // Если нет географии, то поискать центр для найденного узла.
          // Для нормальных узлов (не районов) следует возвращать клиенту их координату.
          val nodeLocEdges = mnode.edges
            .withPredicateIter( MPredicates.NodeLocation )
            .to( LazyList )

          // Ленивая коллекция гео-точек для NodeLocation-эджей
          val edgesPoints = nodeLocEdges
            .flatMap { medge =>
              medge.info
                .geoPoints
                .headOption
                .orElse {
                  medge.info.geoShapes
                    .iterator
                    .flatMap { gs =>
                      gs.shape.centerPoint
                    }
                    .nextOption()
                }
            }

          // Надо вернуть
          // - Ближайшую к текущей локации точку, чтобы избежать ситуации.
          // - либо первую центральную точку,
          // - либо первую попавшующся точку вообще.
          val r = _qs.common.locEnv.geoLocOpt
            // Если есть хотя бы 2 точки, то надо выбирать ближайшую.
            .filter { _ => edgesPoints.lengthCompare(1) > 0 }
            .map { geoLoc =>
              // Есть геолокация. Найти ближайшую точку среди имеющихся.
              edgesPoints.minBy { centerPoint =>
                CoordOps.distanceXY[MGeoPoint, GeoCoord_t]( centerPoint, geoLoc.point )
                  .abs
                  .doubleValue
              }( Ordering.Double.TotalOrdering )
            }
            // Нет геолокации - ищем первую попавщуюся центральную точку:
            .orElse {
              edgesPoints.headOption
            }
            // Нет центральных точек - взять первую попавшуюся из любого шейпа.
            .orElse {
              (for {
                nlEdge <- nodeLocEdges.iterator
                gs <- nlEdge.info.geoShapes
              } yield {
                gs.shape.firstPoint
              })
                .nextOption()
            }

          LOGGER.trace(s"$logPrefix Node#${mnode.id} geo pt. => $r")
          r
        }

        Future.successful(mgpOpt)
      }

      /** RespAction для index-ответа. */
      val indexRespActionFut: Future[Option[MSc3IndexResp]] = {
        val _logoMediaInfoOptFut = logoMediaInfoOptFut
        val _welcomeInfoOptFut   = welcomeInfoOptFut
        val _nodeGeoPointOptFut  = nodeGeoPointOptFut
        val _isMyNodeFut         = isMyNodeFut
        // Возвращать геолокацию юзера только если затребовано в исходном запросе.
        val _reqGeoLocOptFut =
          if (_scIndexArgs.retUserLoc) geoIpLocOptFut
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
            ntype         = mnode.common.ntype,
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
      val _geoIpResOptFut   = logic.geoIpResOptFut

      // Исполнение синхронных задач.
      val _remoteIp         = logic._remoteIp

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



    /** true, если вызов идёт из [[ScIndexAdOpen]]. */
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
          whPx   = whPxOpt
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
    def apply(qs: MScQs)(implicit request: IReq[_]): ScIndexLogic = {
      val scApiVsn = qs.common.apiVsn
      if (scApiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        ScIndexLogicV3(qs)(request)
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
  case class ScIndexLogicV3(override val _qs: MScQs)
                           (override implicit val _request: IReq[_]) extends ScIndexLogic


}
