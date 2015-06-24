package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.sjs.v.vutil.SetStyleDisplay
import io.suggest.sc.sjs.vm.util.CssSzImplicits
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 18:13
 * Description:
 */
object GBlock {

  /**
   * Внести поправку в указанную абсолютную координату с помощью строковых данных по имеющейся относительной.
   * @param src Исходная строка, содержащая абсолютную координату.
   * @param abs Целевая абсолютная координата.
   * @return Новая относительная координата на основе abs и возможного значения из src.
   */
  def fixRelCoord(src: String, abs: Int): Int = {
    DataUtil.extractInt(src)
      .fold(abs)(abs - _)
  }

}


import GBlock._


case class GBlock(override val _underlying: HTMLDivElement) extends SafeElT with SetStyleDisplay with CssSzImplicits {

  override type T = HTMLDivElement

  /**
   * Двинуть блок на экране в указанные координаты. С помощью анимации, если возможно.
   * @param leftPx x-координата.
   * @param topPx y-координата.
   * @param cssPrefixes Задетекченные css-префиксы css-анимации.
   */
  def moveBlock(leftPx: Int, topPx: Int, cssPrefixes: List[String], withAnim: Boolean = true): Unit = {
    //el.style.opacity = "1"
    if (withAnim && cssPrefixes.nonEmpty) {
      displayBlock(_underlying)
      // Браузер умеет 3d-трансформации.
      val suf = "transform"
      // translate3d(+x, +y) работает с относительными координатами. Надо поправлять их с учетом ВОЗМОЖНЫХ значений style.top и style.left.
      val leftPx1 = fixRelCoord(_underlying.style.left, leftPx)
      val topPx1  = fixRelCoord(_underlying.style.top,  topPx)
      val value = "translate3d(" + leftPx1.px + "," + topPx1.px + ",0)"
      for (cssPrefix <- cssPrefixes) {
        val prop = if (!cssPrefix.isEmpty) cssPrefix + suf else suf
        _underlying.style.setProperty(prop, value)
      }

    } else {
      // Анимация отключена. Отпозиционировать по хардкору:
      _underlying.style.top  = topPx.px
      _underlying.style.left = leftPx.px
      displayBlock(_underlying)
    }
  }

}
