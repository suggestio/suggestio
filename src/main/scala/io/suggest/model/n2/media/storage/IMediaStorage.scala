package io.suggest.model.n2.media.storage

import com.google.inject.Inject
import io.suggest.fio.{IReadResponse, IWriteRequest}
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.primo.TypeT
import io.suggest.util.SioEsUtil.DocField
import play.api.inject.Injector
import play.api.libs.json._

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:53
 * Description: Данные по backend-хранилищу, задействованному в
 */
class IMediaStorages @Inject() (
  injector  : Injector
)
  extends IGenEsMappingProps
  with IMediaStorageStatic
{

  /** Карта инстансов статических моделей поддерживаемых media-сторэджей. */
  val STORAGES_MAP: Map[MStorage, IMediaStorageStaticImpl] = {
    MStorages.valuesT
      .iterator
      .map { mStorType =>
        val inst = injector.instanceOf( mStorType.companionCt )
        mStorType -> inst
      }
      .toMap
  }

  /** Поддержка кросс-модельной JSON-сериализации/десериализации. */
  implicit val FORMAT: OFormat[IMediaStorage] = {
    val READS: Reads[IMediaStorage] = new Reads[IMediaStorage] {
      override def reads(json: JsValue): JsResult[IMediaStorage] = {
        json.validate( MStorages.STYPE_FN_FORMAT )
          .flatMap { stype =>
            json.validate( STORAGES_MAP(stype).FORMAT )
          }
      }
    }
    val WRITES: OWrites[IMediaStorage] = new OWrites[IMediaStorage] {
      override def writes(o: IMediaStorage): JsObject = {
        val model = STORAGES_MAP(o.sType)
        model.FORMAT.writes( o.asInstanceOf[model.T] )
      }
    }
    OFormat(READS, WRITES)
  }

  override def generateMappingProps: List[DocField] = {
    MStorFns.values
      .iterator
      .map { _.esMappingProp }
      .toList
  }

  override type T = IMediaStorage

  private def _getModel(ptr: T) = STORAGES_MAP(ptr.sType)

  override def read(ptr: T): Future[IReadResponse] = {
    val model = _getModel(ptr)
    model.read(ptr.asInstanceOf[model.T])
  }

  override def delete(ptr: T): Future[_] = {
    val model = _getModel(ptr)
    model.delete( ptr.asInstanceOf[model.T] )
  }

  override def write(ptr: T, data: IWriteRequest): Future[_] = {
    val model = _getModel(ptr)
    model.write( ptr.asInstanceOf[model.T], data )
  }

  override def isExist(ptr: T): Future[Boolean] = {
    val model = _getModel(ptr)
    model.isExist( ptr.asInstanceOf[model.T] )
  }

  def assignNew(stype: MStorage): Future[IMediaStorage] = {
    STORAGES_MAP(stype).assignNew()
  }

}


/** Интерфейс для статической модели. */
trait IMediaStorageStatic extends TypeT {

  /**
    * Тип инстанса модели.
    * В media-моделях разделены понятия дескриптора данных и этих самых данных.
    * T -- по сути тип указателя на блоб media-данных, с которым работает статическая часть модели.
    */
  override type T <: IMediaStorage

  /** Поддержка сериализации-десериализации JSON. */
  def FORMAT: OFormat[T]

  /**
   * Асинхронное поточное чтение хранимого файла.
   * @return Енумератор блоба.
   */
  def read(ptr: T): Future[IReadResponse]

  /**
   * Запустить асинхронное стирание контента в backend-хранилище.
   * @return Фьючерс для синхронизации.
   */
  def delete(ptr: T): Future[_]

  /**
   * Выполнить сохранение (стриминг) блоба в хранилище.
   * @param data Асинхронный поток данных.
   * @return Фьючерс для синхронизации.
   */
  def write(ptr: T, data: IWriteRequest): Future[_]

  /** Есть ли в хранилище текущий файл? */
  def isExist(ptr: T): Future[Boolean]

}

trait IMediaStorageStaticImpl extends IMediaStorageStatic {

  def assignNew(): Future[T]

}


/** Интерфейс моделей хранилищ. */
trait IMediaStorage {

  /** Тип стораджа. */
  def sType: MStorage

}
