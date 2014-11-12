package models

import io.suggest.ym.model.common.LogoImgOptI
import models.im.DevScreen
import play.api.mvc.{Call, QueryStringBindable}
import play.twirl.api.HtmlFormat
import util.cdn.CdnUtil
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.08.14 14:22
 * Description: Контейнеры для аргументов, передаваемых в компоненты showcase.
 * 2014.nov.11: "Showcase" и "SMShowcase" в названиях классов сокращены до "Sc"
 */
object ScReqArgs {

  val GEO_SUF               = ".geo"
  val SCREEN_SUF            = ".screen"
  val NODES_SCR_OPENED_SUF  = ".nodesScrOpened"
  val SEARCH_SCR_OPENED_SUF = ".searchScrOpened"

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],
                   devScreenB: QueryStringBindable[Option[DevScreen]],
                   boolOptB: QueryStringBindable[Option[Boolean]] ) = {
    new QueryStringBindable[ScReqArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScReqArgs]] = {
        for {
          maybeGeo                <- strOptB.bind(key + GEO_SUF, params)
          maybeDevScreen          <- devScreenB.bind(key + SCREEN_SUF, params)
          maybeNodesScreenOpened  <- boolOptB.bind(key + NODES_SCR_OPENED_SUF, params)
          maybeSearchScreenOpened <- boolOptB.bind(key + SEARCH_SCR_OPENED_SUF, params)
        } yield {
          Right(new ScReqArgsDflt {
            override val geo = {
              GeoMode.maybeApply(maybeGeo)
                .filter(_.isWithGeo)
                .getOrElse(GeoIp)
            }
            // Игнорим неверные размеры, ибо некритично.
            override lazy val screen: Option[DevScreen] = maybeDevScreen
            override val nodesScreenOpened = maybeNodesScreenOpened.getOrElse(false)
            override val searchScreenOpened = maybeSearchScreenOpened.getOrElse(false)
          })
        }
      }

      override def unbind(key: String, value: ScReqArgs): String = {
        List(
          strOptB.unbind(key + GEO_SUF, value.geo.toQsStringOpt),
          devScreenB.unbind(key + SCREEN_SUF, value.screen),
          boolOptB.unbind(key + NODES_SCR_OPENED_SUF, Some(value.nodesScreenOpened).filter(identity)),
          boolOptB.unbind(key + SEARCH_SCR_OPENED_SUF, Some(value.searchScreenOpened).filter(identity))
        )
          .filter { us => !us.isEmpty }
          .mkString("&")
      }
    }
  }

  def empty: ScReqArgs = new ScReqArgsDflt {}

}


trait ScReqArgs {
  def geo                 : GeoMode
  def screen              : Option[DevScreen]
  def nodesScreenOpened   : Boolean
  def searchScreenOpened  : Boolean
  /** Заинлайненные отрендеренные элементы плитки. Передаются при внутренних рендерах, вне HTTP-запросов и прочего. */
  def inlineTiles         : Seq[HtmlFormat.Appendable]

  override def toString: String = {
    import QueryStringBindable._
    ScReqArgs.qsb.unbind("a", this)
  }
}
trait ScReqArgsDflt extends ScReqArgs {
  override def geo                  : GeoMode = GeoNone
  override def screen               : Option[DevScreen] = None
  override def nodesScreenOpened    = false
  override def searchScreenOpened   = false
  override def inlineTiles: Seq[HtmlFormat.Appendable] = Nil
}
/** Враппер [[ScReqArgs]] для имитации вызова copy(). */
trait ScReqArgsWrapper extends ScReqArgs {
  def reqArgsUnderlying: ScReqArgs
  override def geo                  = reqArgsUnderlying.geo
  override def screen               = reqArgsUnderlying.screen
  override def nodesScreenOpened    = reqArgsUnderlying.nodesScreenOpened
  override def searchScreenOpened   = reqArgsUnderlying.searchScreenOpened
  override def inlineTiles          = reqArgsUnderlying.inlineTiles
}


/** Статическая утиль для аргументов рендера showcase-шаблонов. */
object ScRenderArgs {

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

import ScRenderArgs._

/**
 * Аргументы для рендера market/showcase/indexTpl.
 * Экстендим LogoImgOptI чтобы компилятор был в курсе изменений API полей логотипов в sioutil.
 */
trait ScRenderArgs extends LogoImgOptI with ScReqArgs {
  /** bgColor Используемый цвет выдачи. */
  def bgColor       : String
  def fgColor       : String
  def name          : String
  /** Категории для отображения. */
  def mmcats        : Seq[MMartCategory]
  /** Статистика по категориям. */
  def catsStats     : Map[String, Long]
  /** Поисковый запрос. */
  def spsr          : AdSearch
  /** Абсолютный URL для выхода из выдачи через кнопку. */
  def onCloseHref   : String
  def geoListGoBack : Option[Boolean] = None
  /** Логотип, если есть. */
  def logoImgOpt    : Option[MImgInfoT] = None
  /** Список магазинов в торговом центре. */
  def shops         : Map[String, MAdnNode] = Map.empty
  /** Приветствие, если есть. */
  def welcomeOpt    : Option[WelcomeRenderArgsT] = None
  def searchInAdnId : Option[String] = None

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

  override def toString: String = {
    val sb = new StringBuilder(256, "req:")
    sb.append( super.toString )
      .append( ";render:" )
      .append("bgColor=").append(bgColor).append('&')
      .append("fgColor=").append(fgColor).append('&')
      .append("name=").append(name).append('&')
      .append("mmcats=[").append(mmcats.size).append(']').append('&')
      .append("catsStats=[").append(catsStats.size).append(']').append('&')
      .append("spsr=").append(spsr.toString).append('&')
      .append("onCloseHref='").append(onCloseHref).append('\'').append('&')
      .append("geoListGoBack").append(geoListGoBack.toString).append('&')
      .append("shops=[").append(shops.size).append(']').append('&')
    val _lio = logoImgOpt
    if (_lio.isDefined)
      sb.append("logoImg=").append(_lio.get.filename).append('&')
    val _waOpt = welcomeOpt
    if (_waOpt.isDefined)
      sb.append("welcome=").append(_waOpt.get.toString).append('&')
    val _searchInAdnId = searchInAdnId
    if (_searchInAdnId.isDefined)
      sb.append("searchInAdnId=").append(_searchInAdnId.get)
    sb.toString()
  }
}


/** Данные по рендеру приветствия. */
trait WelcomeRenderArgsT {

  /** Фон. Либо Left(цвет), либо Right(инфа по картинке). */
  def bg: Either[String, ImgUrlInfoT]

  def fgImage: Option[MImgInfoT]

  /** Текст, который надо отобразить. Изначально использовался, когда нет fgImage. */
  def fgText: Option[String]

  override def toString: String = {
    val sb = new StringBuilder(64, "bg=")
    bg match {
      case Right(ii) => sb.append(ii.call.url)
      case Left(color) => sb.append(color)
    }
    sb.append('&')
    val _fgi = fgImage
    if (_fgi.isDefined)
      sb.append("fgImage='").append(_fgi.get.filename).append('\'').append('&')
    val _fgt = fgText
    if (_fgt.isDefined)
      sb.append("fgText='").append(_fgt.get).append('\'')
    sb.toString()
  }
}



/** Контейнер для аргументов, передаваемых в demoWebSiteTpl. */
trait ScSiteArgs {
  /** Цвет оформления. */
  def bgColor       : String
  /** Адрес для showcase */
  def showcaseCall  : Call
  /** id узла, в рамках которого орудуем. */
  def adnId         : Option[String]
  /** Отображаемый заголовок. */
  def title         : Option[String] = None
  def withJsSc      : Boolean = true
  /** Инлайновый рендер индексной страницы выдачи. В параметре содержится отрендеренный HTML. */
  def inlineIndex   : Option[HtmlFormat.Appendable] = None

  // Имитируем поведение параметра, чтобы в будущем не рисовать костыли в коде шаблонов.
  def withGeo = adnId.isEmpty

  override def toString: String = {
    val sb = new StringBuilder(64)
    sb.append("bgColor=").append(bgColor).append('&')
      .append("showcaseCall=").append(showcaseCall).append('&')
      .append("title=").append(title).append('&')
      .append("withJsSc").append(withJsSc)
    if (adnId.isDefined)
      sb.append('&').append("adnId=").append(adnId)
    if (inlineIndex.isDefined)
      sb.append('&').append("inlineIndex=yes")
    sb.toString()
  }
}
/** Враппер для аргументов рендера "сайта" выдачи. */
trait ScSiteArgsWrapper extends ScSiteArgs {
  def _scSiteArgs: ScSiteArgs

  override def bgColor = _scSiteArgs.bgColor
  override def adnId = _scSiteArgs.adnId
  override def showcaseCall = _scSiteArgs.showcaseCall
  override def title = _scSiteArgs.title
  override def withJsSc = _scSiteArgs.withJsSc
  override def inlineIndex = _scSiteArgs.inlineIndex

  override def withGeo = _scSiteArgs.withGeo
  override def toString: String = _scSiteArgs.toString
}



object ScJsState {
  
  val ADN_ID_FN               = "mart_id"
  val CAT_SCR_OPENED_FN       = "cat_screen.is_opened"
  val GEO_SCR_OPENED_FN       = "geo_screen.is_opened"
  val FADS_SCREEN_OPENED_FN   = "fads.is_opened"
  val GENERATION_FN           = "generation"

  def qsbStandalone: QueryStringBindable[ScJsState] = {
    import QueryStringBindable._
    qsb
  }

  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],
                   boolOptB: QueryStringBindable[Option[Boolean]],
                   longOptB: QueryStringBindable[Option[Long]] ) = {
    new QueryStringBindable[ScJsState] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScJsState]] = {
        for {
          maybeAdnId            <- strOptB.bind(ADN_ID_FN, params)
          maybeCatScreenOpened  <- boolOptB.bind(CAT_SCR_OPENED_FN, params)
          maybeGeoScreenOpened  <- boolOptB.bind(GEO_SCR_OPENED_FN, params)
          maybeFadsOpened       <- boolOptB.bind(FADS_SCREEN_OPENED_FN, params)
          maybeGeneration       <- longOptB.bind(GENERATION_FN, params)
        } yield {
          val res = new ScJsState {
            override def adnId = maybeAdnId
            override def searchScrOpenedOpt = maybeCatScreenOpened
            override def navScrOpenedOpt = maybeGeoScreenOpened
            override def fadsOpenedOpt = maybeFadsOpened
            override def generationOpt = maybeGeneration
          }
          Right(res)
        }
      }

      override def unbind(key: String, value: ScJsState): String = {
        List(
          strOptB.unbind(ADN_ID_FN, value.adnId),
          boolOptB.unbind(CAT_SCR_OPENED_FN, value.searchScrOpenedOpt),
          boolOptB.unbind(GEO_SCR_OPENED_FN, value.navScrOpenedOpt),
          boolOptB.unbind(FADS_SCREEN_OPENED_FN, value.fadsOpenedOpt),
          longOptB.unbind(GENERATION_FN, value.generationOpt)
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}


/** Класс, отражающий состояние js-выдачи на клиенте. */
trait ScJsState {

  implicit protected def orFalse(boolOpt: Option[Boolean]): Boolean = {
    if (boolOpt.isDefined)
      boolOpt.get
    else
      false
  }

  def adnId               : Option[String]   = None
  def searchScrOpenedOpt  : Option[Boolean]  = None
  def navScrOpenedOpt     : Option[Boolean]  = None
  def fadsOpenedOpt       : Option[Boolean]  = None
  def generationOpt       : Option[Long]     = None


  def isSearchScrOpened : Boolean = searchScrOpenedOpt
  def isNavScrOpened    : Boolean = navScrOpenedOpt
  def isFadsOpened      : Boolean = fadsOpenedOpt

  def generation: Long = generationOpt.getOrElse(System.currentTimeMillis)

  // TODO Нужно считывания geo-состояния из qs.
  def geo: GeoMode = GeoNone

}

