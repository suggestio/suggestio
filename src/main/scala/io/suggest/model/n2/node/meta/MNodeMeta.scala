package io.suggest.model.n2.node.meta

import org.joda.time.DateTime
import io.suggest.model.{IGenEsMappingProps, EsModel}
import io.suggest.util.SioEsUtil._, FieldIndexingVariants.FieldIndexingVariant
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 19:39
 * Description: Модель контейнера метаданных одного узла [[io.suggest.model.n2.node.MNode]].
 */

object MNodeMeta extends IGenEsMappingProps {

  // Название под-поля.
  val NOT_TOKENIZED_SUBFIELD_SUF = "nt"

  val NAME_ESFN               = "name"
  val NAME_SHORT_ESFN         = "sn"    // до созданию multifield было имя "ns"
  val NAME_SHORT_NOTOK_ESFN   = NAME_SHORT_ESFN + "." + NOT_TOKENIZED_SUBFIELD_SUF
  val TOWN_ESFN               = "town"
  val ADDRESS_ESFN            = "address"
  val DATE_CREATED_ESFN       = "dateCreated"
  val PHONE_ESFN              = "phone"
  val SITE_URL_ESFN           = "siteUrl"
  val HIDDEN_DESCR_ESFN       = "description"
  val AUDIENCE_DESCR_ESFN     = "audDescr"
  val HUMAN_TRAFFIC_AVG_ESFN  = "htAvg"
  val INFO_ESFN               = "info"
  val BG_COLOR_ESFN           = "color"
  val FG_COLOR_ESFN           = "fgColor"
  val WELCOME_AD_ID           = "welcomeAdId"
  val FLOOR_ESFN              = "floor"
  val SECTION_ESFN            = "section"
  val LANGS_ESFN              = "lang"
  val PERSON_ESFN             = "p"


  val empty = MNodeMeta()


  private def _fieldString(fn: String, iia: Boolean = true, index: FieldIndexingVariant = FieldIndexingVariants.no) = {
    FieldString(fn, include_in_all = iia, index = index)
  }

  override def generateMappingProps: List[DocField] = List(
    FieldString(NAME_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    // 2014.oct.01: Разделение поля на analyzed и not_analyzed. Последнее нужно для сортировки.
    FieldString(NAME_SHORT_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true, fields = Seq(
      FieldString(NOT_TOKENIZED_SUBFIELD_SUF, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
    )),
    _fieldString(HIDDEN_DESCR_ESFN, iia = true),
    FieldDate(DATE_CREATED_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    // legal
    FieldString(TOWN_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    _fieldString(ADDRESS_ESFN, iia = true),
    _fieldString(PHONE_ESFN, iia = true),
    FieldString(FLOOR_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),   // Внезапно, вдруг кто-то захочет найти все магазины на первом этаже.
    _fieldString(SECTION_ESFN, iia = true),
    _fieldString(SITE_URL_ESFN),
    // 2014.06.30: Рекламные характеристики узла.
    _fieldString(AUDIENCE_DESCR_ESFN),
    FieldNumber(HUMAN_TRAFFIC_AVG_ESFN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.analyzed, include_in_all = false),
    FieldString(INFO_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    // Перемещено из visual - TODO Перенести в conf.
    FieldString(BG_COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(FG_COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(WELCOME_AD_ID, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(LANGS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldObject(PERSON_ESFN, enabled = true, properties = MPersonMeta.generateMappingProps)
  )

  /** JSON сериализатор-десериализатор на базе play.json. */
  implicit val FORMAT: Format[MNodeMeta] = (
    (__ \ NAME_ESFN).formatNullable[String] and
    (__ \ NAME_SHORT_ESFN).formatNullable[String] and
    (__ \ HIDDEN_DESCR_ESFN).formatNullable[String] and
    (__ \ DATE_CREATED_ESFN).format(EsModel.Implicits.jodaDateTimeFormat) and
    (__ \ TOWN_ESFN).formatNullable[String] and
    (__ \ ADDRESS_ESFN).formatNullable[String] and
    (__ \ PHONE_ESFN).formatNullable[String] and
    (__ \ FLOOR_ESFN).formatNullable[String] and
    (__ \ SECTION_ESFN).formatNullable[String] and
    (__ \ SITE_URL_ESFN).formatNullable[String] and
    (__ \ AUDIENCE_DESCR_ESFN).formatNullable[String] and
    (__ \ HUMAN_TRAFFIC_AVG_ESFN).formatNullable[Int] and
    (__ \ INFO_ESFN).formatNullable[String] and
    (__ \ BG_COLOR_ESFN).formatNullable[String] and
    (__ \ FG_COLOR_ESFN).formatNullable[String] and
    (__ \ WELCOME_AD_ID).formatNullable[String] and
    (__ \ LANGS_ESFN).formatNullable[List[String]]
      .inmap[List[String]](
        { _ getOrElse Nil },
        {ls => if (ls.isEmpty) None else Some(ls)}
      ) and
    (__ \ PERSON_ESFN).formatNullable[MPersonMeta]
      .inmap[MPersonMeta](
        { _ getOrElse MPersonMeta.empty },
        { mpm => if (mpm.isEmpty) None else Some(mpm) }
      )
  )(apply, unlift(unapply))

}


/**
 * Экземпляр контейнера метаданных узла.
 * @param nameOpt Отображаемое имя/название, если есть.
 * @param nameShortOpt Необязательно короткое имя узла.
 * @param hiddenDescr Скрытое описание, задаётся и читается только в /sys/.
 * @param town Город.
 * @param address Адрес в городе.
 * @param phone Телефонный номер.
 * @param floor Этаж.
 * @param section Номер секции/павильона/кабинета/помещения и т.д.
 * @param siteUrl Ссылка на сайт.@param dateCreated
 * @param color Цвет оформления.
 * @param fgColor Цвет элементов переднего плана. Должен контрастировать с цветом оформления.
 * @param welcomeAdId id карточки приветствия в [[io.suggest.ym.model.MWelcomeAd]].
 * @param info Описание бизнес-стороны узла. Например, описание товаров и услуг.
 */
case class MNodeMeta(
  // Класс обязательно immutable! Никаких var, ибо companion.DEFAULT.
  nameOpt       : Option[String] = None,
  nameShortOpt  : Option[String] = None,
  hiddenDescr   : Option[String] = None,
  dateCreated   : DateTime = DateTime.now,
  // перемещено из legal
  town          : Option[String] = None,
  address       : Option[String] = None,
  phone         : Option[String] = None,
  floor         : Option[String] = None,
  section       : Option[String] = None,
  siteUrl       : Option[String] = None,
  // 2014.06.30: Рекламные характеристики узла-producer'а.
  audienceDescr : Option[String] = None,
  humanTrafficAvg: Option[Int]   = None,
  info          : Option[String] = None,
  // перемещено из visual
  // TODO Нужно цвета объеденить в карту цветов.
  color         : Option[String] = None,
  fgColor       : Option[String] = None,
  welcomeAdId   : Option[String] = None,   // TODO Перенести в поле MAdnNode.conf.welcomeAdId
  langs         : List[String]   = Nil,
  person        : MPersonMeta    = MPersonMeta.empty
)
  extends MBasicMetaUtil

