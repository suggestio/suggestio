package io.suggest.model.n2.media

import javax.inject.{Inject, Singleton}

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model._
import io.suggest.es.search.EsDynSearchStatic
import io.suggest.model.n2.media.search.MMediaSearch
import io.suggest.model.n2.media.storage.{IMediaStorage, IMediaStorages}
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 16:31
 * Description: Модель инфы о загруженных и производных медифайлах: картинки, видео, аудио и т.д.
 * Вообще теоретически тут могут описываться любые файлы.
 *
 * Модель создана по мотивам m_media. Имя файла вынесено в MNode.meta.name.
 * Поле _id должно формироваться клиентом и включать в себя значение поля nodeId.
 */
@Singleton
class MMedias @Inject() (
                          iMediaStorages          : IMediaStorages,
                          override val mCommonDi  : IEsModelDiVal
                        )
  extends EsModelStatic
  with EsmV2Deserializer
  with MacroLogsImpl
  with EsModelJsonWrites
  with EsDynSearchStatic[MMediaSearch]
{ that =>

  import iMediaStorages.FORMAT

  override type T = MMedia

  override val ES_TYPE_NAME = "media"

  @deprecated("use deserializeOne2() instead", "2015.sep.27")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    throw new UnsupportedOperationException("deprecated api not implemented")
  }

  /** Поддержка JSON для сериализации-десериализации тела документа elasticsearch. */
  val FORMAT_DATA: OFormat[T] = {
    val F = MMediaFields
    (
      (__ \ F.NODE_ID_FN).format[String] and
      (__ \ F.FileMeta.FILE_META_FN).format[MFileMeta] and
      (__ \ F.Storage.STORAGE_FN).format[IMediaStorage] and
      (__ \ F.PictureMeta.PICTURE_META_FN).formatNullable[MPictureMeta]
        .inmap[MPictureMeta](
          EmptyUtil.opt2ImplMEmptyF(MPictureMeta),
          EmptyUtil.implEmpty2OptF[MPictureMeta]
        )
    )(
      {(nodeId, fileMeta, storage, pictureMeta) =>
        MMedia(
          nodeId    = nodeId,
          file      = fileMeta,
          storage   = storage,
          picture   = pictureMeta,
          id        = None
        )
      },
      {mmedia =>
        (mmedia.nodeId, mmedia.file, mmedia.storage, mmedia.picture)
      }
    )
  }

  override def esDocWrites = FORMAT_DATA
  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    FORMAT_DATA
      .map { _.withDocMeta(meta) }
  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    val F = MMediaFields
    List(
      FieldKeyword(F.NODE_ID_FN, index = true, include_in_all = true),
      FieldObject(F.FileMeta.FILE_META_FN, enabled = true, properties = MFileMeta.generateMappingProps),
      FieldObject(F.Storage.STORAGE_FN, enabled = true, properties = iMediaStorages.generateMappingProps),
      FieldObject(F.PictureMeta.PICTURE_META_FN, enabled = true, properties = MPictureMeta.generateMappingProps)
    )
  }

}


object MMedia {

  /**
   * Сборка id'шников для экземпляров модели.
   *
   * @param imgNodeId id ноды картинки.
   * @param qOpt Опциональный qualifier. Обычно None, если это файл-оригинал.
   *             Some() если хранится дериватив.
   * @return Строка для поля _id.
   */
  def mkId(imgNodeId: String, qOpt: Option[String]): String = {
    qOpt.fold(imgNodeId)(imgNodeId + "?" + _)
  }

}


/** Класс модели mmedia, хранящей инфу о файле.
  *
  * @param nodeId id узла.
  * @param file Данные по файлу.
  * @param storage Хранилище.
  * @param id id записи.
  * @param picture Метаданные картинки, если это картинка.
  * @param versionOpt Версия.
  */
case class MMedia(
  nodeId                    : String,
  file                      : MFileMeta,
  storage                   : IMediaStorage,
  override val id           : Option[String],
  picture                   : MPictureMeta          = MPictureMeta.empty,
  override val versionOpt   : Option[Long]          = None
)
  extends EsModelT
  with EsModelVsnedT[MMedia]
{

  def withDocMeta(dmeta: IEsDocMeta)          : MMedia  = copy(id = dmeta.id, versionOpt = dmeta.version)

  def withStorage(storage: IMediaStorage)     : MMedia  = copy(storage = storage)
  def withId(id: Option[String])              : MMedia  = copy(id = id)
  def withPicture(picture: MPictureMeta)      : MMedia  = copy(picture = picture)
  override def withVersion(versionOpt: Option[Long]): MMedia = copy(versionOpt = versionOpt)

}


// Поддержка JMX.
trait MMediasJmxMBean extends EsModelJMXMBeanI

final class MMediasJmx @Inject()(
  override val companion  : MMedias,
  override val ec         : ExecutionContext
)
  extends EsModelJMXBaseImpl
  with MMediasJmxMBean
{
  override type X = MMedia
}

/** Интерфейс для поля с DI-инстансом [[MMedias]]. */
trait IMMedias {
  def mMedias: MMedias
}

