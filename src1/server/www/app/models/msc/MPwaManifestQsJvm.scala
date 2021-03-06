package models.msc

import io.suggest.sc.MScApiVsn
import io.suggest.sc.pwa.MPwaManifestQs
import io.suggest.url.bind.QueryStringBindableUtil._
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.02.18 17:13
  * Description: jvm-only утиль для модели MPwaManifestQs.
  */
object MPwaManifestQsJvm {

  /** Поддержка биндинга из/в query string для MPwaManifestQs. */
  implicit def mpwaManifestQsQsb(implicit apiVsnB: QueryStringBindable[MScApiVsn]): QueryStringBindable[MPwaManifestQs] = {
    new AbstractQueryStringBindable[MPwaManifestQs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MPwaManifestQs]] = {
        val k = key1F(key)
        for {
          apiVsnE       <- apiVsnB.bind(k(MPwaManifestQs.Fields.API_VSN_FN), params)
        } yield {
          for {
            apiVsn      <- apiVsnE
          } yield {
            MPwaManifestQs(
              apiVsn = apiVsn
            )
          }
        }
      }

      override def unbind(key: String, value: MPwaManifestQs): String = {
        val k = key1F(key)
        apiVsnB.unbind(k(MPwaManifestQs.Fields.API_VSN_FN), value.apiVsn)
      }

    }
  }

}
