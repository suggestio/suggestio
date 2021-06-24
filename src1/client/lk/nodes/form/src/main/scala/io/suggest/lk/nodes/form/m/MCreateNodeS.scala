package io.suggest.lk.nodes.form.m

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.lk.m.input.MTextFieldS
import io.suggest.n2.node.{MNodeType, MNodeTypes}
import io.suggest.scalaz.NodePath_t
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

  implicit object MCreateNodeSFastEq extends FastEq[MCreateNodeS] {
    override def eqv(a: MCreateNodeS, b: MCreateNodeS): Boolean = {
      (a.nodeType ===* b.nodeType) &&
      (a.parentPath ===* b.parentPath) &&
      (a.name ===* b.name) &&
      (a.id ===* b.id) &&
      (a.saving ===* b.saving)
    }
  }

  @inline implicit def univEq: UnivEq[MCreateNodeS] = UnivEq.derive


  def nodeType = GenLens[MCreateNodeS](_.nodeType)
  def parentPath = GenLens[MCreateNodeS](_.parentPath)
  val name = GenLens[MCreateNodeS](_.name)
  val id = GenLens[MCreateNodeS](_.id)
  def saving = GenLens[MCreateNodeS](_.saving)

}


/** Состояние формочки создания нового узла.
  *
  * @param nodeType Type of node.
  * @param parentPath Путь в дереве формы до родительского узла.
  * @param name Название узла, задаётся юзером.
  * @param id Заданный id-узла. Для маячков, в первую очередь.
  * @param saving Сейчас происходит сохранение узла?
  */
case class MCreateNodeS(
                         nodeType       : MTextFieldS        = MTextFieldS.empty,
                         typeDisabled   : Boolean            = false,
                         parentPath     : Option[NodePath_t] = None,
                         name           : MTextFieldS     = MTextFieldS.empty,
                         id             : MTextFieldS     = MTextFieldS.empty,
                         saving         : Pot[_]          = Pot.empty,
                       ) {

  // Deserialized node-type. TODO Somehow inline MNodeType into nodeType.value, instead of MNodeType <= String convertion.
  lazy val nodeTypeOpt: Option[MNodeType] =
    OptionUtil.maybeOpt( nodeType.value.nonEmpty )( MNodeTypes.withValueOpt(nodeType.value) )

  def isValid = {
    nodeType.isValidNonEmpty &&
    name.isValidNonEmpty &&
    id.isValidNonEmpty &&
    parentPath.nonEmpty
  }

}
