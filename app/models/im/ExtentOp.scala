package models.im

import io.suggest.ym.model.common.MImgSizeT
import org.im4java.core.IMOperation
import util.qsb.QSBs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 14:38
 * Description:
 */

object ExtentOp {

  def apply(raw: String): ExtentOp = {
    val imeta = QSBs.parseWxH(raw).get
    ExtentOp(imeta)
  }

}

case class ExtentOp(wh: MImgSizeT) extends ImOp {
  override def opCode = ImOpCodes.Extent

  override def qsValue: String = {
    QSBs.unParseWxH(wh)
  }

  override def addOperation(op: IMOperation): Unit = {
    op.extent(wh.width, wh.height)
  }
}
