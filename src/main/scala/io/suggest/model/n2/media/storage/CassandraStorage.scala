package io.suggest.model.n2.media.storage

import java.util.UUID

import io.suggest.model.MUserImg2
import play.api.libs.iteratee.Enumerator
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

}
