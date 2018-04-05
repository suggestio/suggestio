package io.suggest.adn.edit.m

import diode.FastEq
import io.suggest.adn.edit.{MAdnEditForm, MAdnEditFormConf}
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
        (a.conf ===* b.conf)
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
                           conf   : MAdnEditFormConf,
                           node   : MAdnNodeS
                         ) {

  def withNode(node: MAdnNodeS)         = copy(node = node)

  /** Экспорт в кросс-платформенный класс формы редактирования adn-узла. */
  def toForm: MAdnEditForm = {
    MAdnEditForm(
      mmeta = node.meta
    )
  }

}
