package models.msc

import models.mwc.MWelcomeRenderArgs
import play.twirl.api.Html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.04.15 15:17
  * Description: Модель параметров рендера шаблона sc/indexTpl.
  *
  * 2016.sep.14: Была попытка распилить эту сложную модель, но какая-то неудачная.
  * Только исходные qs-поля были вынесены в qs-контейнер типа MScIndexArgs в ходе распиливания той модели.
  */

abstract class ScRenderArgs
  extends IColors
  with ILogoRenderArgs
  with IHBtnRenderArgs
  with IHBtnArgsFieldImpl
{

  /** Приветствие, если есть. */
  def welcomeOpt    : Option[MWelcomeRenderArgs]

  /** Дефолтовые параметры для рендера кнопок на панели. Тут нужен case-класс. */
  def hBtnArgs: HBtnArgs

  /** Какую кнопку навигации надо рендерить для в левом верхнем углу indexTpl? */
  def topLeftBtnHtml: Html

  override def toString: String = {
    val sb = new StringBuilder(256, "req:")
    sb.append( super.toString )
      .append( ";render:" )
      // TODO Вынести унаследованные поля в соотв.модели.
      .append("bgColor=").append(bgColor).append('&')
      .append("fgColor=").append(fgColor).append('&')
      .append("name=").append(title).append('&')
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

