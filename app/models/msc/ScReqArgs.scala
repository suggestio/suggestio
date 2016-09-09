package models.msc

import io.suggest.model.play.qsb.QueryStringBindableImpl
import models._
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html
import io.suggest.sc.ScConstants.ReqArgs._
import views.js.sc.m.scReqArgsJsUnbindTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:16
 * Description: qs-аргументы запроса к sc/index.
 */

// TODO Переименовать в ScIndexArgs?

object ScReqArgs {

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit
                   geoOptB    : QueryStringBindable[Option[GeoMode]],
                   intOptB    : QueryStringBindable[Option[Int]],
                   devScrB    : QueryStringBindable[Option[DevScreen]],
                   strOptB    : QueryStringBindable[Option[String]],
                   apiVsnB    : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[ScReqArgs] = {
    new QueryStringBindableImpl[ScReqArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScReqArgs]] = {
        val f = key1F(key)
        for {
          geoOptE             <- geoOptB.bind(f(GEO),             params)
          devScreenE          <- devScrB.bind(f(SCREEN),          params)
          withWelcomeAdE      <- intOptB.bind(f(WITH_WELCOME),    params)
          prevAdnIdOptE       <- strOptB.bind(f(PREV_ADN_ID_FN),  params)
          apiVsnE             <- apiVsnB.bind(f(VSN),             params)
          adnIdOptE           <- strOptB.bind(f(ADN_ID_FN),       params)
        } yield {
          for {
            _apiVsn           <- apiVsnE.right
            _geoOpt           <- geoOptE.right
            _prevAdnIdOpt     <- prevAdnIdOptE.right
            _adnIdOpt         <- adnIdOptE.right
            // До 2016.sep.9 в devScreenE подавлялся Left в None. Т.к. было некритично, хотя уже год, как это стало критично.
            _devScreen        <- devScreenE.right
          } yield {
            val _withWelcomeAd: Boolean = {
              withWelcomeAdE.fold(
                {_ => true},
                {vOpt => vOpt.isEmpty || vOpt.get > 0}
              )
            }
            val _geo: GeoMode = {
              _geoOpt
                .filter(_.isWithGeo)
                .getOrElse(GeoIp)
            }
            new ScReqArgsDfltImpl {
              override def geo            = _geo
              override def screen         = _devScreen
              override def withWelcomeAd  = _withWelcomeAd
              override def apiVsn         = _apiVsn
              override def prevAdnId      = _prevAdnIdOpt
              override def adnIdOpt       = _adnIdOpt
            }
          }
        }
      }

      override def unbind(key: String, value: ScReqArgs): String = {
        _mergeUnbinded {
          val f = key1F(key)
          Iterator(
            geoOptB.unbind(f(GEO),              Some(value.geo)),
            devScrB.unbind(f(SCREEN),           value.screen),
            intOptB.unbind(f(WITH_WELCOME),     if (value.withWelcomeAd) None else Some(0)),
            strOptB.unbind(f(PREV_ADN_ID_FN),   value.prevAdnId),
            strOptB.unbind(f(ADN_ID_FN),        value.adnIdOpt),
            apiVsnB.unbind(f(VSN),              value.apiVsn)
          )
        }
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
  /** id (текущего) узла-ресивера, для которого запрашивается sc index. */
  def adnIdOpt            : Option[String]

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
  override def geo                  : GeoMode             = GeoNone
  override def screen               : Option[DevScreen]   = None
  override def withWelcomeAd        : Boolean             = true
  override def prevAdnId            : Option[String]      = None
  override def apiVsn               : MScApiVsn           = MScApiVsns.unknownVsn
  override def adnIdOpt             : Option[String]      = None
  override def inlineTiles          : Seq[IRenderedAdBlock] = Nil
  override def focusedContent       : Option[Html]        = None
  override def inlineNodesList      : Option[Html]        = None
  override def adnNodeCurrentGeo    : Option[MNode]       = None
}
/** Реализация [[ScReqArgsDflt]] для облегчения скомпиленного байткода. */
class ScReqArgsDfltImpl extends ScReqArgsDflt


/** Враппер [[ScReqArgs]] для имитации вызова copy(). */
trait ScReqArgsWrapper extends ScReqArgs {
  def reqArgsUnderlying: ScReqArgs

  override def geo                  = reqArgsUnderlying.geo
  override def screen               = reqArgsUnderlying.screen
  override def withWelcomeAd        = reqArgsUnderlying.withWelcomeAd
  override def prevAdnId            = reqArgsUnderlying.prevAdnId
  override def apiVsn               = reqArgsUnderlying.apiVsn
  override def adnIdOpt             = reqArgsUnderlying.adnIdOpt

  override def inlineTiles          = reqArgsUnderlying.inlineTiles
  override def focusedContent       = reqArgsUnderlying.focusedContent
  override def inlineNodesList      = reqArgsUnderlying.inlineNodesList
  override def adnNodeCurrentGeo    = reqArgsUnderlying.adnNodeCurrentGeo
  override def jsStateOpt           = reqArgsUnderlying.jsStateOpt

}

