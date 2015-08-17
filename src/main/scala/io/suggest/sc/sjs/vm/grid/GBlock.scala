package io.suggest.sc.sjs.vm.grid

import io.suggest.common.css.CssSzImplicits
import io.suggest.sc.ScConstants.Block._
import io.suggest.sc.sjs.m.mgrid.{GridBlockClick, IBlockInfo}
import io.suggest.sc.sjs.vm.util.InitOnClickToFsmT
import io.suggest.sc.sjs.vm.util.domvm.{IApplyEl, FindElPrefixedIdT}
import io.suggest.sc.sjs.vm.util.domvm.walk.{PrevNextSiblingCousinUtilT, PrevNextSiblingsVmT}
import io.suggest.sjs.common.util.DataUtil
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.SetDisplayEl
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 18:13
 * Description: Модель для блока плитки.
 */
object GBlock extends FindElPrefixedIdT with IApplyEl {

  override def DOM_ID = ID_SUFFIX
  override type Dom_t = HTMLDivElement
  override type T = GBlock

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


trait GBlockT extends SafeElT with SetDisplayEl with CssSzImplicits with IBlockInfo with InitOnClickToFsmT with PrevNextSiblingsVmT {

  override type T = HTMLDivElement
  override type Self_t <: GBlockT

  // Быстрый доступ к кое-каким аттрибутам.
  override def id           = _underlying.id
  /** Ширина карточки в css-пикселях. */
  override def width        = getIntAttributeStrict(BLK_WIDTH_ATTR).get
  /** Высота карточки в css-пикселях. */
  override def height       = getIntAttributeStrict(BLK_HEIGHT_ATTR).get
  /** ES id карточки. */
  def madId                 = getAttribute(MAD_ID_ATTR)
  /** Порядковый номер карточки. */
  def index                 = getIntAttributeStrict(BLK_INDEX_ATTR).get

  /**
   * Двинуть блок на экране в указанные координаты без анимации.
   * @param leftPx x-координата.
   * @param topPx y-координата.
   */
  def moveBlock(leftPx: Int, topPx: Int): Unit = {
    val s = _underlying.style
    s.top  = topPx.px
    s.left = leftPx.px
    displayBlock()
  }

  /** Анимированно переместить блок в указанную позицию. */
  def moveBlockAnimated(leftPx: Int, topPx: Int, cssPrefixes: List[String]): Unit = {
    displayBlock()
    // Браузер умеет 3d-трансформации.
    val suf = "transform"
    // translate3d(+x, +y) работает с относительными координатами. Надо поправлять их с учетом ВОЗМОЖНЫХ значений style.top и style.left.
    val s = _underlying.style
    val leftPx1 = fixRelCoord(s.left, leftPx)
    val topPx1  = fixRelCoord(s.top,  topPx)
    val value = "translate3d(" + leftPx1.px + "," + topPx1.px + ",0)"
    for (cssPrefix <- cssPrefixes) {
      val prop = if (!cssPrefix.isEmpty) cssPrefix + suf else suf
      s.setProperty(prop, value)
    }
  }

  override def div = _underlying

  /** Статический компаньон модели для сборки сообщений. */
  override protected[this] def _clickMsgModel = GridBlockClick

  def parentFragment: Option[GContainerFragment] = {
    Option( _underlying.parentElement.asInstanceOf[GContainerFragment.Dom_t] )
      .map { GContainerFragment.apply }
  }

}


/** Дефолтовая реализация экземпляра модели [[GBlockT]]. */
case class GBlock(override val _underlying: HTMLDivElement)
  extends GBlockT with PrevNextSiblingCousinUtilT {

  override type Self_t = GBlock
  override type Parent_t = GContainerFragment
  override protected def _parent = parentFragment
  override protected def _companion = GBlock

  /** Предыдущий блок. Расширенная версия previous() с переходом между fragment'ами. */
  override def previous: Option[Self_t] = {
    super.previous orElse {
      __prevNextCousinHelper(_.previous)(_.lastBlock)
    }
  }
  /** Следующий блок. Расширенная версия next() с переходом между fragment'ами. */
  override def next: Option[GBlock] = {
    super.next orElse {
      __prevNextCousinHelper(_.next)(_.firstBlock)
    }
  }
}
