package models.msc

import models._
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html
import util.qsb.QsbKey1T
import util.qsb.QsbUtil._
import io.suggest.sc.ScConstants.ReqArgs._
import views.js.sc.m.scReqArgsJsUnbindTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:16
 * Description: qs-аргументы запроса к sc/index.
 */

object ScReqArgs {

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit
                   geoOptB    : QueryStringBindable[Option[GeoMode]],
                   intOptB    : QueryStringBindable[Option[Int]],
                   devScrB    : QueryStringBindable[Option[DevScreen]],
                   apiVsnB    : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[ScReqArgs] = {
    new QueryStringBindable[ScReqArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScReqArgs]] = {
        val f = key1F(key)
        for {
          maybeGeoOpt             <- geoOptB.bind(f(GEO),           params)
          maybeDevScreen          <- devScrB.bind(f(SCREEN),        params)
          maybeWithWelcomeAd      <- intOptB.bind(f(WITH_WELCOME),  params)
          maybeApiVsn             <- apiVsnB.bind(f(VSN),           params)
        } yield {
          for {
            _apiVsn <- maybeApiVsn.right
            _geoOpt <- maybeGeoOpt.right
          } yield {
            val _withWelcomeAd: Boolean = {
              maybeWithWelcomeAd.fold(
                {_ => true},
                {vOpt => vOpt.isEmpty || vOpt.get > 0}
              )
            }
            val _geo: GeoMode =  {
              _geoOpt
                .filter(_.isWithGeo)
                .getOrElse(GeoIp)
            }
            new ScReqArgsDflt {
              override def geo = _geo
              // Игнорим неверные размеры, ибо некритично.
              override lazy val screen: Option[DevScreen] = maybeDevScreen
              override def withWelcomeAd = _withWelcomeAd
              override def apiVsn = _apiVsn
            }
          }
        }
      }

      override def unbind(key: String, value: ScReqArgs): String = {
        val f = key1F(key)
        Iterator(
          geoOptB.unbind(f(GEO),          Some(value.geo)),
          devScrB.unbind(f(SCREEN),       value.screen),
          intOptB.unbind(f(WITH_WELCOME), if (value.withWelcomeAd) None else Some(0)),
          apiVsnB.unbind(f(VSN),          value.apiVsn)
        )
          .filter { us => !us.isEmpty }
          .mkString("&")
      }

      /** unbind на клиенте происходит из json-объекта с именами полей, которые соответствуют указанным в модели qs-именам. */
      override def javascriptUnbind: String = {
        scReqArgsJsUnbindTpl().body
      }
    }
  }

  def empty: ScReqArgs = new ScReqArgsDflt {}

}

trait ScReqArgs extends SyncRenderInfo {
  def geo                 : GeoMode
  def screen              : Option[DevScreen]
  def withWelcomeAd       : Boolean
  def apiVsn              : MScApiVsn
  /** Заинлайненные отрендеренные элементы плитки. Передаются при внутренних рендерах, вне HTTP-запросов и прочего. */
  def inlineTiles         : Seq[RenderedAdBlock]
  def focusedContent      : Option[Html]
  def inlineNodesList     : Option[Html]
  /** Текущая нода согласно геоопределению, если есть. */
  def adnNodeCurrentGeo   : Option[MAdnNode]

  override def toString: String = {
    import QueryStringBindable._
    ScReqArgs.qsb.unbind("a", this)
  }
}
trait ScReqArgsDflt extends ScReqArgs with SyncRenderInfoDflt {
  override def geo                  : GeoMode = GeoNone
  override def screen               : Option[DevScreen] = None
  override def withWelcomeAd        : Boolean = true
  override def apiVsn               : MScApiVsn = MScApiVsns.unknownVsn
  override def inlineTiles          : Seq[RenderedAdBlock] = Nil
  override def focusedContent       : Option[Html] = None
  override def inlineNodesList      : Option[Html] = None
  override def adnNodeCurrentGeo    : Option[MAdnNode] = None
}
/** Враппер [[ScReqArgs]] для имитации вызова copy(). */
trait ScReqArgsWrapper extends ScReqArgs {
  def reqArgsUnderlying: ScReqArgs

  override def geo                  = reqArgsUnderlying.geo
  override def screen               = reqArgsUnderlying.screen
  override def inlineTiles          = reqArgsUnderlying.inlineTiles
  override def focusedContent       = reqArgsUnderlying.focusedContent
  override def inlineNodesList      = reqArgsUnderlying.inlineNodesList
  override def adnNodeCurrentGeo    = reqArgsUnderlying.adnNodeCurrentGeo
  override def withWelcomeAd        = reqArgsUnderlying.withWelcomeAd
  override def apiVsn               = reqArgsUnderlying.apiVsn

  override def jsStateOpt           = reqArgsUnderlying.jsStateOpt
}

