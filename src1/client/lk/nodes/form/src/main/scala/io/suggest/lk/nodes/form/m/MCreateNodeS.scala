package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.lk.m.input.MTextFieldS
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.03.17 18:05
  * Description: Модель состояния данных добавляемого узла.
  *
  */
object MCreateNodeS {

  def empty = MCreateNodeS()

  implicit object MCreateNodeSFastEq extends FastEq[MCreateNodeS] {
    override def eqv(a: MCreateNodeS, b: MCreateNodeS): Boolean = {
      (a.name ===* b.name) &&
      (a.id ===* b.id) &&
      (a.saving ===* b.saving)
    }
  }

  @inline implicit def univEq: UnivEq[MCreateNodeS] = UnivEq.derive


  val name = GenLens[MCreateNodeS](_.name)
  val id = GenLens[MCreateNodeS](_.id)
  def saving = GenLens[MCreateNodeS](_.saving)

}


/** Состояние формочки создания нового узла.
  *
  * @param name Название узла, задаётся юзером.
  * @param id Заданный id-узла. Для маячков, в первую очередь.
  * @param saving Сейчас происходит сохранение узла?
  */
case class MCreateNodeS(
                         name           : MTextFieldS     = MTextFieldS.empty,
                         id             : MTextFieldS     = MTextFieldS.empty,
                         saving         : Pot[_]          = Pot.empty,
                       ) {

  def isValid = name.isValidNonEmpty && id.isValidNonEmpty

}
