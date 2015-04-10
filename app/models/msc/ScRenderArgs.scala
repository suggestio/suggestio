package models.msc

import io.suggest.ym.model.common.LogoImgOptI
import models._
import util.cdn.CdnUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:17
 * Description: Модель параметров рендера шаблона sc/indexTpl.
 */


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


import models.msc.ScRenderArgs._


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
    Context.SC_URL_PREFIX + path
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

