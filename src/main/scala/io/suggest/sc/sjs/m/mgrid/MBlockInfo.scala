package io.suggest.sc.sjs.m.mgrid

import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 17:45
 * Description: Рантаймовая инфа по блоку плитки представлена этой моделью.
 * Модель блоков плитки нужна для быстрого манипулирования этой плиткой, без гуляний по дереву и парсинга аттрибутов.
 */
case class MBlockInfo(
  id      : String,
  width   : Int,
  height  : Int,
  block   : HTMLDivElement
) {

  override def toString: String = {
    "B#" + id + "(" + width + "x" + height + ")"
  }

}
