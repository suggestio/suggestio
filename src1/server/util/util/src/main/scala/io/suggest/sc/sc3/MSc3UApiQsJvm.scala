package io.suggest.sc.sc3

import io.suggest.dev.MScreen
import io.suggest.geo.MLocEnv
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sc.MScApiVsn
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.18 14:25
  * Description: Серверная поддержка моделей [[MSc3UApiQs]], [[MSc3UApiCommonQs]].
  */
object MSc3UApiQsJvm {

  /** QSB-поддержка для [[MSc3UApiCommonQs]]. */
  implicit def mSc3UApiCommonQsQsb(implicit
                                   screenOptB   : QueryStringBindable[Option[MScreen]],
                                   apiVsnB      : QueryStringBindable[MScApiVsn],
                                   locEnvB      : QueryStringBindable[MLocEnv]
                                  ): QueryStringBindable[MSc3UApiCommonQs] = {
    import MSc3UApiCommonQs.Fields

    new QueryStringBindableImpl[MSc3UApiCommonQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MSc3UApiCommonQs]] = {
        val k = key1F(key)
        for {
          screenOptE    <- screenOptB.bind  ( k(Fields.SCREEN_FN),  params )
          apiVsnE       <- apiVsnB.bind     ( k(Fields.API_VSN_FN), params )
          locEnvE       <- locEnvB.bind     ( k(Fields.LOC_ENV_FN), params )
        } yield {
          for {
            screenOpt   <- screenOptE.right
            apiVsn      <- apiVsnE.right
            locEnv      <- locEnvE.right
          } yield {
            MSc3UApiCommonQs(
              screen = screenOpt,
              apiVsn = apiVsn,
              locEnv = locEnv
            )
          }
        }
      }

      override def unbind(key: String, value: MSc3UApiCommonQs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          screenOptB.unbind   ( k(Fields.SCREEN_FN),  value.screen ),
          apiVsnB.unbind      ( k(Fields.API_VSN_FN), value.apiVsn ),
          locEnvB.unbind      ( k(Fields.LOC_ENV_FN), value.locEnv )
        )
      }

    }
  }


  /*
  implicit def mSc3UApiQsQsb(implicit
                             uapiCommonQsB    : QueryStringBindable[MSc3UApiCommonQs],
                             findAdsReqOptB   : QueryStringBindable[]
                            )
  */

}
