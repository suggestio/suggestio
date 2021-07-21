package io.suggest.sys.mdr.m

import diode.FastEq
import io.suggest.sys.mdr.MMdrConf
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.10.18 22:04
  * Description: Корневая модель состояния компонента модерации карточек.
  */
object MSysMdrRootS {

  /** Поддержка FastEq для корневой модели. */
  implicit object MSysMdrRootSFastEq extends FastEq[MSysMdrRootS] {
    override def eqv(a: MSysMdrRootS, b: MSysMdrRootS): Boolean = {
      (a.node ===* b.node) &&
      (a.dialogs ===* b.dialogs) &&
      (a.form ===* b.form) &&
      (a.conf ===* b.conf)
    }
  }

  @inline implicit def univEq: UnivEq[MSysMdrRootS] = UnivEq.derive

  val node    = GenLens[MSysMdrRootS](_.node)
  def dialogs = GenLens[MSysMdrRootS](_.dialogs)
  def form    = GenLens[MSysMdrRootS](_.form)
  def conf    = GenLens[MSysMdrRootS](_.conf)

}


/** Корневой контейнер данных состояния react-формы sys-mdr.
  *
  * @param node Инстанс состояния модерации узла.
  * @param conf Конфиг формы.
  */
case class MSysMdrRootS(
                         node         : MMdrNodeS         = MMdrNodeS.empty,
                         dialogs      : MMdrDialogs       = MMdrDialogs.empty,
                         form         : MMdrFormS         = MMdrFormS.empty,
                         conf         : MMdrConf,
                       )
