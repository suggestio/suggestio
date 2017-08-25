package io.suggest.ad.edit.m

import io.suggest.jd.render.m.{MJdArgs, MJdRenderArgs}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:33
  * Description: Модель состояния документа в редакторе.
  */
case class MDocS(
                  jdArgs    : MJdArgs
                ) {

  def withJdArgs(jdArgs: MJdArgs) = copy(jdArgs = jdArgs)

  /** Выдать экземпляр данных для рендера json-документа, т.е. контента рекламной карточки. */
  def jdCommonRa: MJdRenderArgs = ???

}
