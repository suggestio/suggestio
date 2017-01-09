package util.blocks

import models.blk.ed.{AdFormM, BindResult, BindAcc}
import play.api.data.FormError
import play.api.data.Mapping

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.10.14 16:34
 * Description:
 */
object Width extends MergeBindAcc[Int] {

  val BF_WIDTH_NAME_DFLT = "width"
  val BF_WIDTH_DFLT = BfWidth(BF_WIDTH_NAME_DFLT)

  /** Как-то обновить акк. */
  override def updateAcc(offerN: Int, acc0: BindAcc, v: Int): BindAcc = {
    if (acc0.width != v) {
      acc0.copy(
        width = v
      )
    } else {
      acc0
    }
  }

}


import Width._


/** Базовый интерфейс для поля heightBf. */
trait WidthI {
  def widthBf: BfWidth
}


trait Width extends ValT with WidthI {

  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = {
    widthBf :: super.blockFieldsRev(af)
  }

  override def widthBf = Width.BF_WIDTH_DFLT

  private def m = widthBf.getStrictMapping.withPrefix(widthBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeWidth = m.bind(data)
    mergeBindAcc(maybeAcc0, maybeWidth)
  }

  abstract override def unbind(value: BindResult): Map[String, String] = {
    val v = m.unbind( value.blockMeta.width )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BindResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.blockMeta.width
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }

}
