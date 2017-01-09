package io.suggest.maps.vm.img

import io.suggest.common.geom.coord.ICoords2di
import io.suggest.common.geom.d2.{ISize2di, Size2di}
import io.suggest.common.maps.MapFormConstants._
import io.suggest.sjs.common.geom.Coords2di
import io.suggest.sjs.common.vm.attr.{AttrVmT, GetImgSrc}
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLImageElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.11.15 22:06
 * Description: common-код для leaflet marker icon vm'ок.
 * Дедубликация кода моделей этого пакета.
 */
trait IconVmStaticT extends FindElT {
  override type Dom_t = HTMLImageElement
  override type T <: IconVmT
}

trait IconVmT
  extends AttrVmT
  with GetImgSrc
{

  override type T = HTMLImageElement

  private def _pair2v[T](a1: String, a2: String)(f: (Int, Int) => T): Option[T] = {
    for {
      a <- getIntAttribute(a1)
      b <- getIntAttribute(a2)
    } yield {
      f(a, b)
    }
  }

  /** Получить ширину и длину из аттрибутов. */
  def wh: Option[ISize2di] = {
    _pair2v(ATTR_IMG_WIDTH, ATTR_IMG_HEIGHT)(Size2di.apply)
  }

  /** Получить какую-то координату из аттрибутов. */
  def xy: Option[ICoords2di] = {
    _pair2v(ATTR_IMG_ANCHOR_X, ATTR_IMG_ANCHOR_Y)(Coords2di.apply)
  }

}
