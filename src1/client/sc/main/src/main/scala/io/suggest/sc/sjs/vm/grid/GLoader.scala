package io.suggest.sc.sjs.vm.grid

import io.suggest.sc.ScConstants.HIDDEN_CSS_CLASS
import io.suggest.sc.ScConstants.Grid
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 16:33
 * Description: ViewModel для доступа к laoder'у плитки выдачи.
 * Предшественником модели был grid.LoaderView и MGridDom.loader.
 */
object GLoader extends FindDiv {

  override def DOM_ID = Grid.LOADER_DIV_ID
  override type T = GLoader

}


/** Логика работы safe-элемента вынесена сюда. */
trait GLoaderT extends VmT {

  override type T = HTMLDivElement

  /** Отобразить лоадер. */
  def show(): Unit = {
    removeClass( HIDDEN_CSS_CLASS )
  }

  /** Скрыть лоадер. Не удаляем, т.к. при поиске лоадер придется вернуть. */
  def hide(): Unit = {
    // TODO Скорее всего, скрытая таким образом анимация жрёт ресурсы устройства. Нужно это пофиксить, желательно на уровне CSS.
    addClasses( HIDDEN_CSS_CLASS )
  }

  /** Выставить ширину, заданную css-строкой, вместе с единицами измерения.
    * Используется из межмодельного внутреннего API для согласования ширины. */
  protected[grid] def _setWidthPx(widthCss: String): Unit = {
    _underlying.style.width = widthCss
  }

}


/** Экземпляр модели. Дефолтовая реализация [[GLoaderT]]. */
case class GLoader(override val _underlying: HTMLDivElement) extends GLoaderT
