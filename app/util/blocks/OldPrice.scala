package util.blocks

import models._
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:23
 * Description: Утиль для сборки блоков, содержащих поле старой цены oldPriceBf.
 */

object OldPrice {
  val BF_NAME_DFLT = "oldPrice"
  val BF_OLD_PRICE_DFLT = BfPrice(BF_NAME_DFLT)

  def mergeBindAccWithOldPrice(maybeAcc: Either[Seq[FormError], BindAcc],
                            offerN: Int,
                            maybeOldPrice: Either[Seq[FormError], Option[AOPriceField]]):  Either[Seq[FormError], BindAcc] = {
    (maybeAcc, maybeOldPrice) match {
      case (Right(acc0), Right(oldPriceOpt)) =>
        if (oldPriceOpt.isDefined) {
          acc0.offers.find { _.n == offerN } match {
            case Some(blk) =>
              blk.oldPrice = oldPriceOpt
            case None =>
              val blk = AOBlock(n = offerN, oldPrice = oldPriceOpt)
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

  def getOldPrice(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.oldPrice)
}


import OldPrice._


/** Базовый интерфейсный трейт для реализаций oldPriceBf.
  * Добавляет поле в список полей формы редактора. */
trait OldPriceT extends ValT {
  def oldPriceBf: BfPrice
  abstract override def blockFieldsRev: List[BlockFieldT] = oldPriceBf :: super.blockFieldsRev

  // Mapping
  private def m = oldPriceBf.getOptionalStrictMapping.withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDescr = m.bind(data)
    mergeBindAccWithOldPrice(maybeAcc0, offerN = 0, maybeDescr)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( getOldPrice(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getOldPrice(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}


/** Статическая реализация поля. Используется общий статический инстанс поля oldPriceBf. */
trait OldPriceStatic extends OldPriceT {
  override def oldPriceBf = OldPrice.BF_OLD_PRICE_DFLT
}


/** Динамическая реализация oldPriceBf. */
trait OldPrice extends OldPriceT {
  def oldPriceDefaultValue: Option[AOPriceField] = None
  def oldPriceFontSizes: Set[Int] = Set.empty
  def oldPriceWithCoords: Boolean = false
  override def oldPriceBf = BfPrice(
    name = OldPrice.BF_NAME_DFLT,
    defaultValue = oldPriceDefaultValue,
    withFontSizes = oldPriceFontSizes,
    withCoords = oldPriceWithCoords
  )
}

