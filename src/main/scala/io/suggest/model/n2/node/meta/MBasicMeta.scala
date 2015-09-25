package io.suggest.model.n2.node.meta

import io.suggest.model.EsModel.Implicits.jodaDateTimeFormat
import io.suggest.model.IGenEsMappingProps
import org.joda.time.DateTime
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
  
  // Название под-поля.
  val NOT_TOKENIZED_SUBFIELD_SUF = "nt"

  val NAME_FN                 = "n"
  val NAME_SHORT_FN           = "ns"    // до созданию multifield было имя "ns"
  val NAME_SHORT_NOTOK_FN     = NAME_SHORT_FN + "." + NOT_TOKENIZED_SUBFIELD_SUF
  val HIDDEN_DESCR_FN         = "hd"
  val DATE_CREATED_FN         = "dc"
  val DATE_EDITED_FN          = "de"
  val LANGS_ESFN              = "l"

  /** Поддержка JSON в модели. */
  implicit val FORMAT: OFormat[MBasicMeta] = (
    (__ \ NAME_FN).formatNullable[String] and
    (__ \ NAME_SHORT_FN).formatNullable[String] and
    (__ \ HIDDEN_DESCR_FN).formatNullable[String] and
    (__ \ DATE_CREATED_FN).format(jodaDateTimeFormat) and
    (__ \ DATE_EDITED_FN).formatNullable(jodaDateTimeFormat) and
    (__ \ LANGS_ESFN).formatNullable[List[String]]
      .inmap[List[String]](
        { _ getOrElse Nil },
        {ls => if (ls.isEmpty) None else Some(ls)}
      )
  )(apply, unlift(unapply))

  import io.suggest.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    List(
      FieldString(NAME_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
      // 2014.oct.01: Разделение поля на analyzed и not_analyzed. Последнее нужно для сортировки.
      FieldString(NAME_SHORT_FN, index = FieldIndexingVariants.analyzed, include_in_all = true, fields = Seq(
        FieldString(NOT_TOKENIZED_SUBFIELD_SUF, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
      )),
      FieldString(HIDDEN_DESCR_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_CREATED_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldDate(DATE_EDITED_FN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(LANGS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )
  }

}


/** Все экземпляры модели принадлежат к этому классу. */
case class MBasicMeta(
  nameOpt       : Option[String] = None,
  nameShortOpt  : Option[String] = None,
  hiddenDescr   : Option[String] = None,
  dateCreated   : DateTime = DateTime.now,
  dateEdited    : Option[DateTime] = None,
  langs         : List[String]   = Nil
)
  extends MBasicMetaUtil


/** Утиль для basic-meta моделей: текущей и legacy [[MNodeMeta]]. */
trait MBasicMetaUtil {

  def nameOpt: Option[String]
  def langs: Seq[String]
  def nameShortOpt: Option[String]

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

}
