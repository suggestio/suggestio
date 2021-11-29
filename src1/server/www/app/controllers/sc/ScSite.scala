package controllers.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.geo.{MGeoLoc, MGeoPoint, MLocEnv}
import io.suggest.i18n.{I18nConst, MLanguages}
import io.suggest.maps.MMapProps
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNode
import io.suggest.sc.MScApiVsns
import io.suggest.sc.sc3.{MSc3Conf, MSc3Init, MScCommonQs, MScQs}
import io.suggest.spa.SioPages
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.ContextUtil
import models.msc._
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import util.adv.geo.AdvGeoLocUtil
import util.ext.ExtServicesUtil
import util.i18n.JsMessagesUtil
import util.sec.CspUtil
import OptionUtil.BoolOptOps
import com.google.inject.Inject
import controllers.Assets
import controllers.Assets.Asset
import io.suggest.ad.blk.{BlockHeights, BlockPaddings, BlockWidths}
import io.suggest.ble.MUidBeacon
import io.suggest.dev.MScreen
import io.suggest.es.model.MEsUuId
import io.suggest.playx.CacheApiUtil
import io.suggest.sc.ads.{MAdsSearchReq, MScFocusArgs, MScGridArgs, MScNodesArgs}
import io.suggest.sc.index.MScIndexArgs
import io.suggest.sc.ssr.{MScSsrArgs, ScSsrProto, SsrSetState}
import io.suggest.sec.csp.{Csp, CspPolicy}
import views.html.sc.SiteTpl
import japgolly.univeq._
import net.sf.uadetector.{ReadableUserAgent, UserAgentType}
import play.api.Configuration
import play.api.http.HttpErrorHandler
import scalaz.NonEmptyList
import util.showcase.ScSsrUtil

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:42
 * Description: Экшены для рендера "сайтов" выдачи, т.е. html-страниц, возвращаемых при непоср.реквестах.
 * Бывает рендер через geo, который ищет подходящий узел, и рендер напрямую.
 */

/** Базовый трейт с утилью для сборки конкретных реализация экшенов раздачи "сайтов" выдачи. */
final class ScSite @Inject() (
                               val scCtlUtil          : ScCtlUtil,
                             )
  extends MacroLogsImpl
{

  import scCtlUtil._
  import scCtlUtil.sioControllerApi._

  protected val scCtlApi = injector.instanceOf[ScCtlUtil]
  private lazy val extServicesUtil = injector.instanceOf[ExtServicesUtil]
  private lazy val contextUtil = injector.instanceOf[ContextUtil]
  private lazy val advGeoLocUtil = injector.instanceOf[AdvGeoLocUtil]
  private lazy val jsMessagesUtil = injector.instanceOf[JsMessagesUtil]
  private lazy val cspUtil = injector.instanceOf[CspUtil]
  private lazy val scJsRouter = injector.instanceOf[ScJsRouter]
  private lazy val siteTpl = injector.instanceOf[SiteTpl]
  private lazy val cacheApiUtil = injector.instanceOf[CacheApiUtil]
  private lazy val errorHandler = injector.instanceOf[HttpErrorHandler]
  private lazy val configuration = injector.instanceOf[Configuration]
  private lazy val assets = injector.instanceOf[Assets]

  private lazy val scSsrUtil = injector.instanceOf[ScSsrUtil]
  private lazy val scUniApi = injector.instanceOf[ScUniApi]



  import esModel.api._
  import cspUtil.Implicits._


  /** Изначальное значение флага отладки js-выдачи управляется флагом в конфиге. */
  private lazy val SC_JS_DEBUG = configuration.getOptional[Boolean]("sc.js.debug").getOrElseFalse

  private lazy val SSR_ENABLED = configuration.getOptional[Boolean]("sc.ssr.enabled").getOrElseTrue

  /** Настраиваемая логика сборки результата запроса сайта выдачи. */
  protected abstract class SiteLogic extends scCtlApi.LogicCommonT { siteLogic =>

    /** Сюда передаются исходные параметры запроса сайта (qs). */
    def _siteQsArgs: SiteQsArgs

    def _mainScreen: SioPages.Sc3

    /** Исходный http-реквест. */
    implicit def _request: IReq[_]

    def _requestHost = ctx.request.domain


    /** Опциональный экземпляр текущего узла. */
    def nodeOptFut: Future[Option[MNode]] = {
      val _domainNodeOptFut = ctx.domainNode3pOptFut

      // Поиска id узла среди параметров URL QS.
      val qsNodeOptFut = mNodes.maybeGetByIdCached( _siteQsArgs.adnId )

      // Опционально объеденить оба фьючерса.
      OptionUtil.orElseFut( _domainNodeOptFut )( qsNodeOptFut )
    }
    lazy val nodeOptFutVal = nodeOptFut


    /** Добавки к тегу head в siteTpl. */
    def headAfterFut: Future[List[Html]] = {
      (for {
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
      })
        // Отработать случи отсутствия карточки или другие нежелательные варианты.
        .recover { case ex: Throwable =>
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
    def customScStateOptFut: Future[Option[SioPages.Sc3]] = {
      for {
        nodeOpt <- nodeOptFutVal
      } yield {
        for (mnode <- nodeOpt) yield {
          SioPages.Sc3(
            nodeId = mnode.id,
          )
        }
      }
    }


    /** Sync server-side showcase content rendering for web-crawlers. */
    def inlineIndexFut: Future[Option[Html]]

    /** Здесь описывается методика сборки аргументов для рендера шаблонов. */
    def renderArgsFut: Future[MScSiteArgs] = {
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
        MScSiteArgs(
          nodeOpt     = _nodeOpt,
          headAfter   = _headAfter,
          scriptHtml  = _scriptHtml,
          apiVsn      = _siteQsArgs.apiVsn,
          jsStateOpt  = _customScStateOpt,
          mainScreen  = _mainScreen,
        )
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

    // User agent data shared between SSR and for statistics.
    lazy val userAgentOpt = statUtil.userAgentHeader()(ctx)
    lazy val userAgentParsedOpt = userAgentOpt.flatMap( statUtil.parseUserAgent )

    /** scStatFut is a val, because userAgent parsing results are used for showcase-SSR. */
    override lazy val scStatFut: Future[Stat2] = {
      val _userSaOptFut     = statUtil.userSaOptFutFromRequest()
      val _nodeOptFut       = nodeOptFutVal
      val _domainNodeOptFut = ctx.domainNode3pOptFut
      for {
        _userSaOpt        <- _userSaOptFut
        _nodeOpt          <- _nodeOptFut
        _domainNodeOpt    <- _domainNodeOptFut
      } yield {
        new Stat2 {
          override lazy val userAgentOpt = siteLogic.userAgentOpt
          override lazy val userAgentParsedOpt = siteLogic.userAgentParsedOpt
          override def components = MComponents.Site :: super.components
          override def userSaOpt = _userSaOpt
          override def statActions: List[MAction] = {
            // Возможный stat-экшен POV-просмотра сайта с т.з. карточки.
            val mPovActions = _siteQsArgs
              .povAdId
              .fold [List[MAction]] (Nil) { povAdId =>
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
              nodeId    = _nodeOpt
                .flatMap(_.id)
                .toSeq,
              nodeName  = _nodeOpt
                .flatMap(_.guessDisplayName)
                .toSeq,
            )
            // Объединяем все найденные stat-экшены.
            mSiteAction :: mPovActions
          }

          override def domain3p = _Domain.maybeCurrent( _domainNodeOpt.nonEmpty )
        }
      }
    }

  }




  /** Реализация SiteLogic для v3-выдачи на базе react с client-side рендером. */
  protected abstract class SiteScriptLogicV3 extends SiteLogic {

    import views.html.sc.site.v3._

    private def cspMainV3ModF = (
      CspPolicy.allowOsmLeaflet andThen
      CspPolicy.jsUnsafeInline andThen
      CspPolicy.styleUnsafeInline andThen
      // data: для fonts. Почему-то валятся csp-отчёты о необходимости этого в выдаче.
      CspPolicy.fontSrc.modify(_ + Csp.Sources.DATA) andThen
      // Captcha требует blob'а. TODO Дедублицировать строку ниже с CspUtil.CustomPolicies.Captcha
      CspPolicy.imgSrc.modify(_ + Csp.Sources.BLOB)
    )

    override def customCspPolicyOpt: Option[(String, String)] = {
      val cspBase = if ( ctx.isSuggestioDomain ) {
        cspUtil.CSP_DFLT_OPT
      } else {
        cspUtil.mkCspPolicy(
          commonSourcesAppend = contextUtil.SC_HOST_PORT :: Nil,
        )
      }

      cspUtil.mkCustomPolicyHdr(cspBase)( cspMainV3ModF )
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

    def scriptCacheHashCodeFut: Future[Int] = {
      cacheApiUtil.getOrElseFut(_request.host + ":scv3ScriptCacheHashCode", 10.seconds) {
        Future {
          scJsRouter.jsRouterCacheHash( ctx )
        }
      }
    }

    override def scriptHtmlFut: Future[Html] = {
      // Поиска начальную точку для гео.карты.
      val _geoPoint0Fut = geoPoint0Fut
      val _rcvrsMapUrlArgsFut = rcvrNodesUrlArgsFut

      // Синхронно скомпилить js-messages для рендера прямо в html-шаблоне.
      val jsMessagesJs = jsMessagesUtil.sc( Some(I18nConst.WINDOW_JSMESSAGES_NAME) )(ctx.messages)

      val _scriptCacheHashCodeFut = scriptCacheHashCodeFut

      // Рендерить скрипт ServiceWorker'а только для домена suggest.io
      val _withServiceWorkerFut = ctx.domainNode3pOptFut.map(_.isEmpty)

      // Inline index rendering start:
      val _inlineIndexFut = inlineIndexFut

      // Надо ссылку на список ресиверов отправить. Раньше через роутер приходила, но это без CDN как-то не очень.
      // TODO В будущем, можно будет кэширование организовать: хэш в ссылке + длительный кэш.
      // Собрать все результаты в итоговый скрипт.
      for {
        geoPoint0             <- _geoPoint0Fut
        scriptCacheHashCode   <- _scriptCacheHashCodeFut
        rcvrsMapUrlArgs       <- _rcvrsMapUrlArgsFut
        withServiceWorker     <- _withServiceWorkerFut
        _inlineIndex          <- _inlineIndexFut
      } yield {
        // Сборка модели данных инициализации выдачи:
        val state0 = MSc3Init(
          mapProps = MMapProps(
            center = geoPoint0,
            zoom   = MMapProps.ZOOM_DEFAULT
          ),
          conf = MSc3Conf(
            apiVsn            = _siteQsArgs.apiVsn,
            debug             = SC_JS_DEBUG,
            // Хост-порт для запросов через CDN:
            rcvrsMapUrl       = Some( rcvrsMapUrlArgs ),
            language          = Option( MLanguages.byCode( ctx.messages.lang.language ) ),
          )
        )

        // Данные для рендера html-страницы:
        val scriptRenderArgs = MSc3ScriptRenderArgs(
          state0 = Json
            .toJson(state0)(_msc3InitFormat)
            .toString(),
          jsMessagesJs        = jsMessagesJs,
          cacheHashCode       = scriptCacheHashCode,
          withServiceWorker   = withServiceWorker,
          inlineIndex         = _inlineIndex,
        )
        _scriptV3Tpl(scriptRenderArgs)(ctx)
      }
    }

  }


  /** Пользователь заходит в sio.market напрямую через интернет, без помощи сторонних узлов.
    * mainScreen разбирается на клиенте самой выдачей. */
  def geoSite(mainScreen: SioPages.Sc3, siteArgs: SiteQsArgs) = {
    // U.PersonNode запрашивается в фоне для сбора статистики внутри экшена.
    maybeAuth(U.PersonNode).async { implicit request =>
      lazy val logPrefix = s"geoSite(${mainScreen.nodeId.orNull})#${System.currentTimeMillis()}:"
      LOGGER.trace(s"$logPrefix ")

      // Выбор движка выдачи:
      if (siteArgs.apiVsn.majorVsn ==* MScApiVsns.ReactSjs3.majorVsn) {
        // Логика sc3
        val logic = new SiteScriptLogicV3 { siteLogic =>
          override implicit def _request = request
          override def _siteQsArgs = siteArgs
          override def _mainScreen = mainScreen

          /** Sync server-side showcase content rendering for web-crawlers. */
          override def inlineIndexFut: Future[Option[Html]] = if (SSR_ENABLED && userAgentParsedOpt.exists(_.getType == UserAgentType.ROBOT)) {
            // Render static markup: for react-hydrate & web-crawlers.
            val ssrScreen = siteLogic.userAgentParsedOpt
              .map { userAgent =>
                userAgent.getType match {
                  // Mobile browser screen should be shrinked.
                  case UserAgentType.MOBILE_BROWSER =>
                    MScreen.defaulted(
                      // TODO Check UA for pads (ipads, etc), where screen is wider...
                      width = BlockWidths.max.value + 2 * BlockPaddings.max.value,
                      height = BlockHeights.max.value + 120, // should be at least 100% expected mobile screen.
                    )
                  // Web-crawlers and desktop screens are default:
                  case _ => //UserAgentType.ROBOT | UserAgentType.BROWSER =>
                    ScSsrProto.defaultScreen
                }
              }
              .getOrElse {
                // Missing or invalid User-Agent header.
                LOGGER.debug(s"$logPrefix SSR-Screen: Cannot understand user ${siteLogic.userAgentOpt} for screen-size detection.")
                ScSsrProto.defaultScreen
              }

            val scQs = MScQs(
              common = MScCommonQs(
                locEnv = MLocEnv(
                  beacons = for (b <- mainScreen.virtBeacons) yield {
                    MUidBeacon( b, None )
                  },
                  geoLoc = mainScreen.locEnv
                    .map( MGeoLoc(_, None, None) )
                    .toList,
                ),
                screen = Some( ssrScreen ),
              ),
              search = MAdsSearchReq(
                rcvrId = mainScreen.nodeId
                  .map( MEsUuId.apply ),
                genOpt = mainScreen.generation,
                tagNodeId = mainScreen.tagNodeId
                  .map( MEsUuId.apply ),
              ),
              index = Some( MScIndexArgs(
                nodeId = mainScreen.nodeId,
                geoIntoRcvr = false,
                retUserLoc = false,
                returnEphemeral = false,
                withWelcome = {
                  // withWelcome: false for web-crawlers, true - for web-browsers.
                  userAgentParsedOpt.exists { userAgent =>
                    val renderWelcomeFor = UserAgentType.MOBILE_BROWSER :: UserAgentType.BROWSER :: Nil
                    renderWelcomeFor contains[UserAgentType] userAgent.getType
                  }
                },
              )),
              foc = mainScreen.focusedAdId.map { focAdId =>
                MScFocusArgs(
                  indexAdOpen = None,
                  adIds = NonEmptyList( focAdId ),
                )
              },
              grid = Some( MScGridArgs(
                withTitle = false,
              )),
              nodes = Option.when( mainScreen.searchOpened ) {
                MScNodesArgs()
              },
            )
            val scApiLogic = new scUniApi.ScPubApiLogicHttpV3( scQs )(request) {
              override implicit lazy val ctx = siteLogic.ctx
            }
            (for {
              scResp <- scApiLogic.scRespFut
              // Lang detection separated from lang-data to implement optional language switching (do not sent lang data, if lang is NOT changed) TODO Need actor with state.
              ssrLang = scSsrUtil.ssrLang()(ctx).value
              ssrLangData = scSsrUtil.ssrLangDataFromRequest( ssrLang )( ctx )
              indexHtmlStrE <- scSsrUtil.renderShowcaseContent(MScSsrArgs(
                action = SsrSetState( scQs, scResp, ssrLangData ),
              ))
            } yield {
              indexHtmlStrE.fold[Option[Html]](
                {ex =>
                  LOGGER.warn(s"$logPrefix Failed to SSR-render", ex)
                  None
                },
                {htmlStr =>
                  LOGGER.trace(s"$logPrefix Successfully rendered showcase SSR HTML[${htmlStr.length}ch]")
                  Some( Html( htmlStr ) )
                }
              )
            })
              .recover { case ex: Throwable =>
                LOGGER.warn(s"$logPrefix Erorr to prepare or start SSR rendering", ex)
                None
              }
          } else {
            Future.successful( None )
          }
        }

        // Сборка обычного результата.
        val resFut = logic.resultFut

        // Запуск сохранения статистики.
        logic.saveScStat()

        // Вернуть асинхронный результатец.
        resFut

      } else {
        errorHandler.onClientError(request, UPGRADE_REQUIRED, s"API version '${siteArgs.apiVsn}' not implemented.")
      }
    }
  }


  /** Crunch for root-urlpath-only service-worker.js asset. */
  def serviceWorkerJs(path: String, asset: Asset) = assets.versioned(path, asset)

}
