package controllers.sc

import _root_.util.di._
import io.suggest.es.model.IMust
import io.suggest.es.search.MSubSearch
import io.suggest.geo._
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.model.n2.node.{IMNodes, NodeNotFoundException}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models._
import models.im.MImgT
import models.mgeo.MGeoLoc
import models.msc._
import models.msc.resp.{MScResp, MScRespAction, MScRespActionTypes, MScRespIndex}
import org.elasticsearch.common.unit.DistanceUnit
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.adn.INodesUtil
import util.ble.IBleUtilDi
import util.geo.IGeoIpUtilDi
import util.stat.{IStatCookiesUtilDi, IStatUtil}
import views.html.sc._

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
  def index(args: MScIndexArgs) = maybeAuth(U.PersonNode).async { implicit request =>
    val logic = new ScIndexUniLogicImpl {
      override def _reqArgs  = args
      override def _syncArgs = MScIndexSyncArgs.empty
      override def _request  = request
    }

    // Собираем http-ответ.
    val resultFut = for (res <- logic.result) yield {
      // Настроить кэш-контрол. Если нет locEnv, то для разных ip будут разные ответы, и CDN кешировать их не должна.
      val (cacheControlMode, hdrs0) = if (!args.locEnv.isEmpty) {
        "private" -> List(VARY -> X_FORWARDED_FOR)
      } else {
        "public" -> Nil
      }
      val hdrs1 = CACHE_CONTROL -> s"$cacheControlMode, max-age=${scUtil.SC_INDEX_CACHE_SECONDS}"  ::  hdrs0
      res.withHeaders(hdrs1: _*)
    }

    // Собираем статистику второго поколения...
    logic.saveScStat()

    resultFut.recover { case ex: NodeNotFoundException =>
      // Эта ветвь вообще когда-нибудь вызывается? indexNodeFut всегда какой-то узел вроде возвращает.
      LOGGER.trace(s"index($args) ${logic.logPrefix}: everything is missing", ex)
      NotFound( ex.getMessage )
    }
  }


  /** Унифицированная логика выдачи в фазе index. */
  trait ScIndexUniLogic extends LogicCommonT { logic =>

    /** qs-аргументы реквеста. */
    def _reqArgs: MScIndexArgs

    /** Параметры, приходящие из sync site.  */
    def _syncArgs: IScIndexSyncArgs

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
      geoIpUtil.geoLocOrFromIp( _reqArgs.locEnv.geoLocOpt )(geoIpResOptFut)
    }
    lazy val reqGeoLocFutVal = reqGeoLocFut


    /** #00: поиск узла по id ресивера, указанного в qs.
      * Future[NSEE], когда нет необходимого узла. */
    lazy val l00_rcvrByIdFut: Future[MIndexNodeInfo] = {
      val adnIdOpt = _reqArgs.adnIdOpt

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
      rFut.onFailure {
        case ex: NodeNotFoundException =>
          LOGGER.warn(s"$logPrefix Rcvr node missing: $adnIdOpt! Somebody scanning our nodes? I'll try to geolocate rcvr node.")
        case ex: Throwable if !ex.isInstanceOf[NoSuchElementException] =>
          LOGGER.warn(s"$logPrefix Unknown exception while getting rcvr node $adnIdOpt", ex)
      }
      rFut
    }


    /** #10: Определение текущего узла выдачи по ближним маячкам. */
    lazy val l10_detectUsingNearBeacons: Future[MIndexNodeInfo] = {
      // Ищем активные узлы-ресиверы, относящиеся к видимым маячкам.
      val searchOpt = bleUtil.scoredByDistanceBeaconSearch(
        maxBoost    = 100000F,
        predicates  = Seq( MPredicates.PlacedIn ),
        bcns        = _reqArgs.locEnv.bleBeacons
      )
      searchOpt.fold [Future[MIndexNodeInfo] ] {
        Future.failed( new NoSuchElementException("no beacons") )
      } { bcnSearch =>
        val subSearch = MSubSearch(
          search = bcnSearch,
          must   = IMust.MUST
        )
        val msearch = new MNodeSearchDfltImpl {
          override def subSearches  = Seq(subSearch)
          override def isEnabled    = Some(true)
          override def nodeTypes    = Seq(MNodeTypes.AdnNode)
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
    lazy val l50_detectUsingCoords: Future[MIndexNodeInfo] = {
      // Если с ресивером по id не фартует, но есть данные геолокации, то заодно запускаем поиск узла-ресивера по геолокации.
      // В понятиях старой выдачи, это поиск активного узла-здания.
      // Нет смысла выносить этот асинхронный код за пределы recoverWith(), т.к. он или не нужен, или же выполнится сразу синхронно.
      val _reqGeoLocFut = reqGeoLocFutVal

      _reqGeoLocFut.flatMap { geoLocOpt =>
        // Пусть будет сразу NSEE, если нет данных геолокации.
        val geoLoc = geoLocOpt.get

        // Пройтись по всем геоуровням, запустить везде параллельные поиски узлов в точке, закинув в recover'ы.
        val someTrue = Some(true)
        val nglsResultsFut = Future.traverse(NodeGeoLevels.valuesT: Iterable[NodeGeoLevel]) { ngl =>
          val msearch = new MNodeSearchDfltImpl {
            override def limit = 1
            // Очень маловероятно, что сортировка по близости к точке нужна, но мы её всё же оставим
            override def withGeoDistanceSort: Option[MGeoPoint] = {
              Some(geoLoc.center)
            }
            // Неактивные узлы сразу вылетают из выдачи.
            override def isEnabled = someTrue
            override def outEdges: Seq[ICriteria] = {
              val circle = CircleGs(geoLoc.center, Distance(10, DistanceUnit.METERS))
              val qShape = CircleGs.toEsQueryMaker( circle )
              val gsCr = GsCriteria(
                levels = ngl :: Nil,
                shapes = qShape :: Nil
              )
              val cr = Criteria(
                predicates  = MPredicates.NodeLocation :: Nil,
                gsIntersect = Some(gsCr)
              )
              cr :: Nil
            }
          }

          for (mnodeOpt <- mNodes.dynSearchOne(msearch)) yield {
            for (mnode <- mnodeOpt) yield {
              MIndexNodeInfo(
                mnode  = mnode,
                isRcvr = ngl.isLowest     // Только здания могут выступать ресиверами.
              )
            }
          }
        }
        // Получить первый успешный результат или вернуть NSEE.
        val fut1 = for (rs <- nglsResultsFut) yield {
          rs.iterator
            .flatten
            .next
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
    lazy val l95_ephemeralNodeFromPool: Future[MIndexNodeInfo] = {
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
          nameOpt     = Some("iSuggest"),
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
    def logoImgOptFut: Future[Option[MImgT]] = {
      for {
        currNode     <- indexNodeFutVal
        logoOptRaw   <- logoUtil.getLogoOfNode(currNode.mnode)
        logoOptScr   <- logoUtil.getLogoOpt4scr(logoOptRaw, _reqArgs.screen)
      } yield {
        logoOptScr
      }
    }


    /** Получение карточки приветствия. */
    def welcomeOptFut: Future[Option[WelcomeRenderArgsT]] = {
      if (_reqArgs.withWelcome) {
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


    /** Рендер indexTpl. */
    def respHtmlFut: Future[Html] = {
      for (renderArgs <- indexTplRenderArgsFut) yield {
        indexTpl(renderArgs)(ctx)
      }
    }

    /** Рендер минифицированного indexTpl. */
    def respHtml4JsFut = respHtmlFut.map( htmlCompressUtil.html2str4json )

    /** Контейнер палитры выдачи. */
    lazy val colorsFut: Future[IColors] = {
      for (mnodeInfo <- indexNodeFutVal) yield {
        def _codeF(mcd: MColorData) = mcd.code
        val colors = mnodeInfo.mnode.meta.colors
        val _bgColor = colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_codeF)
        val _fgColor = colors.fg.fold(scUtil.SITE_FGCOLOR_DFLT)(_codeF)
        Colors(bgColor = _bgColor, fgColor = _fgColor)
      }
    }

    lazy val hBtnArgsFut: Future[HBtnArgs] = {
      for (colors <- colorsFut) yield {
        HBtnArgs(fgColor = colors.fgColor)
      }
    }


    /** Если узел с географией не связан, и есть "предыдущий" узел, то надо отрендерить кнопку "назад". */
    def topLeftBtnHtmlFut: Future[Html] = {
      // Сразу запускаем сборку аргументов hbtn-рендера. Не здесь, так в super-методе они понадобятся точно.
      val _hBtnArgsFut = hBtnArgsFut

      // Отрендерить кнопку "назад на предыдущий узел", только если все проверки выполнены на ок...
      val htmlFut = for {
        // В методе логика немного разветвляется и асинхронна внутри. false-ветвь реализована через Future.failed.
        _     <- {
          if (_reqArgs.prevAdnId.nonEmpty) {
            Future.successful( None )
          } else {
            Future.failed( new NoSuchElementException() )
          }
        }
        mnodeInfo <- indexNodeFutVal
        mnode = mnodeInfo.mnode
        // Продолжать только если текущий узел не связан с географией.
        if {
          val directGpIter = mnode.edges
            .withPredicateIter( MPredicates.GeoParent.Direct )
          directGpIter.isEmpty && {
            // Если в город (верхний узел) перешли из левого подузла, то у города НЕ должна отображаться кнопка "назад",
            // несмотря на отсутствие гео-родителей.
            val stiOpt = mnode.extras.adn
              .flatMap( _.shownTypeIdOpt )
              .flatMap( AdnShownTypes.maybeWithName )
            !stiOpt.exists( _.isTopLevel )
          }
        }
        // Наконец, обратиться к аргументам рендера кнопки.
        hBtnArgs0 <- _hBtnArgsFut
      } yield {
        // Отрендерить кнопку, внеся кое-какие коррективы в аргументы рендера.
        val hBtnArgs2 = hBtnArgs0.copy(adnId = _reqArgs.prevAdnId)
        ScHdrBtns.Back2UpperNode(hBtnArgs2)
      }

      // Что-то не так, но обычно это нормально.
      htmlFut.recoverWith { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.warn(s"$logPrefix topLeftBtnHtmlFut(): Workarounding unexpected expection", ex)

        for (_hBtnArgs <- _hBtnArgsFut) yield {
          val rargs = new MScIndexSyncArgsWrap with IHBtnArgsFieldImpl {
            override def _syncArgsUnderlying = _syncArgs
            override def hBtnArgs = _hBtnArgs
          }
          hdr._navPanelBtnTpl(rargs)(ctx)
        }
      }
    }

    /** Определение заголовка выдачи. */
    lazy val titleFut: Future[String] = {
      for (inxNodeInfo <- indexNodeFutVal) yield {
        inxNodeInfo.mnode
          .meta
          .basic
          .nameOpt
          .getOrElse {
            ctx.messages
              .translate("isuggest", Nil)
              .getOrElse("iSuggest")
          }
      }
    }

        /** Сборка аргументов рендера indexTpl. */
    def indexTplRenderArgsFut: Future[ScRenderArgs] = {
      val _logoImgOptFut      = logoImgOptFut
      val _welcomeOptFut      = welcomeOptFut
      val _topLeftBtnHtmlFut  = topLeftBtnHtmlFut
      val _colorsFut          = colorsFut
      val _hBtnArgsFut        = hBtnArgsFut
      val _titleFut           = titleFut
      for {
        _logoImgOpt           <- _logoImgOptFut
        _welcomeOpt           <- _welcomeOptFut
        _topLeftBtnHtml       <- _topLeftBtnHtmlFut
        _colors               <- _colorsFut
        _hBtnArgs             <- _hBtnArgsFut
        _title                <- _titleFut
      } yield {
        new ScRenderArgs with IColorsWrapper with MScIndexSyncArgsWrap {
          override def hBtnArgs             = _hBtnArgs
          override def _underlying          = _colors
          override def topLeftBtnHtml       = _topLeftBtnHtml
          override def title                = _title
          override def _syncArgsUnderlying  = _syncArgs
          override def logoImgOpt           = _logoImgOpt
          override def welcomeOpt           = _welcomeOpt
          override def apiVsn               = _reqArgs.apiVsn
        }
      }
    }

    def currInxNodeIdOptFut: Future[Option[String]] = {
      for (inxNodeInfo <- indexNodeFutVal) yield {
        inxNodeInfo.mnode.id
          .filter(_ => inxNodeInfo.isRcvr)
      }
    }

    def respArgsFut: Future[MScResp] = {
      val _htmlFut  = respHtml4JsFut
      val _currAdnIdOptFut = currInxNodeIdOptFut

      val _geoPointOptFut = for {
        currAdnIdOpt  <- _currAdnIdOptFut
        // Если нет данных по текущему узлу, то надо вернуть точку из обычной геолокации или geoip.
        geoLocOpt     <- currAdnIdOpt
          .fold[Future[Option[MGeoLoc]]] (reqGeoLocFutVal) { _ => Future.successful(None) }
      } yield {
        geoLocOpt
          .map(_.center)
      }

      val _titleFut = titleFut
      for {
        html              <- _htmlFut
        currAdnIdOpt      <- _currAdnIdOptFut
        _title            <- _titleFut
        _geoPointOpt      <- _geoPointOptFut
      } yield {
        LOGGER.trace(s"$logPrefix adnId=$currAdnIdOpt geoPoint=${_geoPointOpt} | html=${html.length}c | title: '''${_title}'''")
        MScResp(
          scActions = Seq(
            MScRespAction(
              acType = MScRespActionTypes.Index,
              index = Some(
                MScRespIndex(
                  indexHtml     = html,
                  currAdnId     = currAdnIdOpt,
                  title         = Some( _title ),
                  geoPoint      = _geoPointOpt
                )
              )
            )
          )
        )
      }

    }

    /** HTTP-ответ клиенту. */
    def result: Future[Result] = {
      for {
        args <- respArgsFut
      } yield {
        statCookiesUtil.resultWithStatCookie {
          _resultVsn(args)
        }(ctx.request)
      }
    }

    /** Результат запроса. */
    protected def _resultVsn(args: MScResp): Result = {
      val v = _reqArgs.apiVsn
      if (v.majorVsn == MScApiVsns.Sjs1.majorVsn) {
        Ok(Json.toJson(args))
      } else {
        throw new UnsupportedOperationException("Unsupported API vsn: " + v)
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
              actions   = Seq(actType),
              nodeId    = _indexNode.mnode.id.toSeq,
              nodeName  = _indexNode.mnode.guessDisplayName.toSeq
            )
            List(inxSa)
          }
          override def components = MComponents.Index :: super.components
          override def remoteAddr   = _remoteIp
          override def devScreenOpt = _reqArgs.screen
          override def locEnvOpt    = Some(_reqArgs.locEnv)
          override def geoIpLoc     = _geoIpResOpt
        }
      }
    }

  }

  /** Дефолтовая реализация логики ScIndexUniLogicImpl для снижения объемов кодогенерации байткода
    * в конкретных реализациях. */
  abstract class ScIndexUniLogicImpl extends ScIndexUniLogic

}
