package io.suggest.sc.sjs.vm.search.tabs.htag

import io.suggest.sc.ScConstants.Search.Nodes._
import io.suggest.sc.sjs.c.scfsm.ScFsm
import io.suggest.sc.sjs.m.msearch.TagRowClick
import io.suggest.sc.sjs.vm.util.OnClick
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.content.ClearT
import io.suggest.sjs.common.vm.Vm
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.{Event, Node}
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.09.15 14:14
 * Description: VM'ка непосредственного div'а списка тегов.
 * Подтеги этого тега уже являются логическими элементами списка.
 */
object StList extends FindDiv {
  override type T     = StList
  override def DOM_ID = LIST_ID
}


/** Логика работы списка живёт тут. */
trait StListT extends OnClick with IInitLayout with ClearT {

  override type T = HTMLDivElement

  def appendElements(elemsHtml: String): Unit = {
    val div = VUtil.newDiv(elemsHtml)
    _underlying.appendChild(div)
  }

  /** Подписать ScFsm на возможные клики в списке. */
  override def initLayout(): Unit = {
    onClick { e: Event =>
      val tgVm = Vm( e.target.asInstanceOf[Node] )
      for (rowDivVm <- VUtil.hasCssClass(tgVm, ROW_DIV_CLASS)) {
        val div = rowDivVm._underlying.asInstanceOf[ StListRow.Dom_t ]
        ScFsm !! TagRowClick( StListRow(div) )
      }
    }
  }

}


/** Дефолтовая реализация VM'ки списка тегов/узлов. */
case class StList(
  override val _underlying: HTMLDivElement
)
  extends StListT
