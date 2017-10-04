package io.suggest.crypto.hash

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 15:35
  * Description: Jvm-only утиль для [[HashesHex]].
  */
object HashesHexJvm {

  /** Поддержка URL-qs вида "x.s1=aahh45234234&x.s256=aa543525325..."  */
  implicit def hashesHexQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[HashesHex] = {
    new QueryStringBindableImpl[HashesHex] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, HashesHex]] = {
        val hashesHexOptEithSeq = for (mhash <- MHashes.values) yield {
          for {
            hexValueEith <- strB.bind( key1(key, mhash.value), params )
          } yield {
            for (hexValue <- hexValueEith) yield {
              mhash -> hexValue
            }
          }
        }

        if (hashesHexOptEithSeq.isEmpty || hashesHexOptEithSeq.forall(_.isEmpty)) {
          None

        } else {
          val hashesHexEithSeq = hashesHexOptEithSeq
            .flatten
          val errorsIter = hashesHexEithSeq
            .iterator
            .flatMap(_.left.toOption)
          val eith = if (errorsIter.nonEmpty) {
            Left( errorsIter.mkString(",") )
          } else {
            val hhMap: HashesHex = hashesHexEithSeq
              .iterator
              .flatMap(_.right.toOption)
              .toMap
            Right(hhMap)
          }
          Some(eith)
        }
      }


      override def unbind(key: String, value: HashesHex): String = {
        _mergeUnbinded {
          for ((mhash, hexValue) <- value.iterator) yield {
            strB.unbind(key1(key, mhash.value), hexValue)
          }
        }
      }

    }
  }

}
