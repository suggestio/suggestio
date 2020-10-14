package io.suggest.sc.m.dia

import io.suggest.id.login.LoginFormCircuit
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.08.2020 15:29
  * Description: Контейнер состояния внутреннего модуля логин-формы.
  */
object MScLoginS {

  def empty = apply()

  @inline implicit def univEq: UnivEq[MScLoginS] = UnivEq.derive

  def circuit = GenLens[MScLoginS]( _.circuit )


  implicit final class ScLoginExt( private val scLogin: MScLoginS ) extends AnyVal {

    def isDiaOpened: Boolean =
      scLogin.circuit.isDefined

  }

}


final case class MScLoginS(
                            circuit         : Option[LoginFormCircuit]          = None,
                          )
