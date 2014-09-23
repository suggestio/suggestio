package io.suggest.model

import java.util.UUID
import io.suggest.util.UuidUtil
import com.websudos.phantom.Implicits._
import SioCassandraClient.session

import scala.concurrent.Future
import MPict.Q_USER_IMG_ORIG

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.14 14:47
 * Description: Модель для отдельного хранения метаданных по картинкам.
 * Модель используется те же квалифиеры, что и orig-img-модель, но собственную CF (таблицу).
 */
case class MUserImgMeta2(
  md: Map[String, String],
  q: String,
  id: UUID = UUID.randomUUID()
) extends IdStr {

  def save = MUserImgMeta2.insertMd(this)
  def delete = MUserImgMeta2.deleteById(id)

}


/** phantom-часть статической стороны модели. */
sealed class MUserImgMetaRecord extends CassandraTable[MUserImgMetaRecord, MUserImgMeta2] {

  object id extends UUIDColumn(this) with PartitionKey[UUID]
  object q extends StringColumn(this) with PrimaryKey[String]
  object md extends MapColumn[MUserImgMetaRecord, MUserImgMeta2, String, String](this)

  override def fromRow(row: Row): MUserImgMeta2 = {
    MUserImgMeta2(md(row), q(row), id(row))
  }

}


/** Статическая сторона модели. */
object MUserImgMeta2 extends MUserImgMetaRecord {

  override val tableName = "im"

  def getByStrId(idStr: String, q: Option[String] = None): Future[Option[MUserImgMeta2]] = {
    val id = UuidUtil.base64ToUuid(idStr)
    getById(id, q)
  }

  def getById(id: UUID, q: Option[String] = None): Future[Option[MUserImgMeta2]] = {
    val q1 = q.getOrElse(Q_USER_IMG_ORIG)
    select.where(_.id eqs id).and(_.q eqs q1).one()
  }

  def insertMd(m: MUserImgMeta2) = {
    insert
      .value(_.id, m.id)
      .value(_.q, m.q)
      .value(_.md, m.md)
      .future()
  }

  def createTable = create.execute()

  def deleteById(id: UUID) = delete.where(_.id eqs id).execute()
  def deleteByStrId(id: String) = deleteById(UuidUtil.base64ToUuid(id))

}
