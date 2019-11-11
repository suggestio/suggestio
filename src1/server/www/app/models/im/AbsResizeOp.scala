package models.im

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import org.im4java.core.IMOperation
import japgolly.univeq._

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.10.14 14:16
 * Description: поддержка resize WxH[flag], например "1024x768c"
 */

object AbsResizeOp extends JavaTokenParsers {
  
  val numRe = "\\d{1,4}".r
  val flagRe = {
    val flags = ImResizeFlags.values
    val sb = new StringBuilder(flags.size + 6, "(?i)[")
    flags.foreach { flag =>
      sb append flag.urlSafeChar
    }
    sb.append("]")
      .toString()
      .r
  }
  val whDelimRe = "[xX]".r

  def flagsP = rep(flagRe ^^ ImResizeFlags.maybeWithName) ^^ {
    _.flatten
  }

  def sizeP: Parser[ISize2di] = {
    val numP = numRe ^^ { _.toInt }
    (numP ~ (whDelimRe ~> numP)) ^^ {
      case width ~ height  =>  MSize2di(width = width, height = height)
    }
  }

  def aroParser: Parser[AbsResizeOp] = {
    (sizeP ~ flagsP) ^^ {
      case sz ~ flags  =>  AbsResizeOp(sz, flags = flags)
    }
  }

  def apply(raw: String): AbsResizeOp = {
    parse(aroParser, raw).get
  }

  def apply(sz: ISize2di, flag: ImResizeFlag): AbsResizeOp = {
    AbsResizeOp(sz, Seq(flag))
  }

  /** im4java может принимать null-значения в качестве ширины/длины. Это означает опущенное значение, которое
    * im должна додумать автоматом на основе остальных данных. */
  private def sizeIntg(sz: Int): Integer = {
    if (sz > 0) sz else null
  }

}


case class AbsResizeOp(sz: ISize2di, flags: Seq[ImResizeFlag] = Nil) extends ImOp {

  override def opCode = ImOpCodes.AbsResize

  def flagsStr = flags.iterator.map(_.imChar).mkString("")

  override def addOperation(op: IMOperation): Unit = {
    // 2014.oct.21: Поддержка автоматически-выставляемых размеров ресайза. Типа "300x" или "x200".
    val w: Integer = AbsResizeOp.sizeIntg(sz.width)
    val h: Integer = AbsResizeOp.sizeIntg(sz.height)
    if (flags.isEmpty) {
      op.resize(w, h)
    } else if (flags.lengthCompare(1) ==* 0) {
      op.resize(w, h, flags.head.imChar)
    } else {
      op.resize(w, h, flagsStr)
    }
  }

  override def qsValue: String = {
    val sb = new StringBuilder(16)
    sb.append(sz.width)
      .append('x')
      .append(sz.height)
    flags.foreach { f =>
      sb append f.urlSafeChar
    }
    sb.toString()
  }

  override def unwrappedValue: Option[String] = {
    val sb = new StringBuilder(16, sz.toString)
    flags.foreach { flag =>
      sb append flag.imChar
    }
    Some( sb.toString() )
  }
}

