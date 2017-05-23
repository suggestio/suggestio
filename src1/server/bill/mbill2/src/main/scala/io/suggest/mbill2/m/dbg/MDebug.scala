package io.suggest.mbill2.m.dbg

import com.google.inject.{Inject, Singleton}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.slick.profile.pg.SioPgSlickProfileT
import slick.lifted.ProvenShape

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.05.17 22:30
  * Description: slick-модель, представляющая хранилище отладочной инфы по биллинга.
  */
@Singleton
class MDebugs @Inject() (
                          protected val profile  : SioPgSlickProfileT
                        ) {

  import profile.api._

  /** Название таблицы. */
  val TABLE_NAME = "debug"

  /** Статические имена полей. */
  object Fields {

    def OBJECT_ID_FN = "object_id"
    def KEY_FN       = "key"
    def VSN_FN       = "vsn"
    def DATA_FN      = "data"

  }


  /** Slick-спека SQL-таблицы debug. */
  class MDebugTable(tag: Tag)
    extends Table[MDebug](tag, TABLE_NAME)
  {

    def objectId  = column[Gid_t]( Fields.OBJECT_ID_FN, O.PrimaryKey )

    def keyStr    = column[String]( Fields.KEY_FN, O.PrimaryKey )
    def key       = keyStr <> (MDbgKeys.withName, MDbgKeys.unapply)

    def vsn       = column[DbgVsn_t]( Fields.VSN_FN )
    def data      = column[Array[Byte]]( Fields.DATA_FN )

    override def * : ProvenShape[MDebug] = {
      (objectId, key, vsn, data) <> ((MDebug.apply _).tupled, MDebug.unapply)
    }

  }

  val query = TableQuery[MDebugTable]


  def insertOne(md: MDebug): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .insertOrUpdate(md)
  }


  def deleteByObjectId(objectId: Gid_t*): DBIOAction[Int, NoStream, Effect.Write] = {
    deleteByObjectIds( objectId )
  }
  def deleteByObjectIds(objectId: Traversable[Gid_t]): DBIOAction[Int, NoStream, Effect.Write] = {
    query
      .filter( _.objectId.inSet( objectId ) )
      .delete
  }

}

/** Интерфейс для поля с DI-инстансом [[MDebugs]]. */
trait IMDebugs {
  def mDebugs: MDebugs
}


/** Инстанс одной дебажной записи.
  *
  * @param objectId id записи биллинга.
  * @param key Ключ, описывающий данные.
  * @param vsn Версия используемого формата данных.
  * @param data Бинарь с произвольными данными.
  */
case class MDebug(
                  objectId  : Gid_t,
                  key       : MDbgKey,
                  vsn       : Short,
                  data      : Array[Byte]
                 )
{

  override def toString: String = {
    s"${getClass.getSimpleName}($objectId#$key,v$vsn,${data.length}b)"
  }

}
