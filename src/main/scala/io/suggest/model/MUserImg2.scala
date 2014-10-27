package io.suggest.model

import java.nio.ByteBuffer
import java.util.UUID
import io.suggest.util.UuidUtil
import org.joda.time.DateTime
import com.websudos.phantom.Implicits._
import SioCassandraClient.session
import MPict.Q_USER_IMG_ORIG

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.14 15:24
 * Description: Cassandra-модель для хранения картинок (в виде блобов).
 */
case class MUserImg2(
  q         : String,
  img       : ByteBuffer,
  timestamp : DateTime = DateTime.now,
  id        : UUID = UUID.randomUUID()
) extends UuidIdStr with CassandraImgWithTimestamp {

  def save = MUserImg2.insertImg(this)
  def delete = MUserImg2.deleteById(id)
  def isExists = MUserImg2.isExists(id, Some(q))
}


/** Статическая схема модели. */
sealed class MUserImgRecord extends CassandraTable[MUserImgRecord, MUserImg2] {

  object id extends UUIDColumn(this) with PartitionKey[UUID]
  object q extends StringColumn(this) with PrimaryKey[String]
  object timestamp extends DateTimeColumn(this)
  object img extends BlobColumn(this)

  override def fromRow(r: Row): MUserImg2 = {
    MUserImg2(
      q = q(r),
      img = img(r),
      timestamp = timestamp(r),
      id = id(r)
    )
  }
}


/** Статическая сторона модели. */
object MUserImg2 extends MUserImgRecord with CassandraStaticModel[MUserImgRecord, MUserImg2] with DeleteByStrId {

  override val tableName = "i2"

  def isExists(id: UUID, q: Option[String] = None): Future[Boolean] = {
    count
      .where(_.id eqs id)
      .limit(1)
      .one()
      .map { _.exists(_ > 0L) }
  }

  def getByStrId(idStr: String, q: Option[String] = None): Future[Option[MUserImg2]] = {
    val id = UuidUtil.base64ToUuid(idStr)
    getById(id, q)
  }

  def getById(id: UUID, q: Option[String] = None): Future[Option[MUserImg2]] = {
    val q1 = q getOrElse Q_USER_IMG_ORIG
    select.where(_.id eqs id).and(_.q eqs q1).one()
  }

  def insertImg(m: MUserImg2) = {
    insert
      .value(_.id, m.id)
      .value(_.q, m.q)
      .value(_.img, m.img)
      .value(_.timestamp, m.timestamp)
      .future()
  }

  def deleteById(id: UUID) = delete.where(_.id eqs id).future()

}



// JMX
trait MUserImg2JmxMBean extends CassandraModelJxmMBeanI
class MUserImg2Jmx extends CassandraModelJmxMBeanImpl with MUserImg2JmxMBean {
  override def companion = MUserImg2
}

