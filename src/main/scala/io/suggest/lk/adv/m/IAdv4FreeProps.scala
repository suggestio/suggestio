package io.suggest.lk.adv.m

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 21:51
  * Description: В React-форме бесплатного размещения пропертисы Adv4Free-компонента рендерятся на сервере,
  * чтобы скрыть оригинальное имя поля и месседж от любопытных глаз.
  * Тут модель приходящих с сервера данных, разрешающих бесплатное размещение, когда у юзера прав достаточно.
  */
@js.native
trait IAdv4FreeProps extends js.Object {

  /** Имя form-поля: "freeAdv" */
  val fn: String = js.native

  /** Текст галочки: "Размещать бесплатно, без подтверждения?" */
  val title: String = js.native

}
