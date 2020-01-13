package io.suggest.n2.node.meta

import java.time.OffsetDateTime

import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
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

    val NAME_FN             = "n"

    object NameShort extends PrefixedFn {
      val NAME_SHORT_FN       = "ns"
      override protected def _PARENT_FN = NAME_SHORT_FN
      def NOTOK_SUF           = "nt"
      def NAME_SHORT_NOTOK_FN = _fullFn( NOTOK_SUF )
    }

    val HIDDEN_DESCR_FN       = "hd"
    val TECHNICAL_NAME_FN     = "tn"
    val DATE_CREATED_FN       = "dci"
    val DATE_EDITED_FN        = "de"
    val LANGS_ESFN            = "l"
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
    (__ \ LANGS_ESFN).formatNullable[List[String]]
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

  val nameOpt  = GenLens[MBasicMeta](_.nameOpt)
  val techName = GenLens[MBasicMeta](_.techName)
  val langs    = GenLens[MBasicMeta](_.langs)
  val dateEdited = GenLens[MBasicMeta](_.dateEdited)

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
case class MBasicMeta(
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

  def withNameOpt(nameOpt: Option[String]) = copy(nameOpt = nameOpt)
  def withTechName(techName: Option[String] = None) = copy(techName = techName)

}
