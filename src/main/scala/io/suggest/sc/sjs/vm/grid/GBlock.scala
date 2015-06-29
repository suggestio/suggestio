package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.Block
import io.suggest.sc.sjs.c.ScFsm
import io.suggest.sc.sjs.m.mgrid.{GridBlockClick, IBlockInfo}
import io.suggest.sc.sjs.v.vutil.{OnClickSelfT, SetStyleDisplay}
import io.suggest.sc.sjs.vm.util.CssSzImplicits
import io.suggest.sjs.common.util.{SjsLogger, DataUtil}
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.{Event, Node}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 18:13
 * Description: Модель для блока плитки.
 */
object GBlock extends SjsLogger {

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

  /**
   * Есть узел DOM, который должен быть блоком. Попытаться завернуть в модель.
   * @param node Узел DOM.
   * @return Some(GBlock).
   */
  def fromNode(node: Node): Option[GBlock] = {
    // TODO Проверять, что элемент является необходимым div'ом и возвращать None.
    val ediv = node.asInstanceOf[HTMLDivElement]
    Some( GBlock(ediv) )
  }

}


import GBlock._


trait GBlockT extends SafeElT with SetStyleDisplay with CssSzImplicits with IBlockInfo with OnClickSelfT {

  override type T = HTMLDivElement

  // Быстрый доступ к кое-каким аттрибутам.
  override def id      = _underlying.id
  override def width   = getIntAttributeStrict(Block.BLK_WIDTH_ATTR).get
  override def height  = getIntAttributeStrict(Block.BLK_HEIGHT_ATTR).get


  /**
   * Двинуть блок на экране в указанные координаты без анимации.
   * @param leftPx x-координата.
   * @param topPx y-координата.
   */
  def moveBlock(leftPx: Int, topPx: Int): Unit = {
    _underlying.style.top  = topPx.px
    _underlying.style.left = leftPx.px
    displayBlock(_underlying)
  }

  /** Анимированно переместить блок в указанную позицию. */
  def moveBlockAnimated(leftPx: Int, topPx: Int, cssPrefixes: List[String]): Unit = {
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
  }

  override def div = _underlying

  /** Повесить листенеры для событий DOM. */
  def initLayout(): Unit = {
    onClick { e: Event =>
      ScFsm ! GridBlockClick(e)
    }
  }

}

/** Дефолтовая реализация экземпляра модели [[GBlockT]]. */
case class GBlock(override val _underlying: HTMLDivElement) extends GBlockT
