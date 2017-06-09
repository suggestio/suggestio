package io.suggest.sjs.common.vm.spa

import io.suggest.common.spa.SpaConst
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.rm.SelfRemoveT
import io.suggest.sjs.common.vm.style.ShowHideDisplayT
import org.scalajs.dom.raw.HTMLImageElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.12.16 18:24
  * Description: VM'ка для работы с прелодером.
  */
object LkPreLoader extends FindElT {

  override type Dom_t = HTMLImageElement
  override def DOM_ID = SpaConst.LkPreLoaderConst.DOM_ID
  override type T     = LkPreLoader


  /** Вернуть ссылку на картинку-прелоадер, если таковая имеется.
    * Этот контейнер ссылки используется для исчезающего и появляющегося прелоадера, изначально отрендеренного
    * на сервере. */
  lazy val PRELOADER_IMG_URL = find().flatMap(_.url)

}


import LkPreLoader.Dom_t


case class LkPreLoader(override val _underlying: Dom_t) extends VmT with ShowHideDisplayT with SelfRemoveT {

  override type T = Dom_t

  def url = getAttribute("src")

}
