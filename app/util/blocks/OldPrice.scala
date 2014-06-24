package util.blocks

import models._
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:23
 * Description: Утиль для сборки блоков, содержащих поле старой цены oldPriceBf.
 */

object OldPrice extends MergeBindAccAOBlock[AOPriceField] {
  val BF_NAME_DFLT = "oldPrice"
  val BF_OLD_PRICE_DFLT = BfPrice(BF_NAME_DFLT)


  /** Обновить указанный изменяемый AOBlock с помощью текущего значения. */
  override def updateAOBlockWith(blk: AOBlock, oldPriceOpt: Option[AOPriceField]) {
    blk.oldPrice = oldPriceOpt
  }

  def getOldPrice(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.oldPrice)
}


import OldPrice._


/** Базовый интерфейсный трейт для реализаций oldPriceBf.
  * Добавляет поле в список полей формы редактора. */
trait OldPrice extends ValT {
  def oldPriceBf: BfPrice = BF_OLD_PRICE_DFLT
  abstract override def blockFieldsRev: List[BlockFieldT] = oldPriceBf :: super.blockFieldsRev

  // Mapping
  private def m = oldPriceBf.getOptionalStrictMapping.withPrefix(oldPriceBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeOldPriceOpt = m.bind(data)
    mergeBindAcc(maybeAcc0, maybeOldPriceOpt)
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

