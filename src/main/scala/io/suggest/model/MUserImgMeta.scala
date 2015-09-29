package io.suggest.model

import java.util.UUID
import io.suggest.model.img.IImgMeta
import io.suggest.util.UuidUtil
import SioCassandraClient.session
import com.websudos.phantom.Implicits._
import org.joda.time.DateTime

import scala.collection.immutable.StringOps
import scala.concurrent.Future
import io.suggest.util.SioFutureUtil.guavaFuture2scalaFuture    // НЕ УДАЛЯТЬ!!!
import MUserImg2.qOpt2q
import io.suggest.common.fut.FutureUtil.tryCatchFut

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.14 14:47
 * Description: Модель для отдельного хранения метаданных по картинкам.
 * Модель используется те же квалифиеры, что и orig-img-модель, но собственную CF (таблицу).
 */
case class MUserImgMeta2(
  md        : Map[String, String],
  q         : String,
  id        : UUID = UUID.randomUUID(),
  timestamp : DateTime = DateTime.now()
) extends UuidIdStr with CassandraImgMeta with IImgMeta {

  def save = MUserImgMeta2.insertMd(this)
  def delete = MUserImgMeta2.deleteById(id)

  override def dateCreated = timestamp
  private def _szK2i(key: String): Int = {
    val raw = md(key)
    (raw: StringOps).toInt
  }
  override def height = _szK2i("w")
  override def width  = _szK2i("h")

}


/** phantom-часть статической стороны модели. */
sealed class MUserImgMetaRecord extends CassandraTable[MUserImgMetaRecord, MUserImgMeta2] {

  object id extends UUIDColumn(this) with PartitionKey[UUID]
  object q extends StringColumn(this) with PrimaryKey[String]
  object md extends MapColumn[MUserImgMetaRecord, MUserImgMeta2, String, String](this)
  object timestamp extends DateTimeColumn(this)

  override def fromRow(row: Row): MUserImgMeta2 = {
    // Раньше колонки timestamp не было.
    val dt: DateTime = try {
      timestamp(row)
    } catch {
      // Если нет таймштампа (старый формат ряда), то выставляем статический. Этого должно быть достаточно.
      case ex: Exception => new DateTime(2014, 9, 30, 12, 12, 12)
    }
    MUserImgMeta2(md(row), q(row), id(row), dt)
  }

}


/** Статическая сторона модели. */
object MUserImgMeta2 extends MUserImgMetaRecord with CassandraStaticModel[MUserImgMetaRecord, MUserImgMeta2] with DeleteByStrId {

  override val tableName = "im"

  def getByStrId(idStr: String, q: Option[String] = None): Future[Option[MUserImgMeta2]] = {
    val id = UuidUtil.base64ToUuid(idStr)
    getById(id, q)
  }

  def getMany(limit: Int): Future[Seq[MUserImgMeta2]] = {
    tryCatchFut {
      select
        .limit(limit)
        .fetch()
    }
  }

  def getById(id: UUID, q: Option[String] = None): Future[Option[MUserImgMeta2]] = {
    tryCatchFut {
      select
        .where(_.id eqs id)
        .and(_.q eqs qOpt2q(q))
        .one()
    }
  }

  def insertMd(m: MUserImgMeta2) = {
    tryCatchFut {
      insert
        .value(_.id, m.id)
        .value(_.q, m.q)
        .value(_.md, m.md)
        .value(_.timestamp, m.timestamp)
        .future()
    }
  }

  def isExists(id: UUID, q: Option[String] = None): Future[Boolean] = {
    tryCatchFut {
      select(_.timestamp)
        .where(_.id eqs id)
        .and(_.q eqs qOpt2q(q))
        .one()
        .map { _.isDefined }
    }
  }

  def deleteById(id: UUID) = {
    tryCatchFut {
      delete.where(_.id eqs id)
        .future()
    }
  }

  def deleteOne(id: UUID, q: Option[String] = None): Future[_] = {
    tryCatchFut {
      delete
        .where(_.id eqs id)
        .and(_.q eqs qOpt2q(q))
        .future()
    }
  }

  def countAllQualified(qOpt: Option[String]): Future[Long] = {
    tryCatchFut {
      val q = MUserImg2.qOpt2q(qOpt)
      val qb = count
        .where(_.q eqs q)
      // TODO Нужно "allow filtering" добавить в query.
      println(getClass.getSimpleName + ": Cassandra: " + qb.queryString)
      qb.one()
        .map { _ getOrElse 0L }
    }
  }

  /**
   * 2014.09.30: добавлялка поля timestamp. Потом надо удалить это отсюда и из jmx.
   * @return Фьючерс для синхронизации.
   */
  def addTimestampColumn(): Future[ResultSet] = {
    session.executeAsync(s"ALTER TABLE $tableName ADD timestamp timestamp;")
  }

}



// JMX
trait MUserImgMeta2JmxMBean extends CassandraModelJxmMBeanI {
  def addTimestampColumn(): String

  def getMany(limit: Int): String

  def countAllOriginals: Long
}

class MUserImgMeta2Jmx extends CassandraModelJmxMBeanImpl with MUserImgMeta2JmxMBean {

  override def companion = MUserImgMeta2

  override def addTimestampColumn(): String = {
    val fut = companion.addTimestampColumn()
      .map(_.toString)
    awaitString(fut)
  }

  override def getMany(limit: Int): String = {
    val fut = for {
      res <- MUserImgMeta2.getMany(limit)
    } yield {
      res.iterator
        .map { _.toString }
        .mkString("\n\n")
    }
    awaitString(fut)
  }

  override def countAllOriginals: Long = {
    MUserImgMeta2.countAllQualified(None)
  }

}
