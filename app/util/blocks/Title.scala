package util.blocks

import models._
import util.blocks.BlocksEditorFields.BefText
import play.api.data.{Mapping, FormError}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:13
 * Description: Утиль для блоков, содержащих titleBf.
 */

object Title extends MergeBindAcc[AOStringField] {
  val BF_NAME_DFLT = "title"
  val BF_TITLE_DFLT = BfText(BF_NAME_DFLT)

  override def updateAOBlockWith(blk: AOBlock, titleOpt: Option[AOStringField]) {
    blk.text1 = titleOpt
  }

  def getTitle(bmr: BlockMapperResult) = bmr.flatMapFirstOffer(_.text1)
}


import Title._


/** Базовый трейт для статических и динамических bfTitle. Добавляет поле в форму. */
trait Title extends ValT {
  def titleBf: BfText = BF_TITLE_DFLT
  abstract override def blockFieldsRev: List[BlockFieldT] = titleBf :: super.blockFieldsRev

  // Mapping
  private def m = titleBf.getOptionalStrictMapping.withPrefix(titleBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDescr = m.bind(data)
    mergeBindAcc(maybeAcc0, offerN = 0, maybeDescr)
  }

  abstract override def unbind(value: BlockMapperResult): Map[String, String] = {
    val v = m.unbind( getTitle(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BlockMapperResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getTitle(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

