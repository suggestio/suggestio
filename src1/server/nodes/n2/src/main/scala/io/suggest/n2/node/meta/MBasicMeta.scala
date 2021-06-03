package io.suggest.n2.node.meta

import java.time.OffsetDateTime
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import io.suggest.xplay.json.PlayJsonUtil
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.09.15 12:16
 * Description: Кусок MNodeMeta, содержащий метаданные, пригодные для любого узла:
 * названия, даты создания и редактирования, язык и т.д.
 * Эта meta-модель может иметь обязательные поля.
 */

object MBasicMeta extends IEsMappingProps {

  object Fields {

    val NAME_FN               = "name"

    object NameShort extends PrefixedFn {
      val NAME_SHORT_FN       = "shortName"
      override protected def _PARENT_FN = NAME_SHORT_FN
      def NOTOK_SUF           = "noTok"
      def NAME_SHORT_NOTOK_FN = _fullFn( NOTOK_SUF )
    }

    val TECHNICAL_NAME_FN     = "techName"
    val HIDDEN_DESCR_FN       = "hiddenDescr"
    val DATE_CREATED_FN       = "dateCreated"
    val DATE_EDITED_FN        = "dateEdited"
    val LANGS_FN              = "language"
  }


  import Fields._
  import Fields.NameShort._

  /** Поддержка JSON в модели. */
  implicit val FORMAT: OFormat[MBasicMeta] = (
    (__ \ NAME_FN).formatNullable[String] and
    (__ \ NAME_SHORT_FN).formatNullable[String] and
    (__ \ TECHNICAL_NAME_FN).formatNullable[String] and
    (__ \ HIDDEN_DESCR_FN).formatNullable[String] and
    (__ \ DATE_CREATED_FN).format[OffsetDateTime] and
    (__ \ DATE_EDITED_FN).formatNullable[OffsetDateTime] and
    (__ \ LANGS_FN).formatNullable[List[String]]
      .inmap[List[String]](
        { _ getOrElse Nil },
        {ls => if (ls.isEmpty) None else Some(ls)}
      )
  )(apply, unlift(unapply))

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    Json.obj(
      NAME_FN -> FText.indexedJs,
      NAME_SHORT_FN -> FText(
        index = someTrue,
        fields = Some( Json.obj(
          NOTOK_SUF -> FKeyWord.indexedJs,
        )),
      ),
      TECHNICAL_NAME_FN -> FText.notIndexedJs,
      HIDDEN_DESCR_FN -> FText.notIndexedJs,
      DATE_CREATED_FN -> FDate.indexedJs,
      DATE_EDITED_FN -> FDate.indexedJs,
    )
  }

  def nameOpt  = GenLens[MBasicMeta](_.nameOpt)
  def techName = GenLens[MBasicMeta](_.techName)
  def langs    = GenLens[MBasicMeta](_.langs)
  def dateEdited = GenLens[MBasicMeta](_.dateEdited)

}


/**
 * Все экземпляры модели принадлежат к этому классу.
 * @param nameOpt Индексируемое отображаемое имя узла. Например "Василеостровский район"
 * @param nameShortOpt Короткое индексируемое имя.
 *                     Например "Васильевский". Также это поле используется для name-сортировки.
 * @param techName Скрытое/техническое неиндексируемое имя. Бывают динамические имена,
 *                 которые не требуется индексировать.
 * @param hiddenDescr Скрытое описание. Выставляется и отображается только администрацией sio.
 * @param dateCreated Дата создания.
 * @param dateEdited Дата редактирования.
 * @param langs Названия языков.
 */
final case class MBasicMeta(
  nameOpt       : Option[String]          = None,
  nameShortOpt  : Option[String]          = None,
  techName      : Option[String]          = None,
  hiddenDescr   : Option[String]          = None,
  dateCreated   : OffsetDateTime          = OffsetDateTime.now(),
  dateEdited    : Option[OffsetDateTime]  = None,
  langs         : List[String]            = Nil
) {

  def guessDisplayName: Option[String] = {
    nameOpt
      .orElse( nameShortOpt )
      .orElse( techName )
  }

  def dateEditedOrCreated: OffsetDateTime = {
    dateEdited getOrElse dateCreated
  }

  /** Изначально поле name было обязательным. */
  def name: String = {
    nameOpt.getOrElse("")
  }

  /** Узнать основной язык узла. */
  // А не удалить ли это? По дефолту желательно иметь инглиш или какой-то дефолтовый язык из настроек.
  def lang: String = {
    langs.headOption.getOrElse("ru")
  }

  /** Выдать имя, по возможности короткое. */
  def nameShort: String = {
    if (nameShortOpt.isDefined)
      nameShortOpt.get
    else
      name
  }

}
