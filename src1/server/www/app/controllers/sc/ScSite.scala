package controllers.sc

import controllers.routes
import io.suggest.adn.MAdnRights
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.i18n.{I18nConst, MsgCodes}
import io.suggest.maps.MMapProps
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.extra.domain.{DomainCriteria, MDomainModes}
import io.suggest.model.n2.node.{IMNodes, MNode}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sc.MScApiVsns
import io.suggest.sc.sc3.{MSc3Conf, MSc3Init}
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.text.util.UrlUtil
import io.suggest.util.logs.IMacroLogs
import models.mctx.IContextUtilDi
import models.msc._
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.adv.geo.{IAdvGeoLocUtilDi, IAdvGeoRcvrsUtilDi}
import util.ext.IExtServicesUtilDi
import util.i18n.IJsMessagesUtilDi
import util.sec.ICspUtilDi
import util.showcase.IScUtil
import util.stat.IStatUtil
import OptionUtil.BoolOptOps
import io.suggest.es.model.EsModelDi
import views.html.sc._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:42
 * Description: Трейты с экшенами для рендера "сайтов" выдачи, т.е. html-страниц, возвращаемых при непоср.реквестах.
 * Бывает рендер через geo, который ищет подходящий узел, и рендер напрямую.
 */

/** Базовый трейт с утилью для сборки конкретных реализация экшенов раздачи "сайтов" выдачи. */
trait ScSite
  extends ScController
  with IMacroLogs
  with IStatUtil
  with IScUtil
  with IExtServicesUtilDi
  with IMNodes
  with IContextUtilDi
  with ICspUtilDi
  with IMaybeAuth
  with IAdvGeoLocUtilDi
  with IJsMessagesUtilDi
  with IAdvGeoRcvrsUtilDi
  with EsModelDi
{

  import sioControllerApi._
  import mCommonDi._
  import esModel.api._
  import cspUtil.Implicits._

  /** Изначальное значение флага отладки js-выдачи управляется флагом в конфиге. */
  private lazy val SC_JS_DEBUG = configuration.getOptional[Boolean]("sc.js.debug").getOrElseFalse


  /** Настраиваемая логика сборки результата запроса сайта выдачи. */
  protected abstract class SiteLogic extends LogicCommonT {

    /** Сюда передаются исходные параметры запроса сайта (qs). */
    def _siteQsArgs: SiteQsArgs

    /** Исходный http-реквест. */
    implicit def _request: IReq[_]

    // 2016.sep.9: Геолокация выходит за пределы geo. Тут добавляется поддержка доменов в качестве подсказки для поиска узла:
    lazy val domainNodeOptFut: Future[Option[MNode]] = {
      val myHost = ctx.request.host
        .replaceFirst(":.+$", "")

      OptionUtil.maybeFut( !ctxUtil.isMyHostSio(myHost) ) {
        // Логгируем этот этап работы.
        lazy val logPrefix = s"${classOf[SiteLogic].getSimpleName}.nodeOptFut(myHost=$myHost):"

        val dkey = try {
          UrlUtil.host2dkey(myHost)
        } catch {
          case ex: Throwable =>
            LOGGER.warn(s"$logPrefix Failed to normalize host '$myHost' into dkey", ex)
            myHost
        }

        val msearch = new MNodeSearchDfltImpl {
          override def domains: Seq[DomainCriteria] = {
            val cr = DomainCriteria(
              dkeys = dkey :: Nil,
              modes = MDomainModes.ScServeIncomingRequests :: Nil
            )
            cr :: Nil
          }
          override def limit          = 1
          override def isEnabled      = Some(true)
          override def withAdnRights  = MAdnRights.RECEIVER :: Nil
        }
        val fut = mNodes.dynSearchOne(msearch)

        fut.onComplete {
          case Success(None)    => LOGGER.debug(s"$logPrefix No linked nodes not found. Request from ${_request.remoteClientAddress}")
          case Success(Some(r)) => LOGGER.trace(s"$logPrefix Found node[${r.idOrNull}] ${r.guessDisplayNameOrIdOrEmpty}")
          case Failure(ex)      => LOGGER.warn(s"$logPrefix Unable to make nodes search request:\n $msearch", ex)
        }

        // Вернуть основной фьючерс поиска подходящего под домен узла.
        fut
      }
    }

    /** Опциональный экземпляр текущего узла. */
    def nodeOptFut: Future[Option[MNode]] = {
      val _domainNodeOptFut = domainNodeOptFut

      // Поиска id узла среди параметров URL QS.
      val qsNodeOptFut = mNodes.maybeGetByIdCached( _siteQsArgs.adnId )

      // Опционально объеденить оба фьючерса.
      OptionUtil.orElseFut( _domainNodeOptFut )( qsNodeOptFut )
    }
    lazy val nodeOptFutVal = nodeOptFut


    /** Добавки к тегу head в siteTpl. */
    def headAfterFut: Future[List[Html]] = {
      val fut = for {
        madOpt <- mNodes.maybeGetByIdCached( _siteQsArgs.povAdId )
        mad = madOpt.get
        if mad.edges
          .withPredicateIter( MPredicates.Receiver )
          .nonEmpty  //.exists(_.info.sls.nonEmpty)
        renders <- {
          val futs = for ((_, svcHelper) <- extServicesUtil.HELPERS) yield {
            for (renderables <- svcHelper.adMetaTagsRender(mad)) yield {
              for (renderable <- renderables) yield {
                renderable.render()(ctx)
              }
            }
          }
          Future.sequence( futs.toList )
        }
      } yield {
        renders.flatten
      }
      // Отработать случи отсутствия карточки или другие нежелательные варианты.
      fut.recover { case ex: Throwable =>
        if (!ex.isInstanceOf[NoSuchElementException])
          LOGGER.warn("Failed to collect meta-tags for ad " + _siteQsArgs.povAdId, ex)
        List.empty[Html]
      }
    }

    /** Какой скрипт рендерить? */
    def scriptHtmlFut: Future[Html]

    /** Кастомное опциональное состояние выдачи, которое должно быть отрендерено прямо в шаблоне и
      * прочитано оттуда выдачей. Изначальное появилось для передачи adnId (id текущего узла-ресивера),
      * но сразу было переимплеменчено в более универсальный инструмент. */
    def customScStateOptFut: Future[Option[ScJsState]] = {
      for {
        nodeOpt <- nodeOptFutVal
      } yield {
        for (mnode <- nodeOpt) yield {
          ScJsState(adnId = mnode.id)
        }
      }
    }


    /** Значение флага sysRender, пробрасывается напрямую в ScSiteArgs. */
    def _syncRender: Boolean

    /** Здесь описывается методика сборки аргументов для рендера шаблонов. */
    def renderArgsFut: Future[ScSiteArgs] = {
      val _nodeOptFut             = nodeOptFutVal
      val _headAfterFut           = headAfterFut
      val _scriptHtmlFut          = scriptHtmlFut
      val _customScStateOptFut    = customScStateOptFut
      for {
        _nodeOpt                  <- _nodeOptFut
        _headAfter                <- _headAfterFut
        _scriptHtml               <- _scriptHtmlFut
        _customScStateOpt         <- _customScStateOptFut
      } yield {
        new ScSiteArgs {
          override def nodeOpt    = _nodeOpt
          override def headAfter: Iterable[Html] = {
            super.headAfter ++ _headAfter
          }
          override def scriptHtml = _scriptHtml
          override def apiVsn     = _siteQsArgs.apiVsn
          override def jsStateOpt = _customScStateOpt
          override def syncRender = _syncRender
        }
      }
    }

    /** Кастомные настройки CSP, или None, если не требуются. */
    def customCspPolicyOpt: Option[(String, String)]

    /** Собрать ответ на HTTP-запрос сайта. */
    def resultFut: Future[Result] = {
      for (rargs <- renderArgsFut) yield {
        val render = siteTpl(rargs)(ctx)
        Ok(render)
          .cacheControl( 600 )
          .withCspHeader( customCspPolicyOpt )
      }
    }

    override def scStat: Future[Stat2] = {
      val _userSaOptFut     = statUtil.userSaOptFutFromRequest()
      val _nodeOptFut       = nodeOptFutVal
      val _domainNodeOptFut = domainNodeOptFut
      for {
        _userSaOpt        <- _userSaOptFut
        _nodeOpt          <- _nodeOptFut
        _domainNodeOpt    <- _domainNodeOptFut
      } yield {
        new Stat2 {
          override def components = MComponents.Site :: super.components
          override def userSaOpt = _userSaOpt
          override def statActions: List[MAction] = {
            // Возможный stat-экшен POV-просмотра сайта с т.з. карточки.
            val mPovActions = _siteQsArgs.povAdId.fold [List[MAction]] (Nil) { povAdId =>
              val mPovAct = MAction(
                actions   = MActionTypes.PovNode :: Nil,
                nodeId    = povAdId :: Nil,
                nodeName  = Nil
              )
              mPovAct :: Nil
            }
            // Экшен посещения sc site.
            val mSiteAction = MAction(
              actions   = MActionTypes.ScSite :: Nil,
              nodeId    = _nodeOpt.flatMap(_.id).toSeq,
              nodeName  = _nodeOpt.flatMap(_.guessDisplayName).toSeq
            )
            // Объединяем все найденные stat-экшены.
            mSiteAction :: mPovActions
          }

          override def domain3p = _Domain.maybeCurrent( _domainNodeOpt.nonEmpty )
        }
      }
    }

  }


  private val cspV3 = cspUtil.mkCustomPolicyHdr { pol0 =>
    pol0
      .allowOsmLeaflet
      .jsUnsafeInline
      .styleUnsafeInline
  }


  /** Реализация SiteLogic для v3-выдачи на базе react с client-side рендером. */
  protected abstract class SiteScriptLogicV3 extends SiteLogic {

    import views.html.sc.site.v3._

    override def customCspPolicyOpt: Option[(String, String)] = {
      cspV3
    }

    /** Добавки к тегу head в siteTpl. */
    override def headAfterFut: Future[List[Html]] = {
      val fut0 = super.headAfterFut
      val headAfterHtml = _headAfterV3Tpl()(ctx)
      for (htmls0 <- fut0) yield {
        headAfterHtml :: htmls0
      }
    }

    private def geoPoint0Fut: Future[MGeoPoint] = {
      import advGeoLocUtil.Detectors._

      FromRemoteAddr( _request.remoteClientAddress )
        .orElse( FromDefaultGeoPoint )
        .get
    }

    // Кэшируем часто-используемый инстанс JSON formatter'а для MSc3Init.
    private val _msc3InitFormat = MSc3Init.MSC3_INIT_FORMAT

    /** Рассчёт и кэширование хэша для сборки URL для JSON-карты ресиверов. */
    private def rcvrNodesUrlArgsFut = advGeoRcvrsUtil.rcvrsMapUrlArgs()(ctx)


    /** Какой узел должен быть за about? */
    private def aboutSioNodeIdFut: Future[String] = {
      // TODO Надо, в зависимости от языка юзера, выдавать разные узлы.
      // TODO Желательно кэшировать результат, если он вдруг асинхронный.
      val aboutSioNodeId = ctx.messages( MsgCodes.`About.sio.node.id` )
      Future.successful( aboutSioNodeId )
    }

    def scriptCacheHashCodeFut: Future[Int] = {
      cacheApiUtil.getOrElseFut(_request.host + ":scv3ScriptCacheHashCode", 10.seconds) {
        Future {
          ScJsRouter.jsRouterCacheHash( ctx )
        }
      }
    }

    override def scriptHtmlFut: Future[Html] = {
      // Поиска начальную точку для гео.карты.
      val _geoPoint0Fut = geoPoint0Fut
      val _rcvrsMapUrlArgsFut = rcvrNodesUrlArgsFut

      // Синхронно скомпилить js-messages для рендера прямо в html-шаблоне.
      val jsMessagesJs = jsMessagesUtil.scJsMsgsFactory( Some(I18nConst.WINDOW_JSMESSAGES_NAME) )(ctx.messages)
      val _aboutSioNodeIdFut = aboutSioNodeIdFut

      val _scriptCacheHashCodeFut = scriptCacheHashCodeFut

      // Рендерить скрипт ServiceWorker'а только для домена suggest.io
      val _withServiceWorkerFut = domainNodeOptFut.map(_.isEmpty)

      // Надо ссылку на список ресиверов отправить. Раньше через роутер приходила, но это без CDN как-то не очень.
      // TODO В будущем, можно будет кэширование организовать: хэш в ссылке + длительный кэш.
      // Собрать все результаты в итоговый скрипт.
      for {
        geoPoint0             <- _geoPoint0Fut
        aboutSioNodeId        <- _aboutSioNodeIdFut
        scriptCacheHashCode   <- _scriptCacheHashCodeFut
        rcvrsMapUrlArgs       <- _rcvrsMapUrlArgsFut
        withServiceWorker     <- _withServiceWorkerFut
      } yield {
        // Сборка модели данных инициализации выдачи:
        val state0 = MSc3Init(
          mapProps = MMapProps(
            center = geoPoint0,
            zoom   = MMapProps.ZOOM_DEFAULT
          ),
          conf = MSc3Conf(
            isLoggedIn        = _request.user.isAuth,
            aboutSioNodeId    = aboutSioNodeId,
            apiVsn            = _siteQsArgs.apiVsn,
            debug             = SC_JS_DEBUG,
            // Хост-порт для запросов через CDN:
            rcvrsMapUrl          = rcvrsMapUrlArgs,
          )
        )

        // Данные для рендера html-страницы:
        val scriptRenderArgs = MSc3ScriptRenderArgs(
          state0 = Json
            .toJson(state0)(_msc3InitFormat)
            .toString(),
          jsMessagesJs        = jsMessagesJs,
          cacheHashCode       = scriptCacheHashCode,
          withServiceWorker   = withServiceWorker
        )
        _scriptV3Tpl(scriptRenderArgs)(ctx)
      }
    }

    override def _syncRender = false

  }


  // Экшены реализации поддержки sc-сайта.

  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов. */
  // U.PersonNode запрашивается в фоне для сбора статистики внутри экшена.
  def geoSite(maybeJsState: ScJsState, siteArgs: SiteQsArgs) = maybeAuth(U.PersonNode).async { implicit request =>
    if (maybeJsState.nonEmpty) {
      // Было раньше MovedPermanently, но почему-то оно может сбойнуть и закешироваться на CDN.
      // 2016.02.04 Логгирование тут усилено для отлова memleak'а с зацикливанием здесь.
      LOGGER.trace(s"geoSite($siteArgs): Qs js state is nonEmpty, redirecting from ${request.path} [${request.remoteClientAddress}]")
      val call = routes.Sc.geoSite(x = siteArgs).url + "#!?" + maybeJsState.toQs()
      Redirect(call)

    } else {
      // Выбор движка выдачи:
      if (siteArgs.apiVsn.majorVsn == MScApiVsns.ReactSjs3.majorVsn) {
        // Логика sc3
        val logic = new SiteScriptLogicV3 {
          override implicit def _request = request
          override def _siteQsArgs = siteArgs
        }
        _geoSiteResult(logic)

      } else {
        errorHandler.onClientError(request, NOT_IMPLEMENTED, s"sc.api.vsn=${siteArgs.apiVsn} not implemented")
      }
    }
  }


  /**
   * Раздавалка "сайта" выдачи первой страницы. Можно переопределять, для изменения/расширения функционала.
   */
  protected def _geoSiteResult(logic: SiteLogic): Future[Result] = {
    // Сборка обычного результата.
    val resFut = logic.resultFut

    // Запуск сохранения статистики.
    logic.saveScStat()

    // Вернуть асинхронный результатец.
    resFut
  }

}
