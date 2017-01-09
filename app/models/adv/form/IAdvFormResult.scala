package models.adv.form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.16 11:35
  * Description: Все формы adv после биндинга выдают результат биндинга, реализующий этот интерфейс.
  */
trait IAdvFormResult {

  /** Период размещения. */
  def period    : MDatesPeriod

}
