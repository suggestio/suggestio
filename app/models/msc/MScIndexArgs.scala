package models.msc

import io.suggest.model.play.qsb.QueryStringBindableImpl
import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import io.suggest.sc.ScConstants.ReqArgs._
import models.mgeo.MLocEnv
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
                   locEnvB    : QueryStringBindable[MLocEnv],
                   boolB      : QueryStringBindable[Boolean],
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
          adnIdOptE           <- strOptB.bind(f(ADN_ID_FN),           params)
        } yield {
          for {
            _apiVsn           <- apiVsnE.right
            _locEnv           <- locEnvE.right
            _prevAdnIdOpt     <- prevAdnIdOptE.right
            _adnIdOpt         <- adnIdOptE.right
            _devScreen        <- devScreenE.right
            _withWelcome      <- withWelcomeE.right
          } yield {
            new MScIndexArgsDfltImpl {
              override def locEnv         = _locEnv
              override def screen         = _devScreen
              override def withWelcome    = _withWelcome
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
            locEnvB.unbind(f(LOC_ENV_FN),           value.locEnv),
            devScrB.unbind(f(SCREEN_FN),            value.screen),
            boolB.unbind  (f(WITH_WELCOME_FN),      value.withWelcome),
            strOptB.unbind(f(PREV_ADN_ID_FN),       value.prevAdnId),
            strOptB.unbind(f(ADN_ID_FN),            value.adnIdOpt),
            apiVsnB.unbind(f(VSN_FN),               value.apiVsn)
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
  def locEnv              : MLocEnv
  def screen              : Option[DevScreen]
  def withWelcome         : Boolean
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
  override def locEnv               : MLocEnv             = MLocEnv.empty
  override def screen               : Option[DevScreen]   = None
  override def withWelcome          : Boolean             = true
  override def prevAdnId            : Option[String]      = None
  override def apiVsn               : MScApiVsn           = MScApiVsns.unknownVsn
  override def adnIdOpt             : Option[String]      = None
}
/** Реализация [[MScIndexArgsDflt]] для облегчения скомпиленного байткода. */
class MScIndexArgsDfltImpl extends MScIndexArgsDflt


/** Враппер [[MScIndexArgs]] для имитации вызова copy(). */
trait MScIndexArgsWrapper extends MScIndexArgs {
  def reqArgsUnderlying: MScIndexArgs

  override def locEnv               = reqArgsUnderlying.locEnv
  override def screen               = reqArgsUnderlying.screen
  override def withWelcome          = reqArgsUnderlying.withWelcome
  override def prevAdnId            = reqArgsUnderlying.prevAdnId
  override def apiVsn               = reqArgsUnderlying.apiVsn
  override def adnIdOpt             = reqArgsUnderlying.adnIdOpt

}

