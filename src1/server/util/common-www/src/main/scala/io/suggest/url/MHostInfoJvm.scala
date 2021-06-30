package io.suggest.url

import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.mvc.QueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.10.17 15:55
  * Description: Server-only поддержка модели [[MHostInfo]].
  */
object MHostInfoJvm {

  /** Поддержка биндинга в URL qs подстроку. */
  implicit def mHostInfoQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MHostInfo] = {
    new AbstractQueryStringBindable[MHostInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MHostInfo]] = {
        val k = key1F(key)
        val F = MHostInfo.Fields
        for {
          hostIntE    <- strB.bind( k(F.NAME_INTERNAL_FN), params )
          hostPubE    <- strB.bind( k(F.NAME_PUBLIC_FN),   params )
        } yield {
          for {
            hostInt   <- hostIntE
            hostPub   <- hostPubE
          } yield {
            MHostInfo(
              nameInt     = hostInt,
              namePublic  = hostPub,
            )
          }
        }
      }

      override def unbind(key: String, value: MHostInfo): String = {
        val k = key1F(key)
        val F = MHostInfo.Fields
        _mergeUnbinded1(
          strB.unbind( k(F.NAME_INTERNAL_FN), value.nameInt),
          strB.unbind( k(F.NAME_PUBLIC_FN),   value.namePublic ),
        )
      }
    }
  }

}
