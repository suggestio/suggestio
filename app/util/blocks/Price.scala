package util.blocks

import models._
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:20
 * Description: Утиль для поля priceBf, содержащего актуальную цену товара/услуги в блоке.
 */

object Price extends MergeBindAccAOBlock[AOPriceField] {
  val BF_NAME_DFLT = "price"
  val BF_PRICE_DFLT = BfPrice(BF_NAME_DFLT)

  override def updateAOBlockWith(blk: AOBlock, priceOpt: Option[AOPriceField]) {
    blk.price = priceOpt
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
    val maybePrice = m.bind(data)
    mergeBindAcc(maybeAcc0, maybePrice)
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

