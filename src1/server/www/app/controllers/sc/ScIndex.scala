package controllers.sc

import _root_.util.di._
import io.suggest.adn.MAdnRights
import io.suggest.color.MColorData
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.common.geom.d2.MSize2di
import io.suggest.es.model.IMust
import io.suggest.es.search.MSubSearch
import io.suggest.geo.{MGeoLoc, _}
import io.suggest.i18n.MsgCodes
import io.suggest.media.{IMediaInfo, MMediaInfo, MMediaTypes}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{IMNodes, MNodeTypes, NodeNotFoundException}
import io.suggest.sc.MScApiVsns
import io.suggest.sc.index.{MSc3IndexResp, MWelcomeInfo}
import io.suggest.sc.sc3.{MSc3Resp, MSc3RespAction, MScQs, MScRespActionTypes}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.url.MHostInfo
import io.suggest.util.logs.IMacroLogs
import models.im.{MImgT, MImgWithWhInfo}
import models.msc._
import models.mwc.MWelcomeRenderArgs
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc._
import util.acl._
import util.adn.INodesUtil
import util.ble.IBleUtilDi
import util.geo.IGeoIpUtilDi
import util.img.IDynImgUtil
import util.stat.{IStatCookiesUtilDi, IStatUtil}
import japgolly.univeq._
import util.showcase.IScUtil

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.14 12:12
  * Description:
  * Трейт sc index c геолокацей, не завязанной вокруг конкретных узлов, конкретных GeoMode,
  * и работающей далеко за их пределами.
  *
  * Прошлый код вложенных друг в друга куч ScIndex-логик, распиленных на два файла, настолько сложен и запутан,
  * что его перепиливание на новые идеи оказалось слишком непосильной задачей: проще переписать.
  */
trait ScIndex
  extends ScController
  with IMacroLogs
  with IStatCookiesUtilDi
  with IMaybeAuth
  with IMNodes
  with INodesUtil
  with IWelcomeUtil
  with IScUtil
  with IGeoIpUtilDi
  with IStatUtil
  with IBleUtilDi
  with IDynImgUtil
  with IIsNodeAdmin
{

  import mCommonDi._


  /**
    * Второе поколение геолокационной v2-выдачи.
    * Произошла унификация geoShowcase() и showcase()-экшенов, зоопарка расходящихся index-логик.
    * + by-design возможность выхода за пределы мышления в понятиях узлов-ресиверов.
    *
    * @param args Все qs-данные запроса экшена ровно одним набором.
    * @return 200 OK + index выдачи в виде JSON.
    */
  // U.PersonNode запрашивается в фоне для сбора статистики внутри экшена.
  def index(args: MScQs) = maybeAuth(U.PersonNode).async { implicit request =>
    lazy val logPrefix = s"index[${System.currentTimeMillis()}]:"

    // Чтобы избегать корявой многоэтажности, экспериментируем с Either:
    (
      if (args.index.nonEmpty) Right(None)
      else Left {
        LOGGER.debug(s"$logPrefix No or invalid .index in scQs = $args")
        BadRequest("index args missing")
      }
    )
      // В зависимости от версии API выбрать используемую логику сборки ответа.
      .flatMap { _ =>
        val logicOrNull = if (args.common.apiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
          ScIndexLogicHttp(args)(request)
        } else {
          LOGGER.error(s"$logPrefix No logic available for api vsn: ${args.common.apiVsn}. Forgot to implement? args = $args")
          null
        }
        Option( logicOrNull )
          .toRight(
            NotImplemented(s"Sc Index API not implemented for ${args.common.apiVsn}.")
          )
      }
      // Запустить выбранную логику на исполнение:
      .map { logic =>
        // Собираем http-ответ.
        val resultFut = for (res <- logic.resultFut) yield {
          // Настроить кэш-контрол. Если нет locEnv, то для разных ip будут разные ответы, и CDN кешировать их не должна.
          val (cacheControlMode, hdrs0) = if (!args.common.locEnv.isEmpty) {
            "private" -> ((VARY -> X_FORWARDED_FOR) :: Nil)
          } else {
            "public" -> Nil
          }
          val hdrs1 = CACHE_CONTROL -> s"$cacheControlMode, max-age=${scUtil.SC_INDEX_CACHE_SECONDS}" ::
            hdrs0
          res.withHeaders(hdrs1: _*)
        }

        // Собираем статистику второго поколения...
        logic.saveScStat()

        resultFut.recover { case ex: NodeNotFoundException =>
          // Эта ветвь вообще когда-нибудь вызывается? indexNodeFut всегда какой-то узел вроде возвращает.
          LOGGER.trace(s"$logPrefix ${logic.logPrefix}: everything is missing; args = $args", ex)
          NotFound( ex.getMessage )
        }
      }
      .fold[Future[Result]]( identity(_), identity )
  }


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

    /** ip-геолокация, когда гео-координаты или иные полезные данные клиента отсутствуют. */
    def reqGeoLocFut: Future[Option[MGeoLoc]] = {
      geoIpUtil.geoLocOrFromIp( _qs.common.locEnv.geoLocOpt )(geoIpResOptFut)
    }
    lazy val reqGeoLocFutVal = reqGeoLocFut


    /** #00: поиск узла по id ресивера, указанного в qs.
      * Future[NSEE], когда нет необходимого узла. */
    def l00_rcvrByIdFut: Future[MIndexNodeInfo] = {
      val adnIdOpt = _scIndexArgs.nodeId

      val rFut = for {
        mnodeOpt <- mNodesCache.maybeGetByIdCached( adnIdOpt )
      } yield {
        // Если узел-ресивер запрошен, но не найден, прервать работу над index: это какая-то нештатная ситуация.
        if (mnodeOpt.isEmpty && adnIdOpt.nonEmpty)
          throw new NodeNotFoundException(adnIdOpt.orNull)
        // Может быть узел найден?
        val mnode = mnodeOpt.get
        // Узел есть, вернуть положительный результат работы.
        LOGGER.trace(s"$logPrefix Found rcvr node[${mnode.idOrNull}]: ${mnode.guessDisplayNameOrIdOrEmpty}")
        MIndexNodeInfo(mnode, isRcvr = true)
      }
      // Залоггировать возможное неизвестное исключение
      rFut.failed.foreach {
        case _: NodeNotFoundException =>
          LOGGER.warn(s"$logPrefix Rcvr node missing: $adnIdOpt! Somebody scanning our nodes? I'll try to geolocate rcvr node.")
        case ex: Throwable if !ex.isInstanceOf[NoSuchElementException] =>
          LOGGER.warn(s"$logPrefix Unknown exception while getting rcvr node $adnIdOpt", ex)
        case _ => // do nothing
      }
      rFut
    }


    /** #10: Определение текущего узла выдачи по ближним маячкам. */
    def l10_detectUsingNearBeacons: Future[MIndexNodeInfo] = {
      // Ищем активные узлы-ресиверы, относящиеся к видимым маячкам.
      val searchOpt = bleUtil.scoredByDistanceBeaconSearch(
        maxBoost    = 100000F,
        predicates  = Seq( MPredicates.PlacedIn ),
        bcns        = _qs.common.locEnv.bleBeacons
      )
      searchOpt.fold [Future[MIndexNodeInfo] ] {
        Future.failed( new NoSuchElementException("no beacons") )
      } { bcnSearch =>
        val subSearch = MSubSearch(
          search = bcnSearch,
          must   = IMust.MUST
        )
        val msearch = new MNodeSearchDfltImpl {
          override def subSearches  = subSearch :: Nil
          override def isEnabled    = Some(true)
          override def nodeTypes    = MNodeTypes.AdnNode :: Nil
          override def limit        = 1
        }
        val nearBeaconHolderOptFut = mNodes.dynSearchOne(msearch)
        for (mnodeOpt <- nearBeaconHolderOptFut) yield {
          LOGGER.trace(s"$logPrefix detectUsingNearBeacons: => ${mnodeOpt.flatMap(_.id)} ${mnodeOpt.flatMap(_.guessDisplayNameOrId)}")
          val mnode = mnodeOpt.get
          MIndexNodeInfo(
            mnode = mnode,
            isRcvr = true
          )
        }
      }
    }

    /** поискать покрывающий ТЦ/город/район. */
    def l50_detectUsingCoords: Future[MIndexNodeInfo] = {
      // Если с ресивером по id не фартует, но есть данные геолокации, то заодно запускаем поиск узла-ресивера по геолокации.
      // В понятиях старой выдачи, это поиск активного узла-здания.
      // Нет смысла выносить этот асинхронный код за пределы recoverWith(), т.к. он или не нужен, или же выполнится сразу синхронно.
      val _reqGeoLocFut = reqGeoLocFutVal

      _reqGeoLocFut.flatMap { geoLocOpt =>
        // Пусть будет сразу NSEE, если нет данных геолокации.
        val geoLoc = geoLocOpt.get
        LOGGER.trace(s"$logPrefix Detect node using geo-loc: $geoLoc")

        // Пройтись по всем геоуровням, запустить везде параллельные поиски узлов в точке, закинув в recover'ы.
        val someTrue = Some(true)
        val circle = CircleGs(geoLoc.point, radiusM = 1)
        val qShape = CircleGsJvm.toEsQueryMaker( circle )
        val someGeoPoint = Some(geoLoc.point)
        val nodeLocPred = MPredicates.NodeLocation

        // Если запрещено погружение в реальные узлы-ресиверы (геолокация), то запрещаем получать узлы-ресиверы от elasticsearch:
        val (withAdnRights1, adnRightsMustOrNot1) = if (!_scIndexArgs.geoIntoRcvr) {
          // Запрещено погружаться в ресиверы. Значит, ищем просто узел-обёртку для выдачи, а не ресивер.
          (MAdnRights.RECEIVER :: Nil, false)
        } else {
          (Nil, true)
        }

        val nglsResultsFut = Future.traverse(MNodeGeoLevels.values: Iterable[MNodeGeoLevel]) { ngl =>
          val msearch = new MNodeSearchDfltImpl {
            override def limit = 1
            // Очень маловероятно, что сортировка по близости к точке нужна, но мы её всё же оставим
            override def withGeoDistanceSort = someGeoPoint
            // Неактивные узлы сразу вылетают из выдачи.
            override def isEnabled = someTrue
            override def outEdges: Seq[ICriteria] = {
              val gsCr = GsCriteria(
                levels = ngl :: Nil,
                shapes = qShape :: Nil
              )
              val cr = Criteria(
                predicates  = nodeLocPred :: Nil,
                gsIntersect = Some(gsCr)
              )
              cr :: Nil
            }
            override def withAdnRights = withAdnRights1
            override def adnRightsMustOrNot = adnRightsMustOrNot1
          }
          // Запустить поиск по запрошенным адресам.
          for (
            mnodeOpt <- mNodes.dynSearchOne(msearch)
          ) yield {
            LOGGER.trace(s"$logPrefix $geoLoc on level $ngl => ${mnodeOpt.flatMap(_.id).orNull}")
            for (mnode <- mnodeOpt) yield {
              MIndexNodeInfo(
                mnode  = mnode,
                // 2018-03-23 Проверка упрощена. TODO Можно попытаться вынести её на уровень поиска в индексе по adnRights. Сортировать по RCVR и ngl (а как по дважды-nested сортировать???), и сразу получить нужный элемент.
                isRcvr = mnode.extras.isRcvr
              )
            }
          }
        }

        // Получить первый успешный результат или вернуть NSEE.
        val fut1 = for (rs <- nglsResultsFut) yield {
          val resNode = rs
            .iterator
            .flatten
            .next
          LOGGER.trace(s"$logPrefix First-detected node#${resNode.mnode.idOrNull} isRcvr?${resNode.isRcvr}")
          resNode
        }

        // Записываем в логи промежуточные итоги геолокации.
        fut1.onComplete {
          case Success(info) =>
            LOGGER.trace(s"$logPrefix Geolocated receiver[${info.mnode.id.orNull}] ''${info.mnode.guessDisplayNameOrIdOrEmpty}'' for $geoLoc")
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
    def l95_ephemeralNodeFromPool: Future[MIndexNodeInfo] = {
      // Нужно выбирать эфемерный узел с учётом языка реквеста. Используем i18n как конфиг.
      val ephNodeId = Option ( ctx.messages("conf.sc.node.ephemeral.id") )
        .filter(_.nonEmpty)
        .get
      val _mnodeOptFut = mNodesCache.getById( ephNodeId )
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
        )
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

    /** Узел, для которого будем рендерить index.
      * Хоть какой-то узел обязательно, но не обязательно реальный существующий узел-ресивер.
      */
    def indexNodeFut: Future[MIndexNodeInfo] = {
      /* Логика работы:
       * - Поискать id желаемого ресивера внутрях reqArgs.
       * - Поискать узла-владельца ближайшего BLE-маячка.
       * - Если нет, попробовать поискать узлы с помощью геолокации.
       * - С помощью геолокации можно найти как узла-ресивера, так и покрывающий узел (район, город),
       * который может использоваться для доступа к title, welcome, дизайну выдачи.
       * - Если узла по-прежнему не видно, то надо бы его придумать или взять из некоего пула универсальных узлов.
       */

      // Пытаемся пробить ресивера по переданному id узла выдачи.
      var mnodeFut: Future[MIndexNodeInfo] = l00_rcvrByIdFut

      // Ищем на очень ближних маячках.
      mnodeFut = mnodeFut.recoverWith { case _: NoSuchElementException =>
        LOGGER.trace(s"$logPrefix mnode by id not found, trying to lookup for beacons nearby...")
        l10_detectUsingNearBeacons
      }

      // Надо отработать ситуацию, когда нет узла в данной точки: поискать покрывающий город/район.
      mnodeFut = mnodeFut.recoverWith { case _: NoSuchElementException =>
        LOGGER.trace(s"$logPrefix no beacons nearby, trying to use geoloc...")
        l50_detectUsingCoords
      }

      // Если всё ещё нет узла. Это нормально, надобно найти в пуле или придумать какой-то рандомный узел, желательно без id даже.
      mnodeFut = mnodeFut.recoverWith { case _: NoSuchElementException =>
        l95_ephemeralNodeFromPool
      }

      // Should never happen. Придумывание узла "на лету".
      mnodeFut = mnodeFut.recover { case ex: NoSuchElementException =>
        LOGGER.warn(s"$logPrefix Unable to find index node, making new ephemeral node", ex)
        l99_ephemeralNode
      }

      // Вернуть получившийся в итоге асинхронный результат
      mnodeFut
    }

    lazy val indexNodeFutVal = indexNodeFut


    /** Получение графического логотипа узла, если возможно. */
    lazy val logoImgOptFut: Future[Option[MImgT]] = {
      for {
        currNode     <- indexNodeFutVal
        logoOptRaw = logoUtil.getLogoOfNode(currNode.mnode)
        logoOptScr   <- logoUtil.getLogoOpt4scr(logoOptRaw, _qs.common.screen)
      } yield {
        logoOptScr
      }
    }


    /** Получение карточки приветствия. */
    lazy val welcomeOptFut: Future[Option[MWelcomeRenderArgs]] = {
      if (_scIndexArgs.withWelcome) {
        for {
          inxNodeInfo <- indexNodeFutVal
          waOpt       <- welcomeUtil.getWelcomeRenderArgs(inxNodeInfo.mnode, ctx.deviceScreenOpt)(ctx)
        } yield {
          waOpt
        }
      } else {
        Future.successful(None)
      }
    }


    /** Определение заголовка выдачи. */
    def titleFut: Future[String] = {
      for (inxNodeInfo <- indexNodeFutVal) yield {
        inxNodeInfo.mnode
          .meta
          .basic
          .nameOpt
          .getOrElse {
            ctx.messages
              .translate("isuggest", Nil)
              .getOrElse( MsgCodes.`iSuggest` )
          }
      }
    }

    lazy val currInxNodeIdOptFut: Future[Option[String]] = {
      for (inxNodeInfo <- indexNodeFutVal) yield {
        inxNodeInfo.mnode.id
          .filter(_ => inxNodeInfo.isRcvr)
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
            val actType = if (_indexNode.isRcvr) {
              MActionTypes.ScIndexRcvr
            } else {
              MActionTypes.ScIndexCovering
            }
            val inxSa = MAction(
              actions   = actType :: Nil,
              nodeId    = _indexNode.mnode.id.toSeq,
              nodeName  = _indexNode.mnode.guessDisplayName.toSeq
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
      val _logoImgOptFut = logoImgOptFut

      for {
        welcomeOpt <- welcomeOptFut
        logoImgOpt <- _logoImgOptFut

        // Узнать узлы, на которых хранятся связанные картинки.
        mediaHostsMap <- nodesUtil.nodeMediaHostsMap(
          logoImgOpt = logoImgOpt,
          welcomeOpt = welcomeOpt
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
          val bgImageFut = _imgWithWhOpt2mediaInfo( wc.bg.right.toOption )
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

    /** Поиск отображаемой координаты узла.
      * Карта на клиенте будет отцентрована по этой точке. */
    def nodeGeoPointOptFut: Future[Option[MGeoPoint]] = {
      // Если география уже активна на уровне index-запроса, то тут ничего делать не требуется.
      if (!isFocusedAdOpen && _qs.common.locEnv.geoLocOpt.nonEmpty) {
        // Не искать гео-точку для узла, если география на клиенте и так активна.
        Future.successful( None )

      } else {
        // Если нет географии, то поискать центр для найденного узла.
        for (inxNode <- indexNodeFutVal) yield {
          // Для нормальных узлов (не районов) следует возвращать клиенту их координату.
          OptionUtil.maybeOpt( inxNode.isRcvr ) {
            def nodeLocEdges =
              inxNode.mnode.edges
                .withPredicateIter( MPredicates.NodeLocation )

            val iterCenters = nodeLocEdges
              .flatMap { medge =>
                medge.info.geoPoints
                  .headOption
                  .orElse {
                    medge.info.geoShapes
                      .iterator
                      .flatMap { gs =>
                        gs.shape.centerPoint
                      }
                      .toStream
                      .headOption
                  }
              }

            val iterFirstPoints = nodeLocEdges
              .flatMap(_.info.geoShapes)
              .map(_.shape.firstPoint)

            val r = (iterCenters ++ iterFirstPoints)
              .toStream
              .headOption

            LOGGER.trace(s"$logPrefix Node#${inxNode.mnode.id} geo pt. => $r")
            r
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
        currInxNodeIdOptFut.flatMap { currNodeIdOpt =>
          currNodeIdOpt.fold ( Future.successful(false) ) { nodeId =>
            isNodeAdmin.isAdnNodeAdmin(adnId = nodeId, user = _request.user)
              .map(_.nonEmpty)
          }
        }
      }
    }

    /** index-ответ сервера с данными по узлу. */
    override def respActionFut: Future[MSc3RespAction] = {
      val _nodeIdOptFut        = currInxNodeIdOptFut
      val _nodeNameFut         = titleFut
      val _nodeInfoFut         = indexNodeFutVal
      val _logoMediaInfoOptFut = logoMediaInfoOptFut
      val _welcomeInfoOptFut   = welcomeInfoOptFut
      val _nodeGeoPointOptFut  = nodeGeoPointOptFut
      val _isMyNodeFut         = isMyNodeFut
      for {
        nodeIdOpt       <- _nodeIdOptFut
        nodeName        <- _nodeNameFut
        nodeInfo        <- _nodeInfoFut
        logoOpt         <- _logoMediaInfoOptFut
        welcomeOpt      <- _welcomeInfoOptFut
        nodeGeoPointOpt <- _nodeGeoPointOptFut
        isMyNode        <- _isMyNodeFut
      } yield {
        val resp = MSc3IndexResp(
          nodeId        = nodeIdOpt,
          name          = Option( nodeName ),
          colors        = nodeInfo.mnode.meta.colors,
          logoOpt       = logoOpt,
          welcome       = welcomeOpt,
          geoPoint      = nodeGeoPointOpt,
          isMyNode      = isMyNode,
          isRcvr        = nodeInfo.isRcvr
        )
        MSc3RespAction(
          acType = respActionType,
          index  = Some( resp )
        )
      }
    }

    /**
      * v3-выдача рендерится на клиенте через react.js.
      * С сервера отправляются только данные для рендера.
      */
    def resultFut: Future[Result] = {
      for {
        indexRespAction <- respActionFut
      } yield {
        // Завернуть index-экшен в стандартный scv3-контейнер:
        val scResp = MSc3Resp(
          respActions = indexRespAction :: Nil
        )

        // Вернуть HTTP-ответ.
        Ok( Json.toJson(scResp) )
          .cacheControl( 20 )
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
  case class ScIndexLogicHttp(override val _qs: MScQs)
                             (override implicit val _request: IReq[_]) extends ScIndexLogic

}
