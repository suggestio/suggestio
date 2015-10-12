package io.suggest.model.n2.media.storage

import com.google.inject.Inject
import io.suggest.model.IGenEsMappingProps
import io.suggest.util.SioEsUtil.DocField
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._

import scala.concurrent.{Future, ExecutionContext}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:53
 * Description: Данные по backend-хранилищу, задействованному в
 */
class IMediaStorage_ @Inject() (swfsStorage: SwfsStorage_) extends IGenEsMappingProps {

  val READS: Reads[IMediaStorage] = new Reads[IMediaStorage] {
    override def reads(json: JsValue): JsResult[IMediaStorage] = {
      json.validate(CassandraStorage.READS)
        .orElse { json.validate(swfsStorage.READS) }
    }
  }

  val WRITES: Writes[IMediaStorage] = new Writes[IMediaStorage] {
    override def writes(o: IMediaStorage): JsValue = {
      o.toJson
    }
  }

  implicit val FORMAT = Format(READS, WRITES)

  override def generateMappingProps: List[DocField] = {
    MStorFns.values
      .iterator
      .map { _.esMappingProp }
      .toList
  }

}


/** Интерфейс моделей хранилищ. */
trait IMediaStorage {

  /** Тип стораджа. */
  def sType: MStorage

  /** Сериализация инстанса в JSON. */
  def toJson: JsValue

  /**
   * Асинхронное поточное чтение хранимого файла.
   * @return Енумератор данных блоба.
   */
  def read(implicit ec: ExecutionContext): Enumerator[Array[Byte]]

  /**
   * Запустить асинхронное стирание контента в backend-хранилище.
   * @return Фьючерс для синхронизации.
   */
  def delete(implicit ex: ExecutionContext): Future[_]

  /**
   * Выполнить сохранение (стриминг) блоба в хранилище.
   * @param data Асинхронный поток данных.
   * @return Фьючерс для синхронизации.
   */
  def write(data: Enumerator[Array[Byte]])(implicit ec: ExecutionContext): Future[_]

  /** Есть ли в хранилище текущий файл? */
  def isExist(implicit ec: ExecutionContext): Future[Boolean]

}
