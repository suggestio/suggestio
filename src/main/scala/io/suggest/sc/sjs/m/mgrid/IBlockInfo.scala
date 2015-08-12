package io.suggest.sc.sjs.m.mgrid

import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 17:45
 * Description: Рантаймовая инфа по блоку плитки представлена этой моделью.
 * Модель блоков плитки нужна для быстрого манипулирования этой плиткой, без гуляний по дереву и парсинга аттрибутов.
 */
trait IBlockInfo {
  // Этот интерфейс появился для сглаживания перелома архитектуры с v1 на FSM-MVM.
  // По сути он делает MBlockInfo и GBlock полиморфными, что так нужно билдеру сетки.

  def id      : String
  def width   : Int
  def height  : Int
  def div     : HTMLDivElement

}
