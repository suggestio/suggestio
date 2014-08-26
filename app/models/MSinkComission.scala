package models

import java.sql.Connection

import anorm._
import util.SqlModelSave
import util.anorm.AnormAdnSink._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.14 16:10
 * Description: Коэффициенты комиссионных в рамках контрактов для разных методик выдачи.
 * Рекламодатели должны видеть одни и те же тарифы, но прибыль s.io должны гибко различаться.
 */
object MSinkComission extends SqlModelStatic with FindByContract {
  override type T = MSinkComission

  override val TABLE_NAME = "sink_comission"

  override val rowParser: RowParser[T] = {
    import SqlParser._
    get[Option[Int]]("id") ~ get[AdnSink]("sink") ~ int("contract_id") ~ float("sio_comission") map {
      case id ~ sink ~ contractId ~ sioComission =>
        MSinkComission(id = id, sink = sink, contractId = contractId, sioComission = sioComission)
    }
  }

}


import MSinkComission._


case class MSinkComission(
  contractId    : Int,
  sink          : AdnSink,
  sioComission  : Float,
  id            : Option[Int] = None
) extends SqlModelSave[MSinkComission] with SqlModelDelete {

  override def hasId = id.isDefined
  override def companion = MSinkComission

  override def saveInsert(implicit c: Connection): MSinkComission = {
    SQL("INSERT INTO " + TABLE_NAME + "(sink, contract_id, sio_comission) VALUES ({sink}, {contractId}, {sioComission})")
      .on('sink -> sink, 'contractId -> contractId, 'sioComission -> sioComission)
      .executeInsert(rowParser single)
  }

  override def saveUpdate(implicit c: Connection): Int = {
    SQL(s"UPDATE " + TABLE_NAME + " SET sink = {sink}, sio_comission = {sioComission} WHERE id = {id}")
      .on('sink -> sink, 'sioComission -> sioComission)
      .executeUpdate()
  }

}