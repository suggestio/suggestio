package io.suggest.sc.sjs.vm.mapbox

import io.suggest.sc.map.ScMapConstants
import io.suggest.sjs.common.vm.attr.StringInputValueT
import io.suggest.sjs.common.vm.find.FindElT
import org.scalajs.dom.raw.HTMLInputElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.16 22:36
  * Description: VM'ка инпута с ссылкой на GeoJSON-карту всех узлов.
  */
object AllNodesUrl extends FindElT {
  override def DOM_ID: String = ScMapConstants.Nodes.ALL_URL_INPUT_ID
  override type Dom_t = HTMLInputElement
  override type T = AllNodesUrl
}


import AllNodesUrl.Dom_t


trait AllNodesUrlT extends StringInputValueT {
  override type T = Dom_t
}


case class AllNodesUrl(override val _underlying: Dom_t)
  extends AllNodesUrlT
