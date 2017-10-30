package io.suggest.file.up

import io.suggest.crypto.hash.HashesHex
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.10.17 18:18
  * Description: JVM-only утиль для обслуживания кросс.модели [[MFile4UpProps]] на стороне сервера.
  */
object MFile4UpPropsJvm {

  object Fields {
    def SIZE_B_FN     = "s"
    def HASHES_HEX_FN = "h"
    def MIME_TYPE_FN  = "m"
  }


  /** Поддержка биндинга инстансов [[MFile4UpProps]] из URL query string. */
  implicit def file4upPropsQsb(implicit
                               longB         : QueryStringBindable[Long],
                               hashesHexB    : QueryStringBindable[HashesHex],
                               strB          : QueryStringBindable[String]
                              ): QueryStringBindable[MFile4UpProps] = {
    new QueryStringBindableImpl[MFile4UpProps] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MFile4UpProps]] = {
        val k = key1F(key)
        val F = Fields
        for {
          sizeE           <- longB.bind       ( k(F.SIZE_B_FN),     params )
          hashesHexE      <- hashesHexB.bind  ( k(F.HASHES_HEX_FN), params )
          mimeTypeE       <- strB.bind        ( k(F.MIME_TYPE_FN),  params )
        } yield {
          for {
            sizeBytes     <- sizeE.right
            hashesHex     <- hashesHexE.right
            mimeType      <- mimeTypeE.right
          } yield {
            MFile4UpProps(
              sizeB       = sizeBytes,
              hashesHex   = hashesHex,
              mimeType    = mimeType
            )
          }
        }
      }

      override def unbind(key: String, value: MFile4UpProps): String = {
        val k = key1F(key)
        val F = Fields
        _mergeUnbinded1(
          longB.unbind      ( k(F.SIZE_B_FN),       value.sizeB ),
          hashesHexB.unbind ( k(F.HASHES_HEX_FN),   value.hashesHex ),
          strB.unbind       ( k(F.MIME_TYPE_FN),    value.mimeType )
        )
      }

    }
  }


}
