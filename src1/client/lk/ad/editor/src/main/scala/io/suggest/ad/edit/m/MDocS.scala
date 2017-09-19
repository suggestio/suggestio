package io.suggest.ad.edit.m

import diode.FastEq
import io.suggest.ad.edit.m.edit.{MAddS, MColorsState, MFileInfo, MQdEditS}
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.jd.render.m.MJdArgs
import io.suggest.model.n2.edge.EdgeUid_t

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
object MDocS {

  /** Реализация FastEq для инстансов [[MDocS]]. */
  implicit object MDocSFastEq extends FastEq[MDocS] {
    override def eqv(a: MDocS, b: MDocS): Boolean = {
      (a.jdArgs eq b.jdArgs) &&
        (a.qdEdit eq b.qdEdit) &&
        (a.stripEd eq b.stripEd) &&
        (a.addS eq b.addS) &&
        (a.colorsState eq b.colorsState) &&
        (a.files eq b.files)
    }
  }

}


/** Класс модели состояния работы с документом.
  *
  * @param jdArgs Текущий набор данных для рендера шаблона.
  * @param qdEdit Состояние редактирования контента, если есть.
  * @param stripEd Состояние strip-редактора, если открыт.
  * @param addS Состояние формочки добавления нового элемента.
  * @param colorsState Общее состояние редактирования цветов:
  *                    разные часто-используемые или подходящие цвета, например.
  * @param files Карта всякой рантаймовой инфы по файлам, где ключ -- uid эджа.
  */
case class MDocS(
                  jdArgs        : MJdArgs,
                  qdEdit        : Option[MQdEditS]              = None,
                  stripEd       : Option[MStripEdS]             = None,
                  addS          : Option[MAddS]                 = None,
                  colorsState   : MColorsState                  = MColorsState.empty,
                  files         : Map[EdgeUid_t, MFileInfo]     = Map.empty
                ) {

  def withJdArgs(jdArgs: MJdArgs) = copy(jdArgs = jdArgs)

  def withQdEdit(qdEdit: Option[MQdEditS]) = copy(qdEdit = qdEdit)
  def withOutQdEdit = if (qdEdit.nonEmpty) withQdEdit(None) else this

  def withStripEd(stripEd: Option[MStripEdS]) = copy(stripEd = stripEd)
  def withOutStripEd = if (stripEd.nonEmpty) withStripEd(None) else this

  def withAddS(addS: Option[MAddS]) = copy(addS = addS)

  def withColorsState(colorsState : MColorsState) = copy(colorsState = colorsState)

  def withFiles(files: Map[EdgeUid_t, MFileInfo]) = copy(files = files)

}
