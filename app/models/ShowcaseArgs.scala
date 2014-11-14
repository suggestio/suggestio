package models

import io.suggest.ym.model.common.LogoImgOptI
import models.im.DevScreen
import play.api.mvc.{Call, QueryStringBindable}
import play.twirl.api.{Html, HtmlFormat}
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

  /** routes-Биндер для параметров showcase'а. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],
                   devScreenB: QueryStringBindable[Option[DevScreen]],
                   boolOptB: QueryStringBindable[Option[Boolean]] ) = {
    new QueryStringBindable[ScReqArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScReqArgs]] = {
        for {
          maybeGeo                <- strOptB.bind(key + GEO_SUF, params)
          maybeDevScreen          <- devScreenB.bind(key + SCREEN_SUF, params)
        } yield {
          Right(new ScReqArgsDflt {
            override val geo = {
              GeoMode.maybeApply(maybeGeo)
                .filter(_.isWithGeo)
                .getOrElse(GeoIp)
            }
            // Игнорим неверные размеры, ибо некритично.
            override lazy val screen: Option[DevScreen] = maybeDevScreen
          })
        }
      }

      override def unbind(key: String, value: ScReqArgs): String = {
        List(
          strOptB.unbind(key + GEO_SUF, value.geo.toQsStringOpt),
          devScreenB.unbind(key + SCREEN_SUF, value.screen)
        )
          .filter { us => !us.isEmpty }
          .mkString("&")
      }
    }
  }

  def empty: ScReqArgs = new ScReqArgsDflt {}

}


trait SyncRenderInfo {
  def jsStateOpt: Option[ScJsState]
  def syncRender: Boolean = jsStateOpt.isDefined
}
trait SyncRenderInfoDflt extends SyncRenderInfo {
  override def jsStateOpt: Option[ScJsState] = None
}


trait ScReqArgs extends SyncRenderInfo {
  def geo                 : GeoMode
  def screen              : Option[DevScreen]
  /** Заинлайненные отрендеренные элементы плитки. Передаются при внутренних рендерах, вне HTTP-запросов и прочего. */
  def inlineTiles         : Seq[Html]
  def focusedContent      : Option[Html]
  def inlineNodesList     : Option[Html]
  /** Текущая нода согласно геоопределению, если есть. */
  def adnNodeCurrentGeo   : Option[MAdnNode]

  override def toString: String = {
    import QueryStringBindable._
    ScReqArgs.qsb.unbind("a", this)
  }
}
trait ScReqArgsDflt extends ScReqArgs with SyncRenderInfoDflt {
  override def geo                  : GeoMode = GeoNone
  override def screen               : Option[DevScreen] = None
  override def inlineTiles          : Seq[Html] = Nil
  override def focusedContent       : Option[Html] = None
  override def inlineNodesList      : Option[Html] = None
  override def adnNodeCurrentGeo    : Option[MAdnNode] = None
}
/** Враппер [[ScReqArgs]] для имитации вызова copy(). */
trait ScReqArgsWrapper extends ScReqArgs {
  def reqArgsUnderlying: ScReqArgs
  override def geo                  = reqArgsUnderlying.geo
  override def screen               = reqArgsUnderlying.screen
  override def inlineTiles          = reqArgsUnderlying.inlineTiles
  override def focusedContent       = reqArgsUnderlying.focusedContent
  override def inlineNodesList      = reqArgsUnderlying.inlineNodesList
  override def adnNodeCurrentGeo    = reqArgsUnderlying.adnNodeCurrentGeo

  override def jsStateOpt           = reqArgsUnderlying.jsStateOpt
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
      .append("syncRender=").append(syncRender).append('&')
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
trait ScSiteArgs extends SyncRenderInfoDflt {
  /** Цвет оформления. */
  def bgColor       : String
  /** Адрес для showcase */
  def showcaseCall  : Call
  /** id узла, в рамках которого орудуем. */
  def adnId         : Option[String]
  /** Отображаемый заголовок. */
  def title         : Option[String] = None
  /** Инлайновый рендер индексной страницы выдачи. В параметре содержится отрендеренный HTML. */
  def inlineIndex   : Option[HtmlFormat.Appendable] = None

  // Имитируем поведение параметра, чтобы в будущем не рисовать костыли в коде шаблонов.
  def withGeo = adnId.isEmpty

  override def toString: String = {
    val sb = new StringBuilder(64)
    sb.append("bgColor=").append(bgColor).append('&')
      .append("showcaseCall=").append(showcaseCall).append('&')
      .append("title=").append(title).append('&')
      .append("syncRender=").append(syncRender).append('&')
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

  override def bgColor      = _scSiteArgs.bgColor
  override def adnId        = _scSiteArgs.adnId
  override def showcaseCall = _scSiteArgs.showcaseCall
  override def title        = _scSiteArgs.title
  override def inlineIndex  = _scSiteArgs.inlineIndex

  override def withGeo      = _scSiteArgs.withGeo
  override def toString     = _scSiteArgs.toString
  override def syncRender   = _scSiteArgs.syncRender
}



object ScJsState {
  
  val ADN_ID_FN               = "mart_id"
  val CAT_SCR_OPENED_FN       = "cat_screen.is_opened"
  val GEO_SCR_OPENED_FN       = "geo_screen.is_opened"
  val FADS_CURRENT_AD_ID_FN   = "fads.current_ad_id"
  val FADS_OFFSET_FN          = "fads.offset"
  val GENERATION_FN           = "generation"

  def qsbStandalone: QueryStringBindable[ScJsState] = {
    import QueryStringBindable._
    qsb
  }

  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]],
                   boolOptB: QueryStringBindable[Option[Boolean]],
                   longOptB: QueryStringBindable[Option[Long]],
                   intOptB: QueryStringBindable[Option[Int]] ) = {
    new QueryStringBindable[ScJsState] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScJsState]] = {
        for {
          maybeAdnId            <- strOptB.bind(ADN_ID_FN, params)
          maybeCatScreenOpened  <- boolOptB.bind(CAT_SCR_OPENED_FN, params)
          maybeGeoScreenOpened  <- boolOptB.bind(GEO_SCR_OPENED_FN, params)
          maybeGeneration       <- longOptB.bind(GENERATION_FN, params)
          maybeFadsOpened       <- strOptB.bind(FADS_CURRENT_AD_ID_FN, params)
          maybeFadsOffset       <- intOptB.bind(FADS_OFFSET_FN, params)
        } yield {
          val res = ScJsState(
            adnId               = maybeAdnId,
            searchScrOpenedOpt  = maybeCatScreenOpened,
            navScrOpenedOpt     = maybeGeoScreenOpened,
            generationOpt       = maybeGeneration,
            fadsOpenedOpt       = maybeFadsOpened,
            fadsOffsetOpt       = maybeFadsOffset
          )
          Right(res)
        }
      }

      override def unbind(key: String, value: ScJsState): String = {
        List(
          strOptB.unbind(ADN_ID_FN, value.adnId),
          boolOptB.unbind(CAT_SCR_OPENED_FN, value.searchScrOpenedOpt),
          boolOptB.unbind(GEO_SCR_OPENED_FN, value.navScrOpenedOpt),
          strOptB.unbind(FADS_CURRENT_AD_ID_FN, value.fadsOpenedOpt),
          longOptB.unbind(GENERATION_FN, value.generationOpt),
          intOptB.unbind(FADS_OFFSET_FN, value.fadsOffsetOpt)
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}


/** Класс, отражающий состояние js-выдачи на клиенте. */
case class ScJsState(
  adnId               : Option[String]   = None,
  searchScrOpenedOpt  : Option[Boolean]  = None,
  navScrOpenedOpt     : Option[Boolean]  = None,
  generationOpt       : Option[Long]     = None,
  fadsOpenedOpt       : Option[String]   = None,
  fadsOffsetOpt       : Option[Int]      = None
) {

  implicit protected def orFalse(boolOpt: Option[Boolean]): Boolean = {
    boolOpt.isDefined && boolOpt.get
  }

  implicit protected def orZero(intOpt: Option[Int]): Int = {
    if (intOpt.isDefined)  intOpt.get  else  0
  }

  protected def bool2boolOpt(bool: Boolean): Option[Boolean] = {
    if (bool) Some(bool) else None
  }


  def isSearchScrOpened : Boolean = searchScrOpenedOpt
  def isNavScrOpened    : Boolean = navScrOpenedOpt
  def isAnyPanelOpened  : Boolean = isSearchScrOpened || isNavScrOpened
  def isFadsOpened      : Boolean = fadsOpenedOpt.isDefined
  def isSomethingOpened : Boolean = isAnyPanelOpened || isFadsOpened

  def fadsOffset        : Int = fadsOffsetOpt

  def generation: Long = generationOpt.getOrElse(System.currentTimeMillis)

  // Пока что считывание geo-состояния из qs не нужно, т.к. HTML5 Geolocation доступно только в js-выдаче.
  def geo: GeoMode = GeoIp

  /**
   * Переключить состояние поля navScrOpenedOpt, сгенерив новое состояние.
   * @return Копия текущего состояния с новым значением поля navScrOpenedOpt.
   */
  def toggleNavScreen = copy( navScrOpenedOpt = bool2boolOpt(!isNavScrOpened) )

  def toggleSearchScreen = copy( searchScrOpenedOpt = bool2boolOpt(!isSearchScrOpened) )

}

