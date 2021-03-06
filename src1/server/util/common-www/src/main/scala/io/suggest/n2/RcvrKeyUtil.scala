package io.suggest.n2

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.xplay.psb.PathBindableImpl
import io.suggest.xplay.qsb.{QsbSeq, AbstractQueryStringBindable}
import play.api.mvc.{PathBindable, QueryStringBindable}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.17 11:56
  * Description: Утиль для серверной работы с моделью RcvrKey.
  */
object RcvrKeyUtil {

  def RCVR_KEY_MAXLEN = 7

  /** Неявности всякие живут в отдельном контейнере: */
  object Implicits {

    /** PathBindable для инстанса RcvrKey. */
    implicit def rcvrKeyPb(implicit strB: PathBindable[String]): PathBindable[RcvrKey] = {
      new PathBindableImpl[RcvrKey] {

        override def bind(key: String, value: String): Either[String, RcvrKey] = {
          if (value.isEmpty) {
            Left("error.required")
          } else {
            val nodeIdsArr = value.split( RcvrKey.PATH_DELIM.head )
            if (nodeIdsArr.length < 1) {
              Left( "error.empty" )
            } else if (nodeIdsArr.length <= RCVR_KEY_MAXLEN) {
              Right( nodeIdsArr.toList )
            } else {
              Left( "e.rcvrKey.tooLong" )
            }
          }
        }

        override def unbind(key: String, value: RcvrKey): String = {
          value.mkString( RcvrKey.PATH_DELIM )
        }

      }
    }


    /** QSB для rcvrKey. */
    implicit def rcvrKeyQsb(implicit qsbSeqStrB: QueryStringBindable[QsbSeq[String]]): QueryStringBindable[RcvrKey] = {
      new AbstractQueryStringBindable[RcvrKey] {

        override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, RcvrKey]] = {
          for {
            qsbSeqE <- qsbSeqStrB.bind(key, params)
          } yield {
            for {
              qsbSeq <- qsbSeqE
            } yield {
              qsbSeq.items
                .headOption
                .filter { nodeId =>
                  (qsbSeq.items.lengthIs == 1) &&
                  RcvrKey.isPath( nodeId )
                }
                .fold {
                  RcvrKey.from( qsbSeq.items )
                } {
                  RcvrKey.urlPath2RcvrKey
                }
            }
          }
        }

        override def unbind(key: String, value: RcvrKey): String = {
          qsbSeqStrB.unbind(key, QsbSeq(value))
        }

      }
    }

  }

}
