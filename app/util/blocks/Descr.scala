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

object Descr extends MergeBindAcc[AOStringField] {
  val BF_NAME_DFLT = "descr"
  val BF_DESCR_DFLT = BfText(BF_NAME_DFLT)

  override def updateAOBlockWith(blk: AOBlock, descrOpt: Option[AOStringField]) {
    blk.text2 = descrOpt
  }

  def getDescr(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.text2)
}


import Descr._


/** Базовый трейт для descrBf-трейтов, как статических так и динамических. */
trait Descr extends ValT {
  def descrBf: BfText = BF_DESCR_DFLT
  abstract override def blockFieldsRev: List[BlockFieldT] = descrBf :: super.blockFieldsRev

  // Mapping
  private def m = descrBf.getOptionalStrictMapping.withPrefix(descrBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDescr = m.bind(data)
    mergeBindAcc(maybeAcc0, offerN = 0, maybeDescr)
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


