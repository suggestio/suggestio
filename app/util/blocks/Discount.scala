package util.blocks

import BlocksUtil._
import models._
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:27
 * Description: Утиль для сборки блоков, содержащих поле discountBf.
 */

object Discount {
  val BF_NAME_DFLT = "discount"
  val DISCOUNT_BF_DFLT = BfDiscount(BF_NAME_DFLT)

  def mergeBindAccWithDiscount(maybeAcc: Either[Seq[FormError], BindAcc],
                            offerN: Int,
                            maybeDiscount: Either[Seq[FormError], Option[AOFloatField]]):  Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeDiscount) match {
      case (Right(acc0), Right(discountOpt)) =>
        if (discountOpt.isDefined) {
          acc0.offers.find { _.n == offerN } match {
            case Some(blk) =>
              blk.discount = discountOpt
            case None =>
              val blk = AOBlock(n = offerN, discount = discountOpt)
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

  def getDiscount(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.discount)
}


import Discount._


/** Базовый интерфейсный трейт для реализаций поля discountBf. */
trait Discount extends ValT {
  def discountBf: BfDiscount = DISCOUNT_BF_DFLT
  abstract override def blockFieldsRev: List[BlockFieldT] = discountBf :: super.blockFieldsRev

  // Mapping
  private def m = discountBf.getOptionalStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDisco = m.bind(data)
    mergeBindAccWithDiscount(maybeAcc0, offerN = 0, maybeDisco)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( getDiscount(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getDiscount(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

