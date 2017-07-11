package io.suggest.sc.sjs.m.magent

import io.suggest.common.geom.d2.ISize2di
import io.suggest.sjs.common.log.{ILog, Log}
import io.suggest.sjs.common.msg.WarnMsgs
import io.suggest.sjs.common.vm.wnd.WindowVm

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:19
 * Description: Модель данных по экрану устройства.
 */
trait IMScreen extends ISize2di with ILog {

  def pxRatioOpt: Option[Double] = {
    WindowVm()
      .devicePixelRatio
      .map { pxr => js.Math.round(pxr * 10) / 10 }
  }

  /** Плотность пикселей экрана. */
  def pxRatio: Double = pxRatioOpt.getOrElse {
    LOG.warn( WarnMsgs.SCREEN_PX_RATIO_MISSING )
    1.0
  }

  /** Сериализовать для передачи на сервер. */
  def toQsValue: String = {
    // Округлять pxRatio до первого знака после запятой:
    var acc = width.toString + "x" + height.toString
    for (pxr <- pxRatioOpt) {
      acc = acc + "," + pxr
    }
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
  with Log
{
  override lazy val pxRatioOpt = super.pxRatioOpt
}


object MScreen {

  def apply(scrSz: ISize2di): MScreen = {
    MScreen(width = scrSz.width, height = scrSz.height)
  }

}
