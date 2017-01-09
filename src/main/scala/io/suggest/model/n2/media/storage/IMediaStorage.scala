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

  override type T = IMediaStorage

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
  implicit val FORMAT: OFormat[T] = {
    val READS: Reads[T] = new Reads[T] {
      override def reads(json: JsValue): JsResult[T] = {
        json.validate( MStorages.STYPE_FN_FORMAT )
          .flatMap { stype =>
            json.validate( _getModel(stype).FORMAT )
          }
      }
    }
    val WRITES: OWrites[T] = new OWrites[T] {
      override def writes(o: T): JsObject = {
        _getModel(o)
          .FORMAT
          .writes( o )
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

  private type X = T
  private def _getModel(ptr: T): IMediaStorageStaticImpl { type T = X } = {
    _getModel(ptr.sType)
  }
  private def _getModel(sType: MStorage): IMediaStorageStaticImpl { type T = X } = {
    STORAGES_MAP(sType)
      // Страшненький костыль, чтобы избавится от ошибок компилятора по поводу внутреннего типа.
      // Он же не в курсе, что значение в карте жестко связано с MStorage.
      // Данные по конкретным типам в карте сторэджей стёрты, а нужно ведь как-то с ними взаимодействовать...
      .asInstanceOf[ IMediaStorageStaticImpl { type T = X } ]
  }

  override def read(ptr: T): Future[IReadResponse] = {
    _getModel(ptr)
      .read( ptr )
  }

  override def delete(ptr: T): Future[_] = {
    _getModel(ptr)
      .delete( ptr )
  }

  override def write(ptr: T, data: IWriteRequest): Future[_] = {
    _getModel(ptr)
      .write( ptr, data )
  }

  override def isExist(ptr: T): Future[Boolean] = {
    _getModel(ptr)
      .isExist( ptr )
  }

  /** Награбить новый указатель в хранилище указанного типа. */
  def assignNew(stype: MStorage): Future[IMediaStorage] = {
    _getModel(stype)
      .assignNew()
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


/** Расширенная версия интерфейс [[IMediaStorageStatic]], т.к. assignNew() без параметров
  * может быть реализован только в конкретных стораджах, но не в [[IMediaStorages]],
  * т.к. зависим от типа желаемого хранилища. */
trait IMediaStorageStaticImpl extends IMediaStorageStatic {

  /**
    * Подготовить новый указатель в текущем хранилище для загрузки очередного блобика.
    * @return Фьючерс с инстансом свежего указателя.
    */
  def assignNew(): Future[T]

}


/** Интерфейс моделей хранилищ. */
trait IMediaStorage {

  /** Тип стораджа. */
  def sType: MStorage

}
