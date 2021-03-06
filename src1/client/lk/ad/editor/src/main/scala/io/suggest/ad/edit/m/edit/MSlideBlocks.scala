package io.suggest.ad.edit.m.edit

import diode.FastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.11.17 12:25
  * Description: Модель состояния слайд-блоков редактора.
  */
object MSlideBlocks {

  def empty = apply()

  implicit object MSlideBlocksFastEq extends FastEq[MSlideBlocks] {
    override def eqv(a: MSlideBlocks, b: MSlideBlocks): Boolean = {
      a.expanded ===* b.expanded
    }
  }

  @inline implicit def univEq: UnivEq[MSlideBlocks] = UnivEq.derive

  def expanded = GenLens[MSlideBlocks](_.expanded)

}


/** Класс модели состояния слайд-блоков.
  *
  * @param expanded Ключ текущего раскрытого блока.
  *                 Option намекает, что можно раскрывать максимум один блок.
  */
case class MSlideBlocks(
                         expanded: Option[String] = None
                       )
