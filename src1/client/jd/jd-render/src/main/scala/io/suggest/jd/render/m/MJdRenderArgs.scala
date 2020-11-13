package io.suggest.jd.render.m

import diode.FastEq
import io.suggest.color.MColorData
import io.suggest.scalaz.NodePath_t
import japgolly.scalajs.react.vdom.TagMod
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.ReactUnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.08.17 16:03
  * Description: Модель-контейнер данных для рендера одного JSON-документа.
  */

object MJdRenderArgs {

  lazy val empty = apply()

  /** Поддержка FastEq для инстансов [[MJdRenderArgs]]. */
  implicit object MJdRenderArgsFastEq extends FastEq[MJdRenderArgs] {
    override def eqv(a: MJdRenderArgs, b: MJdRenderArgs): Boolean = {
      (a.selPath            ===* b.selPath) &&
      (a.selJdtBgImgMod.isEmpty ==* b.selJdtBgImgMod.isEmpty) &&
      (a.hideNonMainStrips  ==*  b.hideNonMainStrips) &&
      (a.groupOutLined      ===* b.groupOutLined) &&
      (a.dnd                ===* b.dnd)
    }
  }

  @inline implicit def univEq: UnivEq[MJdRenderArgs] = UnivEq.derive

  def selPath = GenLens[MJdRenderArgs](_.selPath)
  def selJdtBgImgMod = GenLens[MJdRenderArgs](_.selJdtBgImgMod)
  def hideNonMainStrips = GenLens[MJdRenderArgs](_.hideNonMainStrips)
  def groupOutLined = GenLens[MJdRenderArgs](_.groupOutLined)
  def dnd = GenLens[MJdRenderArgs](_.dnd)

}


/** Класс-контейнер обобщённых данных для реднера JSON-документа.
  *
  * @param selPath Путь до текущего выделенного элемента (с которым происходит взаимодействие юзера в редакторе).
  * @param selJdtBgImgMod Трансформировать bgImg у выбранного тега с помощью этого TagMod.
  *                       Появилась для возможности заглядывать "под" изображение в редакторе, чтобы увидеть фон.
  * @param hideNonMainStrips Скрыть все неглавные стрипы? Раньше была прозрачность, но она конфликтует с grid'ом.
  * @param groupOutLined Использовать group-выделение указанного цвета.
  *                      Выделение группы ориентировано на визуально-непрерываное выделение близко-находящихся карточек.
  * @param dnd Состояние драг-н-дропа, который может прийти сюда из неизвестности.
  */
case class MJdRenderArgs(
                          selPath             : Option[NodePath_t]          = None,
                          selJdtBgImgMod      : Option[TagMod]              = None,
                          hideNonMainStrips   : Boolean                     = false,
                          groupOutLined       : Option[MColorData]          = None,
                          dnd                 : MJdDndS                     = MJdDndS.empty,
                        ) {

  lazy val selPathRev: Option[NodePath_t] =
    selPath.map(_.reverse)

}
