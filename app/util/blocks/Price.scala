package util.blocks

import models._
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:20
 * Description: Утиль для поля priceBf, содержащего актуальную цену товара/услуги в блоке.
 */

object Price {
  val BF_NAME_DFLT = "price"
  val BF_PRICE_DFLT = BfPrice(BF_NAME_DFLT)
  
  def mergeBindAccWithPrice(maybeAcc: Either[Seq[FormError], BindAcc],
                            offerN: Int,
                            maybePrice: Either[Seq[FormError], Option[AOPriceField]]):  Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybePrice) match {
      case (Right(acc0), Right(priceOpt)) =>
        if (priceOpt.isDefined) {
          acc0.offers.find { _.n == offerN } match {
            case Some(blk) =>
              blk.price = priceOpt
            case None =>
              val blk = AOBlock(n = offerN, price = priceOpt)
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

  def getPrice(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.price)
}


import Price._


/** Базовый трейт для трейтов-реализаций bfPrice. Добавляет поле в форму редактора. */
trait Price extends ValT {
  def priceBf: BfPrice = BF_PRICE_DFLT
  abstract override def blockFieldsRev: List[BlockFieldT] = priceBf :: super.blockFieldsRev

  // Mapping
  private def m = priceBf.getOptionalStrictMapping.withPrefix(priceBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDescr = m.bind(data)
    mergeBindAccWithPrice(maybeAcc0, offerN = 0, maybeDescr)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( getPrice(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getPrice(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

