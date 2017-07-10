package io.suggest.sc

import ScScalaCssDefaults._
import io.suggest.sc.root.v.ScRootR
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import japgolly.scalajs.react.vdom.Implicits._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 11:42
  * Description: Запускалка выдачи третьего поколения.
  */
object Sc3Main {

  import io.suggest.sc.root.m.MScRoot.MScRootFastEq

  /** Здесь начинается исполнение кода выдачи. */
  def main(args: Array[String]): Unit = {

    // Добавить статичные стили в css-документа. Динамические стили будут рендерится прямо через <.style() теги.
    ScCss.addToDocument()

    // Самый корневой рендер -- отрабатывается первым.
    val rootDiv = VUtil.newDiv()
    DocumentVm().body.appendChild( rootDiv )
    Sc3Circuit
      .wrap(m => m)( ScRootR.apply )
      .renderIntoDOM(rootDiv)

    // TODO Запустить разные FSM: геолокация, platform, BLE. Переписав их в circuit'ы предварительно.
  }

}
