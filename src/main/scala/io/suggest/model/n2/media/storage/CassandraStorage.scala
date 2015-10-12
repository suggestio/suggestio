package io.suggest.model.n2.media.storage

import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.UUID

import io.suggest.fio.{ReadResponse, IWriteRequest}
import io.suggest.model.MUserImg2
import org.joda.time.DateTime
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.libs.functional.syntax._
import MStorages.STYPE_FN_FORMAT

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:17
 * Description: Модель данных хранилища кассандры.
 */
object CassandraStorage {

  val QUALIFIER_FN_FORMAT = (__ \ MStorFns.QUALIFIER.fn).formatNullable[String]
  val ROW_KEY_FN_FORMAT   = (__ \ MStorFns.ROW_KEY.fn).format[UUID]

  val READS: Reads[CassandraStorage] = (
    STYPE_FN_FORMAT.filter { _ == MStorages.Cassandra } and
    ROW_KEY_FN_FORMAT and
    QUALIFIER_FN_FORMAT
  )(
    { (_, rowKey, qOpt) => CassandraStorage(rowKey, qOpt) }
  )

  val WRITES: OWrites[CassandraStorage] = (
    (STYPE_FN_FORMAT : OWrites[MStorage]) and
    ROW_KEY_FN_FORMAT and
    QUALIFIER_FN_FORMAT
  ) { cs =>
    (cs.sType, cs.rowKey, cs.qOpt)
  }

  implicit val FORMAT = Format(READS, WRITES)

}


case class CassandraStorage(
  rowKey  : UUID,
  qOpt    : Option[String]
)
  extends IMediaStorage
{
  override def sType: MStorage = MStorages.Cassandra

  override def toJson = Json.toJson(this)

  override def read(implicit ec: ExecutionContext): Future[ReadResponse] = {
    MUserImg2.getById(rowKey, qOpt)
      .flatMap {
        case Some(v) =>
          val rr = ReadResponse(
            contentType = "application/octet-stream",
            data  = Enumerator(v.imgBytes),
            sizeB = v.imgBytes.length
          )
          Future successful rr
        case None =>
          Future failed new NoSuchElementException("Key not found: " + rowKey + " q=" + qOpt)
      }
  }

  override def delete(implicit ex: ExecutionContext): Future[_] = {
    MUserImg2.deleteOne(rowKey, qOpt)
  }

  override def write(data: IWriteRequest)(implicit ec: ExecutionContext): Future[_] = {
    val barr = Files.readAllBytes( data.file.toPath )
    // Сдампить блобики в один единый блоб.
    val mimg2 = MUserImg2(
      q         = MUserImg2.qOpt2q(qOpt),
      img       = ByteBuffer.wrap(barr),
      timestamp = DateTime.now(),
      id        = rowKey
    )
    mimg2.save
  }

  /** Есть ли в хранилище текущий файл? */
  override def isExist(implicit ec: ExecutionContext): Future[Boolean] = {
    MUserImg2.isExists(rowKey, qOpt)
  }

}
