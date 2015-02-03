package models

import java.sql.Connection

import anorm._
import util.SqlModelSave

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.06.14 10:16
 * Description: Роялти, т.е. отчисления узла другим узлам.
 */
object MBillRoyalty extends FindByContract {
  import SqlParser._

  override type T = MBillRoyalty

  override val TABLE_NAME = "bill_royalty"

  override val rowParser: RowParser[MBillRoyalty] = {
    get[Option[Int]]("id") ~ get[Int]("contract_id") ~ get[Float]("royalty") ~ get[Boolean]("is_internal") ~ get[String]("to_adn_id") map {
      case id ~ contractId ~ royalty ~ isInternal ~ toAdnId =>
        MBillRoyalty(
          id = id,
          contractId = contractId,
          royalty = royalty,
          isInternal = isInternal,
          toAdnId = toAdnId
        )
    }
  }

}


import MBillRoyalty._


final case class MBillRoyalty(
  contractId: Int,
  royalty: Float,
  isInternal: Boolean,
  toAdnId: String,
  id: Option[Int] = None
) extends SqlModelSave with MBillContractSel {

  override def hasId = id.isDefined

  override type T = MBillRoyalty
  override def companion = MBillRoyalty

  def isExternal = !isInternal

  override def saveInsert(implicit c: Connection): T = {
    SQL("INSERT INTO " + TABLE_NAME + "(contract_id, royalty, is_internal, to_adn_id) " +
      "VALUES({contractId}, {royalty}, {isInternal}, {toAdnId})")
      .on('contractId -> contractId, 'royalty -> royalty, 'isInternal -> isInternal, 'toAdnId -> toAdnId)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL("UPDATE " + TABLE_NAME + " SET royalty = {royalty}, is_internal = {isInternal}, to_adn_id = {toAdnId} WHERE id = {id}")
      .on('id -> id.get, 'royalty -> royalty, 'isInternal -> isInternal, 'toAdnId -> toAdnId)
      .executeUpdate()
  }

}
