package models.mcal

import javax.inject.{Inject, Singleton}
import io.suggest.cal.m.{MCalType, MCalTypes}
import io.suggest.es.MappingDsl
import io.suggest.es.model._
import io.suggest.util.logs.MacroLogsImpl
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.05.14 18:36
 * Description: Модель для хранения календарей в текстовых форматах.
 */
@deprecated("MNodes and MNode().extras.calendar", "2021-05-20")
@Singleton
final class MCalendars
  extends EsModelStatic
  with MacroLogsImpl
  with EsmV2Deserializer
  with EsModelJsonWrites
{

  override type T = MCalendar


  override def ES_INDEX_NAME = "sio.main.v5"
  override def ES_TYPE_NAME = "holyCal"

  /** Идентификаторы полей на стороне elasticsearch. */
  object Fields {
    def NAME_FN       = "name"
    def DATA_FN       = "data"
    /** Идентификатор локализации календаря среди множества оных. */
    def CAL_TYPE_FN   = "ctype"
  }


  /** Сборка маппинга индекса по новому формату. */
  override def indexMapping(implicit dsl: MappingDsl): dsl.IndexMapping = {
    import dsl._
    val F = Fields
    IndexMapping(
      source = Some( FSource(someTrue) ),
      properties = Some( Json.obj(
        F.NAME_FN       -> FText.indexedJs,
        F.DATA_FN       -> FText.notIndexedJs,
        F.CAL_TYPE_FN   -> FKeyWord.indexedJs,
      ))
    )
  }

  // Кеш для частично-собранного десериализатора.
  private def DATA_FORMAT: OFormat[MCalendar] = {
    val F = Fields
    (
      (__ \ F.NAME_FN).format[String] and
      (__ \ F.DATA_FN).format[String] and
      (__ \ F.CAL_TYPE_FN).formatNullable[MCalType]
    )(
      (name, data, calType) => MCalendar(name, data, calType.orNull),
      mcal => (mcal.name, mcal.data, Some(mcal.calType) )
    )
  }

  /**
    * 2015.feb.10 появились идентификаторы локализации календарей.
    *
    * @param id id проблемного календаря.
    * @return Дефолтовый [[MCalType]].
    */
  private def _dfltCalType(id: Option[String]): MCalType = {
    val default = MCalTypes.default
    LOGGER.warn(s"No cal type defined for calendar[${id.orNull}], fallback to ${default.i18nCode}")
    default
  }

  override protected def esDocReads(meta: IEsDocMeta): Reads[T] = {
    for (mcal <- DATA_FORMAT) yield {
      mcal.copy(
        id          = meta.id,
        versionOpt  = meta.version,
        calType     = Option( mcal.calType ) getOrElse _dfltCalType(meta.id),
      )
    }
  }

  override def esDocWrites = DATA_FORMAT

}


@deprecated("MNode().extras.calendar", "2021-05-20")
final case class MCalendar(
                            name                    : String,
                            data                    : String,
                            calType                 : MCalType,
                            id                      : Option[String]  = None,
                            versionOpt              : Option[Long]    = None
                          )
  extends EsModelT
