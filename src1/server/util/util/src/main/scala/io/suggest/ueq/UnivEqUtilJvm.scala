package io.suggest.ueq

import akka.actor.ActorRef
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 21:31
  * Description: UnivEq-доп.утиль на стороне JVM.
  */
object UnivEqUtilJvm {

  @inline implicit def actorRefUe       : UnivEq[ActorRef]        = UnivEq.force

}
