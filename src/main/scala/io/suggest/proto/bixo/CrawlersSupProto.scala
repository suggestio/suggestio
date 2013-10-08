package io.suggest.proto.bixo

import akka.actor.ActorRef
import collection.immutable.Seq

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.13 18:21
 * Description: Сообщения для общения с супервизором кравлеров.
 */

object CrawlersSupProto {
  type MaybeBootstrapDkeyReply_t = Option[ActorRef]
  type AskRefForDkeyReply_t      = MaybeBootstrapDkeyReply_t

  case class AskRefForDkey(dkey: String) extends Serializable
  case class MaybeBootstrapDkey(dkey: String, seedUrls: Seq[String]) extends Serializable
}

