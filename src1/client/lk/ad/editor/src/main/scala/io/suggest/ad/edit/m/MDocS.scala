package io.suggest.ad.edit.m

import com.quilljs.delta.Delta
import diode.FastEq
import io.suggest.ad.edit.m.edit.MAddS
import io.suggest.ad.edit.m.edit.strip.MStripEdS
import io.suggest.jd.render.m.MJdArgs
import io.suggest.ad.edit.m.edit.MColorsState

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
        (a.qDelta eq b.qDelta) &&
        (a.stripEd eq b.stripEd) &&
        (a.addS eq b.addS) &&
        (a.colorsState eq b.colorsState)
    }
  }

}


/** Класс модели состояния работы с документом.
  *
  * @param jdArgs Текущий набор данных для рендера шаблона.
  * @param qDelta Исходная Delta редактируемного текста в quill-редакторе.
  *               Выставляется в начале редактирования, и НЕ обновляется во время редактирования.
  * @param stripEd Состояние strip-редактора, если открыт.
  * @param addS Состояние формочки добавления нового элемента.
  * @param colorsState Общее состояние редактирования цветов: разные часто-используемые или подходящие цвета, например.
  */
case class MDocS(
                  jdArgs        : MJdArgs,
                  qDelta        : Option[Delta]         = None,
                  stripEd       : Option[MStripEdS]     = None,
                  addS          : Option[MAddS]         = None,
                  colorsState   : MColorsState          = MColorsState.empty
                ) {

  def withJdArgs(jdArgs: MJdArgs) = copy(jdArgs = jdArgs)

  def withQDelta(qDelta: Option[Delta]) = copy(qDelta = qDelta)
  def withOutQDelta = if (qDelta.nonEmpty) withQDelta(None) else this

  def withStripEd(stripEd: Option[MStripEdS]) = copy(stripEd = stripEd)
  def withOutStripEd = if (stripEd.nonEmpty) withStripEd(None) else this

  def withAddS(addS: Option[MAddS]) = copy(addS = addS)

  def withColorsState(colorsState : MColorsState) = copy(colorsState = colorsState)

}
