package io.suggest.grid.react

import com.github.dantrain.react.stonecutter.ItemProps
import io.suggest.ad.blk.BlockMeta

import scala.scalajs.js
import scala.language.implicitConversions
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 18:38
  * Description: Модель-контейнер расширенных аттрибутов одного item'а.
  */
trait MItemPropsExtData extends js.Object {

  val blockMeta: BlockMeta

}


@js.native
trait ItemPropsExt extends js.Object {

  @JSName(itemAttrsExtAttrName)
  val ext: MItemPropsExtData = js.native

}

object ItemPropsExt {
  implicit def fromItemProps(itemProps: ItemProps): ItemPropsExt = {
    itemProps.asInstanceOf[ItemPropsExt]
  }
}
