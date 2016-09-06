package io.suggest.model.n2.media

import com.google.inject.{Singleton, Inject}
import io.suggest.model.es._
import io.suggest.model.n2.media.storage.{IMediaStorages, IMediaStorage}
import io.suggest.util.MacroLogsImpl
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.collection.Map
import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 16:31
 * Description: Модель инфы о загруженных и производных медифайлах: картинки, видео, аудио и т.д.
 * Вообще теоретически тут могут описываться быть любые файлы.
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
{ that =>

  import iMediaStorages.FORMAT

  override type T = MMedia

  override val ES_TYPE_NAME = "media"

  val NODE_ID_FN      = "ni"
  val FILE_META_FN    = "fm"
  val PICTURE_META_FN = "pm"
  val STORAGE_FN      = "st"

  @deprecated("use deserializeOne2() instead", "2015.sep.27")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    throw new UnsupportedOperationException("deprecated api not implemented")
  }

  /** Поддержка JSON для сериализации-десериализации тела документа elasticsearch. */
  val FORMAT_DATA: OFormat[T] = (
    (__ \ NODE_ID_FN).format[String] and
    (__ \ FILE_META_FN).format[MFileMeta] and
    (__ \ STORAGE_FN).format[IMediaStorage] and
    (__ \ PICTURE_META_FN).formatNullable[MPictureMeta]
  )(
    {(nodeId, fileMeta, storage, pictureMetaOpt) =>
      MMedia(
        nodeId    = nodeId,
        file      = fileMeta,
        storage   = storage,
        picture   = pictureMetaOpt,
        id        = None
      )
    },
    {mmedia =>
      (mmedia.nodeId, mmedia.file, mmedia.storage, mmedia.picture)
    }
  )

  override def esDocWrites = FORMAT_DATA
  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    FORMAT_DATA
      .map { _.withDocMeta(meta) }
  }


  import io.suggest.util.SioEsUtil._

  override def generateMappingStaticFields: List[Field] = {
    List(
      FieldAll(enabled = false),
      FieldSource(enabled = true)
    )
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NODE_ID_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),
      FieldObject(FILE_META_FN, enabled = true, properties = MFileMeta.generateMappingProps),
      FieldObject(STORAGE_FN, enabled = true, properties = iMediaStorages.generateMappingProps),
      FieldObject(PICTURE_META_FN, enabled = true, properties = MPictureMeta.generateMappingProps)
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

case class MMedia(
  nodeId                    : String,
  file                      : MFileMeta,
  storage                   : IMediaStorage,
  override val id           : Option[String],
  picture                   : Option[MPictureMeta]  = None,
  override val versionOpt   : Option[Long]          = None
)
  extends EsModelT
{

  def withDocMeta(dmeta: IEsDocMeta): MMedia = {
    copy(id = dmeta.id, versionOpt = dmeta.version)
  }

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

