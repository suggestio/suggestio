package models

import io.suggest.ym.model.common.LogoImgOptI
import models.im.DevScreen
import play.api.mvc.{Call, QueryStringBindable}
import util.cdn.CdnUtil
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 14:22
 * Description: Контейнеры для аргументов, передаваемых в компоненты showcase.
 */
object SMShowcaseReqArgs {

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]], devScreenB: QueryStringBindable[Option[DevScreen]]) = {
    new QueryStringBindable[SMShowcaseReqArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SMShowcaseReqArgs]] = {
        for {
          maybeGeo        <- strOptB.bind(key + ".geo", params)
          maybeDevScreen  <- devScreenB.bind(key + ".screen", params)
        } yield {
          Right(SMShowcaseReqArgs(
            geo  = GeoMode.maybeApply(maybeGeo)
              .filter(_.isWithGeo)
              .getOrElse(GeoIp),
            screen = maybeDevScreen     // Игнорим неверные размеры, ибо некритично.
          ))
        }
      }

      override def unbind(key: String, value: SMShowcaseReqArgs): String = {
        List(
          strOptB.unbind(key + ".geo", value.geo.toQsStringOpt),
          devScreenB.unbind(key + ".screen", value.screen)
        )
          .filter { us => !us.isEmpty }
          .mkString("&")
      }
    }
  }

  val empty = SMShowcaseReqArgs()

}

case class SMShowcaseReqArgs(
  geo: GeoMode = GeoNone,
  screen: Option[DevScreen] = None
) {
  override def toString: String = s"${geo.toQsStringOpt.map { "geo=" + _ }}"
}



/** Статическая утиль для аргументов рендера showcase-шаблонов. */
object SMShowcaseRenderArgs {

  /** Регэксп для нахождения первого словесного символа в строке. */
  val NON_PUNCTUATION_CHAR = "(?U)\\w".r

  /** Найти первую словесную букву. */
  def firstWordChar(str: String): Char = {
    // TODO Может надо использовать Character.isLetterOrDigit()?
    NON_PUNCTUATION_CHAR.findFirstIn(str).get.charAt(0)
  }

  /**
   * При сортировке по символам используется space-префикс для русских букв, чтобы они были сверху.
   * @param c Символ.
   * @return Строка.
   */
  private def russianFirst(c: Char): String = {
    val sb = new StringBuilder(2)
    if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC) {
      sb.append(' ')
    }
    sb.append(c).toString()
  }

}

import SMShowcaseRenderArgs._

/**
 * Аргументы для рендера market/showcase/indexTpl.
 * Экстендим LogoImgOptI чтобы компилятор был в курсе изменений API полей логотипов в sioutil.
 * @param bgColor Используемый цвет выдачи.
 * @param mmcats Категории для отображения.
 * @param catsStats Статистика по категориям.
 * @param spsr Поисковый запрос.
 * @param oncloseHref Абсолютный URL перехода при закрытии выдачи.
 * @param logoImgOpt Логотип, если есть.
 * @param shops Список магазинов в торговом центре.
 * @param welcomeOpt Приветствие, если есть.
 */
case class SMShowcaseRenderArgs(
  bgColor       : String,
  fgColor       : String,
  name          : String,
  mmcats        : Seq[MMartCategory],
  catsStats     : Map[String, Long],
  spsr          : AdSearch,
  oncloseHref   : String,
  geoListGoBack : Option[Boolean] = None,
  logoImgOpt    : Option[MImgInfoT] = None,
  shops         : Map[String, MAdnNode] = Map.empty,
  welcomeOpt    : Option[WelcomeRenderArgsT] = None,
  searchInAdnId : Option[String] = None
) extends LogoImgOptI {

  /** Генерация списка групп рекламодателей по первым буквам. */
  lazy val shopsLetterGrouped = {
    shops
      .values
      // Сгруппировать по первой букве или цифре.
      .groupBy { node =>
        val firstChar = firstWordChar(node.meta.nameShort)
        java.lang.Character.toUpperCase(firstChar)
      }
      // Отсортировать ноды по названиям в рамках группы.
      .mapValues { nodes =>
        nodes.toSeq.sortBy(_.meta.nameShort.toLowerCase)
      }
      .toSeq
      // Отсортировать по первой букве группы, но русские -- сверху.
      .sortBy { case (c, _) => russianFirst(c) }
      .zipWithIndex
  }

  /** Абсолютная ссылка на логотип для рендера. */
  def logoImgUrl(implicit ctx: Context): String = {
    val path = logoImgOpt.fold {
      CdnUtil.asset("images/market/showcase-logo.png")
    } { logoImg =>
      CdnUtil.dynImg(logoImg.filename)
    }
    util.Context.MY_AUDIENCE_URL + path
  }

}


/** Данные по рендеру приветствия. */
trait WelcomeRenderArgsT {

  /** Фон. Либо Left(цвет), либо Right(инфа по картинке). */
  def bg: Either[String, ImgUrlInfoT]

  def fgImage: Option[MImgInfoT]

  /** Текст, который надо отобразить. Изначально использовался, когда нет fgImage. */
  def fgText: Option[String]
}



/**
 * Набор аргументов для передачи в demoWebSiteTpl.
 * @param bgColor Цвет оформления.
 * @param showcaseCall Адрес для showcase
 * @param title Заголовок.
 * @param adnId id узла, в рамках которого орудуем.
 */
case class SMDemoSiteArgs(
  bgColor       : String,
  showcaseCall  : Call,
  adnId         : Option[String],
  title         : Option[String] = None
) {
  // Имитируем поведение параметра, чтобы в будущем не рисовать костыли в коде шаблонов.
  def withGeo = adnId.isEmpty
}



object JsShowCaseState {
  
  val ADN_ID_FN               = "mart_id"
  val CAT_SCR_OPENED_FN       = "cat_screen.is_opened"
  val GEO_SCR_OPENED_FN       = "geo_screen.is_opened"
  val FADS_SCREEN_OPENED_FN   = "fads.is_opened"

  def qsbStandalone: QueryStringBindable[JsShowCaseState] = {
    import QueryStringBindable._
    qsb
  }

  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]], boolOptB: QueryStringBindable[Option[Boolean]]) = {
    new QueryStringBindable[JsShowCaseState] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, JsShowCaseState]] = {
        for {
          maybeAdnId            <- strOptB.bind(ADN_ID_FN, params)
          maybeCatScreenOpened  <- boolOptB.bind(CAT_SCR_OPENED_FN, params)
          maybeGeoScreenOpened  <- boolOptB.bind(GEO_SCR_OPENED_FN, params)
          maybeFadsOpened       <- boolOptB.bind(FADS_SCREEN_OPENED_FN, params)
        } yield {
          val res = JsShowCaseState(
            adnId               = maybeAdnId,
            catScreenOpenedOpt  = maybeCatScreenOpened,
            geoScreenOpenedOpt  = maybeGeoScreenOpened,
            fadsOpenedOpt       = maybeFadsOpened
          )
          Right(res)
        }
      }

      override def unbind(key: String, value: JsShowCaseState): String = {
        List(
          strOptB.unbind(ADN_ID_FN, value.adnId),
          boolOptB.unbind(CAT_SCR_OPENED_FN, Some(value.catScreenOpened)),
          boolOptB.unbind(GEO_SCR_OPENED_FN, Some(value.geoScreenOpened)),
          boolOptB.unbind(FADS_SCREEN_OPENED_FN, Some(value.fadsOpened))
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }


  implicit private def orFalse(boolOpt: Option[Boolean]): Boolean = {
    if (boolOpt.isDefined)
      boolOpt.get
    else
      false
  }

}


/** Класс, отражающий состояние js-выдачи на клиенте. */
case class JsShowCaseState(
  adnId              : Option[String]  = None,
  catScreenOpenedOpt : Option[Boolean] = None,
  geoScreenOpenedOpt : Option[Boolean] = None,
  fadsOpenedOpt      : Option[Boolean] = None
) {

  import JsShowCaseState.orFalse

  def catScreenOpened : Boolean = catScreenOpenedOpt
  def geoScreenOpened : Boolean = geoScreenOpenedOpt
  def fadsOpened      : Boolean = fadsOpenedOpt

}

