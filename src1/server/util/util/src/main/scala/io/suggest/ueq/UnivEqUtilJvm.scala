package io.suggest.ueq

import akka.actor.ActorRef
import japgolly.univeq.UnivEq
import play.api.data.validation.ValidationResult

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 21:31
  * Description: UnivEq-доп.утиль на стороне JVM.
  */
object UnivEqUtilJvm {

  @inline implicit def actorRefUe       : UnivEq[ActorRef]        = UnivEq.force

  @inline implicit def playSessionUe    : UnivEq[play.api.mvc.Session] = UnivEq.force

  @inline implicit def playValidationResult: UnivEq[ValidationResult] = UnivEq.force

}
