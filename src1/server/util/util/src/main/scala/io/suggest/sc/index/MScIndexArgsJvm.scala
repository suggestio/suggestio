package io.suggest.sc.index

import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.dev.MScreen
import io.suggest.geo.MLocEnv
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sc.MScApiVsn
import io.suggest.sc.ScConstants.ReqArgs._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.15 15:16
  * Description: qs-аргументы запроса к sc/index.
  *
  * До web21:97e437abd7b3 модель называлась ScReqArgs, что было немного мимо кассы.
  */
object MScIndexArgsJvm {

  /** routes-Биндер для параметров showcase'а. */
  implicit def mScIndexArgsQsb(implicit
                               locEnvB    : QueryStringBindable[MLocEnv],
                               boolB      : QueryStringBindable[Boolean],
                               boolOptB   : QueryStringBindable[Option[Boolean]],
                               devScrB    : QueryStringBindable[Option[MScreen]],
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
          apiVsnE             <- apiVsnB.bind(f(VSN_FN),              params)
          adnIdOptE           <- strOptB.bind(f(NODE_ID_FN),          params)
          geoIntoRcvrE        <- boolOptB.bind(f(GEO_INTO_RCVR_FN),   params)
        } yield {
          for {
            _apiVsn           <- apiVsnE.right
            _locEnv           <- locEnvE.right
            _adnIdOpt         <- adnIdOptE.right
            _devScreen        <- devScreenE.right
            _withWelcome      <- withWelcomeE.right
            _geoIntoRcvr      <- geoIntoRcvrE.right
          } yield {
            MScIndexArgs(
              locEnv          = _locEnv,
              screen          = _devScreen,
              withWelcome     = _withWelcome,
              apiVsn          = _apiVsn,
              nodeId          = _adnIdOpt,
              geoIntoRcvr     = _geoIntoRcvr.getOrElseTrue
            )
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
          strOptB.unbind(f(NODE_ID_FN),           value.nodeId),
          apiVsnB.unbind(f(VSN_FN),               value.apiVsn)
        )
      }

    }
  }

}

