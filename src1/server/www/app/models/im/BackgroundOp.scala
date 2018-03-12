package models.im
import org.im4java.core.IMOperation

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.03.18 22:02
  * Description: Поддержка ключа -background Color в ImageMagick.
  */
object BackgroundOp {

  def apply( vs: Seq[String] ): BackgroundOp = {
    BackgroundOp(
      colorOpt = vs.headOption.filter(_.nonEmpty)
    )
  }

}


case class BackgroundOp( colorOpt: Option[String] ) extends ImOp {

  override def opCode: ImOpCode = ImOpCodes.Background

  override def addOperation(op: IMOperation): Unit = {
    op.background( colorOpt.getOrElse("none") )
  }

  override def qsValue: String = {
    colorOpt.getOrElse("")
  }

}
