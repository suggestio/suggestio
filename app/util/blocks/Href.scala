package util.blocks

import models.blk.ed.{AdFormM, BindResult, BindAcc}
import play.api.data.FormError
import util.FormUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.06.14 21:19
 * Description: Поле для ввода ссылки, которая будет присоединена к экземпляру AOBlock.
 */
object Href extends MergeBindAcc[Option[String]] {

  val BF_NAME_DFLT = "href"

  val BF_HREF_DFLT = new BfString(
    name = BF_NAME_DFLT,
    minLen = 6,
    maxLen = 512
  ) {
    override def mappingBase = FormUtil.urlStrM
    override def getOptionalStrictMapping = FormUtil.urlStrOptM
  }

  /** Как-то обновить акк. */
  override def updateAcc(offerN: Int, acc0: BindAcc, v: Option[String]): BindAcc = {
    acc0.copy(
      href = v
    )
  }

  // TODO Нужно сделать href отдельным полем в BindAcc.
  def getHref(bmr: BindResult) = bmr.href

}


import Href._


trait Href extends ValT {

  def hrefBf = BF_HREF_DFLT

  override def hrefBlock = true

  abstract override def blockFieldsRev(af: AdFormM) = hrefBf :: super.blockFieldsRev(af)

  private def m = hrefBf.getOptionalStrictMapping.withPrefix(hrefBf.name).withPrefix(key)

  abstract override def mappingsAcc = m :: super.mappingsAcc

  abstract override def bindAcc(data: Map[String, String]): Either[Seq[FormError], BindAcc] = {
    val maybeAcc0 = super.bindAcc(data)
    val maybeHref = m.bind(data)
    mergeBindAcc(maybeAcc0, maybeHref)
  }

  abstract override def unbind(value: BindResult): Map[String, String] = {
    val v = m.unbind( getHref(value) )
    super.unbind(value) ++ v
  }

  abstract override def unbindAndValidate(value: BindResult): (Map[String, String], Seq[FormError]) = {
    val (ms, fes) = super.unbindAndValidate(value)
    val c = getHref(value)
    val (cms, cfes) = m.unbindAndValidate(c)
    (ms ++ cms) -> (fes ++ cfes)
  }
}
