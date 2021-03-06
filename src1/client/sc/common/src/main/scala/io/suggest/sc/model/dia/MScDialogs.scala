package io.suggest.sc.model.dia

import io.suggest.ueq.UnivEqUtil._
import diode.FastEq
import io.suggest.sc.model.dia.err.MScErrorDia
import io.suggest.sc.model.dia.first.MWzFirstOuterS
import io.suggest.sc.model.dia.settings.MScSettingsDia
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.01.19 12:14
  * Description: Контейнер данных диалогов.
  * Неявно-пустая модель.
  */
object MScDialogs {

  def empty = apply()

  implicit object MScDialogsFastEq extends FastEq[MScDialogs] {
    override def eqv(a: MScDialogs, b: MScDialogs): Boolean = {
      (a.first ===* b.first) &&
      (a.error ===* b.error) &&
      (a.settings ===* b.settings) &&
      (a.login ===* b.login) &&
      (a.nodes ===* b.nodes)
    }
  }

  @inline implicit def univEq: UnivEq[MScDialogs] = UnivEq.derive

  def first = GenLens[MScDialogs](_.first)
  def error = GenLens[MScDialogs](_.error)
  def settings = GenLens[MScDialogs](_.settings)
  def login = GenLens[MScDialogs](_.login)
  val nodes = GenLens[MScDialogs](_.nodes)

}


/** Модель-контейнер верхнего уровня для состояний диалогов.
  *
  * @param first Диалог первого запуска, когда открыт.
  * @param error Состояние диалога возникшей ошибки.
  * @param settings Диалог настроек.
  * @param login Состояние логин-формы.
  * @param nodes Состояние формы управления узлами.
  */
case class MScDialogs(
                       first      : MWzFirstOuterS          = MWzFirstOuterS.empty,
                       error      : Option[MScErrorDia]     = None,
                       settings   : MScSettingsDia          = MScSettingsDia.empty,
                       login      : MScLoginS               = MScLoginS.empty,
                       nodes      : MScNodes                = MScNodes.empty,
                     )
