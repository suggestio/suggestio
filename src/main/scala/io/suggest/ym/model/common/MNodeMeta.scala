package io.suggest.ym.model.common

import io.suggest.model.es.{IGenEsMappingProps, EsModelUtil}
import io.suggest.model.n2.node.meta._
import io.suggest.model.n2.node.meta.colors.{MColorData, MColors}
import io.suggest.util.SioEsUtil.FieldIndexingVariants.FieldIndexingVariant
import io.suggest.util.SioEsUtil._
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 19:39
 * Description: Модель контейнера метаданных одного узла [[io.suggest.model.n2.node.MNode]].
 */

object MNodeMeta extends IGenEsMappingProps {

  // Название под-поля.
  val NOT_TOKENIZED_SUBFIELD_SUF = "nt"

  val NAME_FN               = "name"
  val NAME_SHORT_FN         = "sn"    // до созданию multifield было имя "ns"
  val NAME_SHORT_NOTOK_FN   = NAME_SHORT_FN + "." + NOT_TOKENIZED_SUBFIELD_SUF
  val TOWN_FN               = "town"
  val ADDRESS_FN            = "address"
  val DATE_CREATED_FN       = "dateCreated"
  val PHONE_FN              = "phone"
  val SITE_URL_FN           = "siteUrl"
  val HIDDEN_DESCR_FN       = "description"
  val AUDIENCE_DESCR_FN     = "audDescr"
  val HUMAN_TRAFFIC_AVG_FN  = "htAvg"
  val INFO_FN               = "info"
  val BG_COLOR_FN           = "color"
  val FG_COLOR_FN           = "fgColor"
  val WELCOME_AD_ID_FN         = "welcomeAdId"
  val FLOOR_FN              = "floor"
  val SECTION_FN            = "section"
  val LANGS_FN              = "lang"
  val PERSON_FN             = "p"


  val empty = MNodeMeta()


  private def _fieldString(fn: String, iia: Boolean = true, index: FieldIndexingVariant = FieldIndexingVariants.no) = {
    FieldString(fn, include_in_all = iia, index = index)
  }

  override def generateMappingProps: List[DocField] = List(
    FieldString(NAME_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    // 2014.oct.01: Разделение поля на analyzed и not_analyzed. Последнее нужно для сортировки.
    FieldString(NAME_SHORT_FN, index = FieldIndexingVariants.analyzed, include_in_all = true, fields = Seq(
      FieldString(NOT_TOKENIZED_SUBFIELD_SUF, index = FieldIndexingVariants.not_analyzed, include_in_all = true)
    )),
    _fieldString(HIDDEN_DESCR_FN, iia = true),
    FieldDate(DATE_CREATED_FN, index = FieldIndexingVariants.no, include_in_all = false),
    // legal
    FieldString(TOWN_FN, index = FieldIndexingVariants.analyzed, include_in_all = true),
    _fieldString(ADDRESS_FN, iia = true),
    _fieldString(PHONE_FN, iia = true),
    FieldString(FLOOR_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = true),   // Внезапно, вдруг кто-то захочет найти все магазины на первом этаже.
    _fieldString(SECTION_FN, iia = true),
    _fieldString(SITE_URL_FN),
    // 2014.06.30: Рекламные характеристики узла.
    _fieldString(AUDIENCE_DESCR_FN),
    FieldNumber(HUMAN_TRAFFIC_AVG_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.analyzed, include_in_all = false),
    FieldString(INFO_FN, index = FieldIndexingVariants.no, include_in_all = false),
    // Перемещено из visual - TODO Перенести в conf.
    FieldString(BG_COLOR_FN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(FG_COLOR_FN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(WELCOME_AD_ID_FN, index = FieldIndexingVariants.no, include_in_all = false),
    FieldString(LANGS_FN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
    FieldObject(PERSON_FN, enabled = true, properties = MPersonMeta.generateMappingProps)
  )

  /** JSON сериализатор-десериализатор на базе play.json. */
  implicit val FORMAT: Format[MNodeMeta] = (
    (__ \ NAME_FN).formatNullable[String] and
    (__ \ NAME_SHORT_FN).formatNullable[String] and
    (__ \ HIDDEN_DESCR_FN).formatNullable[String] and
    (__ \ DATE_CREATED_FN).format(EsModelUtil.Implicits.jodaDateTimeFormat) and
    (__ \ TOWN_FN).formatNullable[String] and
    (__ \ ADDRESS_FN).formatNullable[String] and
    (__ \ PHONE_FN).formatNullable[String] and
    (__ \ FLOOR_FN).formatNullable[String] and
    (__ \ SECTION_FN).formatNullable[String] and
    (__ \ SITE_URL_FN).formatNullable[String] and
    (__ \ AUDIENCE_DESCR_FN).formatNullable[String] and
    (__ \ HUMAN_TRAFFIC_AVG_FN).formatNullable[Int] and
    (__ \ INFO_FN).formatNullable[String] and
    (__ \ BG_COLOR_FN).formatNullable[String] and
    (__ \ FG_COLOR_FN).formatNullable[String] and
    (__ \ WELCOME_AD_ID_FN).formatNullable[String] and
    (__ \ LANGS_FN).formatNullable[List[String]]
      .inmap[List[String]](
        { _ getOrElse Nil },
        {ls => if (ls.isEmpty) None else Some(ls)}
      ) and
    (__ \ PERSON_FN).formatNullable[MPersonMeta]
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
{

  /** Приведение к модели [[MMeta]]. */
  lazy val toMMeta: MMeta = {
    MMeta(
      basic = MBasicMeta(
        nameOpt       = nameOpt,
        nameShortOpt  = nameShortOpt,
        hiddenDescr   = hiddenDescr,
        dateCreated   = dateCreated,
        langs         = langs
      ),
      address = MAddress(
        town      = town,
        address   = address,
        phone     = phone,
        floor     = floor,
        section   = section
      ),
      business = MBusinessInfo(
        siteUrl           = siteUrl,
        audienceDescr     = audienceDescr,
        humanTrafficAvg   = humanTrafficAvg,
        info              = info
      ),
      colors = MColors(
        bg = color.map { MColorData(_) },
        fg = fgColor.map { MColorData(_) }
      ),
      person = person
    )
  }

}
