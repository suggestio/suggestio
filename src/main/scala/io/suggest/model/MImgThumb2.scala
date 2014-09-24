package io.suggest.model

import java.nio.ByteBuffer
import java.util.UUID
import io.suggest.util.UuidUtil
import org.joda.time.DateTime
import com.websudos.phantom.Implicits._
import SioCassandraClient.session

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.09.14 16:57
 * Description: Модель нового поколения для хранения img thumb'ов.
 * По типам не очень-то совместима с оригинальной MImgThumb, поэтому живёт отдельно от кравлера.
 * В будущем стоило бы их как-то объеденить с оригинальной MImgThumb.
 *
 * Cassandra имеет плоское пространство ключей, а "таблицы" - это column family, поля - это колонки.
 */

case class MImgThumb2(
  imageUrl  : Option[String],
  img       : ByteBuffer,
  timestamp : DateTime = DateTime.now,
  id        : UUID = UUID.randomUUID()
) extends UuidIdStr with CassandraImgWithTimestamp {

  def save = MImgThumb2.insertThumb(this)
  def delete = MImgThumb2.deleteById(id)
}


/** Прослойка для object'а, которая занимается сериализацией-десериализацией данных. */
sealed class MImgThumb2Record extends CassandraTable[MImgThumb2Record, MImgThumb2] {

  object id extends UUIDColumn(this) with PartitionKey[UUID]
  object imageUrl extends OptionalStringColumn(this)
  object thumb extends BlobColumn(this)
  object timestamp extends DateTimeColumn(this)

  /** Десериализация ряда в экземпляр модели. */
  override def fromRow(row: Row): MImgThumb2 = {
    MImgThumb2(imageUrl(row), thumb(row), timestamp(row), id(row))
  }

}


/** Статическая сторона модели MImgThumb2. */
object MImgThumb2 extends MImgThumb2Record with CassandraStaticModel[MImgThumb2Record, MImgThumb2] with DeleteByStrId {

  override val tableName = "it"

  /**
   * Прочитать из базы по id.
   * @param uuidB64 строка-идентификатор картинки.
   * @return Фьючерс для синхронизации.
   */
  def getThumbByStrId(uuidB64: String): Future[Option[MImgThumb2]] = {
    val uuid = UuidUtil.base64ToUuid(uuidB64)
    getThumbById(uuid)
  }

  def getThumbById(uuid: UUID): Future[Option[MImgThumb2]] = {
    select.where(_.id eqs uuid).one()
  }

  def deleteById(id: UUID) = delete.where(_.id eqs id).future()

  /** Сохранение экземпляра модели в хранилище. */
  def insertThumb(mit2: MImgThumb2) = {
    insert
      .value(_.id, mit2.id)
      .value(_.imageUrl, mit2.imageUrl)
      .value(_.thumb, mit2.img)
      .value(_.timestamp, mit2.timestamp)
      .future()
  }

}


// JMX
trait MImgThumb2JmxMBean extends CassandraModelJxmMBeanI
class MImgThumb2Jmx extends CassandraModelJmxMBeanImpl with MImgThumb2JmxMBean {
  override def companion = MImgThumb2
}

