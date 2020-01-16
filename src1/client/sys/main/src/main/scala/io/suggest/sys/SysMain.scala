package io.suggest.sys

import io.suggest.lk.flash.FlashInitRouter
import io.suggest.n2.edge.edit.EdgeEditInit
import io.suggest.sys.mdr.SysMdrInit

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.01.2020 12:54
  * Description: main() для sys-js-компонентов.
  */
object SysMain {

  def main(args: Array[String]): Unit = {
    (new SysInit)
      .init()
  }

}


class SysInit
  extends FlashInitRouter
  with EdgeEditInit
  with SysMdrInit
