package models.im

import java.text.DecimalFormat

import io.suggest.common.empty.OptionUtil
import io.suggest.common.geom.d2.ISize2di
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.xplay.qsb.QueryStringBindableImpl
import org.im4java.core.IMOperation
import util.FormUtil

import scala.util.parsing.combinator.JavaTokenParsers

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.14 15:49
 * Description: Код для работы с трансформациями картинок ImageMagic в рамках query-string.
 * Что-то подобное было в зотонике, где список qs биндился в список im-действий над картинкой.
 */

object ImOp extends MacroLogsImpl with JavaTokenParsers {

  val SPLIT_ON_BRACKETS_RE = "[\\[\\]]+".r

  /** qsb для биндинга списка трансформаций картинки (последовательности ImOp). */
  implicit def imOpsSeqQsb = new ImOpsQsb

  def twoFracZeroesFormat: DecimalFormat = {
    val df = new DecimalFormat("0.00")
    qsDoubleFormat(df)
  }

  def optFracZeroesFormat: DecimalFormat = {
    val df = new DecimalFormat("0.##")
    qsDoubleFormat(df)
  }

  private def qsDoubleFormat(df: DecimalFormat): DecimalFormat = {
    val dcs = df.getDecimalFormatSymbols
    dcs.setDecimalSeparator('.')
    dcs.setMinusSign('-')
    df.setDecimalFormatSymbols(dcs)
    df
  }


  /** Дефолтовый StringBuilder для unbind-методов. */
  def unbindSbDflt = new StringBuilder(64)

  /**
   * Для разных вариантов unbind'а используется этот метод
   * @param keyDotted Префикс ключа.
   * @param value Значения (im-операции).
   * @param withOrderInx Добавлять ли индексы операций? true если нужен будет вызов bind() над результатом.
   * @param sb Используемый для работы StrinbBuilder. По умолчанию - создавать новый.
   * @return Строка im-операций.
   */
  def unbindImOps(keyDotted: String, value: Seq[ImOp], withOrderInx: Boolean, sb: StringBuilder = unbindSbDflt): String = {
    unbindImOpsSb(keyDotted, value, withOrderInx, sb)
      .toString()
  }
  def unbindImOpsSb(keyDotted: String, value: Seq[ImOp], withOrderInx: Boolean, sb: StringBuilder = unbindSbDflt): StringBuilder = {
    value
      // Порядковый номер нужен для восстановления порядка вызовов при bind'е.
      .iterator
      .zipWithIndex
      .foreach { case (imOp, i) =>
        sb.append(keyDotted)
          .append(imOp.opCode.value)
        if (withOrderInx) {
          sb.append('[').append(i).append(']')
        }
        sb
          .append('=')
          .append(imOp.qsValue)
          .append('&')
      }
    // Убрать последний '&'
    if (value.nonEmpty)
      sb.setLength(sb.length - 1)
    sb
  }

  /** Забиндить распарсенное по итератору. */
  def bindImOps(keyDotted: String, rawOps: Iterator[(String, Seq[String])]): Iterator[ImOp] = {
    rawOps
      .flatMap { case (k, vs) =>
        val opCodeStr = k.substring(keyDotted.length)
        ImOpCodes.withValueOpt(opCodeStr)
          .map { _ -> vs }
      }
      // Попытаться сгенерить результат
      .map {
        case (opCode, vs)  =>  opCode.mkOp(vs)
      }
  }

  /** Забиндить исходную qs-строку, предварительно протокенизировав. */
  def bindImOps(keyDotted: String, raw: String): Iterator[ImOp] = {
    val iter0 = FormUtil.parseToPairs(raw)
      .map { case (k, v)  =>  k -> List(v) }
    bindImOps(keyDotted, iter0)
  }


  /** Попытаться узнать точный размер результирующей картинки на основе im-операций кропа/ресайза.
    *
    * @param ops Операции в обычном порядке.
    * @return Опциональные размеры картинки.
    *         Some(Some()) достоверные размеры картинки.
    *         Some(None) достоверные размеры отсутсвуют и ОТЛИЧАЮТСЯ от оригинала
    *         None - нет инфы по размерами, но они совпадает с оригиналом.
    */
  def getWhFromOps(ops: Seq[ImOp]): Option[Option[ISize2di]] = {
    OptionUtil.maybeOpt(ops.nonEmpty) {
      getWhFromOpsRev( ops.reverseIterator )
    }
  }
  /** Попытаться узнать точный размер результирующей картинки на основе im-операций в ОБРАТНОМ ПОРЯДКЕ.
    *
    * @param opsRev Im-операции в ОБРАТНОМ порядке.
    * @return Опционально: найденный размер выхлопа.
    */
  def getWhFromOpsRev(opsRev: IterableOnce[ImOp]): Option[Option[ISize2di]] = {
    opsRev
      .iterator
      // Надо найти операцию, затрагивающую фактический размер, и попытаться извлечь из неё wh.
      .flatMap {
        case op: AbsCropOp =>
          Some(op.crop) :: Nil
        case op: AbsResizeOp =>
          // Ресайз можно использовать как размер, только если указанный размер точно соответствует результату.
          val isResizeStrict = op.flags.contains( ImResizeFlags.FillArea ) || op.flags.contains( ImResizeFlags.IgnoreAspectRatio )
          OptionUtil.maybe(isResizeStrict)(op.sz) :: Nil
        case op: ExtentOp =>
          Some(op.wh) :: Nil
        case _ =>
          Nil
      }
      .buffered
      // Интересует только первая с конца возможная операция, задающая картинке достоверный размер:
      .headOption
  }

}


import ImOp._


/** qsb-биндер. */
class ImOpsQsb extends QueryStringBindableImpl[Seq[ImOp]] {

  import LOGGER._

  /**
   * Забиндить список операций, ранее сериализованный в qs.
   * @param keyDotted Ключ-префикс вместе с точкой. Может быть и пустой строкой.
   * @param params ListMap с параметрами.
   */
  override def bind(keyDotted: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[ImOp]]] = {
    val splitOnBracketsRe = SPLIT_ON_BRACKETS_RE
    try {
      val ops0 = params
        .iterator
        // в карте params содержит также всякий посторонний мусор. Он нам неинтересен.
        .filter { _._1 startsWith keyDotted }
        // Извлечь порядковый номер из ключа.
        .flatMap { case (k, v) =>
          splitOnBracketsRe.split(k) match {
            case Array(k2, iStr) =>
              val i = iStr.toInt
              ((k2, v), i) :: Nil
            case _ =>
              Nil
          }
        }
        // Восстановить исходный порядок.
        .toSeq
        .sortBy(_._2)
        // Распарсить команды.
        .iterator
        .map(_._1)
      val ops = ImOp.bindImOps(keyDotted, ops0)
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
    unbindImOps(keyDotted, value, withOrderInx = true)
  }

}


/** Действие, запрограммирование в аргументах. */
trait ImOp {
  def opCode: ImOpCode
  def addOperation(op: IMOperation): Unit
  def qsValue: String
  def i18nValueCode: Option[String] = None
  def unwrappedValue: Option[String] = None
}



/** quality для результата. */
case class QualityOp(quality: Int) extends ImOp {
  override def opCode = ImOpCodes.Quality
  override def addOperation(op: IMOperation): Unit = {
    op.quality(quality.toDouble)
  }
  override def qsValue: String = {
    optFracZeroesFormat
      .format(quality)
  }
}


/** strip для урезания картинки. */
case object StripOp extends ImOp {
  override def opCode = ImOpCodes.Strip
  override def qsValue = ""
  override def addOperation(op: IMOperation): Unit = {
    op.strip()
  }
}


/** Размывка по гауссу. */
case class GaussBlurOp(radiusSigma: Int) extends ImOp {
  override def opCode = ImOpCodes.GaussBlur
  override def addOperation(op: IMOperation): Unit = {
    val d = radiusSigma.toDouble: java.lang.Double
    op.gaussianBlur(d, d)
  }
  override def qsValue: String = {
    optFracZeroesFormat
      .format(radiusSigma)
  }
}

