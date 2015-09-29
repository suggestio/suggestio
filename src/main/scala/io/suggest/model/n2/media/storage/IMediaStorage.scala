package io.suggest.model.n2.media.storage

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
object IMediaStorage extends IGenEsMappingProps {

  val STYPE_FN_FORMAT = (__ \ MStorFns.STYPE.fn).format[MStorage]

  implicit val READS: Reads[IMediaStorage] = new Reads[IMediaStorage] {
    override def reads(json: JsValue): JsResult[IMediaStorage] = {
      json.validate[CassandraStorage]
        .orElse { json.validate[SwfsStorage] }
    }
  }

  implicit val WRITES: Writes[IMediaStorage] = new Writes[IMediaStorage] {
    override def writes(o: IMediaStorage): JsValue = {
      o match {
        case cs: CassandraStorage =>
          Json.toJson(cs)
        case swfs: SwfsStorage =>
          Json.toJson(swfs)
      }
    }
  }

  override def generateMappingProps: List[DocField] = {
    MStorFns.values
      .iterator
      .map { _.esMappingProp }
      .toList
  }

}



trait IMediaStorage {

  /** Тип стораджа. */
  def sType: MStorage

  /** Асинхронное поточное чтение хранимого файла. */
  def read(implicit ec: ExecutionContext): Enumerator[Array[Byte]]

  /** Запустить асинхронное стирание контента в backend-хранилище. */
  def delete(implicit ex: ExecutionContext): Future[_]

}
