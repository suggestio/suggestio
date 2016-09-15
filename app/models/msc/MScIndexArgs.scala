package models.msc

import io.suggest.model.play.qsb.QueryStringBindableImpl
import models._
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import io.suggest.sc.ScConstants.ReqArgs._
import views.js.sc.m.scReqArgsJsUnbindTpl

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.15 15:16
  * Description: qs-аргументы запроса к sc/index.
  *
  * До web21:97e437abd7b3 модель называлась ScReqArgs, что было немного мимо кассы.
  */

object MScIndexArgs {

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit
                   geoOptB    : QueryStringBindable[Option[GeoMode]],
                   intOptB    : QueryStringBindable[Option[Int]],
                   devScrB    : QueryStringBindable[Option[DevScreen]],
                   strOptB    : QueryStringBindable[Option[String]],
                   apiVsnB    : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[MScIndexArgs] = {
    new QueryStringBindableImpl[MScIndexArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScIndexArgs]] = {
        val f = key1F(key)
        for {
          geoOptE             <- geoOptB.bind(f(GEO_FN),             params)
          devScreenE          <- devScrB.bind(f(SCREEN_FN),          params)
          withWelcomeAdE      <- intOptB.bind(f(WITH_WELCOME_FN),    params)
          prevAdnIdOptE       <- strOptB.bind(f(PREV_ADN_ID_FN),  params)
          apiVsnE             <- apiVsnB.bind(f(VSN_FN),             params)
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
            new MScIndexArgsDfltImpl {
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

      override def unbind(key: String, value: MScIndexArgs): String = {
        _mergeUnbinded {
          val f = key1F(key)
          Iterator(
            geoOptB.unbind(f(GEO_FN),              Some(value.geo)),
            devScrB.unbind(f(SCREEN_FN),           value.screen),
            intOptB.unbind(f(WITH_WELCOME_FN),     if (value.withWelcomeAd) None else Some(0)),
            strOptB.unbind(f(PREV_ADN_ID_FN),   value.prevAdnId),
            strOptB.unbind(f(ADN_ID_FN),        value.adnIdOpt),
            apiVsnB.unbind(f(VSN_FN),              value.apiVsn)
          )
        }
      }

      /** unbind на клиенте происходит из json-объекта с именами полей, которые соответствуют указанным в модели qs-именам. */
      override def javascriptUnbind: String = {
        scReqArgsJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

  def empty: MScIndexArgs = new MScIndexArgsDfltImpl

}


/** Модель аргументов запроса к выдаче. */
trait MScIndexArgs {
  def geo                 : GeoMode
  def screen              : Option[DevScreen]
  def withWelcomeAd       : Boolean
  def prevAdnId           : Option[String]
  def apiVsn              : MScApiVsn
  /** id (текущего) узла-ресивера, для которого запрашивается sc index. */
  def adnIdOpt            : Option[String]

  override def toString: String = {
    import QueryStringBindable._
    MScIndexArgs.qsb.unbind("a", this)
  }
}


/** Дефолтовая реализация полей трейта [[MScIndexArgs]]. */
trait MScIndexArgsDflt extends MScIndexArgs {
  override def geo                  : GeoMode             = GeoNone
  override def screen               : Option[DevScreen]   = None
  override def withWelcomeAd        : Boolean             = true
  override def prevAdnId            : Option[String]      = None
  override def apiVsn               : MScApiVsn           = MScApiVsns.unknownVsn
  override def adnIdOpt             : Option[String]      = None
}
/** Реализация [[MScIndexArgsDflt]] для облегчения скомпиленного байткода. */
class MScIndexArgsDfltImpl extends MScIndexArgsDflt


/** Враппер [[MScIndexArgs]] для имитации вызова copy(). */
trait MScIndexArgsWrapper extends MScIndexArgs {
  def reqArgsUnderlying: MScIndexArgs

  override def geo                  = reqArgsUnderlying.geo
  override def screen               = reqArgsUnderlying.screen
  override def withWelcomeAd        = reqArgsUnderlying.withWelcomeAd
  override def prevAdnId            = reqArgsUnderlying.prevAdnId
  override def apiVsn               = reqArgsUnderlying.apiVsn
  override def adnIdOpt             = reqArgsUnderlying.adnIdOpt

}

