package controllers.sc

import java.util.NoSuchElementException

import play.twirl.api.HtmlFormat
import util.showcase._
import util.acl._
import ShowcaseUtil._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.mvc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 18:47
 * Description: Поддержка гео-выдачи в showcase-контроллере.
 */

/** Аддон для контроллера, добавляющий экшены от гео-indexTpl, которые представляют выдачу вне явно-заданного узла. */
trait ScIndexGeo extends ScIndexCommon with ScIndexConstants with ScIndexNodeCommon { that =>

  /**
   * indexTpl для выдачи, отвязанной от конкретного узла.
   * Этот экшен на основе параметров думает на тему того, что нужно отрендерить. Может отрендерится showcase узла,
   * либо geoShowcase на дефолтовых параметрах.
   * @param args Аргументы.
   */
  def geoShowcase(args: ScReqArgs) = MaybeAuth.async { implicit request =>
    // Собираем хелпер, который займётся выстраиванием результата работы.
    case class LogicResult(result: Result, nodeOpt: Option[MAdnNode], helper: ScIndexHelperBase)
    val logic = new GeoIndexLogic {
      type T = LogicResult
      override def _reqArgs = args
      override implicit def _request = request
      // gsiOptFut в любом случае понадобится, поэтому делаем его val'ом.
      override val gsiOptFut = super.gsiOptFut

      /** Нет ноды. */
      override def nodeNotDetected(): Future[T] = {
        nodeNotDetectedHelperFut().flatMap { _helper =>
          _helper.result
            .map { result => LogicResult(result, None, _helper) }
        }
      }

      /** Нода найдена с помощью геолокации. */
      override def nodeFound(gdr: GeoDetectResult): Future[T] = {
        nodeFoundHelperFut(gdr).flatMap { _helper =>
          _helper.result
            .map { result => LogicResult(result, Some(gdr.node), _helper) }
        }
      }
    }
    // Запускаем хелпер на генерацию асинхронного результата:
    val resultFut = logic()
    // Собираем статистику асинхронно
    resultFut onSuccess { case logRes =>
      ScIndexStatUtil(Some(AdnSinks.SINK_GEO), logic.gsiOptFut, logRes.helper.ctx.deviceScreenOpt, logRes.nodeOpt)
        .saveStats
        .onFailure { case ex =>
          LOGGER.warn("geoShowcase(): Failed to save statistics: args = " + args, ex)
        }
    }
    // Готовим настройки кеширования. Если геолокация по ip, то значит возможно только private-кеширование на клиенте.
    val (cacheControlMode, hdrs0) = if (!args.geo.isExact)
      "private" -> List(VARY -> X_FORWARDED_FOR)
    else
      "public" -> Nil
    val hdrs1 = CACHE_CONTROL -> s"$cacheControlMode, max-age=$SC_INDEX_CACHE_SECONDS"  ::  hdrs0
    // Возвращаем асинхронный результат, добавив в него клиентский кеш.
    resultFut map { logRes =>
      logRes
        .result
        .withHeaders(hdrs1 : _*)
    }
  }


  /** Только голый рендер содержимого indexTpl, подходящего под запрос. */
  protected def _geoShowCaseHtml(args: ScReqArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[HtmlFormat.Appendable] = {
    val logic = new GeoIndexLogic {
      override type T = HtmlFormat.Appendable

      override def _reqArgs: ScReqArgs = args
      override implicit def _request = request

      implicit private def helper2respHtml(h: Future[ScIndexHelperBase]): Future[T] = {
        h.flatMap(_.respHtmlFut)
      }

      /** Нет ноды. */
      override def nodeNotDetected(): Future[T] = {
        nodeNotDetectedHelperFut()
      }

      /** Нода найдена с помощью геолокации. */
      override def nodeFound(gdr: GeoDetectResult): Future[T] = {
        nodeFoundHelperFut(gdr)
      }
    }
    logic.apply()
  }


  /** Хелпер для рендера голой выдачи (вне ноды). Вероятно, этот код никогда не вызывается. */
  trait ScIndexGeoHelper extends ScIndexHelperBase {
    override def isGeo = true

    override def currAdnIdFut = Future successful None

    override def renderArgsFut: Future[ScRenderArgs] = {
      val (catsStatsFut, mmcatsFut) = getCats(None)
      for {
        mmcats    <- mmcatsFut
        catsStats <- catsStatsFut
      } yield {
        ScRenderArgs(
          bgColor   = SITE_BGCOLOR_GEO,
          fgColor   = SITE_FGCOLOR_GEO,
          name      = SITE_NAME_GEO,
          mmcats    = mmcats,
          catsStats = catsStats,
          spsr = AdSearch(
            levels = List(AdShowLevels.LVL_START_PAGE),
            geo = GeoIp
          ),
          oncloseHref = ONCLOSE_HREF_DFLT
        )
      }
    }
  }


  /** Хелпер для рендера гео-выдачи в рамках узла. */
  trait ScIndexNodeGeoHelper extends ScIndexNodeSimpleHelper {
    def gdrFut: Future[GeoDetectResult]

    override def adnNodeFut = gdrFut.map(_.node)

    override def geoListGoBackFut: Future[Option[Boolean]] = {
      gdrFut.map { gdr => Some(gdr.ngl.isLowest) }
    }

    override def isGeo: Boolean = true
  }


  /**
   * Совсем абстрактная логика работы из экшена geoShowCase() тут.
   * Абстрагирован от типа результата. Используется для сборки helper'ов, генерирующих конкретные результаты.
   * Запуск логики поиска подходящего узла и выбора выдачи осуществляется через apply().
   */
  trait GeoIndexLogicBase { that2 =>
    /** Тип возвращаемого значения в методах этого хелпера. */
    type T

    // Типы возвращаемых хелперов в соотв.методах.
    type NndHelper_t <: ScIndexHelperBase
    type NfHelper_t <: ScIndexNodeHelper

    def _reqArgs: ScReqArgs
    implicit def _request: AbstractRequestWithPwOpt[_]

    def gsiOptFut = _reqArgs.geo.geoSearchInfoOpt

    /** Запуск логики, которая раньше лежала в geoShowcase()-экшене. */
    def apply(): Future[T] = {
      lazy val logPrefix = s"GeoIndexLogic(${System.currentTimeMillis}): "
      LOGGER.trace(logPrefix + "Starting, args = " + _reqArgs)
      if (_reqArgs.geo.isWithGeo) {
        val gdrFut = ShowcaseNodeListUtil.detectCurrentNode(_reqArgs.geo, gsiOptFut)
        gdrFut flatMap { gdr =>
          LOGGER.trace(logPrefix + "Choosen adn node according to geo is " + gdr.node.id.get)
          nodeFound(gdr)
        } recoverWith {
          case ex: NoSuchElementException =>
            // Нету узлов, подходящих под запрос.
            LOGGER.debug(logPrefix + "No nodes found nearby " + _reqArgs.geo)
            nodeNotDetected()
        }
      } else {
        nodeNotDetected()
      }
    }

    def nodeNotDetectedHelperFut(): Future[NndHelper_t]

    /** Нет ноды. */
    def nodeNotDetected(): Future[T]

    def nodeFoundHelperFut(gdr: GeoDetectResult): Future[NfHelper_t]

    /** Нода найдена с помощью геолокации. */
    def nodeFound(gdr: GeoDetectResult): Future[T]

    trait ScIndexHelperAddon extends ScIndexHelperBase {
      override implicit def _request = that2._request
      override def _reqArgs = that2._reqArgs
    }
  }


  /** Гибкий абстрактный хелпер для сборки методов, занимающихся раздачей indexTpl с учётом геолокации. */
  trait GeoIndexLogic extends GeoIndexLogicBase {

    override type NndHelper_t = ScIndexGeoHelper
    override type NfHelper_t = ScIndexNodeGeoHelper

    def nodeNotDetectedHelperFut(): Future[NndHelper_t] = {
      val res = new ScIndexGeoHelper with ScIndexHelperAddon
      Future successful res
    }

    def nodeFoundHelperFut(gdr: GeoDetectResult): Future[NfHelper_t] = {
      val helper = new ScIndexNodeGeoHelper with ScIndexHelperAddon {
        override val gdrFut = Future successful gdr
      }
      Future successful helper
    }

  }

}
