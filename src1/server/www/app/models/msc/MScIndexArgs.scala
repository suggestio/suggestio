package models.msc

import io.suggest.geo.MLocEnv
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import io.suggest.sc.ScConstants.ReqArgs._
import io.suggest.common.empty.OptionUtil.BoolOptOps

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
  implicit def mScIndexArgsQsb(implicit
                               locEnvB    : QueryStringBindable[MLocEnv],
                               boolB      : QueryStringBindable[Boolean],
                               boolOptB   : QueryStringBindable[Option[Boolean]],
                               devScrB    : QueryStringBindable[Option[DevScreen]],
                               strOptB    : QueryStringBindable[Option[String]],
                               apiVsnB    : QueryStringBindable[MScApiVsn]
                              ): QueryStringBindable[MScIndexArgs] = {
    new QueryStringBindableImpl[MScIndexArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScIndexArgs]] = {
        val f = key1F(key)
        for {
          locEnvE             <- locEnvB.bind(f(LOC_ENV_FN),          params)
          devScreenE          <- devScrB.bind(f(SCREEN_FN),           params)
          withWelcomeE        <- boolB.bind  (f(WITH_WELCOME_FN),     params)
          prevAdnIdOptE       <- strOptB.bind(f(PREV_ADN_ID_FN),      params)
          apiVsnE             <- apiVsnB.bind(f(VSN_FN),              params)
          adnIdOptE           <- strOptB.bind(f(NODE_ID_FN),          params)
          geoIntoRcvrE        <- boolOptB.bind(f(GEO_INTO_RCVR_FN),   params)
        } yield {
          for {
            _apiVsn           <- apiVsnE.right
            _locEnv           <- locEnvE.right
            _prevAdnIdOpt     <- prevAdnIdOptE.right
            _adnIdOpt         <- adnIdOptE.right
            _devScreen        <- devScreenE.right
            _withWelcome      <- withWelcomeE.right
            _geoIntoRcvr      <- geoIntoRcvrE.right
          } yield {
            new MScIndexArgsDfltImpl {
              override def locEnv         = _locEnv
              override def screen         = _devScreen
              override def withWelcome    = _withWelcome
              override def apiVsn         = _apiVsn
              override def prevAdnId      = _prevAdnIdOpt
              override def adnIdOpt       = _adnIdOpt
              override def geoIntoRcvr    = _geoIntoRcvr.getOrElseTrue
            }
          }
        }
      }

      override def unbind(key: String, value: MScIndexArgs): String = {
        val f = key1F(key)
        _mergeUnbinded1(
          locEnvB.unbind(f(LOC_ENV_FN),           value.locEnv),
          devScrB.unbind(f(SCREEN_FN),            value.screen),
          boolB.unbind  (f(WITH_WELCOME_FN),      value.withWelcome),
          boolB.unbind  (f(GEO_INTO_RCVR_FN),     value.geoIntoRcvr),
          strOptB.unbind(f(PREV_ADN_ID_FN),       value.prevAdnId),
          strOptB.unbind(f(NODE_ID_FN),           value.adnIdOpt),
          apiVsnB.unbind(f(VSN_FN),               value.apiVsn)
        )
      }

    }
  }

  def empty: MScIndexArgs = new MScIndexArgsDfltImpl

}


// TODO ЗАменить на case class, унифицировать с common MScIndexArgs, выкинуть ненужные поля.

/** Модель аргументов запроса к выдаче. */
trait MScIndexArgs {
  def locEnv              : MLocEnv
  def screen              : Option[DevScreen]
  def withWelcome         : Boolean
  def geoIntoRcvr         : Boolean
  // TODO prevAdnId - удалить следом за v2-выдачей
  def prevAdnId           : Option[String]
  def apiVsn              : MScApiVsn
  /** id (текущего) узла-ресивера, для которого запрашивается sc index. */
  def adnIdOpt            : Option[String]

  override def toString: String = {
    import QueryStringBindable._
    import io.suggest.geo.GeoPoint.Implicits._
    import io.suggest.geo.MGeoLocJvm._
    import io.suggest.geo.MLocEnvJvm._
    import io.suggest.ble.MBeaconDataJvm._
    import io.suggest.sc.MScApiVsnsJvm._
    MScIndexArgs.mScIndexArgsQsb.unbind("a", this)
  }
}


/** Дефолтовая реализация полей трейта [[MScIndexArgs]]. */
trait MScIndexArgsDflt extends MScIndexArgs {
  override def locEnv               : MLocEnv             = MLocEnv.empty
  override def screen               : Option[DevScreen]   = None
  override def withWelcome          : Boolean             = true
  override def prevAdnId            : Option[String]      = None
  override def apiVsn               : MScApiVsn           = MScApiVsns.unknownVsn
  override def adnIdOpt             : Option[String]      = None
  override def geoIntoRcvr          : Boolean             = true
}
/** Реализация [[MScIndexArgsDflt]] для облегчения скомпиленного байткода. */
class MScIndexArgsDfltImpl extends MScIndexArgsDflt


