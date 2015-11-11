package controllers.sc

import java.util.NoSuchElementException

import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import models.msc._
import play.twirl.api.Html
import util.di.{IScUtil, IScNlUtil, IScStatUtil}
import util.acl._
import models._
import scala.concurrent.Future
import play.api.mvc._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.11.14 18:47
 * Description: Поддержка гео-выдачи в showcase-контроллере.
 *
 * TODO Из-за "гибкости" тут код стал слишком переусложнённый, нужно упросить.
 * Причина этого отчасти в том, что index-логика тут является подчиненной. В аддоне присутствует
 * ещё одна логика, которая возвращает index-логику в качестве результата своей работы и использует её части.
 */

/** Аддон для контроллера, добавляющий экшены от гео-indexTpl, которые представляют выдачу вне явно-заданного узла. */
trait ScIndexGeo
  extends ScIndexConstants
  with ScIndexNodeCommon
  with IScStatUtil
  with IScNlUtil
  with IScUtil
  with MaybeAuth
{ that =>

  /**
   * indexTpl для выдачи, отвязанной от конкретного узла.
   * Этот экшен на основе параметров думает на тему того, что нужно отрендерить. Может отрендерится showcase узла,
   * либо geoShowcase на дефолтовых параметрах.
   * @param args Аргументы.
   */
  def geoShowcase(args: ScReqArgs) = MaybeAuth.async { implicit request =>
    // Собираем хелпер, который займётся выстраиванием результата работы.
    val logic = new GeoScIndexLogic {
      override def _reqArgs = args
      override implicit def _request = request
    }
    // Запускаем хелпер на генерацию асинхронного результата:
    val resultFut = logic()
    // Собираем статистику асинхронно
    resultFut onSuccess { case logRes =>
      scStatUtil.IndexStat(
        Some(AdnSinks.SINK_GEO),
        logic.gsiOptFut,
        logRes.helper.ctx.deviceScreenOpt,
        logRes.nodeOpt
      )
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

  // TODO Можно объеденить GeoIndexLogicBase и GeoIndexLogic.

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

    def _gdrFut = scNlUtil.detectCurrentNode(_reqArgs.geo, gsiOptFut)
    lazy val gdrFut = _gdrFut

    /** Запуск логики на исполнение. */
    def apply(): Future[T] = {
      lazy val logPrefix = s"GeoIndexLogic(${System.currentTimeMillis}): "
      LOGGER.trace(logPrefix + "Starting, args = " + _reqArgs)
      if (_reqArgs.geo.isWithGeo) {
        gdrFut flatMap { gdr =>
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


    /** Реализация основной index-логики. */
    trait ScIndexHelperAddon extends ScIndexHelperBase {
      override implicit def _request = that2._request
      override def _reqArgs = that2._reqArgs

      /** Предлагаемый заголовок окна выдачи, если возможно. */
      override def titleOptFut: Future[Option[String]] = {
        gdrFut
          .map { gdr =>
            _node2titleOpt( gdr.node )
          }
          .recover { case _ => None }
      }

      /** Фьючерс с определением достаточности имеющиейся геолокации для наилучшего определения узла. */
      override def geoAcurrEnoughtFut: Future[Option[Boolean]] = {
        // Считаем геолокацию достаточно
        val fut = for {
          gdr <- gdrFut
          // Не делать подсчет geo-child-узлов, если узел на уровне, где не может быть child узлов.
          if !gdr.ngl.isLowest
          geoChilderCount <- {
            val msearch = new MNodeSearchDfltImpl {
              override def outEdges: Seq[ICriteria] = {
                val cr = Criteria( gdr.node.id.toSeq, Seq(MPredicates.GeoParent.Direct) )
                Seq(cr)
              }
              override def limit = 1
              // TODO Фильтровать по наличию geoshape'ов
            }
            MNode.dynCount( msearch )
          }
        } yield {
          geoChilderCount <= 0L
        }
        fut recover {
          case ex: NoSuchElementException =>
            true
          case ex: Throwable =>
            LOGGER.error("geoAccurEnoughtFut(): for node " + gdrFut, ex)
            false
        } map {
          Some.apply
        }
      }
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


  /** Реализация GeoIndexLogic для нужд http-экшена geoShowcase(). */
  trait GeoScIndexLogic extends GeoIndexLogic {

    /** Внутренний класс для возврата результата.
      * Вынести в models нельзя, потому аргументом является внутренний хелпер контроллера. */
    case class LogicResult(result: Result, nodeOpt: Option[MNode], helper: ScIndexHelperBase)

    type T = LogicResult
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


  /** Реализация GeoIndexLogic для нужд ScSyncSite.
    * Рендер результата идёт в Html. */
  trait HtmlGeoIndexLogic extends GeoIndexLogic {
    override type T = Html

    private def helper2respHtml(h: Future[ScIndexHelperBase]): Future[T] = {
      h.flatMap(_.respHtmlFut)
    }

    /** Нет ноды. */
    override def nodeNotDetected(): Future[T] = {
      helper2respHtml(
        nodeNotDetectedHelperFut()
      )
    }

    /** Нода найдена с помощью геолокации. */
    override def nodeFound(gdr: GeoDetectResult): Future[T] = {
      helper2respHtml(
        nodeFoundHelperFut(gdr)
      )
    }
  }


  /** Хелпер для рендера голой выдачи (вне ноды). Вероятно, этот код никогда не вызывается. */
  trait ScIndexGeoHelper extends ScIndexHelperBase {
    override def isGeo = true
    override lazy val hBtnArgsFut = super.hBtnArgsFut

    /** Контейнер палитры выдачи. */
    override def colorsFut: Future[IColors] = {
      Future successful Colors(scUtil.SITE_BGCOLOR_GEO, fgColor = scUtil.SITE_FGCOLOR_GEO)
    }

    override def currAdnIdFut = Future successful None

    override def renderArgsFut: Future[ScRenderArgs] = {
      val _colorsFut = colorsFut
      val _topLeftBtnHtmlFut = topLeftBtnHtmlFut
      val _hBtnArgsFut = hBtnArgsFut
      for {
        _colors         <- _colorsFut
        _topLeftBtnHtml <- _topLeftBtnHtmlFut
        _hBtnArgs       <- _hBtnArgsFut
      } yield {
        new ScRenderArgs with ScReqArgsWrapper with IColorsWrapper {
          override def tilesBgFillAlpha   = scUtil.TILES_BG_FILL_ALPHA
          override def _underlying        = _colors
          override def reqArgsUnderlying  = _reqArgs
          override def hBtnArgs           = _hBtnArgs
          override def topLeftBtnHtml     = _topLeftBtnHtml
          override def title              = scUtil.SITE_NAME_GEO
          override def spsr = new AdSearchImpl {
            override def outEdges: Seq[ICriteria] = {
              val cr = Criteria(
                predicates = Seq( MPredicates.Receiver ),
                sls        = Seq( AdShowLevels.LVL_START_PAGE )
              )
              Seq(cr)
            }
            override def geo    = GeoIp
          }
          override def onCloseHref = ONCLOSE_HREF_DFLT
        }
      }
    }
  }


  /** Хелпер для рендера гео-выдачи в рамках узла. */
  trait ScIndexNodeGeoHelper extends ScIndexNodeSimpleHelper {
    def gdrFut: Future[GeoDetectResult]

    override lazy val adnNodeFut = gdrFut.map(_.node)

    override def geoListGoBackFut: Future[Option[Boolean]] = {
      gdrFut.map { gdr => Some(gdr.ngl.isLowest) }
    }

    override def isGeo: Boolean = true
  }

}
