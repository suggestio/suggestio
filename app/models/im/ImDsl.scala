package models.im

import java.text.DecimalFormat

import io.suggest.ym.model.common.MImgSizeT
import org.im4java.core.IMOperation
import models._
import play.api.mvc.QueryStringBindable
import util.PlayMacroLogsImpl
import util.qsb.QSBs

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 15:49
 * Description: Код для работы с трансформациями картинок ImageMagic в рамках query-string.
 * Что-то подобное было в зотонике, где список qs биндился в список im-действий над картинкой.
 */

object ImOp extends PlayMacroLogsImpl {

  import LOGGER._

  /** qsb для биндинга списка трансформаций картинки (последовательности ImOp). */
  implicit def qsbSeq = {
    new QueryStringBindable[Seq[ImOp]] {

      /**
       * Забиндить список операций, ранее сериализованный в qs.
       * @param keyDotted Ключ-префикс вместе с точкой. Может быть и пустой строкой.
       * @param params ListMap с параметрами.
       * @return
       */
      override def bind(keyDotted: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[ImOp]]] = {
        // в карте params может содержать посторонний мусор. Но она идёт в прямом порядке.
        try {
          val ops = params
            .toSeq
            .reverse
            .iterator
            .filter { _._1 startsWith keyDotted }
            // Попытаться распарсить код операции.
            .flatMap { case (k, vs) =>
              val opCodeStr = k.substring(keyDotted.length)
              ImOpCodes.maybeWithName(opCodeStr)
                .map { _ -> vs }
            }
            // Попытаться сгенерить результат
            .map { case (opCode, vs)  =>  opCode.mkOp(vs) }
            .toSeq
          if (ops.isEmpty) {
            None
          } else {
            Some(Right(ops))
          }
        } catch {
          case ex: Throwable =>
            // Should not happen, if unbind() written ok:
            error(s"Failed to parse IM Ops\n  keyDotted=$keyDotted:\n  params = ${params.mkString(", ")}", ex)
            Some(Left("Failed to parse query string."))
        }
      }

      override def unbind(keyDotted: String, value: Seq[ImOp]): String = {
        val sb = new StringBuilder(value.size * 14)
        value.foreach { imOp =>
          sb.append(keyDotted)
            .append(imOp.opCode.strId)
            .append('=')
            .append(imOp.qsValue)
            .append('&')
        }
        // Убрать последний '&'
        if (value.nonEmpty)
          sb.setLength(sb.length - 1)
        sb.toString()
      }
    }
  }

}

/** Действие, запрограммирование в аргументах. */
trait ImOp {
  def opCode: ImOpCode
  def addOperation(op: IMOperation)
  def qsValue: String
}


object ImOpCodes extends Enumeration {
  abstract protected class Val(val strId: String) extends super.Val(strId) {
    def mkOp(vs: Seq[String]): ImOp
  }

  type ImOpCode = Val

  val Crop: ImOpCode = new Val("a") {
    override def mkOp(vs: Seq[String]) = {
      CropOp(ImgCrop(vs.head))
    }
  }
  val Gravity: ImOpCode = new Val("b") {
    override def mkOp(vs: Seq[String]) = {
      GravityOp( ImGravities.withName(vs.head) )
    }
  }
  val AbsResize: ImOpCode = new Val("c") {
    override def mkOp(vs: Seq[String]): ImOp = {
      AbsResizeOp(vs.head)
    }
  }
  val Interlace: ImOpCode = new Val("d") {
    override def mkOp(vs: Seq[String]) = {
      InterlacingOp( ImInterlace.withName(vs.head) )
    }
  }
  val GaussBlur: ImOpCode = new Val("e") {
    override def mkOp(vs: Seq[String]) = {
      GaussBlurOp(vs.head.toDouble)
    }
  }
  val Quality: ImOpCode = new Val("f") {
    override def mkOp(vs: Seq[String]): ImOp = {
      QualityOp(vs.head.toDouble)
    }
  }

  val Extent: ImOpCode = new Val("g") {
    override def mkOp(vs: Seq[String]): ImOp = {
      ExtentOp(vs.head)
    }
  }

  implicit def value2val(x: Value): ImOpCode = x.asInstanceOf[ImOpCode]

  def maybeWithName(n: String): Option[ImOpCode] = {
    values
      .find(_.strId == n)
      .asInstanceOf[Option[ImOpCode]]
  }
}


/**
 * Операция кропа изображения.
 * @param crop инфа о кропе.
 */
case class CropOp(crop: ImgCrop) extends ImOp {
  override def opCode = ImOpCodes.Crop

  override def addOperation(op: IMOperation): Unit = {
    op.crop(crop.w, crop.h, crop.offX, crop.offY)
  }

  override def qsValue: String = crop.toUrlSafeStr
}


object ImGravities extends Enumeration {
  protected case class Val(strId: String, imName: String) extends super.Val(strId)

  type ImGravity = Val

  // Некоторые значения помечены как lazy, т.к. не используются по факту.
  val Center: ImGravity         = Val("c", "Center")
  lazy val North: ImGravity     = Val("n", "North")
  lazy val South: ImGravity     = Val("s", "South")
  lazy val West: ImGravity      = Val("w", "West")
  lazy val East: ImGravity      = Val("e", "East")

  // TODO Добавить ещё?

  implicit def value2val(x: Value): ImGravity = x.asInstanceOf[ImGravity]

}

/** Добавить -gravity South. */
case class GravityOp(gravity: ImGravity) extends ImOp {
  override def opCode = ImOpCodes.Gravity
  override def addOperation(op: IMOperation): Unit = {
    op.gravity(gravity.imName)
  }

  override def qsValue: String = gravity.strId
}



/** quality для результата. */
case class QualityOp(quality: Double) extends ImOp {
  override def opCode = ImOpCodes.Quality
  override def addOperation(op: IMOperation): Unit = {
    op.quality(quality)
  }
  override def qsValue: String = {
    val df = new DecimalFormat("0.00")
    df.format(quality).replace(",", ".")
  }
}


object ImInterlace extends Enumeration {
  protected case class Val(strId: String, imName: String) extends super.Val(strId)

  type ImInterlacing = Val

  val Plane: ImInterlacing            = Val("a", "Plane")
  lazy val None: ImInterlacing        = Val("0", "None")
  lazy val Line: ImInterlacing        = Val("l", "Line")
  val Jpeg: ImInterlacing             = Val("j", "JPEG")
  lazy val Gif: ImInterlacing         = Val("g", "GIF")
  lazy val Png: ImInterlacing         = Val("p", "PNG")
  lazy val Partition: ImInterlacing   = Val("r", "Partition")

  implicit def value2val(x: Value): ImInterlacing = x.asInstanceOf[ImInterlacing]
}

/** Черезсточность. */
case class InterlacingOp(interlacing: ImInterlacing) extends ImOp {
  override def opCode = ImOpCodes.Interlace
  override def addOperation(op: IMOperation): Unit = {
    op.interlace(interlacing.imName)
  }
  override def qsValue: String = interlacing.strId
}


/** Размывка по гауссу. */
case class GaussBlurOp(blur: Double) extends ImOp {
  override def opCode = ImOpCodes.GaussBlur
  override def addOperation(op: IMOperation): Unit = {
    op.gaussianBlur(blur)
  }
  override def qsValue: String = {
    val df = new DecimalFormat("0.00")
    df.format(blur)
  }
}

