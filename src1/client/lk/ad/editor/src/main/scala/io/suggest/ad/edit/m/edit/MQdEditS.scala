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
      (a.initDelta ===* b.initDelta) &&
        (a.realDelta ===* b.realDelta) &&
        (a.bgColorPick ===* b.bgColorPick)
    }
  }

  implicit def univEq: UnivEq[MQdEditS] = UnivEq.derive

}


/** Класс-контейнер данных состояния редактирования тексто-контента.
  *
  * @param initDelta Quill-delta инстанс ДЛЯ ИНИЦИАЛИЗАЦИИ/СБРОСА редактора.
  *               Т.е. он теряет актуальность во время непосредственного редактирования.
  * @param realDelta Реальная текущая дельта, актуальна в любой момент.
  *                  Используется как fallback, чтобы в момент незапланированного пере-рендера
  *                  quill-редактора, всё-таки был доступ к корректной дельте.
  *                  None означает, что текущая актуальная дельта лежит в initDelta.
  * @param bgColorPick Цвет фона, если необходим.
  */
case class MQdEditS(
                     initDelta                  : Delta,
                     realDelta                  : Option[Delta]     = None,
                     override val bgColorPick   : MColorPickerS     = MColorPickerS.empty
                   )
  extends IBgColorPickerS
{

  override type T = MQdEditS

  def withInitRealDelta(initDelta: Delta, realDelta: Option[Delta] = None) = copy(initDelta = initDelta, realDelta = realDelta)
  def withInitDelta(initDelta: Delta)                     = copy(initDelta = initDelta)
  def withRealDelta(realDelta: Option[Delta])             = copy(realDelta = realDelta)
  override def withBgColorPick(bgColor: MColorPickerS)    = copy(bgColorPick = bgColor)


  /** Залить realDelta в init, чтобы принудительно освежить состояние.
    * Удобно, если надо перерендерить редактор.
    */
  def refresh: MQdEditS = {
    realDelta.fold(this) { delta2 =>
      copy(
        initDelta = delta2,
        realDelta = None
      )
    }
  }

}
