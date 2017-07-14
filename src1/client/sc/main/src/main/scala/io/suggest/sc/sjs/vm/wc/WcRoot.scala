package io.suggest.sc.sjs.vm.wc

import io.suggest.dev.MScreen
import io.suggest.sc.sjs.m.mwc.WcClick
import io.suggest.sc.sjs.vm.util.InitOnClickToScFsmT
import io.suggest.sc.ScConstants.Welcome.ROOT_ID
import io.suggest.sc.ScConstants.Welcome.Anim._
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.will.WillAnimateT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.08.15 11:54
 * Description: Модель корневого div'а карточки приветствия.
 */
object WcRoot extends FindDiv {
  override def DOM_ID = ROOT_ID
  override type T = WcRoot
}


/** Логика div'а карточки приветствия тут. */
trait WcRootT extends VmT with WillAnimateT with InitOnClickToScFsmT {

  override type T = HTMLDivElement

  override protected def WILL_ANIMATE_CLASS = WILL_FADEOUT_CSS_CLASS

  /** Фоновая картинка карточки приветствия. */
  def bgImg = WcBgImg.find()

  /** Картинка переднего плана карточки приветствия. */
  def fgImg = WcFgImg.find()

  /** div надписи карточки приветствия. */
  def fgInfo = WcFgInfo.find()

  /** Анимиронно скрыть приветствие. */
  def fadeOut(): Unit = {
    addClasses(TRANS_02_CSS_CLASS, FADEOUT_CSS_CLASS)
  }

  /** Инициализация отображения. */
  def initLayout(screen: MScreen): Unit = {
    // Картинка фона.
    for (_bgImg <- bgImg) {
      _bgImg.adjust(screen)
    }
    // Передний план.
    for (_fgImg <- fgImg) {
      val newSz = _fgImg.adjust()
      for (_fgInfo <- fgInfo) {
        _fgInfo.adjust(newSz)
      }
    }
    // Повесить click listener на всю карточку.
    initLayout()
  }

  // TODO Надо бы ограничить возможность вызова initLayout() напрямую (без screen-аргумента).

  override protected[this] def _clickMsgModel = WcClick

}


/** Дефолтовая реализация модели. */
case class WcRoot(
  override val _underlying: HTMLDivElement
)
  extends WcRootT
{
  override lazy val bgImg   = super.bgImg
  override lazy val fgImg   = super.fgImg
  override lazy val fgInfo  = super.fgInfo
}
