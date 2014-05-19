package util.blocks

import models._
import util.blocks.BlocksEditorFields.BefText
import play.api.data.{FormError, Mapping}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:16
 * Description: Утиль для блоков, содержащих поле descrBf.
 */

object Descr {
  val BF_NAME_DFLT = "descr"
  val LEN_MAX_DFLT = 160
  val DEFAULT_VALUE_DFLT = Some(AOStringField("Только сегодня", AOFieldFont("444444")))
  val BF_DESCR_DFLT = BfText(
    name = BF_NAME_DFLT,
    field = BlocksEditorFields.TextArea,
    maxLen = LEN_MAX_DFLT,
    defaultValue = DEFAULT_VALUE_DFLT
  )

  def mergeBindAccWithDescr(maybeAcc: Either[Seq[FormError], BindAcc],
                            offerN: Int,
                            maybeDescr: Either[Seq[FormError], Option[AOStringField]]):  Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeDescr) match {
      case (Right(acc0), Right(descrOpt)) =>
        if (descrOpt.isDefined) {
          acc0.offers.find { _.n == offerN } match {
            case Some(blk) =>
              blk.text2 = descrOpt
            case None =>
              val blk = AOBlock(n = offerN, text2 = descrOpt)
              acc0.offers ::= blk
          }
        }
        maybeAcc

      case (Left(accFE), Right(descr)) =>
        maybeAcc

      case (Right(_), Left(colorFE)) =>
        Left(colorFE)   // Избыточна пересборка left either из-за right-типа. Можно также вернуть через .asInstanceOf, но это плохо.

      case (Left(accFE), Left(colorFE)) =>
        Left(accFE ++ colorFE)
    }
  }

  def getDescr(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.text2)
}


import Descr._


/** Базовый трейт для descrBf-трейтов, как статических так и динамических. */
trait DescrT extends ValT {
  def descrBf: BfText
  abstract override def blockFieldsRev: List[BlockFieldT] = descrBf :: super.blockFieldsRev

  // Mapping
  private def m = descrBf.getOptionalStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDescr = m.bind(data)
    mergeBindAccWithDescr(maybeAcc0, offerN = 0, maybeDescr)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( getDescr(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getDescr(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


/** Статическая реализация descrBf. Экономит оперативку, когда дефолтовые значения полностью
  * устраивают. */
trait DescrStatic extends DescrT {
  override final def descrBf = Descr.BF_DESCR_DFLT
}


/** Динамическая реализация descrBf. Генерит персональный инстанс descrBf для блока.
  * Параметры сборки поля можно переопределить через соответствующие методы. */
trait Descr extends DescrT {
  import Descr._

  def descrMaxLen: Int = LEN_MAX_DFLT
  def descrDefaultValue: Option[AOStringField] = DEFAULT_VALUE_DFLT
  def descrEditorField: BefText = BlocksEditorFields.TextArea
  def descrFontSizes: Set[Int] = Set.empty

  override def descrBf = BfText(
    name = Descr.BF_NAME_DFLT,
    field = descrEditorField,
    maxLen = descrMaxLen,
    defaultValue = descrDefaultValue
  )
}

