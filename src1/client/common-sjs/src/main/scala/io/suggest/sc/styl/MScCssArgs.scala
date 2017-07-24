package io.suggest.sc.styl

import diode.UseValueEq
import io.suggest.dev.MScreen
import io.suggest.model.n2.node.meta.colors.MColors

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 10:56
  * Description: Реализация common-модели IScCssArgs, которая описывает аргументы для
  * рендера css-шаблона с динамическими стилями выдачи.
  */

case class MScCssArgs(
                       customColorsOpt   : Option[MColors] = None,
                       screen            : MScreen
                     )
  extends IScCssArgs
  // Надо сравнивать строго по значению, т.к. пересборка css-шаблона -- это очень дорогая операция.
  with UseValueEq
