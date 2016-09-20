package controllers.sc

import _root_.util.di._
import _root_.util.PlayMacroLogsI
import io.suggest.model.geo.{CircleGs, Distance}
import io.suggest.model.n2.edge.search.{Criteria, GsCriteria, ICriteria}
import io.suggest.model.n2.node.{IMNodes, NodeNotFoundException}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.im.MImgT
import models.jsm.ScIndexResp
import models.mproj.IMCommonDi
import models.msc._
import play.twirl.api.Html
import util.acl._
import views.html.sc._
import models._
import models.mgeo.MGeoLoc
import org.elasticsearch.common.unit.DistanceUnit

import scala.concurrent.Future
import play.api.mvc._
import util.geo.IGeoIpUtilDi

import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 12:12
 * Description: Экшены, генерирующие indexTpl выдачи для узлов вынесены сюда.
 */

/** Константы, используемые в рамках этого куска контроллера. */
trait ScIndexConstants extends IMCommonDi {

  /** Кеш ответа showcase(adnId) на клиенте. */
  def SC_INDEX_CACHE_SECONDS = 20

}



/** Общая утиль, испрользуемая в контроллере. */
trait ScIndexCommon
  extends ScController
  with PlayMacroLogsI
  with IStatUtil
{

  import mCommonDi._

  /** Базовый трейт для написания генератора производных indexTpl и ответов. */
  trait ScIndexHelperBase extends LazyContext {
    def renderArgsFut: Future[ScRenderArgs]
    def currAdnIdFut: Future[Option[String]]
    def _reqArgs: MScIndexArgs
    def _syncArgs: IScIndexSyncArgs

    /** Фьючерс с определением достаточности имеющиейся геолокации для наилучшего определения узла. */
    def geoAcurrEnoughtFut: Future[Option[Boolean]]

    /** Предлагаемый заголовок окна выдачи, если возможно. */
    def titleOptFut: Future[Option[String]]

    /** Сборка значения для titleFut на основе имеющегося узла. */
    protected def _node2titleOpt(mnode: MNode): Option[String] = {
      val m = mnode.meta
      val title0 = m.basic.name
      val title2 = m.address.town.fold(title0)(title0 + " (" + _ + ")")
      Some(title2)
    }

    /** Контейнер палитры выдачи. */
    def colorsFut: Future[IColors]

    def respHtmlFut: Future[Html] = {
      for (renderArgs <- renderArgsFut) yield {
        indexTpl(renderArgs)(ctx)
      }
    }
    def respHtmlJsFut = respHtmlFut.map( htmlCompressUtil.html2jsStr )


    def hBtnArgsFut: Future[HBtnArgs] = {
      for (colors <- colorsFut) yield {
        HBtnArgs(fgColor = colors.fgColor)
      }
    }

    /** Кнопка навигации, которая будет отрендерена в левом верхнем углу indexTpl. */
    def topLeftBtnHtmlFut: Future[Html] = {
      for (_hBtnArgs <- hBtnArgsFut) yield {
        val rargs = new MScIndexSyncArgsWrap with IHBtnArgsFieldImpl {
          override def _syncArgsUnderlying = _syncArgs
          override def hBtnArgs = _hBtnArgs
        }
        hdr._navPanelBtnTpl(rargs)(ctx)
      }
    }

    def respArgsFut: Future[ScIndexResp] = {
      val _currAdnIdOptFut = currAdnIdFut
      for {
        html              <- respHtmlJsFut
        currAdnIdOpt      <- _currAdnIdOptFut
        titleOpt          <- titleOptFut
      } yield {
        ScIndexResp(
          html            = html,
          currAdnId       = currAdnIdOpt,
          titleOpt        = titleOpt
        )
      }
    }

    protected def _resultVsn(args: ScIndexResp): Future[Result] = {
      _reqArgs.apiVsn match {
        case MScApiVsns.Sjs1 =>
          Ok(args.toJson)
        case other =>
          throw new UnsupportedOperationException("Unsupported API vsn: " + other.versionNumber)
      }
    }
    
    def result: Future[Result] = {
      for {
        args <- respArgsFut
        res  <- _resultVsn(args)
      } yield {
        // TODO Нужен аккуратный кеш тут. Проблемы с просто cache-control возникают, если список категорий изменился или
        // произошло какое-то другое изменение
        statUtil.resultWithStatCookie {
          res
        }(ctx.request)
      }
    }

  }

}



/** Вспомогательная утиль для рендера indexTpl на нодах. */
trait ScIndexNodeCommon
  extends ScIndexCommon
  with ScIndexConstants
  with IWelcomeUtil
  with IScUtil
{

  import mCommonDi._

  /** Логика формирования indexTpl для конкретного узла. */
  trait ScIndexNodeHelper extends ScIndexHelperBase {
    def adnNodeFut        : Future[MNode]
    override lazy val currAdnIdFut: Future[Option[String]] = adnNodeFut.map(_.id)

    /** В рамках showcase(adnId) геолокация не требуется, узел и так известен. */
    override def geoAcurrEnoughtFut: Future[Option[Boolean]] = {
      Future.successful( None )
    }

    /** Есть узел -- есть заголовок. */
    override def titleOptFut: Future[Option[String]] = {
      adnNodeFut
        .map { _node2titleOpt }
    }

    override lazy val hBtnArgsFut = super.hBtnArgsFut

    /** Если узел с географией не связан, и есть "предыдущий" узел, то надо отрендерить кнопку "назад". */
    override def topLeftBtnHtmlFut: Future[Html] = {
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
        mnode <- adnNodeFut
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
          LOGGER.error("topLeftBtnHtmlFut(): Workarounding unexpected expection", ex)
        super.topLeftBtnHtmlFut
      }
    }

    /** Получение карточки приветствия. */
    def welcomeAdOptFut: Future[Option[WelcomeRenderArgsT]] = {
      if (_reqArgs.withWelcome) {
        adnNodeFut.flatMap { adnNode =>
          welcomeUtil.getWelcomeRenderArgs(adnNode, ctx.deviceScreenOpt)(ctx)
        }
      } else {
        Future.successful(None)
      }
    }

    /** Получение графического логотипа узла, если возможно. */
    def logoImgOptFut: Future[Option[MImgT]] = {
      for {
        currNode     <- adnNodeFut
        logoOptRaw   <- logoUtil.getLogoOfNode(currNode)
        logoOptScr   <- logoUtil.getLogoOpt4scr(logoOptRaw, _reqArgs.screen)
      } yield {
        logoOptScr
      }
    }


    /** Контейнер палитры выдачи. */
    override def colorsFut: Future[IColors] = {
      for (adnNode <- adnNodeFut) yield {
        val _bgColor = adnNode.meta.colors.bg.fold(scUtil.SITE_BGCOLOR_DFLT)(_.code)
        val _fgColor = adnNode.meta.colors.fg.fold(scUtil.SITE_FGCOLOR_DFLT)(_.code)
        Colors(bgColor = _bgColor, fgColor = _fgColor)
      }
    }

    /** Приготовить аргументы рендера выдачи. */
    override def renderArgsFut: Future[ScRenderArgs] = {
      val _adnNodeFut         = adnNodeFut
      val _logoImgOptFut      = logoImgOptFut
      val _colorsFut          = colorsFut
      val _hBtnArgsFut        = hBtnArgsFut
      val _topLeftBtnHtmlFut  = topLeftBtnHtmlFut
      for {
        waOpt           <- welcomeAdOptFut
        adnNode         <- _adnNodeFut
        _logoImgOpt     <- _logoImgOptFut
        _colors         <- _colorsFut
        _hBtnArgs       <- _hBtnArgsFut
        _topLeftBtnHtml <- _topLeftBtnHtmlFut
      } yield {
        new ScRenderArgs with IColorsWrapper {
          /** Аргументы исходного запроса экшена. */
          override def _underlying        = _colors
          override def hBtnArgs           = _hBtnArgs
          override def topLeftBtnHtml     = _topLeftBtnHtml
          override def title              = adnNode.meta.basic.name
          override def logoImgOpt         = _logoImgOpt
          override def welcomeOpt         = waOpt
        }
      }
    }

  }

}


/** Экшены для рендера indexTpl нод. */
trait ScIndexNode
  extends ScIndexNodeCommon
  with IScStatUtil
  with AdnNodeMaybeAuth
{

  import mCommonDi._

  /** Базовая выдача для rcvr-узла sio-market. */
  def showcase(adnId: String, args: MScIndexArgs) = AdnNodeMaybeAuth(adnId).async { implicit request =>
    val _adnNodeFut = Future.successful( request.mnode )

    val helper = new ScIndexNodeHelper {
      override def _reqArgs           = args
      override def _syncArgs          = MScIndexSyncArgs.empty
      override def adnNodeFut         = _adnNodeFut
      override lazy val currAdnIdFut  = Future.successful( Some(adnId) )
      override implicit def _request  = request
    }

    val resultFut = helper.result

    // собираем статистику, пока идёт подготовка результата
    val stat = scStatUtil.IndexStat(
      gsiFut    = args.geo.geoSearchInfoOpt,
      screenOpt = helper.ctx.deviceScreenOpt,
      nodeOpt   = Some(request.mnode)
    )
    stat.saveStats.onFailure { case ex =>
      LOGGER.warn(s"showcase($adnId): failed to save stats, args = $args", ex)
    }

    // Возвращаем результат основного действа. Результат вполне кешируем по идее.
    for (res <- resultFut) yield {
      res.withHeaders(CACHE_CONTROL -> s"public, max-age=$SC_INDEX_CACHE_SECONDS")
    }
  }

}


/**
  * Трейт sc index c геолокацей, не завязанной вокруг конкретных узлов, конкретного GeoMode,
  * и работающей далеко за их пределами.
  *
  * Прошлый код вложенных друг в друга куч ScIndex-логик, распиленных на два файла, настолько сложен и запутан,
  * что его перепиливание на новые идеи оказалось непосильной задачей.
  */
trait ScIndex2
  extends ScController
  with PlayMacroLogsI
  with IStatUtil
  with MaybeAuth
  with IMNodes
  with INodesUtil
  with IWelcomeUtil
  with IScUtil
  with IGeoIpUtilDi
  with ScIndexConstants
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
  def index(args: MScIndexArgs) = MaybeAuth().async { implicit request =>
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
      val hdrs1 = CACHE_CONTROL -> s"$cacheControlMode, max-age=$SC_INDEX_CACHE_SECONDS"  ::  hdrs0
      res.withHeaders(hdrs1: _*)
    }

    // TODO Нужна новая и хорошая годная статистика для выдачи с новой геолокацией.

    resultFut.recover { case ex: NodeNotFoundException =>
      LOGGER.trace(s"index($args): node missing", ex)
      NotFound( ex.getMessage )
    }
  }


  /** Унифицированная логика выдачи в фазе index. */
  trait ScIndexUniLogic extends LazyContext { logic =>

    /** qs-аргументы реквеста. */
    def _reqArgs: MScIndexArgs

    /** Параметры, приходящие из sync site.  */
    def _syncArgs: IScIndexSyncArgs


    /** ip-геолокация, когда гео-координаты или иные полезные данные клиента отсутствуют. */
    def reqGeoLocFut: Future[Option[MGeoLoc]] = {
      _reqArgs.locEnv.geoLocOpt.fold [Future[Option[MGeoLoc]]] {
        val remoteIp = geoIpUtil.fixRemoteAddr( _request.remoteAddress )
        lazy val logPrefix = s"reqGeoLocFut(${System.currentTimeMillis}):"
        LOGGER.trace(s"$logPrefix locEnv empty, trying geolocate by ip: $remoteIp")
        val geoLoc2Fut = for (geoIpOpt <- geoIpUtil.findIpCached(remoteIp)) yield {
          for (geoIp <- geoIpOpt) yield {
            MGeoLoc( geoIp.center )
          }
        }
        // Подавить и залоггировать возможные проблемы.
        geoLoc2Fut.recover { case ex: Throwable =>
          LOGGER.warn(s"$logPrefix failed to geolocate by ip=$remoteIp", ex)
          None
        }
      } { r =>
        Future.successful( Some(r) )
      }
    }


    /** Узел, для которого будем рендерить index.
      * Хоть какой-то узел обязательно, но не обязательно реальный существующий узел-ресивер.
      */
    lazy val indexNodeFut: Future[MIndexNodeInfo] = {
      /* Логика работы:
       * - Поискать id желаемого ресивера внутрях reqArgs.
       * - Если нет, попробовать поискать узлы с помощью геолокации.
       * - С помощью геолокации можно найти как узла-ресивера, так и покрывающий узел (район, город),
       * который может использоваться для доступа к title, welcome, дизайну выдачи.
       * - Если узла по-прежнему не видно, то надо бы его придумать или взять из некоего пула универсальных узлов.
       */

      // Запускаем поиск узла-ресивера по возможно переданному id.
      lazy val logPrefix = s"indexNodeFut(${System.currentTimeMillis()}):"

      // Частоиспользуемый id узла из реквеста.
      val adnIdOpt = _reqArgs.adnIdOpt

      // Пытаемся пробить ресивера по переданному id узла выдачи.
      var mnodeFut: Future[MIndexNodeInfo] = for {
        mnodeOpt <- mNodeCache.maybeGetByIdCached( adnIdOpt )
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
      mnodeFut.onFailure {
        case ex: NodeNotFoundException =>
          LOGGER.warn(s"$logPrefix Rcvr node missing: $adnIdOpt! Somebody scanning our nodes? I'll try to geolocate rcvr node.")
        case ex: Throwable if !ex.isInstanceOf[NoSuchElementException] =>
          LOGGER.warn(s"$logPrefix Unknown exception while getting rcvr node $adnIdOpt", ex)
      }

      // TODO Сюда можно затолкать обработку BLE-маячков, указанных в _reqArgs.locEnv.beacons.

      // Надо отработать ситуацию, когда нет узла в данной точки: поискать покрывающий город/район.
      mnodeFut = mnodeFut.recoverWith { case _: NoSuchElementException =>
        // Если с ресивером по id не фартует, но есть данные геолокации, то заодно запускаем поиск узла-ресивера по геолокации.
        // В понятиях старой выдачи, это поиск активного узла-здания.
        // Нет смысла выносить этот асинхронный код за пределы recoverWith(), т.к. он или не нужен, или же выполнится сразу синхронно.
        reqGeoLocFut.flatMap { geoLocOpt =>
          // Пусть будет сразу NSEE, если нет данных геолокации.
          val geoLoc = geoLocOpt.get

          // Пройтись по всем геоуровням, запустить везде параллельные поиски узлов в точке, закинув в recover'ы.
          val nglsResultsFut = Future.traverse(NodeGeoLevels.valuesT: Iterable[NodeGeoLevel]) { ngl =>
            val msearch = new MNodeSearchDfltImpl {
              override def limit = 1
              // Очень маловероятно, что сортировка по близости к точке нужна, но мы её всё же оставим
              override def withGeoDistanceSort: Option[GeoPoint] = {
                Some(geoLoc.center)
              }
              // Неактивные узлы сразу вылетают из выдачи.
              override def isEnabled = Some(true)
              override def outEdges: Seq[ICriteria] = {
                val gsCr = GsCriteria(
                  levels = Seq(ngl),
                  shapes = Seq(
                    CircleGs(geoLoc.center, Distance(10, DistanceUnit.METERS))
                  )
                )
                val cr = Criteria(
                  predicates  = Seq(MPredicates.NodeLocation),
                  gsIntersect = Some(gsCr)
                )
                Seq(cr)
              }
            }

            for (mnodeOpt <- mNodes.dynSearchOne(msearch)) yield {
              for (mnode <- mnodeOpt) yield {
                MIndexNodeInfo(
                  mnodeOpt.get,
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

      // Если всё ещё нет узла. Это нормально, надобно найти в пуле или придумать какой-то рандомный узел, желательно без id даже.
      mnodeFut = mnodeFut.recoverWith { case _: NoSuchElementException =>
        // Нужно выбирать эфемерный узел с учётом языка реквеста. Используем i18n как конфиг.
        val ephNodeId = Option ( ctx.messages("conf.sc.node.ephemeral.id") )
          .filter(_.nonEmpty)
          .get
        for (mnodeOpt <- mNodeCache.getById( ephNodeId )) yield {
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

      // Should never happen. Придумывание узла "на лету".
      mnodeFut = mnodeFut.recover { case ex: NoSuchElementException =>
        LOGGER.warn(s"$logPrefix Unable to find index node, making new ephemeral node", ex)

        MIndexNodeInfo(
          mnode = nodesUtil.userNodeInstance(
            nameOpt     = Some("iSuggest"),
            personIdOpt = None
          ),
          isRcvr = false
        )
      }

      // Вернуть получившийся в итоге асинхронный результат
      mnodeFut
    }


    /** Получение графического логотипа узла, если возможно. */
    def logoImgOptFut: Future[Option[MImgT]] = {
      for {
        currNode     <- indexNodeFut
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
          inxNodeInfo <- indexNodeFut
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
    def respHtml4JsFut = respHtmlFut.map( htmlCompressUtil.html2jsStr )

    /** Контейнер палитры выдачи. */
    lazy val colorsFut: Future[IColors] = {
      for (mnodeInfo <- indexNodeFut) yield {
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
        mnodeInfo <- indexNodeFut
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
          LOGGER.warn("topLeftBtnHtmlFut(): Workarounding unexpected expection", ex)

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
      for (inxNodeInfo <- indexNodeFut) yield {
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
        }
      }
    }

    def currInxNodeIdOptFut: Future[Option[String]] = {
      for (inxNodeInfo <- indexNodeFut) yield {
        inxNodeInfo.mnode.id
          .filter(_ => inxNodeInfo.isRcvr)
      }
    }

    def respArgsFut: Future[ScIndexResp] = {
      val _htmlFut  = respHtml4JsFut
      val _currAdnIdOptFut = currInxNodeIdOptFut
      val _titleFut = titleFut
      for {
        html              <- _htmlFut
        currAdnIdOpt      <- _currAdnIdOptFut
        _title            <- _titleFut
      } yield {
        ScIndexResp(
          html            = html,
          currAdnId       = currAdnIdOpt,
          titleOpt        = Some(_title)
        )
      }
    }

    /** Результат запроса. */
    protected def _resultVsn(args: ScIndexResp): Result = {
      _reqArgs.apiVsn match {
        case MScApiVsns.Sjs1 =>
          Ok(args.toJson)
        case other =>
          throw new UnsupportedOperationException("Unsupported API vsn: " + other)
      }
    }

    /** HTTP-ответ клиенту. */
    def result: Future[Result] = {
      for {
        args <- respArgsFut
      } yield {
        statUtil.resultWithStatCookie {
          _resultVsn(args)
        }(ctx.request)
      }
    }

  }

  /** Дефолтовая реализация логики ScIndexUniLogicImpl для снижения объемов кодогенерации байткода
    * в конкретных реализациях. */
  abstract class ScIndexUniLogicImpl extends ScIndexUniLogic

}
