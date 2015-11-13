package util.blocks

import models.blk.ed.{AdFormM, BindAcc, BindResult}
import models.{MEntity, TextEnt}
import play.api.data.{FormError, Mapping}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.05.14 22:13
 * Description: Утиль для блоков, содержащих titleBf.
 */

object Title extends MergeBindAccAOBlock[TextEnt] {

  val BF_NAME_DFLT = "title"
  val BF_TITLE_DFLT = BfText(BF_NAME_DFLT)

  override def updateEntityWith(blk: MEntity, titleOpt: Option[TextEnt]): MEntity = {
    blk.copy(
      text = titleOpt
    )
  }

  def getTitle(bmr: BindResult) = bmr.flatMapFirstOffer(_.text)
}


import util.blocks.Title._


/** Базовый трейт для статических и динамических bfTitle. Добавляет поле в форму. */
trait Title extends ValT {
  def titleBf: BfText = BF_TITLE_DFLT
  abstract override def blockFieldsRev(af: AdFormM): List[BlockFieldT] = titleBf :: super.blockFieldsRev(af)

  // Mapping
  private def m = titleBf.getOptionalStrictMapping.withPrefix(titleBf.name).withPrefix(key)

  abstract override def mappingsAcc: List[Mapping[_]] = {
    m :: super.mappingsAcc
  }

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeDescr = m.bind(data)
    mergeBindAcc(maybeAcc0, maybeDescr)
  }

  abstract override def unbind(value: BindResult): Map[String, String] = {
    val v = m.unbind( getTitle(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BindResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getTitle(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}

