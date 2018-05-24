package io.suggest.sc.index

import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.model.play.qsb.QueryStringBindableImpl
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
                               boolB      : QueryStringBindable[Boolean],
                               boolOptB   : QueryStringBindable[Option[Boolean]],
                               strOptB    : QueryStringBindable[Option[String]],
                              ): QueryStringBindable[MScIndexArgs] = {
    new QueryStringBindableImpl[MScIndexArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScIndexArgs]] = {
        val f = key1F(key)
        for {
          withWelcomeE        <- boolB.bind  (f(WITH_WELCOME_FN),     params)
          adnIdOptE           <- strOptB.bind(f(NODE_ID_FN),          params)
          geoIntoRcvrE        <- boolOptB.bind(f(GEO_INTO_RCVR_FN),   params)
        } yield {
          for {
            _adnIdOpt         <- adnIdOptE.right
            _withWelcome      <- withWelcomeE.right
            _geoIntoRcvr      <- geoIntoRcvrE.right
          } yield {
            MScIndexArgs(
              withWelcome     = _withWelcome,
              nodeId          = _adnIdOpt,
              geoIntoRcvr     = _geoIntoRcvr.getOrElseTrue
            )
          }
        }
      }

      override def unbind(key: String, value: MScIndexArgs): String = {
        val f = key1F(key)
        _mergeUnbinded1(
          boolB.unbind  (f(WITH_WELCOME_FN),      value.withWelcome),
          boolB.unbind  (f(GEO_INTO_RCVR_FN),     value.geoIntoRcvr),
          strOptB.unbind(f(NODE_ID_FN),           value.nodeId)
        )
      }

    }
  }

}

