package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.04.18 17:36
  * Description: Корневая модель состояния формы редактора ADN-узла.
  */
object MLkAdnEditRoot {

  implicit object MLkAdnEditRootFastEq extends FastEq[MLkAdnEditRoot] {
    override def eqv(a: MLkAdnEditRoot, b: MLkAdnEditRoot): Boolean = {
      (a.node ===* b.node) &&
        (a.popups ===* b.popups) &&
        (a.internals ===* b.internals)
    }
  }

  implicit def univEq: UnivEq[MLkAdnEditRoot] = UnivEq.derive

}


/** Класс-контейнер состояния формы редактирования ADN-узла.
  *
  * @param conf Базовая конфигурация формы.
  * @param node Состояния редактора ADN-узла.
  */
case class MLkAdnEditRoot(
                           node         : MAdnNodeS,
                           internals    : MAdnEditInternals,
                           popups       : MAdnEditPopups    = MAdnEditPopups.empty
                         ) {

  def withNode(node: MAdnNodeS)             = copy(node = node)
  def withInternals(internals: MAdnEditInternals) = copy(internals = internals)
  def withPopups(popups: MAdnEditPopups)    = copy(popups = popups)

  /** Экспорт в кросс-платформенный класс формы редактирования adn-узла. */
  def toForm: MAdnEditForm = {
    node.toForm
  }

}
