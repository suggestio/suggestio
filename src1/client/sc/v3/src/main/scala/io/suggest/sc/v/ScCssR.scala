package io.suggest.sc.v

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.07.17 16:09
  */


// TODO Код ниже - актуален ли вообще? Удалить это, наверное.
import com.softwaremill.macwire._
import io.suggest.sc.styl.{IScCssArgs, ScCss}

/** Factory-модуль для сборки инстансов ScCss, который зависит от аргументов рендера,
  * но допускает использование как-то внешних зависимостей.
  */
class ScCssFactory {

  /** Параметризованная сборка ScCss (здесь можно добавлять DI-зависимости). */
  def mkScCss(args: IScCssArgs): ScCss = {
    wire[ScCss]
  }

}
