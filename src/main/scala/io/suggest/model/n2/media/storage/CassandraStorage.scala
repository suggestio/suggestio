package io.suggest.model.n2.media.storage

import java.nio.ByteBuffer
import java.util.UUID

import io.suggest.model.MUserImg2
import org.joda.time.DateTime
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import IMediaStorage.STYPE_FN_FORMAT

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

  implicit val READS: Reads[CassandraStorage] = (
    STYPE_FN_FORMAT.filter { _ == MStorages.Cassandra } and
    ROW_KEY_FN_FORMAT and
    QUALIFIER_FN_FORMAT
  )(
    { (_, rowKey, qOpt) => CassandraStorage(rowKey, qOpt) }
  )

  implicit val WRITES: OWrites[CassandraStorage] = (
    (STYPE_FN_FORMAT : OWrites[MStorage]) and
    ROW_KEY_FN_FORMAT and
    QUALIFIER_FN_FORMAT
  ) { cs =>
    (cs.sType, cs.rowKey, cs.qOpt)
  }

}


case class CassandraStorage(
  rowKey  : UUID,
  qOpt    : Option[String]
)
  extends IMediaStorage
{
  override def sType: MStorage = MStorages.Cassandra

  override def read(implicit ec: ExecutionContext): Enumerator[Array[Byte]] = {
    val enumFut = MUserImg2.getById(rowKey, qOpt)
      .flatMap {
        case Some(v) =>
          Future successful Enumerator(v.imgBytes)
        case None =>
          Future failed new NoSuchElementException("Key not found: " + rowKey + " q=" + qOpt)
      }
    Enumerator.flatten(enumFut)
  }

  override def delete(implicit ex: ExecutionContext): Future[_] = {
    MUserImg2.deleteOne(rowKey, qOpt)
  }

  override def write(data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[_] = {
    // Сдампить блобики в один единый блоб.
    val itee = Iteratee.fold [Array[Byte], List[Array[Byte]]] (Nil) {
      (acc0, e) =>
        e :: acc0
    }
    (data |>>> itee)
      .map { arraysRev =>
        // Оптимизация для результатов из ровно одного куска.
        if (arraysRev.tail.isEmpty) {
          arraysRev.head

        } else {
          // Если кусков несколько, от восстановить исходных порядок и склеить в один массив.
          arraysRev
            .reverseIterator
            .flatten
            .toArray
        }

      }.flatMap { barr =>
        val mimg2 = MUserImg2(
          q         = MUserImg2.qOpt2q(qOpt),
          img       = ByteBuffer.wrap(barr),
          timestamp = DateTime.now(),
          id        = rowKey
        )
        mimg2.save
      }
  }

  /** Есть ли в хранилище текущий файл? */
  override def isExist(implicit ec: ExecutionContext): Future[Boolean] = {
    MUserImg2.isExists(rowKey, qOpt)
  }

}
