package io.suggest.sc.m.dia

import io.suggest.id.login.LoginFormCircuit
import io.suggest.id.login.m.session.{MLogOutDia, MSessionS}
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

  def ident = GenLens[MScLoginS]( _.ident )
  val session = GenLens[MScLoginS]( _.session )
  val logout = GenLens[MScLoginS]( _.logout )


  implicit final class ScLoginExt( private val scLogin: MScLoginS ) extends AnyVal {

    def isDiaOpened: Boolean =
      scLogin.ident.isDefined

  }

}


final case class MScLoginS(
                            ident               : Option[LoginFormCircuit]          = None,
                            session             : MSessionS                         = MSessionS.empty,
                            logout              : Option[MLogOutDia]                = None,
                          )
