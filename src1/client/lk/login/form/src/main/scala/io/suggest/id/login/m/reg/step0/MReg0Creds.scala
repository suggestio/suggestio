package io.suggest.id.login.m.reg.step0

import diode.FastEq
import io.suggest.id.login.m.reg.ICanSubmit
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.06.19 18:19
  * Description: Модель данных состояния нулевого шага регистрации.
  */
object MReg0Creds {

  def empty = apply()

  implicit object MReg0CredsFastEq extends FastEq[MReg0Creds] {
    override def eqv(a: MReg0Creds, b: MReg0Creds): Boolean = {
      (a ===* b) || (
        (a.email        ===* b.email) &&
        (a.phone        ===* b.phone)
      )
    }
  }

  val email       = GenLens[MReg0Creds]( _.email )
  val phone       = GenLens[MReg0Creds]( _.phone )

  @inline implicit def univEq: UnivEq[MReg0Creds] = UnivEq.derive

}


case class MReg0Creds(
                      email            : MTextFieldS         = MTextFieldS.empty,
                      phone            : MTextFieldS         = MTextFieldS.empty,
                    )
  extends ICanSubmit
{

  override def canSubmit: Boolean = {
    (email :: phone :: Nil)
      .forall( _.isValidNonEmpty )
  }

}
