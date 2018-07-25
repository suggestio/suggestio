package io.suggest.model.n2.node.meta

import java.time.OffsetDateTime

import io.suggest.es.model.IGenEsMappingProps
import io.suggest.model.PrefixedFn
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

object MBasicMeta extends IGenEsMappingProps {

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

  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldText(NAME_FN, index = true, include_in_all = true),
      // 2014.oct.01: Разделение поля на analyzed и not_analyzed. Последнее нужно для сортировки.
      FieldText(NAME_SHORT_FN, index = true, include_in_all = true, fields = Seq(
        FieldKeyword(NOTOK_SUF, index = true, include_in_all = true)
      )),
      FieldText(TECHNICAL_NAME_FN, index = false, include_in_all = false),
      FieldText(HIDDEN_DESCR_FN, index = false, include_in_all = false),
      FieldDate(DATE_CREATED_FN, index = true, include_in_all = false),
      FieldDate(DATE_EDITED_FN, index = true, include_in_all = false),
      FieldKeyword(LANGS_ESFN, index = true, include_in_all = false)
    )
  }

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
