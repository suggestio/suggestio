package models.msc

import io.suggest.sc.grid.GridConstants._

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.05.15 16:06
 * Description: Модель параметров сетки (cbca_grid). Изначально была модель НОВЫХ параметров сетки.
 */

object MGridParams {

  /** Система сериализации экземпляром [[MGridParams]] в JSON. */
  // TODO Сделать как val, когда оно будет достаточно часто использоваться.
  implicit def writes: Writes[MGridParams] = (
    (__ \ CELL_SIZE_CSSPX_FN).write[Int] and
    (__ \ CELL_PADDING_CSSPX_FN).write[Int]
  )(unlift(unapply))

}


/**
 * Экземпляр модели.
 * @param cellSizeCssPx Размер ячейки в css-пикселях.
 * @param cellPaddingCssPx Расстояние между ячейками в css-пикселях.
 */
case class MGridParams(
  cellSizeCssPx    : Int,
  cellPaddingCssPx : Int
)
