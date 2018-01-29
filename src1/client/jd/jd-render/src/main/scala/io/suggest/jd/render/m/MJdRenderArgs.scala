package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.scalaz.NodePath_t
import japgolly.scalajs.react.vdom.TagMod
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.ReactUnivEqUtil._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 16:03
  * Description: Модель-контейнер данных для рендера одного JSON-документа.
  */

object MJdRenderArgs {

  val empty = apply()

  /** Поддержка FastEq для инстансов [[MJdRenderArgs]]. */
  implicit object MJdRenderArgsFastEq extends FastEq[MJdRenderArgs] {
    override def eqv(a: MJdRenderArgs, b: MJdRenderArgs): Boolean = {
      (a.selPath              ===* b.selPath) &&
        (a.selJdtBgImgMod     ===* b.selJdtBgImgMod) &&
        (a.hideNonMainStrips  ==*  b.hideNonMainStrips) &&
        (a.dnd                ===* b.dnd)
    }
  }

  implicit def univEq: UnivEq[MJdRenderArgs] = UnivEq.derive

}


/** Класс-контейнер обобщённых данных для реднера JSON-документа.
  *
  * @param selPath Путь до текущего выделенного элемента (с которым происходит взаимодействие юзера в редакторе).
  * @param selJdtBgImgMod Трансформировать bgImg у выбранного тега с помощью этого TagMod.
  *                       Появилась для возможности заглядывать "под" изображение в редакторе, чтобы увидеть фон.
  * @param hideNonMainStrips Скрыть все неглавные стрипы? Раньше была прозрачность, но она конфликтует с grid'ом.
  * @param dnd Состояние драг-н-дропа, который может прийти сюда из неизвестности.
  */
case class MJdRenderArgs(
                          selPath             : Option[NodePath_t]          = None,
                          selJdtBgImgMod      : Option[TagMod]              = None,
                          hideNonMainStrips   : Boolean                     = false,
                          dnd                 : MJdDndS                     = MJdDndS.empty
                        ) {

  def withSelPath(selPath: Option[NodePath_t])                  = copy(selPath = selPath)
  def withSelJdtBgImgMod(selJdtBgImgMod: Option[TagMod])        = copy(selJdtBgImgMod = selJdtBgImgMod)
  def withHideNonMainStrips(hideNonMainStrips: Boolean)         = copy(hideNonMainStrips = hideNonMainStrips)
  def withDnd(dnd: MJdDndS = MJdDndS.empty)                     = copy(dnd = dnd)

}
