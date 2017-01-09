package io.suggest.dt

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 18:44
  * Description:
  */
object DtUtil {

  /**
    * Конвертация номеров дней недели с западных на наши:
    * У них 0 -- воскресенье, 6 - суббота.
    * А надо от 1 до 7 (пн-вс).
    *
    * @see [[https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/getDay]]
    * @return 1 для понедельника, 7 для воскресенья.
    */
  def jsDow2sioDow(dow: Int): Int = {
    if (dow < 1) 7 else Math.min(dow, 7)
  }

}
