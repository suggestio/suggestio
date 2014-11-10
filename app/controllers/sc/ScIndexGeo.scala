package controllers.sc

import java.util.NoSuchElementException

import util.PlayMacroLogsI
import com.typesafe.scalalogging.slf4j.Logger
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

/** Хелпер для написания кода экшенов, рендерящих indexTpl в зав-ти от результатов определения местоположения. */
abstract sealed class GeoScHelper[T](args: SMShowcaseReqArgs)(implicit request: SioRequestHeader) extends PlayMacroLogsI {

  def gsiOptFut = args.geo.geoSearchInfoOpt

  def apply(): Future[T] = {
    lazy val logPrefix = s"ScIndexGeo.apply(${System.currentTimeMillis}): "
    LOGGER.trace(logPrefix + "Starting, args = " + args)
    if (args.geo.isWithGeo) {
      val gdrFut = ShowcaseNodeListUtil.detectCurrentNode(args.geo, gsiOptFut)
      gdrFut flatMap { gdr =>
        LOGGER.trace(logPrefix + "Choosen adn node according to geo is " + gdr.node.id.get)
        nodeFound(gdr)
      } recoverWith {
        case ex: NoSuchElementException =>
          // Нету узлов, подходящих под запрос.
          LOGGER.debug(logPrefix + "No nodes found nearby " + args.geo)
          nodeNotDetected()
      }
    } else {
      nodeNotDetected()
    }
  }
  /** Нет ноды. */
  def nodeNotDetected(): Future[T]

  /** Нода найдена с помощью геолокации. */
  def nodeFound(gdr: GeoDetectResult): Future[T]
}


/** Гео-indexTpl, дающий выдачу вне явно-заданного узла. */
trait ScIndexGeo extends ScIndexCommon with ScIndexConstants with ScIndexNodeCommon { that =>

  protected type _GScHT = (Result, Option[MAdnNode])
  protected def _geoShowCase(args: SMShowcaseReqArgs)(implicit request: AbstractRequestWithPwOpt[_]): Future[_GScHT] = {
    /** Собираем хелпер-класс с логикой обработки реквеста. */
    val helper = new GeoScHelper[_GScHT](args) {
      override def LOGGER: Logger = that.LOGGER

      /** gsiOptFut в любом случае понадобится, поэтому делаем его val'ом */
      override val gsiOptFut = super.gsiOptFut

      /** Нет ноды. */
      override def nodeNotDetected(): Future[_GScHT] = {
        new ScIndexGeoHelper {
          override implicit def _request = request
        }
          .result
          .map { _ -> None }
      }

      /** Нода найдена с помощью геолокации. */
      override def nodeFound(gdr: GeoDetectResult): Future[_GScHT] = {
        val _helper = new ScIndexNodeGeoHelper {
          override val gdrFut = Future successful gdr
          override implicit def _request = request
        }
        _helper.result
          .map { _ -> Some(gdr.node) }
      }
    }
    val resultFut = helper()
    // Собираем статистику асинхронно
    resultFut onSuccess { case (result, nodeOpt) =>
      ScIndexStatUtil(Some(AdnSinks.SINK_GEO), helper.gsiOptFut, args.screen, nodeOpt)
        .saveStats
        .onFailure { case ex =>
          LOGGER.warn("geoShowcase(): Failed to save statistics: args = " + args, ex)
        }
    }
    resultFut
  }


  /**
   * indexTpl для выдачи, отвязанной от конкретного узла.
   * Этот экшен на основе параметров думает на тему того, что нужно отрендерить. Может отрендерится showcase узла,
   * либо geoShowcase на дефолтовых параметрах.
   * @param args Аргументы.
   */
  def geoShowcase(args: SMShowcaseReqArgs) = MaybeAuth.async { implicit request =>
    val resultFut = _geoShowCase(args)
    // Готовим настройки кеширования. Если геолокация по ip, то значит возможно только private-кеширование на клиенте.
    val (cacheControlMode, hdrs0) = if (!args.geo.isExact)
      "private" -> List(VARY -> X_FORWARDED_FOR)
    else
      "public" -> Nil
    val hdrs1 = CACHE_CONTROL -> s"$cacheControlMode, max-age=$SC_INDEX_CACHE_SECONDS"  ::  hdrs0
    // Возвращаем асинхронный результат, добавив в него клиентский кеш.
    resultFut
      .map { case (result, _) =>
        result.withHeaders(hdrs1 : _*)
      }
  }


  /** Хелпер для рендера голой выдачи (вне ноды). Вероятно, этот код никогда не вызывается. */
  trait ScIndexGeoHelper extends ScIndexHelperBase {
    override def isGeo = true

    override def currAdnIdFut = Future successful None

    override def renderArgsFut: Future[SMShowcaseRenderArgs] = {
      val (catsStatsFut, mmcatsFut) = getCats(None)
      for {
        mmcats    <- mmcatsFut
        catsStats <- catsStatsFut
      } yield {
        SMShowcaseRenderArgs(
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

    override val adnNodeFut = gdrFut.map(_.node)

    override val geoListGoBackFut: Future[Option[Boolean]] = {
      gdrFut.map { gdr => Some(gdr.ngl.isLowest) }
    }

    override def isGeo: Boolean = true
  }

}
