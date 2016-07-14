package models.msc

import io.suggest.model.play.qsb.QsbKey1T
import models._
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html
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
                   strOptB    : QueryStringBindable[Option[String]],
                   apiVsnB    : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[ScReqArgs] = {
    new QueryStringBindable[ScReqArgs] with QsbKey1T {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScReqArgs]] = {
        val f = key1F(key)
        for {
          maybeGeoOpt             <- geoOptB.bind(f(GEO),             params)
          maybeDevScreen          <- devScrB.bind(f(SCREEN),          params)
          maybeWithWelcomeAd      <- intOptB.bind(f(WITH_WELCOME),    params)
          maybePrevAdnId          <- strOptB.bind(f(PREV_ADN_ID_FN),  params)
          maybeApiVsn             <- apiVsnB.bind(f(VSN),             params)
        } yield {
          for {
            _apiVsn     <- maybeApiVsn.right
            _geoOpt     <- maybeGeoOpt.right
            _prevAdnId  <- maybePrevAdnId.right
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
            new ScReqArgsDfltImpl {
              override def geo = _geo
              // Игнорим неверные размеры, ибо некритично.
              override lazy val screen: Option[DevScreen] = maybeDevScreen
              override def withWelcomeAd  = _withWelcomeAd
              override def apiVsn         = _apiVsn
              override def prevAdnId      = _prevAdnId
            }
          }
        }
      }

      override def unbind(key: String, value: ScReqArgs): String = {
        val f = key1F(key)
        Iterator(
          geoOptB.unbind(f(GEO),              Some(value.geo)),
          devScrB.unbind(f(SCREEN),           value.screen),
          intOptB.unbind(f(WITH_WELCOME),     if (value.withWelcomeAd) None else Some(0)),
          strOptB.unbind(f(PREV_ADN_ID_FN),   value.prevAdnId),
          apiVsnB.unbind(f(VSN),              value.apiVsn)
        )
          .filter { us => !us.isEmpty }
          .mkString("&")
      }

      /** unbind на клиенте происходит из json-объекта с именами полей, которые соответствуют указанным в модели qs-именам. */
      override def javascriptUnbind: String = {
        scReqArgsJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

  def empty: ScReqArgs = new ScReqArgsDfltImpl

}


/** Модель аргументов запроса к выдаче. */
trait ScReqArgs extends SyncRenderInfo {
  def geo                 : GeoMode
  def screen              : Option[DevScreen]
  def withWelcomeAd       : Boolean
  def prevAdnId           : Option[String]
  def apiVsn              : MScApiVsn
  /** Заинлайненные отрендеренные элементы плитки. Передаются при внутренних рендерах, вне HTTP-запросов и прочего. */
  def inlineTiles         : Seq[IRenderedAdBlock]
  def focusedContent      : Option[Html]
  def inlineNodesList     : Option[Html]
  /** Текущая нода согласно геоопределению, если есть. */
  def adnNodeCurrentGeo   : Option[MNode]

  override def toString: String = {
    import QueryStringBindable._
    ScReqArgs.qsb.unbind("a", this)
  }
}


/** Дефолтовая реализация полей трейта [[ScReqArgs]]. */
trait ScReqArgsDflt extends ScReqArgs with SyncRenderInfoDflt {
  override def geo                  : GeoMode = GeoNone
  override def screen               : Option[DevScreen] = None
  override def withWelcomeAd        : Boolean = true
  override def prevAdnId            : Option[String] = None
  override def apiVsn               : MScApiVsn = MScApiVsns.unknownVsn
  override def inlineTiles          : Seq[IRenderedAdBlock] = Nil
  override def focusedContent       : Option[Html] = None
  override def inlineNodesList      : Option[Html] = None
  override def adnNodeCurrentGeo    : Option[MNode] = None
}
/** Реализация [[ScReqArgsDflt]] для облегчения скомпиленного байткода. */
class ScReqArgsDfltImpl extends ScReqArgsDflt


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
  override def prevAdnId            = reqArgsUnderlying.prevAdnId
  override def apiVsn               = reqArgsUnderlying.apiVsn

  override def jsStateOpt           = reqArgsUnderlying.jsStateOpt
}

