package util.blocks

import io.suggest.ad.blk.BlockHeight
import models.blk.ed.{AdFormM, BindAcc, BindResult}
import play.api.data.{FormError, Mapping}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:04
 * Description: Утиль для сборки блоков с heightBf-полем.
 * Жесткий велосипед с height необходим из-за серьезных проблем с расширенияеми маппингов через трейты.
 * Бывают родственные блоки, которые могут иметь или не иметь поля высоты. В таких случаях поле
 * Высоты не рендерится в одном из блоков, но всегда доступно для маппинга.
 */

object Height extends MergeBindAcc[BlockHeight] {
  val BF_HEIGHT_NAME_DFLT = "height"
  val BF_HEIGHT_DFLT = BfHeight(BF_HEIGHT_NAME_DFLT)

  def updateAcc(offerN: Int, acc0: BindAcc, height: BlockHeight): BindAcc = {
    if (acc0.height != height) {
      acc0.copy(
        height = height
      )
    } else {
      acc0
    }
  }

}


import Height._


/** Базовый интерфейс для поля heightBf. */
trait HeightI {
  def heightBf: BfHeight
}


/** Если нужно добавление поля в blockFields (т.е. в форму редактора), то нужен этот трейт. */
trait Height extends ValT with HeightI {
  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = heightBf :: super.blockFieldsRev(af)

  override def heightBf = Height.BF_HEIGHT_DFLT

  // Mapping
  private def m = heightBf.getStrictMapping.withPrefix(heightBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeHeight = m.bind(data)
    mergeBindAcc(maybeAcc0, maybeHeight)
  }

  abstract override def unbind(value: BindResult): Map[String, String] = {
    val v = m.unbind( value.blockMeta.h )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BindResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.blockMeta.h
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}
