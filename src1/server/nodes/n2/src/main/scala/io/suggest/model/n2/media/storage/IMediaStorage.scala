package io.suggest.model.n2.media.storage

import javax.inject.Inject

import io.suggest.compress.MCompressAlgo
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.fio.{IDataSource, IWriteRequest}
import io.suggest.primo.TypeT
import io.suggest.es.util.SioEsUtil.DocField
import io.suggest.model.n2.media.storage.swfs.SwfsStorage
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.url.MHostInfo
import play.api.inject.Injector
import play.api.libs.json._
import play.api.mvc.QueryStringBindable

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 18:53
 * Description: Данные по backend-хранилищу, задействованному в хранении файлов.
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
    MStorages.values
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
        json.validate( MStorage.STYPE_FN_FORMAT )
          .flatMap { stype =>
            json.validate( getModel(stype).FORMAT )
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
    getModel(ptr.storageType)
  }

  def getModel(sType: MStorage): IMediaStorageStaticImpl { type T = X } = {
    STORAGES_MAP(sType)
      // Страшненький костыль, чтобы избавится от ошибок компилятора по поводу внутреннего типа.
      // Он же не в курсе, что значение в карте жестко связано с MStorage.
      // Данные по конкретным типам в карте сторэджей стёрты, а нужно ведь как-то с ними взаимодействовать...
      .asInstanceOf[ IMediaStorageStaticImpl { type T = X } ]
  }

  override def read(ptr: X, acceptCompression: Iterable[MCompressAlgo]): Future[IDataSource] = {
    _getModel(ptr)
      .read( ptr, acceptCompression )
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
  def assignNew(stype: MStorage): Future[(IMediaStorage, AnyRef)] = {
    getModel(stype)
      .assignNew()
  }

  def getStorageHost(ptr: T): Future[Seq[MHostInfo]] = {
    _getModel(ptr)
      .getStorageHost(ptr)
  }

  /** Вернуть хосты, связанные с хранением указанных данных.
    *
    * @param ptrs storage-указатели.
    * @return Карта с хостами.
    */
  def getStoragesHosts(ptrs: Iterable[T]): Future[Map[T, Seq[MHostInfo]]] = {
    if (ptrs.isEmpty) {
      Future.successful( Map.empty )
    } else {
      val ptr = ptrs.head
      _getModel(ptr)
        .getStoragesHosts(ptrs)
    }
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
    * @param ptr Описание цели.
    * @param acceptCompression Допускать возвращать ответ в сжатом формате.
    *
    * @return Поток данных блоба + сопутствующие метаданные.
    */
  def read(ptr: T, acceptCompression: Iterable[MCompressAlgo] = Nil): Future[IDataSource]

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
  def assignNew(): Future[(T, AnyRef)]

  /** Вернуть данные Assigned storage для указанных метаданных хранилища.
    *
    * @param ptr Media-указатель.
    * @return Фьючерс с инстансом MAssignedStorage.
    */
  def getStorageHost(ptr: T): Future[Seq[MHostInfo]]

  /** Пакетный аналог для getAssignedStorage().
    *
    * @param ptrs Все указатели.
    * @return Фьючерс с картой хостов.
    */
  def getStoragesHosts(ptrs: Iterable[T]): Future[Map[T, Seq[MHostInfo]]]

}


/** Интерфейс моделей хранилищ.
  * Странная модель. Она может быть и ключом в Map, и в URL qs жить, и т.д.
  * И это интерфейс, некрасиво как-то.
  */
// TODO Сделать sealed, но SwfsStorage живёт в другом файле...
trait IMediaStorage {

  /** Тип стораджа. */
  def storageType: MStorage

  /** Какие-то данные уровня storage, понятные текущему типу стораджа. */
  def storageInfo: String

}

object IMediaStorage {

  object Fields {
    def STORAGE_TYPE_FN = "t"
    def STORAGE_DATA_FN = "d"
  }

  /** Поддержка биндинга из/в URL qs. */
  implicit def mediaStorageQsb( implicit
                                swfsStorageB    : QueryStringBindable[SwfsStorage],
                                storageB        : QueryStringBindable[MStorage]
                              ): QueryStringBindable[IMediaStorage] = {
    new QueryStringBindableImpl[IMediaStorage] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, IMediaStorage]] = {
        val k = key1F(key)
        val F = Fields
        for {
          storageTypeE <- storageB.bind(k(F.STORAGE_TYPE_FN), params)
          storageType  <- storageTypeE.toOption
          storageDataE <- storageType match {
            case MStorages.SeaWeedFs =>
              swfsStorageB.bind(k(F.STORAGE_DATA_FN), params)
          }
        } yield {
          storageDataE
        }
      }

      override def unbind(key: String, value: IMediaStorage): String = {
        val k = key1F(key)
        val F = Fields
        _mergeUnbinded1(
          storageB.unbind( k(F.STORAGE_TYPE_FN), value.storageType ),
          value match {
            case swfs: SwfsStorage =>
              swfsStorageB.unbind( k(F.STORAGE_DATA_FN), swfs )
          }
        )
      }

    }
  }

}
