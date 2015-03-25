package io.suggest.adv.ext.model.im

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:58
 * Description: Размер двумерный целочисленный.
 */
trait ISize2di {
  def width: Int
  def height: Int

  def sizeWhEquals(sz1: ISize2di): Boolean = {
    sz1.width == width  &&  sz1.height == height
  }
}
