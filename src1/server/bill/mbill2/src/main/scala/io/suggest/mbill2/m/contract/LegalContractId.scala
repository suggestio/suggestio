package io.suggest.mbill2.m.contract

import java.text.DecimalFormat

import io.suggest.mbill2.m.gid.Gid_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 11:24
 * Description: Номер контракта описывается строкой из id, crand и суффикса.
 */
object LegalContractId {

  def idFormatter = new DecimalFormat("000")

}


trait LegalContractIdT {

  protected def contractId: Gid_t

  def crand: Int

  def suffix: Option[String]

  /** Напечатать строкой номер договора на основе переданных идентификаторов. */
  def legalContractId: String = {
    val fmt = LegalContractId.idFormatter
    val idStr = fmt.format(contractId)
    val sb = new StringBuilder(idStr)
    // Нужно добавить нули в начале crand
    val crandStr = fmt.format(crand)
    sb.append('-').append(crandStr)
    if (suffix.isDefined)
      sb.append('/').append(suffix.get)
    sb.toString()
  }

}
