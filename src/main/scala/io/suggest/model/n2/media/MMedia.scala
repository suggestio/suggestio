package io.suggest.model.n2.media

import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model._
import io.suggest.util.MacroLogsImpl
import org.elasticsearch.client.Client
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
object MMedia
  extends EsModelStaticT
  with EsmV2Deserializer
  with MacroLogsImpl
  with IEsDocJsonWrites
{

  override type T = MMedia

  override val ES_TYPE_NAME = "media"

  val NODE_ID_FN      = "ni"
  val FILE_META_FN    = "fm"
  val PICTURE_META_FN = "pm"

  @deprecated("use deserializeOne2() instead", "2015.sep.27")
  override def deserializeOne(id: Option[String], m: Map[String, AnyRef], version: Option[Long]): T = {
    throw new UnsupportedOperationException("deprecated api not implemented")
  }

  /** Поддержка JSON для сериализации-десериализации тела документа elasticsearch. */
  val FORMAT_DATA: OFormat[T] = (
    (__ \ NODE_ID_FN).format[String] and
    (__ \ FILE_META_FN).format[MFileMeta] and
    (__ \ PICTURE_META_FN).formatNullable[MPictureMeta]
  )(
    {(nodeId, fileMeta, pictureMetaOpt) =>
      apply(nodeId, fileMeta, pictureMetaOpt, id = None)
    },
    {mmedia =>
      (mmedia.nodeId, mmedia.file, mmedia.picture)
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
      FieldObject(PICTURE_META_FN, enabled = true, properties = MPictureMeta.generateMappingProps)
    )
  }

}


case class MMedia(
  nodeId                    : String,
  file                      : MFileMeta,
  picture                   : Option[MPictureMeta] = None,
  override val id           : Option[String],
  override val versionOpt   : Option[Long]    = None
)
  extends EsModelT
  with EsModelJsonWrites
{

  override type T = MMedia
  override def companion = MMedia

  def withDocMeta(dmeta: IEsDocMeta): T = {
    copy(id = dmeta.id, versionOpt = dmeta.version)
  }

}


// Поддержка JMX.
trait MMediaJmxMBean extends EsModelJMXMBeanI
final class MMediaJmx(implicit val ec: ExecutionContext, val client: Client, val sn: SioNotifierStaticClientI)
  extends EsModelJMXBase
  with MMediaJmxMBean
{
  override def companion = MMedia
}
