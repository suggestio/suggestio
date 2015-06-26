package models.msc

import models._
import models.im.MImg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:17
 * Description: Модель параметров рендера шаблона sc/indexTpl.
 */

object ScRenderArgs {

  type ProdsLetterGrouped_t = Seq[(Char, Seq[MAdnNode])]

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

  /**
   * Группировка узлов по первой букве.
   * @param prods Список узлов.
   * @return Список сгруппированных узлов.
   */
  def groupNodesByNameLetter(prods: Seq[MAdnNode]): ProdsLetterGrouped_t = {
    prods
      // Сгруппировать по первой букве или цифре.
      .groupBy { node =>
        val firstChar = firstWordChar(node.meta.nameShort)
        java.lang.Character.toUpperCase(firstChar)
      }
      // Отсортировать ноды по названиям в рамках группы.
      .mapValues { nodes =>
        nodes.sortBy(_.meta.nameShort.toLowerCase)
      }
      .toSeq
      // Отсортировать по первой букве группы, но русские -- сверху.
      .sortBy { case (c, _) => russianFirst(c) }
  }

}


import models.msc.ScRenderArgs._


/** Аргументы для рендера market/showcase/indexTpl. */
trait ScRenderArgs extends ScReqArgs with IColors with ILogoRenderArgs with IHBtnRenderArgs {
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
  def logoImgOpt    : Option[MImg] = None
  /** Приветствие, если есть. */
  def welcomeOpt    : Option[WelcomeRenderArgsT] = None

  /** Дефолтовые параметры для рендера кнопок на панели. Тут нужен case-класс. */
  def hBtnArgs: HBtnArgs = HBtnArgs(fgColor = fgColor)

  /** Какую кнопку навигации надо рендерить для в левом верхнем углу indexTpl? */
  def topLeftBtn: ScHdrBtn

  /** Назначение выдачи. */
  def target: MScTarget = MScTargets.Primary

  /** Рендерить ли утиль, связанную с "закрытием" выдачи?
    * После удаления API v1, можно заинлайнить в шаблон, выкинув обращение к apiVsn.force..() */
  def withScClose: Boolean = {
    !syncRender && (target.isCloseable || apiVsn.forceScCloseable)
  }

  /** Списка групп рекламодателей с группировкой по буквам. */
  def shopsLetterGrouped: ProdsLetterGrouped_t

  override def toString: String = {
    val sb = new StringBuilder(256, "req:")
    sb.append( super.toString )
      .append( ";render:" )
      // TODO Вынести унаследованные поля в соотв.модели.
      .append("bgColor=").append(bgColor).append('&')
      .append("fgColor=").append(fgColor).append('&')
      .append("name=").append(title).append('&')
      .append("mmcats=[").append(mmcats.size).append(']').append('&')
      .append("catsStats=[").append(catsStats.size).append(']').append('&')
      .append("spsr=").append(spsr.toString).append('&')
      .append("onCloseHref='").append(onCloseHref).append('\'').append('&')
      .append("geoListGoBack").append(geoListGoBack.toString).append('&')
      .append("prodGroups=[").append(shopsLetterGrouped.size).append(']').append('&')
      .append("syncRender=").append(syncRender).append('&')
    val _lio = logoImgOpt
    if (_lio.isDefined)
      sb.append("logoImg=").append(_lio.get.toString).append('&')
    val _waOpt = welcomeOpt
    if (_waOpt.isDefined)
      sb.append("welcome=").append(_waOpt.get.toString).append('&')
    sb.toString()
  }
}

