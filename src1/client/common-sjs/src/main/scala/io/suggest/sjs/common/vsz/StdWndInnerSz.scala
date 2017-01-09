package io.suggest.sjs.common.vsz

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:05
 * Description: Поддержка безопасного доступа к полям window.innerWidth и .innerHeight.
 * Стандартное API будет скорее всего будет генерить UndefinedBehaviour при доступе к inner*() полям из под ie<=8.
 */
@js.native
trait StdWndInnerSzEl extends js.Object {

  /** Ширина внутренней области окна браузера. */
  def innerWidth:  UndefOr[Int] = js.native

  /** Высота внутренней области окна браузера. */
  def innerHeight: UndefOr[Int] = js.native

}

/** Аддон для [[ViewportSz]], добавляющий поддерку чтения размеров viewport из стандартных window.inner*() полей. */
trait StdWndInnerSz extends IViewportSz {

  private def wndSafe: StdWndInnerSzEl = {
    dom.window.asInstanceOf[StdWndInnerSzEl]
  }

  override def widthPx: Option[Int]  = wndSafe.innerWidth.toOption

  override def heightPx: Option[Int] = wndSafe.innerHeight.toOption

}
