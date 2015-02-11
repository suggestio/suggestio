package util.blocks

import models.AdFormM
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.10.14 15:11
 * Description: Добавление поддержки поля с галочкой включения/выключения isWIde-отображения карточки.
 */
object IsWideBg extends MergeBindAcc[Boolean] {

  def IS_WIDE_NAME_DFLT = "isWide"
  def IS_WIDE_BF_DFLT = BfCheckbox(IS_WIDE_NAME_DFLT)

  override def updateAcc(offerN: Int, acc0: BindAcc, v: Boolean): Unit = {
    acc0.isWide = v
  }

}


import IsWideBg._


trait IsWideBgI {
  def isWideBf: BfCheckbox
}

trait IsWideBg extends ValT with IsWideBgI {

  override def isWideBf: BfCheckbox = IsWideBg.IS_WIDE_BF_DFLT

  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = isWideBf :: super.blockFieldsRev(af)

  // Mapping
  private def m = isWideBf.getStrictMapping.withPrefix(isWideBf.name).withPrefix(key)


  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeIsWide = m.bind(data)
    mergeBindAcc(maybeAcc0, maybeIsWide)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( value.bd.blockMeta.wide )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = value.bd.blockMeta.wide
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }

}
