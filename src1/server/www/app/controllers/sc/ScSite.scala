package controllers.sc

import controllers.routes
import io.suggest.adn.MAdnRights
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.MGeoPoint
import io.suggest.i18n.I18nConst
import io.suggest.maps.MMapProps
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.extra.domain.{DomainCriteria, MDomainModes}
import io.suggest.model.n2.node.{IMNodes, MNode}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.sc.init.MSc3Init
import io.suggest.stat.m.{MAction, MActionTypes, MComponents}
import io.suggest.util.logs.IMacroLogs
import models.mctx.IContextUtilDi
import models.msc._
import models.req.IReq
import play.api.libs.json.Json
import play.api.mvc._
import play.twirl.api.Html
import util.acl._
import util.adv.geo.IAdvGeoLocUtilDi
import util.di.IScUtil
import util.ext.IExtServicesUtilDi
import util.i18n.IJsMessagesUtilDi
import util.sec.ICspUtilDi
import util.stat.IStatUtil
import views.html.sc._

import scala.concurrent.Future
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
{

  import mCommonDi._


  /** Настраиваемая логика сборки результата запроса сайта выдачи. */
  protected abstract class SiteLogic extends LogicCommonT {

    /** Сюда передаются исходные параметры запроса сайта (qs). */
    def _siteQsArgs: SiteQsArgs

    /** Исходный http-реквест. */
    implicit def _request: IReq[_]

    // 2016.sep.9: Геолокация выходит за пределы geo. Тут добавляется поддержка доменов в качестве подсказки для поиска узла:
    lazy val domainNodeOptFut: Future[Option[MNode]] = {
      val myHost = ctx.request.host
      OptionUtil.maybeFut( !ctxUtil.isMyHostSio(myHost) ) {
        val msearch = new MNodeSearchDfltImpl {
          override def domains: Seq[DomainCriteria] = {
            val cr = DomainCriteria(
              dkeys = myHost :: Nil,
              modes = MDomainModes.ScServeIncomingRequests :: Nil
            )
            Seq(cr)
          }
          override def limit          = 1
          override def isEnabled      = Some(true)
          override def withAdnRights  = MAdnRights.RECEIVER :: Nil
        }
        val fut = mNodes.dynSearchOne(msearch)

        // Логгируем этот этап работы.
        lazy val logPrefix = s"${classOf[SiteLogic].getSimpleName}.nodeOptFut(myHost=$myHost):"
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
      val qsNodeOptFut = mNodesCache.maybeGetByIdCached( _siteQsArgs.adnId )

      // Опционально объеденить оба фьючерса.
      OptionUtil.orElseFut( _domainNodeOptFut )( qsNodeOptFut )
    }
    lazy val nodeOptFutVal = nodeOptFut


    /** Добавки к тегу head в siteTpl. */
    def headAfterFut: Future[List[Html]] = {
      mNodesCache.maybeGetByIdCached( _siteQsArgs.povAdId )
        .map { _.get }
        // Интересует только карточка с ресивером. TODO И отмодерированная?
        .filter { mad =>
          mad.edges
            .withPredicateIter(MPredicates.Receiver)
            .nonEmpty  //.exists(_.info.sls.nonEmpty)
        }
        // Зарендерить всё параллельно.
        .flatMap { mad =>
          val futs = for (svcHelper <- extServicesUtil.HELPERS) yield {
            for (renderables <- svcHelper.adMetaTagsRender(mad)) yield {
              for (renderable <- renderables) yield {
                renderable.render()(ctx)
              }
            }
          }
          for (renders <- Future.sequence(futs)) yield {
            renders.iterator
              .flatten
              .toList
          }
        }
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
    def customScStateOptFut: Future[Option[ScJsState]]

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
          override val scColors   = scUtil.siteScColors(nodeOpt)
          override def headAfter: Traversable[Html] = {
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
        cspUtil.applyCspHdrOpt( customCspPolicyOpt ) {
          cacheControlShort {
            Ok(render)
          }
        }
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


  /** Когда нужно рендерить site script, подмешиваем это. */
  protected abstract class SiteScriptLogicV2 extends SiteLogic {

    import views.html.sc.site.v2._

    override def customCspPolicyOpt: Option[(String, String)] = {
      cspUtil.CustomPolicies.PageWithMapboxGl
    }

    /** Добавки к тегу head в siteTpl. */
    override def headAfterFut: Future[List[Html]] = {
      val fut0 = super.headAfterFut
      val headAfterHtml = _headAfterV2Tpl()(ctx)
      for (htmls0 <- fut0) yield {
        headAfterHtml :: htmls0
      }
    }

    override def scriptHtmlFut: Future[Html] = {
      val scriptRenderArgs = MSc2ScriptRenderArgs(
        apiVsn = _siteQsArgs.apiVsn
      )
      val html = _scriptV2Tpl(scriptRenderArgs)(ctx)
      Future.successful(html)
    }

    /** Сформулировать данные для начального состояния выдачи. */
    override def customScStateOptFut: Future[Option[ScJsState]] = {
      for {
        nodeOpt <- nodeOptFutVal
      } yield {
        for (mnode <- nodeOpt) yield {
          ScJsState(adnId = mnode.id)
        }
      }
    }

    override def _syncRender = false

  }



  // TODO Сделать val, когда произойдёт переключение на v3-выдачу по дефолту.
  private def cspV3 = cspUtil.mkCustomPolicyHdr { pol0 =>
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

    override def scriptHtmlFut: Future[Html] = {
      // Поиска начальную точку для гео.карты.
      val _geoPoint0Fut = geoPoint0Fut

      // Синхронно скомпилить js-messages для рендера прямо в html-шаблоне.
      val jsMessagesJs = jsMessagesUtil.scJsMsgsFactory( Some(I18nConst.WINDOW_JSMESSAGES_NAME) )(ctx.messages)

      // Собрать все результаты в итоговый скрипт.
      for {
        geoPoint0 <- _geoPoint0Fut
      } yield {
        val state0 = MSc3Init(
          mapProps = MMapProps(
            center = geoPoint0,
            zoom   = 11     // TODO Цифра с потолка.
          )
        )
        val scriptRenderArgs = MSc3ScriptRenderArgs(
          state0        = Json.toJson(state0).toString(),
          jsMessagesJs  = jsMessagesJs
        )
        _scriptV3Tpl(scriptRenderArgs)(ctx)
      }
    }

    /** Сформулировать данные для начального состояния выдачи. */
    override def customScStateOptFut: Future[Option[ScJsState]] = {
      for {
        nodeOpt <- nodeOptFutVal
      } yield {
        for (mnode <- nodeOpt) yield {
          ScJsState(adnId = mnode.id)
        }
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

      if (siteArgs.apiVsn.majorVsn == MScApiVsns.Sjs1.majorVsn) {
        // sc v2 -- первая выдача на scala.js.
        val logic = new SiteScriptLogicV2 {
          override def _siteQsArgs = siteArgs
          override def _request = request
        }
        _geoSiteResult(logic)

      } else if (siteArgs.apiVsn.majorVsn == MScApiVsns.ReactSjs3.majorVsn) {
        // Логика sc3
        val logic = new SiteScriptLogicV3 {
          override implicit def _request = request
          override def _siteQsArgs = siteArgs
        }
        _geoSiteResult(logic)

      } else {
        NotImplemented( s"sc.api.vsn=${siteArgs.apiVsn} not implemented" )
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
