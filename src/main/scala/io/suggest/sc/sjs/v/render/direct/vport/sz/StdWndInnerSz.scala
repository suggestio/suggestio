package io.suggest.sc.sjs.v.render.direct.vport.sz

import io.suggest.adv.ext.model.im.{ISize2di, Size2di}
import org.scalajs.dom
import org.scalajs.dom.Window

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.05.15 18:05
 * Description: Поддержка безопасного доступа к полям window.innerWidth и .innerHeight.
 * Стандартное API будет скорее всего будет генерить UndefinedBehaviour при доступе к inner*() полям из под ie<=8.
 */
trait StdWndInnerSzEl extends js.Object {

  /** Ширина внутренней области окна браузера. */
  def innerWidth:  UndefOr[Int] = js.native

  /** Высота внутренней области окна браузера. */
  def innerHeight: UndefOr[Int] = js.native

}

/** Аддон для [[ViewportSzT]], добавляющий поддерку чтения размеров viewport из стандартных window.inner*() полей. */
trait StdWndInnerSz extends IViewportSz {

  private def wnd2safeSz(wnd: Window): StdWndInnerSzEl = {
    wnd.asInstanceOf[StdWndInnerSzEl]
  }

  override def getViewportSize: Option[ISize2di] = {
    val wnd1 = wnd2safeSz(dom.window)
    for {
      w <- wnd1.innerWidth.toOption
      h <- wnd1.innerHeight.toOption
    } yield {
      Size2di(width = w, height = h)
    }
  }

}
