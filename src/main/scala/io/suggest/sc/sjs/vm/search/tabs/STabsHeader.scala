package io.suggest.sc.sjs.vm.search.tabs

import io.suggest.sc.sjs.vm.search.tabs.geo.SGeoTabBtn
import io.suggest.sc.sjs.vm.search.tabs.htag.ShtTabBtn
import io.suggest.sc.ScConstants.Search.TAB_BTNS_DIV_ID
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 13:44
 * Description: VM контейнера кнопок вкладок.
 */
object STabsHeader extends FindDiv {

  override type T = STabsHeader

  override def DOM_ID: String = TAB_BTNS_DIV_ID

}


/** Логика экземпляра vm'ки живёт в этом трейте. */
trait STabHeaderT extends VmT with IInitLayout {

  override type T = HTMLDivElement

  /** Кнопка таба с геопоиском. */
  def geoBtn = SGeoTabBtn.find()

  /** Кнопка таба с хеш-тегами. */
  def htagsBtn = ShtTabBtn.find()

  /** Все кнопки табов. */
  def btns: Seq[TabBtn] = {
    // Тут какой-то оптимальный велосипед получился. Может запилить вариант по-красивше?
    val lb = List.newBuilder[TabBtn]
    for (gb <- geoBtn)
      lb += gb
    for (hb <- htagsBtn)
      lb += hb
    lb.result()
  }

  /** Инициализировать панель с кнопками поисковых табов. */
  override def initLayout(): Unit = {
    // Вызвать инициализацию в каждой кнопке:
    btns.foreach( IInitLayout.f )
  }

}


/** Дефолтовая реализация vm'ки заголовка. */
case class STabsHeader(
  override val _underlying: HTMLDivElement
)
  extends STabHeaderT
