package io.suggest.lk.adv.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.12.16 21:51
  * Description: В React-форме бесплатного размещения пропертисы Adv4Free-компонента рендерятся на сервере,
  * чтобы скрыть оригинальное имя поля и месседж от любопытных глаз.
  * Тут модель приходящих с сервера данных, разрешающих бесплатное размещение, когда у юзера прав достаточно.
  */
trait IAdv4FreeProps {

  /** Имя form-поля: "freeAdv" */
  def fn: String

  /** Текст галочки: "Размещать бесплатно, без подтверждения?" */
  def title: String

}
