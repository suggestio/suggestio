package io.suggest.sc.sjs.m.magent

import io.suggest.adv.ext.model.im.ISize2di

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.05.15 17:19
 * Description: Модель данных по экрану устройства.
 */
trait IMScreen extends ISize2di {

  /** Плотность пикселей экрана. */
  def pxRatio: Double

  /** Сериализовать для передачи на сервер. */
  def toQsValue: String = {
    // Округлять pxRatio до первого знака после запятой:
    val pxRatio2 = js.Math.round(pxRatio * 10) / 10
    width.toString + "x" + height.toString + "," + pxRatio2
  }

  override def toString: String = toQsValue

}


/** Дефолтовая реализация [[IMScreen]] для описания экрана. */
case class MScreen(
  override val width    : Int,
  override val height   : Int,
  override val pxRatio  : Double
)
  extends IMScreen
