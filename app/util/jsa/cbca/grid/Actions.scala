package util.jsa.cbca.grid

import util.jsa.JsAction

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.11.14 14:54
 * Description: js экшены, влияющие на состояние cbca_grid.
 */

/** Выставить новый размер ячейки. */
case class SetCellSize(cellSizePx: Int) extends JsAction {
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    sb.append("cbca_grid.cell_size=").append(cellSizePx).append(';')
  }
}

/** Выставить новый интервал между ячейками. */
case class SetCellPadding(paddingPx: Int) extends JsAction {
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    sb.append("cbca_grid.cell_padding=").append(paddingPx).append(';')
  }
}
