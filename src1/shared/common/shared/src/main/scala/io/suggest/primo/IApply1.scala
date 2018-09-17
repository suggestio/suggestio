package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.09.16 10:46
  * Description: Интерфейс для метода apply с arity=1.
  */
trait IApply1 extends TypeT {

  type ApplyArg_t

  def apply(arg: ApplyArg_t): T

}


/** Трейт с реализацией метода applyOpt() на базе apply(). */
trait IApplyOpt1 extends IApply1 {

  def applyOpt(elOpt: Option[ApplyArg_t]): Option[T] = {
    elOpt.map(apply)
  }

}
