package io.suggest.ad.edit.m.edit

import com.quilljs.delta.Delta
import diode.FastEq
import io.suggest.ad.edit.m.edit.color.{IBgColorPickerS, MColorPickerS}
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.QuillUnivEqUtil._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.09.17 22:24
  * Description: Модель-контейнер данных состояния редактирования элемента контента.
  */
object MQdEditS {

  /** Поддержка FastEq для инстансов [[MQdEditS]]. */
  implicit object MQdEditSFastEq extends FastEq[MQdEditS] {
    override def eqv(a: MQdEditS, b: MQdEditS): Boolean = {
      (a.qDelta ===* b.qDelta) &&
        (a.bgColorPick ===* b.bgColorPick)
    }
  }

  implicit def univEq: UnivEq[MQdEditS] = UnivEq.derive

}


/** Класс-контейнер данных состояния редактирования тексто-контента.
  *
  * @param qDelta Quill-delta инстанс ДЛЯ ИНИЦИАЛИЗАЦИИ/СБРОСА редактора.
  *               Т.е. он теряет актуальность во время непосредственного редактирования.
  * @param bgColorPick Цвет фона, если необходим.
  */
case class MQdEditS(
                     qDelta                     : Delta,
                     override val bgColorPick   : MColorPickerS     = MColorPickerS.empty
                   )
  extends IBgColorPickerS
{

  override type T = MQdEditS

  def withQDelta(qDelta: Delta) = copy(qDelta = qDelta)
  override def withBgColorPick(bgColor: MColorPickerS) = copy(bgColorPick = bgColor)

}
