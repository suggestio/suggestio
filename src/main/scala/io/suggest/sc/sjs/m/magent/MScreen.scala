package io.suggest.sc.sjs.m.magent

import io.suggest.adv.ext.model.im.ISize2di
import io.suggest.sjs.common.view.safe.wnd.SafeWindow
import org.scalajs.dom

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:19
 * Description: Модель данных по экрану устройства.
 */
trait IMScreen extends ISize2di {

  def pxRatioOpt: Option[Double] = {
    SafeWindow(dom.window)
      .devicePixelRatio
      .map { pxr => js.Math.round(pxr * 10) / 10 }
  }

  /** Плотность пикселей экрана. */
  def pxRatio: Double = pxRatioOpt.getOrElse(1.0)

  /** Сериализовать для передачи на сервер. */
  def toQsValue: String = {
    // Округлять pxRatio до первого знака после запятой:
    var acc = width.toString + "x" + height.toString
    val _pxrOpt = pxRatioOpt
    if (_pxrOpt.isDefined)
      acc = acc + "," + _pxrOpt.get
    acc
  }

  override def toString: String = toQsValue

}


/** Дефолтовая реализация [[IMScreen]] для описания экрана. */
case class MScreen(
  override val width    : Int,
  override val height   : Int
)
  extends IMScreen
{
  override lazy val pxRatioOpt = super.pxRatioOpt
}


object MScreen {

  def apply(scrSz: ISize2di): MScreen = {
    MScreen(width = scrSz.width, height = scrSz.height)
  }

}
