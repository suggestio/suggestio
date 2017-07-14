package io.suggest.sc.sjs.vm.layout

import io.suggest.dev.MScreen
import io.suggest.sc.ScConstants.Layout
import io.suggest.sc.sjs.vm.foc.FRoot
import io.suggest.sc.sjs.vm.grid.GRoot
import io.suggest.sc.sjs.vm.hdr.HRoot
import io.suggest.sc.sjs.vm.nav.NRoot
import io.suggest.sc.sjs.vm.search.SRoot
import io.suggest.sjs.common.vm.content.SetInnerHtml
import io.suggest.sjs.common.vm.create.{CreateDivWithId, CreateVm}
import io.suggest.sc.sjs.vm.wc.WcRoot
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.Node
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 9:58
 * Description: Модель доступа к div sioMartLayout.
 */
object LayContentVm extends FindDiv with CreateDivWithId with CreateVm {

  override type T = LayContentVm
  override def DOM_ID = Layout.LAYOUT_ID

}


/** Абстрактная реализация модели. */
trait LayContentVmT extends VmT with SetInnerHtml {

  override type T = HTMLDivElement

  /** Выставить css-класс отображения для layoutDiv. */
  def setWndClass(scrWh: MScreen): Unit = {
    setWndClass(scrWh.width)
  }
  def setWndClass(w: Int): Unit = {
    // Тут какой-то древний код выставления класса окна в зависимости от ширины оного.
    val cssClassOrNull: String = {
      val prefix = "sm-w-"
      if (w <= 660) {
        prefix + "400"
      } else {
        val w800 = 800
        if (w <= w800) {
          prefix + w800
        } else {
          val w980 = 980
          if (w <= w980) {
            prefix + w980
          } else {
            null
          }
        }
      }
    }
    if (cssClassOrNull != null) {
      _underlying.className = cssClassOrNull
    }
  }

  /** Доступ к корневому div'у плитки, который является дочерним DOM-узлом этой модели. */
  def grid = GRoot.find()
  /** Доступ к корню заголовка выдачи. */
  def header = HRoot.find()
  /** Старая панель навигации. */
  def navPanel = NRoot.find()
  /** Панель поиска. */
  def searchPanel = SRoot.find()
  /** Focused root div. */
  def focused = FRoot.find()
  /** Доступ к welcome div'у. val'ить его точно нельзя, т.к. слишком короткоживущий. */
  final def welcome = WcRoot.find()
  /** Доступ к fullscreen loader'у. */
  def fsLoader = FsLoader.find()

  def insertFirst(node: Node): Unit = {
    _underlying.insertBefore(node, _underlying.firstChild)
  }

  def insertAfterFirst(node: Node): Unit = {
    val secondChild = Option(_underlying.firstChild)
      .flatMap(t => Option(t.nextSibling))
      .orNull
    _underlying.insertBefore(node, secondChild)
  }

}


case class LayContentVm(
  override val _underlying: HTMLDivElement
)
  extends LayContentVmT

